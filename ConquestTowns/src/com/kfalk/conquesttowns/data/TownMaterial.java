package com.kfalk.conquesttowns.data;

import org.bukkit.Material;


public enum TownMaterial {

    JEWEL(Material.DIAMOND, "Jewels"),
    STEEL(Material.IRON_BLOCK, "Steel"),
    ENERGY_CRYSTAL(Material.EMERALD, "Energy Crystals"),
    LUMBER(Material.LOG, "Lumber"),
    AFFLUENT_CRYSTAL(Material.PRISMARINE_CRYSTALS, "Affluent Crystals"),
    DIAMOND_SHARD(Material.PRISMARINE_SHARD, "Diamond Shards"),
    TOWN_LEASE(Material.PAPER, "Town Leases");

    private final Material material;
    private final String alias;

    TownMaterial(Material material, String alias) {
        this.material = material;
        this.alias = alias;
    }

    public String getAlias() {
        return alias;
    }

    public Material getMaterial(){
        return material;
    }

    public static TownMaterial fromMaterial(Material material) {
        for (TownMaterial mat : TownMaterial.values()) {
            if (mat.getMaterial().equals(material)) {
                return mat;
            }
        }
        return null;
    }
}
