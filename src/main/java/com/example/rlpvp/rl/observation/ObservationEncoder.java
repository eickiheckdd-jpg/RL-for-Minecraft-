package com.example.rlpvp.rl.observation;

public class ObservationEncoder {
    public static final int OBS_DIM = 
        SelfObservation.dim() + 
        TargetObservation.dim() + 
        CombatObservation.dim() + 
        AimObservation.dim() + 
        MovementObservation.dim();

    private final float[] encoded;

    public ObservationEncoder() {
        this.encoded = new float[OBS_DIM];
    }

    public float[] encode(SelfObservation self, TargetObservation target, 
                          CombatObservation combat, AimObservation aim, 
                          MovementObservation movement) {
        int offset = 0;
        self.toArray(encoded, offset);
        offset += SelfObservation.dim();
        target.toArray(encoded, offset);
        offset += TargetObservation.dim();
        combat.toArray(encoded, offset);
        offset += CombatObservation.dim();
        aim.toArray(encoded, offset);
        offset += AimObservation.dim();
        movement.toArray(encoded, offset);
        return encoded;
    }

    public float[] getEncoded() {
        return encoded;
    }

    public static int getObsDim() {
        return OBS_DIM;
    }
}