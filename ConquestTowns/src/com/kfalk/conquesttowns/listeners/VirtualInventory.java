package com.kfalk.conquesttowns.listeners;

import com.kfalk.conquesttowns.ConquestTowns;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class VirtualInventory implements Listener {

    private static final String title = ChatColor.AQUA + "Town Chest";

    private Inventory inventory;

    private UUID uuid;

    public VirtualInventory(ItemStack[] contents, Player viewer) {
        uuid = viewer.getUniqueId();
        inventory = Bukkit.createInventory(viewer, 9 * 3, title);
        inventory.setContents(contents);

        Bukkit.getPluginManager().registerEvents(this, ConquestTowns.plugin);

        viewer.openInventory(inventory);
    }

    @EventHandler
    private void close(InventoryCloseEvent evt) {
        if (evt.getPlayer().getUniqueId().equals(uuid)) {
            //discard this
            HandlerList.unregisterAll(this);
        }
    }

    @EventHandler
    private void click(InventoryClickEvent evt) {
        if (evt.getWhoClicked().getUniqueId().equals(uuid)) {
            evt.setCancelled(true);
        }
    }

}
