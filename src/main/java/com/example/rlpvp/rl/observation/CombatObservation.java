package com.example.rlpvp.rl.observation;

public class CombatObservation {
    public float attackCooldown;
    public float attackRange;
    public float recentDamageDealt;
    public float recentDamageReceived;
    public float timeSinceLastHit;
    public float timeSinceReceivedLastHit;
    public float comboLength;
    public float recentWhiff;
    public float targetVulnerability;
    public float combatSpacing;
    public float canCritical;
    public float blockState;

    public void normalize() {
        attackCooldown = Math.min(attackCooldown / 20f, 1f);
        attackRange = Math.min(attackRange / 6f, 1f);
        recentDamageDealt = Math.min(recentDamageDealt / 20f, 1f);
        recentDamageReceived = Math.min(recentDamageReceived / 20f, 1f);
        timeSinceLastHit = Math.min(timeSinceLastHit / 100f, 1f);
        timeSinceReceivedLastHit = Math.min(timeSinceReceivedLastHit / 100f, 1f);
        comboLength = Math.min(comboLength / 10f, 1f);
        combatSpacing = Math.min(combatSpacing / 10f, 1f);
    }

    public void toArray(float[] out, int offset) {
        out[offset++] = attackCooldown;
        out[offset++] = attackRange;
        out[offset++] = recentDamageDealt;
        out[offset++] = recentDamageReceived;
        out[offset++] = timeSinceLastHit;
        out[offset++] = timeSinceReceivedLastHit;
        out[offset++] = comboLength;
        out[offset++] = recentWhiff;
        out[offset++] = targetVulnerability;
        out[offset++] = combatSpacing;
        out[offset++] = canCritical;
        out[offset++] = blockState;
    }

    public static int dim() {
        return 12;
    }
}