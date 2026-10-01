package dev.overprotocol.vehicle;

import net.minecraft.util.Mth;

/** Accessible game flight controls; all values are blocks/tick, rather than real aircraft data. */
public final class B2Flight {
    public static final int THROTTLE_UP = 1, THROTTLE_DOWN = 2, LEFT = 4, RIGHT = 8,
        CLIMB = 16, DESCEND = 32, GEAR = 64, VALID_INPUT = 127;
    // Public high-altitude approximation of Mach .95: 1010 km/h; one block = one metre, 20 ticks/s.
    public static final float MAX_KMH = 1010F;
    public static final float MAX_SPEED = MAX_KMH / 72F;
    public static final float TAXI_SPEED = 108F / 72F, GEAR_SPEED = 180F / 72F;
    public static final float SCROLL_STEP = .05F;
    public static final float MAX_PITCH = 22F, MAX_BANK = 30F;
    public record Step(float throttle, float speed, float pitch, float yaw, float bank, double vertical) {}

    public static Step step(float throttle, float speed, float pitch, float yaw, float bank,
                            double vertical, int input, boolean ground, boolean pilot, boolean gear) {
        return step(throttle, speed, pitch, yaw, bank, vertical, input, 0, 0, ground, pilot, gear);
    }

    /** Positive axes mean right turn and nose up. Simulation always runs at 20 Hz on the server. */
    public static Step step(float throttle, float speed, float pitch, float yaw, float bank,
                            double vertical, int input, float steering, float elevator,
                            boolean ground, boolean pilot, boolean gear) {
        if (!pilot) { input = 0; steering = 0; elevator = 0; }
        int power = bit(input, THROTTLE_UP) - bit(input, THROTTLE_DOWN);
        throttle = Mth.clamp(throttle + (pilot ? power * .015F : -.04F), 0, 1);
        float limit = ground ? TAXI_SPEED : gear ? GEAR_SPEED : MAX_SPEED;
        float target = Math.min(throttle * MAX_SPEED, limit);
        speed = Mth.clamp(Mth.lerp(ground ? .04F : .018F, speed, target), 0, MAX_SPEED);
        if (ground && power < 0) speed = Math.max(0, speed - .03F);
        if (ground && !pilot) speed *= .85F;
        float turn = Mth.clamp(curve(steering) + bit(input, RIGHT) - bit(input, LEFT), -1, 1);
        float climb = Mth.clamp(curve(elevator) + bit(input, CLIMB) - bit(input, DESCEND), -1, 1);
        float authority = Mth.clamp(speed / .55F, 0, 1);
        // Bank takes time to build/reverse. The actual bank steers the aircraft, including during recovery.
        bank = approachAttitude(bank, ground ? 0 : -turn * MAX_BANK * authority, .18F, 3F);
        float turnRate = ground ? turn * 1.6F * Mth.clamp(speed * 2, 0, 1)
            : -bank / MAX_BANK * Mth.lerp(speed / MAX_SPEED, 2.2F, 1.55F) * authority;
        yaw = Mth.wrapDegrees(yaw + turnRate);
        pitch = approachAttitude(pitch, climb * MAX_PITCH * authority, .14F, 1.65F);
        // Assisted level flight: when controls return to centre, both attitude and climb rate settle.
        double lift = Mth.clamp((speed - .18) / .30, 0, 1);
        double targetVertical = Math.sin(Math.toRadians(pitch)) * speed * .70 - .55 * (1 - lift);
        vertical = Mth.clamp(Mth.lerp(.12, vertical, targetVertical), -1.1, 1.1);
        if (ground) {
            vertical = speed > .38F && pitch > 3F ? Math.max(.06, speed * Math.sin(Math.toRadians(pitch)) * .3) : -.04;
            if (vertical < 0) pitch = Math.min(pitch, 4F);
        }
        // Cap total travel speed, including a climb/descent, to the blue-ice boat limit.
        speed = Math.min(speed,(float)Math.sqrt(Math.max(0,MAX_SPEED*MAX_SPEED-vertical*vertical)));
        return new Step(throttle, speed, pitch, yaw, bank, vertical);
    }

    public static boolean validAxis(float axis) { return Float.isFinite(axis) && Math.abs(axis) <= 1; }
    public static boolean validScroll(float scroll) { return Float.isFinite(scroll) && Math.abs(scroll) <= 4; }
    public static float scrollThrottle(float throttle, float scroll) {
        return Mth.clamp(throttle + scroll * SCROLL_STEP, 0, 1);
    }
    private static float curve(float axis) { return axis * (.55F + .45F * axis * axis); }
    private static float approachAttitude(float current, float target, float response, float maxStep) {
        return current + Mth.clamp((target - current) * response, -maxStep, maxStep);
    }
    private static int bit(int value, int flag) { return (value & flag) == 0 ? 0 : 1; }
    private B2Flight() {}
}
