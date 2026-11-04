package io.github.minerguy341.cinteractionstfc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Low-poly 3D hammer and tongs, drawn as lit boxes coloured from the tool's own texture (see {@link ToolPalette}).
 * Every tool is built in its own frame: +Y runs from the grip to the working end (hammer head, tong jaws), +X is the
 * hammer head's long axis, +Z the tongs' opening direction; units are blocks.
 */
final class ToolMesh {
    private static final ResourceLocation WHITE = ResourceLocation.withDefaultNamespace("textures/misc/white.png");

    private ToolMesh() {}

    /** Orientation with the tool's +Y along {@code y} and its +X as close to {@code xHint} as the right angle allows. */
    static Quaternionf frame(Vec3 y, Vec3 xHint) {
        Vec3 up = y.normalize();
        Vec3 across = xHint.subtract(up.scale(up.dot(xHint)));
        if (across.lengthSqr() < 1e-8) across = Math.abs(up.y) < 0.9 ? new Vec3(0, 1, 0).cross(up) : new Vec3(1, 0, 0);
        across = across.normalize();
        Vec3 depth = across.cross(up);
        Matrix3f basis = new Matrix3f(
                (float) across.x, (float) across.y, (float) across.z,
                (float) up.x, (float) up.y, (float) up.z,
                (float) depth.x, (float) depth.y, (float) depth.z);
        return new Quaternionf().setFromNormalized(basis);
    }

    /** A hammer whose grip is at {@code grip}; {@code scale} 1 is a handle about 0.3 blocks long. */
    static void hammer(PoseStack pose, MultiBufferSource buffers, Vec3 camera, Vec3 grip, Quaternionf frame, float scale,
                       ToolPalette palette, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entitySolid(WHITE));
        pose.pushPose();
        pose.translate(grip.x - camera.x, grip.y - camera.y, grip.z - camera.z);
        pose.mulPose(frame);
        pose.scale(scale, scale, scale);
        box(pose, vc, -0.011f, -0.07f, -0.011f, 0.011f, 0.25f, 0.011f, palette.handleFaces(), light);
        box(pose, vc, -0.06f, 0.19f, -0.022f, 0.06f, 0.26f, 0.022f, palette.headFaces(), light);
        pose.popPose();
    }

    /**
     * Tongs whose handle end is at {@code grip}, {@code length} long, with the jaws {@code gap} apart (along the tool's
     * +Z): the two arms cross at a hinge, so closing the jaws brings the handles together.
     */
    static void tongs(PoseStack pose, MultiBufferSource buffers, Vec3 camera, Vec3 grip, Quaternionf frame, float length,
                      float gap, ToolPalette palette, int light) {
        VertexConsumer vc = buffers.getBuffer(RenderType.entitySolid(WHITE));
        pose.pushPose();
        pose.translate(grip.x - camera.x, grip.y - camera.y, grip.z - camera.z);
        pose.mulPose(frame);
        float hinge = length * 0.42f;
        float half = gap / 2f;
        float spread = half * hinge / (length - hinge); // where each arm's handle end lies: across the hinge from its jaw
        arm(pose, vc, 0f, -spread, length, half, palette.allFaces(), light);
        arm(pose, vc, 0f, spread, length, -half, palette.allFaces(), light);
        box(pose, vc, -0.012f, hinge - 0.012f, -0.012f, 0.012f, hinge + 0.012f, 0.012f, palette.handleFaces(), light);
        pose.popPose();
    }

    /** A flat bar from (y0, z0) to (y1, z1) in the tool's Y-Z plane. */
    private static void arm(PoseStack pose, VertexConsumer vc, float y0, float z0, float y1, float z1, int[] colours, int light) {
        float dy = y1 - y0;
        float dz = z1 - z0;
        float length = (float) Math.hypot(dy, dz);
        pose.pushPose();
        pose.translate(0, (y0 + y1) / 2f, (z0 + z1) / 2f);
        pose.mulPose(Axis.XP.rotation((float) Math.atan2(dz, dy)));
        box(pose, vc, -0.009f, -length / 2f, -0.005f, 0.009f, length / 2f, 0.005f, colours, light);
        pose.popPose();
    }

    /** One box; {@code colours} are ARGB for the faces -X, +X, -Y, +Y, -Z, +Z. */
    private static void box(PoseStack pose, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1,
                            int[] colours, int light) {
        quad(pose, vc, colours[0], light, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
        quad(pose, vc, colours[1], light, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
        quad(pose, vc, colours[2], light, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        quad(pose, vc, colours[3], light, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        quad(pose, vc, colours[4], light, 0, 0, -1, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);
        quad(pose, vc, colours[5], light, 0, 0, 1, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
    }

    private static void quad(PoseStack pose, VertexConsumer vc, int argb, int light, float nx, float ny, float nz,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz) {
        Matrix4f m = pose.last().pose();
        int r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        float[][] corners = {{ax, ay, az}, {bx, by, bz}, {cx, cy, cz}, {dx, dy, dz}};
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        for (int i = 0; i < 4; i++) {
            vc.addVertex(m, corners[i][0], corners[i][1], corners[i][2])
                    .setColor(r, g, b, 255)
                    .setUv(uv[i][0], uv[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose.last(), nx, ny, nz);
        }
    }
}
