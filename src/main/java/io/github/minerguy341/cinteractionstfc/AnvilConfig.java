package io.github.minerguy341.cinteractionstfc;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-side options for the anvil scene. */
public final class AnvilConfig {
    public enum HitControl {
        /** The mouse wheel cycles light / medium / hard; a click strikes. */
        MOUSE_WHEEL,
        /** Hold the click to charge a harder hit. Not implemented yet: behaves like MOUSE_WHEEL. */
        HOLD_CHARGE
    }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<HitControl> HIT_CONTROL;
    public static final ModConfigSpec.BooleanValue HEAT_GAUGE;
    public static final ModConfigSpec.BooleanValue PLAN_PANEL;
    public static final ModConfigSpec.DoubleValue TOP_OFFSET;
    public static final ModConfigSpec.DoubleValue CAMERA_BACK;
    public static final ModConfigSpec.DoubleValue CAMERA_HEIGHT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        HIT_CONTROL = builder
                .comment("How the strength of a Hit (light, medium, hard) is chosen.")
                .defineEnum("hitControl", HitControl.MOUSE_WHEEL);
        HEAT_GAUGE = builder
                .comment("Show a readable heat gauge for the workpiece (the glow is always shown).")
                .define("heatGauge", false);
        PLAN_PANEL = builder
                .comment("Show the placeholder floating panel with work, target and the three rule requirements.")
                .define("planPanel", true);
        builder.push("tuning");
        TOP_OFFSET = builder
                .comment("Height of the anvil's working face above the block centre, in blocks. Edit the file while the game runs to tune it.")
                .defineInRange("topOffset", 0.30, -0.5, 1.0);
        CAMERA_BACK = builder
                .comment("How far the camera sits back from the anvil centre, towards the player, in blocks.")
                .defineInRange("cameraBack", 0.8, 0.2, 2.5);
        CAMERA_HEIGHT = builder
                .comment("How high the camera sits above the working face, in blocks.")
                .defineInRange("cameraHeight", 0.7, 0.2, 2.0);
        builder.pop();
        SPEC = builder.build();
    }

    private AnvilConfig() {}
}
