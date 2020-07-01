package com.kfalk.conquesttowns.listeners;

import com.kfalk.conquesttowns.ConquestTowns;
import com.kfalk.conquesttowns.data.Town;
import com.kfalk.conquesttowns.database.TownManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.kitteh.tag.AsyncPlayerReceiveNameTagEvent;


public class TagColouring implements Listener {

    public TagColouring(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, ConquestTowns.plugin);
    }

    @EventHandler
    private void onNameTag(AsyncPlayerReceiveNameTagEvent evt) {
        Player receiver = evt.getPlayer(),
                broadcaster = evt.getNamedPlayer();

        Town town = TownManager.getPlayerTown(broadcaster.getUniqueId());

        if (town == null) {
            //player whos' tag is being coloured is not a member of a town, so break out
            return;
        } else {
            //lets see if these two are town members
            if (town.isMember(receiver.getUniqueId())) {
                //colour names green for both
                evt.setTag(ChatColor.GREEN + broadcaster.getName());
            } else if (town.isAtWar() && town.isMemberOfWarTown(receiver.getUniqueId())) {
                evt.setTag(ChatColor.RED + broadcaster.getName());
            }
        }
    }
}
