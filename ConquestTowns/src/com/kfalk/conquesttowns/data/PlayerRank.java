package com.kfalk.conquesttowns.data;


public enum PlayerRank {

    //default same as member
    DEFAULT(1),
    MANAGER(2),
    OWNER(3);

    final int weight;

    PlayerRank(int weight) {
        this.weight = weight;
    }

    public int getWeight(){
        return weight;
    }

}
