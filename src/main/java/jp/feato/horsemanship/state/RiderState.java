package jp.feato.horsemanship.state;

import java.util.UUID;

public final class RiderState {
    public UUID mount;
    public UUID movementMount;
    public double movementDistance;
    public double distanceOnMount;
    public long mountedSince;
    public long lastCombatExperience;
    public long urgeStart;
    public long urgeActive;
    public long urgeEnd;
    public long cooldownEnd;
    public long fatigueEnd;
    public long oneAsOneEnd;
    public double lastUrgeBonus;
    public boolean firstImpactUsed;
    public boolean urgeEndingProcessed;
    public long lastFeedback;
    public double lastHorizontalSpeed;
    public boolean wasGrounded = true;
    public boolean jumpBoostApplied;
    public void clearMount() {
        mount = null;
        movementMount = null;
        movementDistance = 0;
        distanceOnMount = 0;
        mountedSince = 0;
        urgeStart = urgeActive = urgeEnd = fatigueEnd = oneAsOneEnd = 0;
        lastUrgeBonus = 0;
        firstImpactUsed = false;
        lastHorizontalSpeed = 0;
        wasGrounded = true;
        jumpBoostApplied = false;
        urgeEndingProcessed = false;
    }
}
