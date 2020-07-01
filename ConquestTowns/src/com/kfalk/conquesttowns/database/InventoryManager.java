package com.kfalk.conquesttowns.database;

import com.kfalk.conquesttowns.ConquestTowns;
import com.kfalk.conquesttowns.api.ConquestTownsAPI;
import com.kfalk.conquesttowns.data.Town;
import com.kfalk.conquesttowns.data.TownMaterial;
import org.bukkit.ChatColor;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;


public class InventoryManager {

    public static boolean isChestRewardItem(ItemStack stack) {
        //assume stack is non-null
        TownMaterial mat = TownMaterial.fromMaterial(stack.getType());
       if(stack.hasItemMeta() && stack.getItemMeta().hasDisplayName() && ChatColor.stripColor(stack.getItemMeta().getDisplayName()).endsWith("Vault")){
            //check lore
            return true;
        } else if (mat  != null) {
            //not a town material
            return true;
        }
        return false;
    }

    public static void collateChestInventory(Town town) {
        Map<TownMaterial, Integer> map = new HashMap<TownMaterial, Integer>();

        Chest chest = ((Chest) town.getTownChestLocation().getBukkitLocation().getBlock().getState());
        Inventory inventory = chest.getInventory();

        for (ItemStack stack : inventory.getContents()) {
            if (stack == null) {
                continue;
            } else {
                TownMaterial material = TownMaterial.fromMaterial(stack.getType());
                if (material == null) {
                    ConquestTowns.logger.severe("Found unrecognised item in town chest for town [" + town.getTownName() + "]!!");
                } else {
                    int am = stack.getAmount();

                    if (map.containsKey(material)) {
                        map.put(material, map.get(material) + am);
                    } else {
                        map.put(material, am);
                    }
                }
            }

        }

        ItemStack[] populate = new ItemStack[9 * 3];
        int index = 0;
        for (Map.Entry<TownMaterial, Integer> e : map.entrySet()) {
            populate[index] = ConquestTownsAPI.getRewardStack(e.getKey(), e.getValue());
            index++;
        }

        inventory.setContents(populate);
        chest.update();
    }

}