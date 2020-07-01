package com.kfalk.conquesttowns.data;

import java.util.*;


public class RankedGroup {

    private final UUID owner;

    //including owner
    private Map<UUID, PlayerRank> members = new HashMap<UUID, PlayerRank>();

    private Runnable callback;

    //use this for settlements where our rank doesn't matter
    public RankedGroup(UUID owner, Collection<UUID> members) {
        this.owner = owner;
        for (UUID u : members) {
            this.members.put(u, PlayerRank.DEFAULT);
        }
        this.members.put(owner, PlayerRank.OWNER);
    }

    public RankedGroup(UUID owner) {
        this.owner = owner;
        this.members.put(owner, PlayerRank.OWNER);
    }

    public void addMember(UUID uuid, PlayerRank rank) {
        members.put(uuid, rank);
    }

    public void addMember(UUID uuid) {
        members.put(uuid, PlayerRank.DEFAULT);
        if (callback != null) {
            //callback to save the member list
            callback.run();
        }
    }

    public void registerCallback(Runnable r) {
        this.callback = r;
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
        if (callback != null) {
            //callback to save the member list
            callback.run();
        }
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public UUID getOwner() {
        return owner;
    }

    public Map<UUID, PlayerRank> getMembers() {
        return members;
    }

    public void setRank(UUID uuid, PlayerRank rank) {
        members.put(uuid, rank);
        if (callback != null) {
            //callback to save the member list
            callback.run();
        }
    }

    public String[] membersToStringArray(boolean includeOwner) {
        List<String> arr = new ArrayList<String>();
        for (UUID U : members.keySet()) {
            if (!includeOwner && U.equals(owner)) {
                continue;
            } else {
                arr.add(U.toString());
            }
        }
        return arr.toArray(new String[arr.size()]);
    }

}
