package com.kfalk.conquesttowns.data;

import java.util.UUID;


public class PlayerInvite {

    private final UUID target;
    private final RankedGroup group;

    private final long expiryTime;

    public PlayerInvite(UUID target, RankedGroup group, int expireSeconds) {
        this.target = target;
        this.group = group;
        this.expiryTime = System.currentTimeMillis() + (expireSeconds * 1000);
    }

    public UUID getTarget(){
        return target;
    }

    public RankedGroup getGroup(){
        return group;
    }

    public boolean hasExpired() {
        return System.currentTimeMillis() > expiryTime;
    }
}
