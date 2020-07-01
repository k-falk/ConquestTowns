package com.kfalk.conquesttowns.database;

import com.kfalk.conquesttowns.data.Town;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;


public class WarManager {

    /*
        If the server shutsdown then war ends! Server starts clean, all towns in peace!
     */

    public static void wageWar(Player p, Town town, String namedTown) {

        if (town.isAtWar()) {
            //do not allow
            p.sendMessage(ChatColor.RED + (town.getWarringWith().equalsIgnoreCase(namedTown) ? "You are already at " +
                    "war with this town!" : "You are already at war with another town!"));
        } else {

            Town named = TownManager.getTownByName(namedTown);

            if (named == null) {
                p.sendMessage(ChatColor.RED + "There is no town by that name.");
            } else if (named.getTownName().equalsIgnoreCase(town.getTownName())) {
                p.sendMessage(ChatColor.RED + "A town cannot go to war with itself.");
            } else if (named.isAtWar()) {
                p.sendMessage(ChatColor.RED + "That town is already at war.");
            } else {
                //wage war!

                if (town.getOnlinePlayers() < GeneralConfig.warMembersNeeded) {
                    p.sendMessage(ChatColor.RED + "You need at least " + GeneralConfig.warMembersNeeded + " town members online to wage war.");
                } else if (named.getOnlinePlayers() < GeneralConfig.warMembersNeeded) {
                    p.sendMessage(ChatColor.RED + "The other town does not have enough members online to go to war.");
                } else {

                    long diff = System.currentTimeMillis() - town.getLastWar();
                    long diffSeconds = (int) Math.floor(((double) diff) / (1000D));

                    if (diffSeconds < GeneralConfig.warCooldownSeconds) {
                        p.sendMessage(ChatColor.RED + "You cannot go to war so soon after finishing one.");
                        return;
                    }

                    diff = System.currentTimeMillis() - named.getLastWar();
                    diffSeconds = (int) Math.floor(((double) diff) / (1000D));

                    if (diffSeconds < GeneralConfig.warCooldownSeconds) {
                        p.sendMessage(ChatColor.RED + "That town has been in war recently, try again later.");
                        return;
                    }

                    town.wageWar(named.getTownName(), true);
                    named.wageWar(town.getTownName(), false);

                    Bukkit.broadcastMessage(ChatColor.RED + town.getTownName() + " has waged war upon " + named.getTownName());
                }
            }

        }

    }

}
