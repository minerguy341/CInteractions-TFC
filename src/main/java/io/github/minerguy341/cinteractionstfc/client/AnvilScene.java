package io.github.minerguy341.cinteractionstfc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dyslexic.cinematic.api.client.AddonMenuStation;
import dev.dyslexic.cinematic.client.CameraRig;
import dev.dyslexic.cinematic.client.ItemPlacer;
import dev.dyslexic.cinematic.client.Picking;
import io.github.minerguy341.cinteractionstfc.AnvilConfig;
import java.util.List;
import net.dries007.tfc.common.blockentities.AnvilBlockEntity;
import net.dries007.tfc.common.component.forge.Forging;
import net.dries007.tfc.common.component.forge.ForgeRule;
import net.dries007.tfc.common.component.forge.ForgeStep;
import net.dries007.tfc.common.component.heat.Heat;
import net.dries007.tfc.common.component.heat.HeatCapability;
import net.dries007.tfc.common.container.AnvilContainer;
import net.dries007.tfc.network.ScreenButtonPacket;
import net.dries007.tfc.util.Helpers;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * First-person TFC anvil. The server menu keeps authority over the work: every blow is a TFC {@link ScreenButtonPacket}
 * with the same button id the vanilla TFC screen sends. Where the cursor hovers on the workpiece picks the action,
 * and the mouse wheel picks the strength of a Hit.
 */
public final class AnvilScene extends AddonMenuStation<AnvilContainer> {
    private static final double HALF_LENGTH = 0.30;
    private static final double HALF_WIDTH = 0.10;
    private static final double SWING_SECONDS = 0.28;
    private static final float HAMMER_REST_TILT = 25f;
    /** Which way each sprite's working end points in its own plane: the hammer head is top-right, the tong jaws top-left. */
    private static final Vector3f HAMMER_HEAD = new Vector3f(1, 1, 0);
    private static final Vector3f TONG_JAWS = new Vector3f(-1, 1, 0);
    /** How quickly the drawn hands' tools follow (matches the hands' own spring, see handsStiffness). */
    private static final double HAND_RATE = 17;
    /** Where the hammer lies on the anvil (u along the anvil, v towards the player) and the pick-up timing in seconds. */
    private static final double HOME_U = 0.17, HOME_V = -0.03;
    private static final double REACH_END = 0.40, GRAB_END = 0.58, PICKUP_SECONDS = 1.0;
    private static final ForgeStep[] HITS = {ForgeStep.HIT_LIGHT, ForgeStep.HIT_MEDIUM, ForgeStep.HIT_HARD};

    private static final int SLOT_MAIN = 0, SLOT_SECOND = 1, SLOT_HAMMER = 2, SLOT_CATALYST = 3;

    /** Where on the workpiece the cursor is. */
    private enum Zone { HIT, DRAW, BEND, PUNCH, UPSET, SHRINK }

    private Zone zone;
    private int strength = 1;
    private Vec3 rightEst, leftEst;
    private double estAt;
    private double strikeAt = -10;
    private double pickupAt = -1;
    private boolean hadHammer;

    public AnvilScene(BlockPos pos, Direction side) {
        super(pos, side, AnvilContainer.class, 2);
    }

    // ---- geometry ---------------------------------------------------------------------------------------

    /** The centre of the working face; the height is a client config value so it can be tuned without a rebuild. */
    private Vec3 top() {
        return Vec3.atCenterOf(pos).add(0, AnvilConfig.TOP_OFFSET.get(), 0);
    }

    /** A point on the working face: {@code u} along the anvil (to the player's right), {@code v} towards the player. */
    private Vec3 at(double u, double v, double lift) {
        return top().add(right().scale(u)).add(side().scale(v)).add(0, lift, 0);
    }

    @Override public Vec3 center() { return top(); }

    @Override public List<Vec3> approach(LocalPlayer player) {
        Vec3 stand = top().add(side().scale(0.9));
        return List.of(new Vec3(stand.x, player.getY(), stand.z));
    }

    @Override public CameraRig.Pose lean() {
        return new CameraRig.Pose(top().add(side().scale(AnvilConfig.CAMERA_BACK.get())).add(0, AnvilConfig.CAMERA_HEIGHT.get(), 0), side.getOpposite().toYRot(), 36, 0);
    }

    @Override public Vec3 rest(boolean rightHand) {
        if (!rightHand && haveTongs()) return tongGrip();
        return at(rightHand ? 0.28 : -0.28, 0.32, 0.1);
    }

    // ---- tongs: held in the left hand, jaws on the left end of the ingot -----------------------------------

    private boolean haveTongs() {
        return !Tongs.inHands(Minecraft.getInstance().player).isEmpty();
    }

    /** Where the jaws touch: the left end of the ingot, lifted a little when the tongs hold it. */
    private Vec3 tongTip() {
        return at(-0.095, 0, slot(SLOT_MAIN).isEmpty() ? 0.04 : 0.045);
    }

    /** From the jaws back towards the player's left hand. */
    private Vec3 tongHandleDirection() {
        return right().scale(-0.55).add(side().scale(0.84)).normalize();
    }

    /** The left hand's grip on the tongs. */
    private Vec3 tongGrip() {
        return tongTip().add(tongHandleDirection().scale(AnvilConfig.TONGS_SIZE.get() * 1.35)).add(0, 0.05, 0);
    }

    /** Lying flat with the sprite's working end ({@code end}, in sprite space) turned to point along {@code horizontal}. */
    private static Quaternionf lyingTowards(Vec3 horizontal, Vector3f end, float tiltDegrees) {
        Quaternionf base = ItemPlacer.lying(0f, tiltDegrees);
        Vector3f now = base.transform(new Vector3f(end));
        double wanted = Math.atan2(horizontal.z, horizontal.x);
        double theta = Math.atan2(now.z, now.x) - wanted;
        Quaternionf best = null;
        double bestError = Double.MAX_VALUE;
        for (double candidate : new double[] {theta, -theta}) { // cover either sign convention
            Quaternionf q = new Quaternionf().rotationY((float) candidate).mul(base);
            Vector3f out = q.transform(new Vector3f(end));
            double error = Math.abs(Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(out.z, out.x) - wanted)));
            if (error < bestError) {
                bestError = error;
                best = q;
            }
        }
        return best;
    }

    @Override protected Vec3 restPoint(int slot) {
        return switch (slot) {
            case SLOT_SECOND -> at(0, -0.16, 0.01);
            case SLOT_HAMMER -> at(0.34, 0.14, 0.02);
            case SLOT_CATALYST -> at(-0.34, 0.14, 0.02);
            default -> at(0, 0, 0.01);
        };
    }

    // ---- hover and zones --------------------------------------------------------------------------------

    private static Zone zoneAt(double u, double v) {
        if (Math.abs(u) > HALF_LENGTH || Math.abs(v) > HALF_WIDTH) return null;
        if (u < -0.18) return Zone.UPSET;
        if (u > 0.18) return Zone.SHRINK;
        if (Math.hypot(u, v) < 0.04) return Zone.PUNCH;
        if (v > 0.05) return Zone.DRAW;
        if (v < -0.05) return Zone.BEND;
        return Zone.HIT;
    }

    private ForgeStep step(Zone zone) {
        return switch (zone) {
            case HIT -> HITS[strength];
            case DRAW -> ForgeStep.DRAW;
            case BEND -> ForgeStep.BEND;
            case PUNCH -> ForgeStep.PUNCH;
            case UPSET -> ForgeStep.UPSET;
            case SHRINK -> ForgeStep.SHRINK;
        };
    }

    @Override public void updateCursor(boolean overPanel) {
        zone = null;
        aim = null;
        if (overPanel || menu() == null || slot(SLOT_MAIN).isEmpty()) return;
        Vec3 hit = Picking.cursorOnPlane(top().y + 0.02);
        if (hit == null) return;
        Vec3 offset = hit.subtract(top());
        double u = offset.dot(right());
        double v = offset.dot(side());
        zone = zoneAt(u, v);
        if (zone != null) aim = at(u, v, 0.01);
    }

    @Override public void scroll(double delta) {
        // HOLD_CHARGE is not implemented yet, so both settings use the wheel for now.
        if (delta == 0) return;
        strength = Mth.clamp(strength + (delta > 0 ? 1 : -1), 0, HITS.length - 1);
    }

    // ---- hands ------------------------------------------------------------------------------------------

    @Override public Vec3 handTarget(Vec3 target, Vec3 shoulder, boolean carrying, double tap) {
        return target.add(0, carrying ? 0.13 : 0.08, 0);
    }

    @Override public Vec3 heldItemPoint(Vec3 hand, Vec3 shoulder) {
        return hand.add(0, -0.07, 0);
    }

    /** The working (right) hand: over the hovered spot, lifted during a swing, otherwise at rest. */
    private Vec3 rightTarget() {
        if (aim == null) return rest(true);
        double s = Mth.clamp((now() - strikeAt) / SWING_SECONDS, 0, 1);
        double raise = swinging() ? Math.sin(s * Math.PI) * 0.14 : 0;
        return aim.add(0, 0.10 + raise, 0);
    }

    /** The scene steers both hands whenever a tool is in play and nothing is being carried from the inventory drawer. */
    private boolean steeringHands() {
        if (menu() == null || !menu().getCarried().isEmpty()) return false;
        return haveTongs() || !slot(SLOT_HAMMER).isEmpty();
    }

    // ---- actions ----------------------------------------------------------------------------------------

    private static double now() { return Util.getMillis() / 1000.0; }

    private boolean swinging() { return now() - strikeAt < SWING_SECONDS; }

    private void press(int buttonId) {
        PacketDistributor.sendToServer(new ScreenButtonPacket(buttonId));
    }

    @Override protected void act(int button, boolean shift) {
        ItemStack carried = menu().getCarried();
        if (!carried.isEmpty()) {
            placeCarried(carried);
            return;
        }
        if (button == 1) {
            press(AnvilContainer.PLAN_ID); // opens TFC's plan screen; the scene yields to it until the plan scene exists
            return;
        }
        if (button != 0) return;
        if (shift) {
            if (!slot(SLOT_MAIN).isEmpty()) slotClick(SLOT_MAIN, 0, true);
            return;
        }
        if (zone == null || swinging()) return;
        if (slot(SLOT_HAMMER).isEmpty() || !HeatCapability.view(slot(SLOT_MAIN)).canWork()) {
            deny();
            return;
        }
        press(step(zone).ordinal());
        strikeAt = now();
    }

    private void placeCarried(ItemStack carried) {
        for (int slot : new int[] {SLOT_HAMMER, SLOT_CATALYST, SLOT_MAIN, SLOT_SECOND}) {
            if (accepts(slot, carried) && (slot(slot).isEmpty() || ItemStack.isSameItemSameComponents(slot(slot), carried))) {
                slotClick(slot, 0, false);
                return;
            }
        }
        deny();
    }

    // ---- rendering --------------------------------------------------------------------------------------

    @Override public void renderScene(PoseStack pose, MultiBufferSource buffers, Camera camera, int light) {
        SceneVisibility.mark(pos);
        double dt = Mth.clamp(now() - estAt, 0, 0.1);
        estAt = now();
        Vec3 cam = camera.getPosition();
        ItemStack tongs = Tongs.inHands(Minecraft.getInstance().player);
        ItemStack main = slot(SLOT_MAIN);
        boolean held = !tongs.isEmpty() && !main.isEmpty();

        if (!main.isEmpty()) {
            Vec3 ingot = at(0, 0, held ? 0.045 : 0.01);
            ItemPlacer.render(pose, buffers, cam, main, ingot, side, SLOT_MAIN, 1, 0, light);
            glow(pose, buffers, cam, main, ingot.add(0, 0.03, 0));
        }
        if (!slot(SLOT_SECOND).isEmpty()) {
            ItemPlacer.render(pose, buffers, cam, slot(SLOT_SECOND), restPoint(SLOT_SECOND), side, SLOT_SECOND, 1, 0, light);
        }
        if (!slot(SLOT_CATALYST).isEmpty()) {
            ItemPlacer.render(pose, buffers, cam, slot(SLOT_CATALYST), restPoint(SLOT_CATALYST), side, SLOT_CATALYST, 1, 0, light);
        }
        // Jaw clamping to the item's size is still to do; for now the jaw tips rest on the ingot's left end.
        if (!tongs.isEmpty()) drawTongs(pose, buffers, cam, dt, tongs, light);
        drawHammer(pose, buffers, cam, dt, light);
    }

    @Override public void renderWorld(PoseStack pose, MultiBufferSource buffers, Vec3 camera, int light) {
        SceneVisibility.mark(pos);
    }

    // ---- the hammer: lying on the anvil, picked up, then in the right hand -------------------------------

    /** Where TFC lays the hammer on the anvil before anyone works there: near the right end, handle towards the player. */
    private Vec3 hammerHome() {
        return at(HOME_U, HOME_V, 0.02);
    }

    private static double ease(double x) {
        double c = Mth.clamp(x, 0, 1);
        return c * c * (3 - 2 * c);
    }

    private static Vec3 lerp(Vec3 a, Vec3 b, double f) {
        return a.add(b.subtract(a).scale(f));
    }

    /** True from the moment the hammer is picked up until the hand is back at rest with it. */
    private boolean pickingUp() {
        return pickupAt >= 0 && now() - pickupAt < PICKUP_SECONDS && !slot(SLOT_HAMMER).isEmpty();
    }

    /** The right hand while it reaches over the hammer, closes on it and lifts it away. */
    private Vec3 pickupHand(double t) {
        Vec3 home = hammerHome();
        Vec3 above = home.add(0, 0.09, 0);
        Vec3 grip = home.add(0, 0.035, 0);
        if (t < REACH_END) return lerp(rest(true), above, ease(t / REACH_END));
        if (t < GRAB_END) return lerp(above, grip, ease((t - REACH_END) / (GRAB_END - REACH_END)));
        return lerp(grip, rest(true), ease((t - GRAB_END) / (PICKUP_SECONDS - GRAB_END)));
    }

    @Override public void tick() {
        super.tick();
        boolean has = !slot(SLOT_HAMMER).isEmpty();
        if (has && !hadHammer) pickupAt = now(); // the scene just began with a hammer, or one was just put on the anvil
        hadHammer = has;
    }

    @Override public Vec3[] handsOverride() {
        if (pickingUp()) return new Vec3[] {pickupHand(now() - pickupAt), rest(false)};
        if (steeringHands()) return new Vec3[] {rightTarget(), rest(false)};
        return null;
    }

    @Override public double handsStiffness() {
        return pickingUp() || steeringHands() ? HAND_RATE : super.handsStiffness();
    }

    /** Where the right hand is this frame, smoothed the way the arm's own spring moves it, so the hammer stays in it. */
    private Vec3 rightHandNow(double dt) {
        Vec3 target = pickingUp() ? pickupHand(now() - pickupAt) : rightTarget();
        rightEst = rightEst == null ? target : lerp(rightEst, target, 1 - Math.exp(-HAND_RATE * dt));
        return rightEst;
    }

    private Vec3 leftHandNow(double dt) {
        Vec3 target = rest(false);
        leftEst = leftEst == null ? target : lerp(leftEst, target, 1 - Math.exp(-HAND_RATE * dt));
        return leftEst;
    }

    private void drawHammer(PoseStack pose, MultiBufferSource buffers, Vec3 cam, double dt, int light) {
        ItemStack hammer = slot(SLOT_HAMMER);
        Vec3 hand = rightHandNow(dt);
        if (hammer.isEmpty()) return;
        float size = AnvilConfig.HAMMER_SIZE.get().floatValue();
        double t = pickupAt < 0 ? -1 : now() - pickupAt;
        if (t < GRAB_END) {
            // Still lying where TFC puts it (also the whole intro, before the first tick).
            ItemPlacer.drawOriented(pose, buffers, cam, hammer, hammerHome(), ItemPlacer.lying(ItemPlacer.awayYaw(side), 0), size, light);
            return;
        }
        double a = t < PICKUP_SECONDS ? ease((t - GRAB_END) / (PICKUP_SECONDS - GRAB_END)) : 1;
        double s = Mth.clamp((now() - strikeAt) / SWING_SECONDS, 0, 1);
        float tilt = (float) (HAMMER_REST_TILT * a + (swinging() ? Math.sin(s * Math.PI) * 40 : 0));
        Quaternionf rotation = ItemPlacer.lying(ItemPlacer.awayYaw(side), tilt);
        // The hand holds the handle end: the sprite's centre sits half a length along the head direction from the grip.
        Vector3f head = rotation.transform(new Vector3f(HAMMER_HEAD)).normalize();
        Vec3 grip = hand.add(0, -0.02, 0);
        Vec3 held = grip.add(head.x * size * 0.5, head.y * size * 0.5, head.z * size * 0.5);
        Vec3 centre = a < 1 ? lerp(hammerHome(), held, a) : held;
        ItemPlacer.drawOriented(pose, buffers, cam, hammer, centre, rotation, size, light);
    }

    private void drawTongs(PoseStack pose, MultiBufferSource buffers, Vec3 cam, double dt, ItemStack tongs, int light) {
        Vec3 grip = leftHandNow(dt);
        Vec3 tip = tongTip();
        Vec3 along = tip.subtract(grip);
        Vec3 flat = new Vec3(along.x, 0, along.z);
        if (flat.lengthSqr() < 1e-6) return;
        // The jaws on the ingot, the handle in the left hand, the sprite's jaw corner pointing at the ingot.
        Quaternionf rotation = lyingTowards(flat, TONG_JAWS, 0f);
        ItemPlacer.drawOriented(pose, buffers, cam, tongs, grip.add(along.scale(0.5)), rotation, AnvilConfig.TONGS_SIZE.get().floatValue(), light);
    }

    /** A translucent quad over the workpiece, coloured by TFC's heat level. */
    private void glow(PoseStack pose, MultiBufferSource buffers, Vec3 cam, ItemStack stack, Vec3 centre) {
        float[] c = HeatLook.glow(HeatCapability.getTemperature(stack));
        if (c == null) return;
        VertexConsumer vc = buffers.getBuffer(RenderType.lightning());
        Matrix4f m = pose.last().pose();
        Vec3 r = right().scale(0.17);
        Vec3 s = side().scale(0.085);
        Vec3[] corners = {
                centre.subtract(r).subtract(s), centre.add(r).subtract(s), centre.add(r).add(s), centre.subtract(r).add(s)
        };
        for (Vec3 p : corners) {
            vc.addVertex(m, (float) (p.x - cam.x), (float) (p.y - cam.y), (float) (p.z - cam.z)).setColor(c[0], c[1], c[2], c[3]);
        }
    }

    // ---- placeholder panel and gauge --------------------------------------------------------------------

    @Override public void renderScreen(GuiGraphics graphics, Font font, float partial) {
        ItemStack main = slot(SLOT_MAIN);
        if (menu() == null || main.isEmpty()) return;
        int x = 8, y = 8;

        if (AnvilConfig.HEAT_GAUGE.get()) {
            Heat heat = Heat.getHeat(HeatCapability.getTemperature(main));
            Component label = heat == null
                    ? Component.literal("Cold")
                    : Helpers.translateEnum(heat).copy().withStyle(heat.getColor());
            graphics.drawString(font, Component.literal(String.format("Heat %.0f  ", HeatCapability.getTemperature(main))).append(label), x, y, 0xFFFFFF);
            y += 12;
        }
        if (AnvilConfig.PLAN_PANEL.get()) y = planPanel(graphics, font, main, x, y);

        if (zone != null && aim != null) {
            double[] gui = Picking.toGui(aim);
            if (gui != null) {
                ForgeStep step = step(zone);
                String delta = (step.step() > 0 ? "+" : "") + step.step();
                graphics.drawString(font, Helpers.translateEnum(step).copy().append(" (" + delta + ")"), (int) gui[0] + 10, (int) gui[1] - 14, 0xFFFFFF);
            }
        }
    }

    private int planPanel(GuiGraphics graphics, Font font, ItemStack main, int x, int y) {
        if (!(Minecraft.getInstance().level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil)) return y;
        try {
            Forging forging = anvil.getMainInputForging();
            if (forging.getRecipe() == null) {
                graphics.drawString(font, Component.literal("No plan: right-click to choose one"), x, y, 0xFFAA55);
                return y + 12;
            }
            graphics.drawString(font, Component.literal("Work " + forging.work() + " / Target " + forging.target()), x, y, 0xFFFFFF);
            y += 12;
            for (ForgeRule rule : forging.getRecipe().getRules()) {
                boolean met = forging.matches(rule);
                graphics.drawString(font, Component.literal(met ? "[x] " : "[ ] ").append(rule.getDescriptionId()), x, y,
                        met ? ChatFormatting.GREEN.getColor() : 0xFFFFFF);
                y += 10;
            }
            StringBuilder last = new StringBuilder("Last: ");
            for (ForgeStep step : forging.lastSteps()) last.append(Helpers.translateEnum(step).getString()).append("  ");
            graphics.drawString(font, Component.literal(last.toString()), x, y, 0xAAAAAA);
            return y + 12;
        } catch (RuntimeException e) {
            return y; // the workpiece has no forging data yet
        }
    }

    @Override public Component hint() {
        ForgeStep step = zone == null ? null : step(zone);
        if (slot(SLOT_MAIN).isEmpty()) return Component.literal("Place a heated ingot on the anvil");
        if (slot(SLOT_HAMMER).isEmpty()) return Component.literal("Place a hammer in the anvil");
        return Component.literal("Wheel: hit strength (" + HITS[strength].getSerializedName() + ")"
                + (step == null ? "" : "  |  Click: " + step.getSerializedName()) + "  |  Right-click: choose plan");
    }
}
