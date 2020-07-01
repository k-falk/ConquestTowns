package com.kfalk.conquesttowns.data;


public enum TownRank {

    SETTLEMENT(0),
    FACTION(50),
    TOWN(75),
    METROPOLIS(100),
    KINGDOM(150);

    TownRank(int defaultRadius) {
        this.radius = defaultRadius;
    }

    int radius;

    public void set(int radius) {
        this.radius = radius;
    }

    public int getRadius(){
        return radius;
    }

    public TownRank next() {
        if (this == SETTLEMENT) {
            return FACTION;
        } else if (this == FACTION) {
            return TOWN;
        } else if (this == TOWN) {
            return METROPOLIS;
        } else if (this == METROPOLIS) {
            return KINGDOM;
        } else {
            return null;
        }
    }

    @Override
    public String toString(){
        String s = super.toString();
        return s.substring(0, 1) + s.substring(1).toLowerCase();
    }

}
