package com.example.rlpvp.rl.action;

public class ActionSpace {
    public static final int MOVEMENT_DIM = 6;
    public static final int AIM_DIM = 2;
    public static final int ATTACK_DIM = 1;
    public static final int TOTAL_DIM = MOVEMENT_DIM + AIM_DIM + ATTACK_DIM;

    public static final int IDX_FORWARD = 0;
    public static final int IDX_BACKWARD = 1;
    public static final int IDX_LEFT = 2;
    public static final int IDX_RIGHT = 3;
    public static final int IDX_SPRINT = 4;
    public static final int IDX_JUMP = 5;
    public static final int IDX_YAW_DELTA = 6;
    public static final int IDX_PITCH_DELTA = 7;
    public static final int IDX_ATTACK = 8;

    public static final float MAX_YAW_DELTA = 30f;
    public static final float MAX_PITCH_DELTA = 30f;

    private ActionSpace() {}
}