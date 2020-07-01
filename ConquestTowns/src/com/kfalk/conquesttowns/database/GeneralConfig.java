package com.kfalk.conquesttowns.database;

import com.kfalk.conquesttowns.ConquestTowns;
import com.kfalk.conquesttowns.data.TownMaterial;
import com.kfalk.conquesttowns.data.TownRank;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;



public class GeneralConfig {

    public static int settlementMembersNeeded, additionalSpacing, warCooldownSeconds, townCreationCost,
            warMembersNeeded, minStackSteal, maxStackSteal, minPlayersRaiding, vaultBuildDenyRadius;

    public static boolean debug;

    private static final File configRoot = new File(ConquestTowns.root + File.separator + "Configuration"), config = new File(configRoot + File.separator +
            "config.yml");

    public static Map<TownRank, Map<TownMaterial, Integer>> upgradeRequirements = new HashMap<TownRank, Map<TownMaterial, Integer>>();

    public static void init() {
        configRoot.mkdir();

        FileConfiguration io = YamlConfiguration.loadConfiguration(config);

        if (!config.exists()) {
            try {
                config.createNewFile();
                //number of members needed to convert a settlement into a town
                io.set("settlements.conversion.members.needed", 5);
                for (TownRank rank : TownRank.values()) {
                    String rS = rank.toString().toLowerCase();
                    io.set("town.rank." + rS + ".radius", rank.getRadius());

                    for (TownMaterial mat : TownMaterial.values()) {
                        io.set("town.rank." + rS + ".upgrade." + mat.toString().toLowerCase(), rank.getRadius());
                    }

                }

                //additional spacing to max possible upgrade size
                io.set("town.spacing.additional", 100);

                io.set("town.chest.build.deny.radius", 8);

                //1 hour after war ends before it can be started again
                io.set("war.cooldown.seconds", 3600);
                io.set("war.members.needed", 5);

                io.set("war.raid.chest.items.min", 5);
                io.set("war.raid.chest.items.max", 16);

                io.set("town.raid.min.members.online", 5);

                io.set("town.create.cost", 1000);

                io.set("debug", false);

                io.save(config);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        vaultBuildDenyRadius = io.getInt("town.chest.build.deny.radius");
        settlementMembersNeeded = io.getInt("settlements.conversion.members.needed");

        for (TownRank rank : TownRank.values()) {
            rank.set(io.getInt("town.rank." + rank.toString().toLowerCase() + ".radius"));

            Map<TownMaterial, Integer> map = new HashMap<TownMaterial, Integer>();

            for (TownMaterial mat : TownMaterial.values()) {
                int numberNeeded = io.getInt("town.rank." + rank.toString().toLowerCase() + ".upgrade." + mat.toString().toLowerCase());
                map.put(mat, numberNeeded);
            }

            upgradeRequirements.put(rank, map);

        }

        minPlayersRaiding = io.getInt("town.raid.min.members.online");
        additionalSpacing = io.getInt("town.spacing.additional");
        warCooldownSeconds = io.getInt("war.cooldown.seconds");
        warMembersNeeded = io.getInt("war.members.needed");
        minStackSteal = io.getInt("war.raid.chest.items.min");
        maxStackSteal = io.getInt("war.raid.chest.items.max");

        townCreationCost = io.getInt("town.create.cost");

        debug = io.getBoolean("debug");
    }
}
