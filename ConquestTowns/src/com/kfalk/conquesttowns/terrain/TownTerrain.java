package com.kfalk.conquesttowns.terrain;

import com.kfalk.conquesttowns.data.Town;
import org.bukkit.Location;
import org.bukkit.Material;



public class TownTerrain {

    public static void setBorder(Town town, Material material, int height){
        Location loc = town.getTownChestLocation().getBukkitLocation();

        int range = town.getTownRank().getRadius();

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {

                if (Math.abs(x) != range && Math.abs(z) != range) {
                    //only build along the border
                    continue;
                }

                for (int y = 1; y <= height; y++) {
                    loc.getBlock().getRelative(x, y, z).setType(material);
                }
            }
        }
    }
}
