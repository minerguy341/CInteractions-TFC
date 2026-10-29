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
        SPEC = builder.build();
    }

    private AnvilConfig() {}
}
