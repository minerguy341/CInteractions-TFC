package io.github.minerguy341.cinteractionstfc.client;

import io.github.minerguy341.cinteractionstfc.CInteractionsTfc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import net.minecraft.world.phys.Vec3;

/**
 * Where Cinematic Interactions has put the arms this frame, in every phase (approach, work, leaving). Its
 * {@code Hands.position} is package-private, so it is reached by reflection; when that is not possible the callers fall
 * back to their own estimate and the tools simply stop following the arms during the approach and exit.
 */
final class HandsAccess {
    private static final MethodHandle POSITION;

    static {
        MethodHandle handle = null;
        try {
            Method method = Class.forName("dev.dyslexic.cinematic.client.Hands").getDeclaredMethod("position", boolean.class);
            method.setAccessible(true);
            handle = MethodHandles.lookup().unreflect(method);
        } catch (Throwable problem) {
            CInteractionsTfc.LOGGER.warn("Cannot read Cinematic Interactions' hand positions; tools will not follow the arms while approaching or leaving", problem);
        }
        POSITION = handle;
    }

    private HandsAccess() {}

    /** The arm's hand this frame, or null when it cannot be read. */
    static Vec3 position(boolean right) {
        if (POSITION == null) return null;
        try {
            return (Vec3) POSITION.invoke(right);
        } catch (Throwable problem) {
            return null;
        }
    }
}
