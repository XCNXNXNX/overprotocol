package dev.overprotocol.vehicle;

import net.minecraft.util.Mth;

/** Spring-centred mouse stick. Collect all frame deltas, then advance exactly once per client tick. */
public final class B2MouseInput {
    private double pendingTurn, pendingPitch;
    private float turn, pitch;

    public void add(double turnDegrees, double pitchDegrees) {
        if (Double.isFinite(turnDegrees) && Double.isFinite(pitchDegrees)) {
            pendingTurn += turnDegrees;
            pendingPitch += pitchDegrees;
        }
    }

    public void tick() {
        turn = advance(turn, pendingTurn);
        pitch = advance(pitch, pendingPitch);
        pendingTurn = pendingPitch = 0;
    }

    private static float advance(float previous, double delta) {
        float value = Mth.clamp((float)(previous * .80 + delta / 14), -1, 1);
        return Math.abs(value) < .012F ? 0 : value;
    }

    public float turn() { return turn; }
    public float pitch() { return pitch; }
    public void reset() { pendingTurn = pendingPitch = 0; turn = pitch = 0; }
}
