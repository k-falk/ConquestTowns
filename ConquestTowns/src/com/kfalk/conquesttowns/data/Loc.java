package com.kfalk.conquesttowns.data;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class Loc {

    private String world, toString;

    private double x, y, z;

    private float yaw = 0, pitch = 0;

    public Loc(Location l) {
        if (l.getWorld() == null) {
            System.out.println("World is null!");
        }
        this.world = l.getWorld().getName();
        this.x = l.getBlockX();
        this.y = l.getBlockY();
        this.z = l.getBlockZ();
        this.yaw = l.getYaw();
        this.pitch = l.getPitch();
    }

    public Loc(String world, double x, double y, double z) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Loc(String world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public String toString() {
        if (toString == null) {
            toString = new StringBuilder().append(world).append(",").append(x).append(",").append(y).append(",").append(z).append(",").append(yaw).append(",")
                    .append(pitch).toString();
        }
        return toString;
    }

    public Loc(String in) {
        String[] args = in.split(",");
        world = args[0];
        x = Double.parseDouble(args[1]);
        y = Double.parseDouble(args[2]);
        z = Double.parseDouble(args[3]);
        if (args.length > 4) {
            yaw = Float.parseFloat(args[4]);
            pitch = Float.parseFloat(args[5]);
        }
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public String getWorld() {
        return world;
    }

    public Location getBukkitLocation() {
        return new Location(Bukkit.getWorld(this.world), x, y, z, yaw, pitch);
    }

    public double distance(Location l) {
        double dx = x - l.getX(), dy = y - l.getY(), dz = z - l.getZ();
        return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
    }

    public Loc subtract(Loc l) {
        return new Loc(l.getWorld(), x - l.getX(), y - l.getY(), z - l.getZ());
    }

    public void setWorld(String s) {
        this.world = world;
    }

    public Loc clone(String wn) {
        return new Loc(wn, x, y, z, yaw, pitch);
    }

}
