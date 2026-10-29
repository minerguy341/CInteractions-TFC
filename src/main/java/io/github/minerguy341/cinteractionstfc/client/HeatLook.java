package io.github.minerguy341.cinteractionstfc.client;

import net.dries007.tfc.common.component.heat.Heat;

/** Glow colour for a workpiece temperature, following TFC's own heat levels. */
final class HeatLook {
    private HeatLook() {}

    /** RGBA in 0..1, or null when the metal is too cool to glow. */
    static float[] glow(float temperature) {
        Heat heat = Heat.getHeat(temperature);
        if (heat == null) return null;
        return switch (heat) {
            case WARMING, HOT, VERY_HOT -> null;
            case FAINT_RED -> new float[] {0.45f, 0.05f, 0.03f, 0.18f};
            case DARK_RED -> new float[] {0.65f, 0.07f, 0.04f, 0.26f};
            case BRIGHT_RED -> new float[] {0.9f, 0.12f, 0.05f, 0.34f};
            case ORANGE -> new float[] {1.0f, 0.45f, 0.08f, 0.42f};
            case YELLOW -> new float[] {1.0f, 0.78f, 0.15f, 0.48f};
            case YELLOW_WHITE -> new float[] {1.0f, 0.92f, 0.5f, 0.54f};
            case WHITE -> new float[] {1.0f, 0.98f, 0.82f, 0.6f};
            case BRILLIANT_WHITE -> new float[] {1.0f, 1.0f, 0.95f, 0.68f};
        };
    }
}
