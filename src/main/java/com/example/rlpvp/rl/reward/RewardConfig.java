package com.example.rlpvp.rl.reward;

public class RewardConfig {
    public float hitReward = 1.0f;
    public float damageDealtReward = 0.5f;
    public float criticalHitBonus = 0.5f;
    public float comboReward = 0.2f;
    public float goodTrackingReward = 0.1f;
    public float spacingReward = 0.1f;
    public float counterHitReward = 0.3f;
    public float whiffPunishReward = 0.2f;
    public float survivalReward = 0.01f;
    public float winReward = 10.0f;

    public float damageTakenPenalty = -1.0f;
    public float whiffPenalty = -0.1f;
    public float badSpacingPenalty = -0.05f;
    public float poorTimingPenalty = -0.05f;
    public float randomMovementPenalty = -0.01f;
    public float excessiveJumpPenalty = -0.02f;
    public float loseTargetPenalty = -0.1f;
    public float deathPenalty = -10.0f;
    public float missedOpportunityPenalty = -0.05f;

    public float exploitationSpinPenalty = -0.5f;
    public float exploitationStrafePenalty = -0.3f;
    public float exploitationAttackPenalty = -0.2f;
    public float exploitationDamagePenalty = -0.5f;
    public float exploitationResetPenalty = -1.0f;

    public boolean enableExploitationPrevention = true;
}