package com.kfalk.conquesttowns.api;

import com.kfalk.conquesttowns.data.Town;
import com.kfalk.conquesttowns.data.TownMaterial;
import com.kfalk.conquesttowns.database.InventoryManager;
import com.kfalk.conquesttowns.database.TownManager;
import org.bukkit.ChatColor;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.UUID;


public class ConquestTownsAPI {

    public static Town getTown(Player player) {
        return TownManager.getPlayerTown(player.getUniqueId());
    }

    public static Town getTown(UUID uuid) {
        return TownManager.getPlayerTown(uuid);
    }

    public static Town getTownByName(String townName) {
        return TownManager.getTownByName(townName);
    }

    /**
     * Returns an int, representing how many items failed to be added to the chest, due to the town chest being full.
     * Operation has completed successfully if this method returns 0, indicating all rewards were added successfully.
     * @param town
     * @param material
     * @param quantity
     * @return
     */
    public static int addRewards(Town town, TownMaterial material, int quantity) {
        InventoryManager.collateChestInventory(town);

        Chest chest = (Chest) town.getTownChestLocation().getBukkitLocation().getBlock().getState();

        while (quantity > 0) {
            if (chest.getInventory().firstEmpty() == -1) {
                //town chest is full!
                return quantity;
            }

            if (quantity <= 64) {
                //final stack
                ItemStack stack = new ItemStack(material.getMaterial(), quantity);
                ItemMeta meta = stack.getItemMeta();
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + material.getAlias());
                stack.setItemMeta(meta);

                chest.getInventory().addItem(stack);
                break;
            } else {

                quantity -= 64;
                ItemStack stack = new ItemStack(material.getMaterial(), 64);
                ItemMeta meta = stack.getItemMeta();
                meta.setDisplayName(ChatColor.LIGHT_PURPLE + material.getAlias());
                stack.setItemMeta(meta);

                chest.getInventory().addItem(stack);
            }
        }

        chest.update();

        InventoryManager.collateChestInventory(town);

        return 0;
    }

    public static ItemStack getRewardStack(TownMaterial material, int amount) {
        ItemStack stack = new ItemStack(material.getMaterial(), amount);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + material.getAlias());
        stack.setItemMeta(meta);
        return stack;
    }

}
