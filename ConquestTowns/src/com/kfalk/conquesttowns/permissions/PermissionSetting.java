package com.kfalk.conquesttowns.permissions;


public enum PermissionSetting {

    ALL(0),
    MEMBER(1),
    MANAGER(2),
    OWNER(3);

    final int weight;

    PermissionSetting(int weight) {
        this.weight = weight;
    }

    public static PermissionSetting parse(String value) {
        if (value.equalsIgnoreCase("all")) {
            return ALL;
        } else if (value.equalsIgnoreCase("member")) {
            return MEMBER;
        } else if (value.equalsIgnoreCase("manager")) {
            return MANAGER;
        } else {
            //Default to owner for security reasons
            return OWNER;
        }
    }

    public int getWeight(){
        return weight;
    }

}
