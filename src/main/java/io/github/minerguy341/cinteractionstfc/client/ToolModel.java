package io.github.minerguy341.cinteractionstfc.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.minerguy341.cinteractionstfc.CInteractionsTfc;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * A tool shaped in Blockbench (Java Block/Item JSON, 1 unit = 1 sprite pixel) and drawn with the UVs you painted onto
 * the item's own sprite. Every tool of a kind shares one sprite layout, so one model serves every metal; the sprite
 * comes from whichever stack is drawn. Faces without a texture take the pixel of their cube's textured face.
 *
 * Conventions (see docs/DESIGN.md): the sprite's plane is the model's X-Y plane; groups {@code grip} and {@code tip}
 * mark where the hand holds the tool and where its working end touches (defaults: the two ends of {@code arm_a});
 * groups {@code arm_a} and {@code arm_b} are the tongs' two arms, modelled closed, and open to clamp an item.
 */
final class ToolModel {
    private static final Map<String, ToolModel> CACHE = new HashMap<>();
    private static final ToolModel NONE = new ToolModel();

    /** Texture-space rectangle of a face, in sprite pixels (0 to 16). */
    private record Face(float u1, float v1, float u2, float v2, boolean textured) {}

    private static final class Part {
        String name = "";
        String group = "";
        float[] from = new float[3];
        float[] to = new float[3];
        String rotationAxis = "y";
        float rotationAngle;
        float[] rotationOrigin = new float[3];
        final Map<String, Face> faces = new LinkedHashMap<>();
        float[] pixel; // u, v of the first textured face's centre, for the faces left untextured
    }

    private final List<Part> parts = new ArrayList<>();
    private final Map<String, float[]> groupOrigins = new HashMap<>();
    private float[] grip = {8, 4, 8};
    private float[] tip = {8, 12, 8};
    private float[] hinge = {8, 8, 8};
    private boolean starter = true;
    private boolean hasArms;

    private ToolModel() {}

    /** The model named {@code name} from {@code assets/cinteractions_tfc/models/tool/}, or null when missing or unusable. */
    static ToolModel get(String name) {
        ToolModel model = CACHE.computeIfAbsent(name, ToolModel::load);
        return model == NONE ? null : model;
    }

    static void clearCache() {
        CACHE.clear();
    }

    /** True while the file is still the generated starter (nothing painted onto the sprite yet). */
    boolean isStarter() { return starter; }

    /** Length in model units from the grip to the working end. */
    float gripToTip() {
        return (float) Math.hypot(tip[0] - grip[0], tip[1] - grip[1]);
    }

    private static ToolModel load(String name) {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(CInteractionsTfc.MOD_ID, "models/tool/" + name + ".json");
        try (Reader reader = new InputStreamReader(Minecraft.getInstance().getResourceManager().getResourceOrThrow(file).open(), StandardCharsets.UTF_8)) {
            ToolModel model = new ToolModel();
            model.parse(JsonParser.parseReader(reader).getAsJsonObject());
            CInteractionsTfc.LOGGER.info("Tool model {}: {} cubes, {}, arms {}, grip {} tip {} hinge {}", name, model.parts.size(),
                    model.starter ? "still the starter (built-in shape used)" : "painted", model.hasArms ? "yes" : "no",
                    java.util.Arrays.toString(model.grip), java.util.Arrays.toString(model.tip), java.util.Arrays.toString(model.hinge));
            if (model.hasArms) {
                CInteractionsTfc.LOGGER.info("Tool model {} jaws: tipA {} tipB {} openDistance {} swingToward {} halfSpread {} deg; closed to 3.5 units: {} deg",
                        name, java.util.Arrays.toString(model.tipA), java.util.Arrays.toString(model.tipB),
                        String.format("%.2f", model.jawDistance(0)), model.closeDirection,
                        String.format("%.1f", Math.toDegrees(model.halfSpread)), String.format("%.1f", Math.toDegrees(model.closeAngle(3.5f))));
            }
            return model.parts.isEmpty() ? NONE : model;
        } catch (Exception problem) {
            CInteractionsTfc.LOGGER.warn("Cannot load tool model {}; using the built-in shape", file, problem);
            return NONE;
        }
    }

    private void parse(JsonObject root) {
        for (JsonElement element : root.getAsJsonArray("elements")) {
            JsonObject json = element.getAsJsonObject();
            Part part = new Part();
            part.name = json.has("name") ? json.get("name").getAsString() : "";
            part.from = floats(json.getAsJsonArray("from"));
            part.to = floats(json.getAsJsonArray("to"));
            if (json.has("rotation")) {
                JsonObject rotation = json.getAsJsonObject("rotation");
                part.rotationAngle = rotation.get("angle").getAsFloat();
                part.rotationAxis = rotation.get("axis").getAsString();
                part.rotationOrigin = floats(rotation.getAsJsonArray("origin"));
            }
            if (json.has("faces")) {
                for (Map.Entry<String, JsonElement> face : json.getAsJsonObject("faces").entrySet()) {
                    JsonObject f = face.getValue().getAsJsonObject();
                    float[] uv = f.has("uv") ? floats(f.getAsJsonArray("uv")) : new float[] {0, 0, 16, 16};
                    String texture = f.has("texture") ? f.get("texture").getAsString() : "";
                    boolean textured = texture.startsWith("#") && !texture.equals("#missing");
                    part.faces.put(face.getKey(), new Face(uv[0], uv[1], uv[2], uv[3], textured));
                    if (textured) {
                        if (part.pixel == null) part.pixel = new float[] {(uv[0] + uv[2]) / 2f, (uv[1] + uv[3]) / 2f};
                        boolean whole = uv[0] == 0 && uv[1] == 0 && uv[2] == 16 && uv[3] == 16;
                        if (!whole) starter = false;
                    }
                }
            }
            parts.add(part);
        }
        JsonArray groups = root.has("groups") ? root.getAsJsonArray("groups") : root.has("outliner") ? root.getAsJsonArray("outliner") : new JsonArray();
        for (JsonElement group : groups) {
            if (group.isJsonObject()) readGroup(group.getAsJsonObject());
        }
        hasArms = parts.stream().anyMatch(p -> p.group.equals("arm_a")) && parts.stream().anyMatch(p -> p.group.equals("arm_b"));
        findPoints();
    }

    private void readGroup(JsonObject group) {
        String name = group.has("name") ? group.get("name").getAsString() : "";
        if (group.has("origin")) groupOrigins.put(name, floats(group.getAsJsonArray("origin")));
        if (!group.has("children")) return;
        for (JsonElement child : group.getAsJsonArray("children")) {
            if (child.isJsonObject()) {
                readGroup(child.getAsJsonObject());
            } else {
                int index = child.getAsInt();
                if (index >= 0 && index < parts.size()) parts.get(index).group = name;
            }
        }
    }

    /** Grip, tip and hinge from the marker groups, or from the arm's two ends and the hinge cube. */
    private void findPoints() {
        List<Part> arm = parts.stream().filter(p -> p.group.equals("arm_a")).toList();
        if (arm.isEmpty()) arm = parts;
        float zMid = hasArms ? (minZ(arm) + maxZ(parts.stream().filter(p -> p.group.equals("arm_b")).toList())) / 2f : minZ(arm);
        Part first = arm.get(0);
        Part last = arm.get(arm.size() - 1);
        grip = new float[] {(first.from[0] + first.to[0]) / 2f, (first.from[1] + first.to[1]) / 2f, zMid};
        tip = new float[] {(last.from[0] + last.to[0]) / 2f, (last.from[1] + last.to[1]) / 2f, zMid};
        hinge = new float[] {tip[0], tip[1], zMid};
        for (Part part : parts) {
            if (part.name.equals("hinge")) {
                hinge = new float[] {(part.from[0] + part.to[0]) / 2f, (part.from[1] + part.to[1]) / 2f, zMid};
            }
        }
        if (groupOrigins.containsKey("grip")) grip = groupOrigins.get("grip").clone();
        if (groupOrigins.containsKey("tip")) tip = groupOrigins.get("tip").clone();
        if (hasArms) findJaws();
    }

    /** The centre of the cube of {@code group} that lies furthest from the hinge on the jaw side ({@code jawDirection}). */
    private float[] jawTip(String group, float[] jawDirection) {
        float[] best = null;
        float bestDistance = -1;
        for (Part part : parts) {
            if (!part.group.equals(group)) continue;
            float cx = (part.from[0] + part.to[0]) / 2f - hinge[0];
            float cy = (part.from[1] + part.to[1]) / 2f - hinge[1];
            if (cx * jawDirection[0] + cy * jawDirection[1] <= 0) continue; // behind the hinge: the handle
            float distance = (float) Math.hypot(cx, cy);
            if (distance > bestDistance) {
                bestDistance = distance;
                best = new float[] {cx + hinge[0], cy + hinge[1]};
            }
        }
        return best != null ? best : new float[] {tip[0], tip[1]};
    }

    /** Which way to turn arm_a (and the opposite way arm_b) to bring the jaws together: +1 or -1 about +Z. */
    private float closeDirection = 1f;
    private float[] tipA = {0, 0}, tipB = {0, 0};
    private double halfSpread;

    private void findJaws() {
        float[] towardJaws = {tip[0] - hinge[0], tip[1] - hinge[1]};
        tipA = jawTip("arm_a", towardJaws);
        tipB = jawTip("arm_b", towardJaws);
        double angleA = Math.atan2(tipA[1] - hinge[1], tipA[0] - hinge[0]);
        double angleB = Math.atan2(tipB[1] - hinge[1], tipB[0] - hinge[0]);
        double spread = Math.atan2(Math.sin(angleB - angleA), Math.cos(angleB - angleA)); // signed, -pi..pi
        closeDirection = spread > 0 ? 1f : -1f;
        halfSpread = Math.abs(spread) / 2.0;
    }

    /** Distance between the two jaw tips after each arm has swung {@code angle} towards the other. */
    private double jawDistance(double angle) {
        double a = closeDirection * angle;
        double ax = tipA[0] - hinge[0], ay = tipA[1] - hinge[1];
        double bx = tipB[0] - hinge[0], by = tipB[1] - hinge[1];
        double rax = ax * Math.cos(a) - ay * Math.sin(a), ray = ax * Math.sin(a) + ay * Math.cos(a);
        double rbx = bx * Math.cos(-a) - by * Math.sin(-a), rby = bx * Math.sin(-a) + by * Math.cos(-a);
        return Math.hypot(rax - rbx, ray - rby);
    }

    /** The swing, in radians, that leaves the jaw tips {@code gapUnits} apart (0 when the sprite's own pose is already narrower). */
    private float closeAngle(float gapUnits) {
        if (gapUnits >= jawDistance(0)) return 0f;
        double low = 0, high = halfSpread;
        if (jawDistance(high) >= gapUnits) return (float) high;
        for (int i = 0; i < 28; i++) {
            double mid = (low + high) / 2;
            if (jawDistance(mid) > gapUnits) low = mid; else high = mid;
        }
        return (float) high;
    }

    private static float minZ(List<Part> list) {
        float z = Float.MAX_VALUE;
        for (Part p : list) z = Math.min(z, Math.min(p.from[2], p.to[2]));
        return z;
    }

    private static float maxZ(List<Part> list) {
        float z = -Float.MAX_VALUE;
        for (Part p : list) z = Math.max(z, Math.max(p.from[2], p.to[2]));
        return z;
    }

    private static float[] floats(JsonArray array) {
        float[] out = new float[array.size()];
        for (int i = 0; i < out.length; i++) out[i] = array.get(i).getAsFloat();
        return out;
    }

    // ---- placing ------------------------------------------------------------------------------------------

    /**
     * Rotation taking the model into the world with the grip-to-tip line along {@code toTip} and the model's depth (+Z,
     * the sprite's facing and the way the tong arms open) along {@code depthHint}.
     */
    Quaternionf orient(Vec3 toTip, Vec3 depthHint) {
        float dx = tip[0] - grip[0];
        float dy = tip[1] - grip[1];
        float length = (float) Math.hypot(dx, dy);
        Vector3f e1 = length < 1e-4f ? new Vector3f(0, 1, 0) : new Vector3f(dx / length, dy / length, 0);
        Vector3f e3 = new Vector3f(0, 0, 1);
        Vector3f e2 = e3.cross(e1, new Vector3f());

        Vec3 w1 = toTip.normalize();
        Vec3 w3 = depthHint.subtract(w1.scale(w1.dot(depthHint)));
        if (w3.lengthSqr() < 1e-8) w3 = Math.abs(w1.y) < 0.9 ? new Vec3(0, 1, 0).cross(w1) : new Vec3(1, 0, 0);
        w3 = w3.normalize();
        Vec3 w2 = w3.cross(w1);

        Matrix3f model = new Matrix3f(e1.x, e1.y, e1.z, e2.x, e2.y, e2.z, e3.x, e3.y, e3.z);
        Matrix3f world = new Matrix3f((float) w1.x, (float) w1.y, (float) w1.z, (float) w2.x, (float) w2.y, (float) w2.z,
                (float) w3.x, (float) w3.y, (float) w3.z);
        Matrix3f turn = world.mul(model.transpose(), new Matrix3f());
        return new Quaternionf().setFromNormalized(turn);
    }

    // ---- drawing ------------------------------------------------------------------------------------------

    /**
     * Draws the model with its grip at {@code hand}. {@code unit} is blocks per model unit; {@code gapUnits} is how far
     * apart the tong jaws open (0 closed), ignored for models without {@code arm_a}/{@code arm_b}.
     */
    void draw(PoseStack pose, MultiBufferSource buffers, Vec3 camera, TextureAtlasSprite sprite, Vec3 hand, Quaternionf rotation,
              float unit, float gapUnits, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        float closed = hasArms ? closeAngle(gapUnits) : 0f;
        for (Part part : parts) {
            pose.pushPose();
            pose.translate(hand.x - camera.x, hand.y - camera.y, hand.z - camera.z);
            pose.mulPose(rotation);
            pose.scale(unit, unit, unit);
            pose.translate(-grip[0], -grip[1], -grip[2]);
            int side = !hasArms ? 0 : part.group.equals("arm_a") ? 1 : part.group.equals("arm_b") ? -1 : 0;
            if (side != 0 && closed != 0f) {
                // The jaws splay apart in the sprite's own plane: swing each arm about the hinge, towards the other, until the
                // tips are the item's width apart.
                pose.translate(hinge[0], hinge[1], hinge[2]);
                pose.mulPose(new Quaternionf().rotationZ(side * closeDirection * closed));
                pose.translate(-hinge[0], -hinge[1], -hinge[2]);
            }
            if (part.rotationAngle != 0) {
                float r = (float) Math.toRadians(part.rotationAngle);
                pose.translate(part.rotationOrigin[0], part.rotationOrigin[1], part.rotationOrigin[2]);
                pose.mulPose(switch (part.rotationAxis) {
                    case "x" -> new Quaternionf().rotationX(r);
                    case "z" -> new Quaternionf().rotationZ(r);
                    default -> new Quaternionf().rotationY(r);
                });
                pose.translate(-part.rotationOrigin[0], -part.rotationOrigin[1], -part.rotationOrigin[2]);
            }
            drawPart(pose, vc, sprite, part, light);
            pose.popPose();
        }
    }

    private static void drawPart(PoseStack pose, VertexConsumer vc, TextureAtlasSprite sprite, Part part, int light) {
        float x0 = Math.min(part.from[0], part.to[0]), x1 = Math.max(part.from[0], part.to[0]);
        float y0 = Math.min(part.from[1], part.to[1]), y1 = Math.max(part.from[1], part.to[1]);
        float z0 = Math.min(part.from[2], part.to[2]), z1 = Math.max(part.from[2], part.to[2]);
        // Corner order is top-left, bottom-left, bottom-right, top-right in each face's own texture space (vanilla's).
        face(pose, vc, sprite, part, "down", 0, -1, 0, light, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
        face(pose, vc, sprite, part, "up", 0, 1, 0, light, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
        face(pose, vc, sprite, part, "north", 0, 0, -1, light, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0);
        face(pose, vc, sprite, part, "south", 0, 0, 1, light, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
        face(pose, vc, sprite, part, "west", -1, 0, 0, light, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1);
        face(pose, vc, sprite, part, "east", 1, 0, 0, light, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0);
    }

    private static void face(PoseStack pose, VertexConsumer vc, TextureAtlasSprite sprite, Part part, String name,
                             float nx, float ny, float nz, int light, float... corners) {
        Face face = part.faces.get(name);
        if (face == null) return; // a face left out of the file is culled (hidden behind a neighbouring cube)
        float u1, v1, u2, v2;
        if (face != null && face.textured()) {
            u1 = face.u1();
            v1 = face.v1();
            u2 = face.u2();
            v2 = face.v2();
        } else if (part.pixel != null) {
            u1 = u2 = part.pixel[0];
            v1 = v2 = part.pixel[1];
        } else {
            return;
        }
        float[][] uv = {{u1, v1}, {u1, v2}, {u2, v2}, {u2, v1}};
        Matrix4f m = pose.last().pose();
        for (int i = 0; i < 4; i++) {
            float u = sprite.getU0() + (sprite.getU1() - sprite.getU0()) * uv[i][0] / 16f;
            float v = sprite.getV0() + (sprite.getV1() - sprite.getV0()) * uv[i][1] / 16f;
            vc.addVertex(m, corners[i * 3], corners[i * 3 + 1], corners[i * 3 + 2])
                    .setColor(255, 255, 255, 255)
                    .setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose.last(), nx, ny, nz);
        }
    }
}
