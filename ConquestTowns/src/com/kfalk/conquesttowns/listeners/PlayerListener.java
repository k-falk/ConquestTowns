package com.kfalk.conquesttowns.listeners;

import com.kfalk.conquesttowns.ConquestTowns;
import com.kfalk.conquesttowns.api.ConquestTownsAPI;
import com.kfalk.conquesttowns.data.Loc;
import com.kfalk.conquesttowns.data.PlayerRank;
import com.kfalk.conquesttowns.data.Town;
import com.kfalk.conquesttowns.data.TownMaterial;
import com.kfalk.conquesttowns.database.GeneralConfig;
import com.kfalk.conquesttowns.database.InventoryManager;
import com.kfalk.conquesttowns.database.TownManager;
import com.kfalk.conquesttowns.permissions.PermissionFlag;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.awt.Event;
import java.util.Map;
import java.util.Random;


public class PlayerListener implements Listener {

    public PlayerListener(Plugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, ConquestTowns.plugin);
    }

    @EventHandler(priority = EventPriority.LOW)
    private void interact(PlayerInteractEvent evt) {
        if (evt.getAction().equals(Action.LEFT_CLICK_BLOCK)) {

            Town town = TownManager.getFromLocation(evt.getClickedBlock().getLocation());

            if (town != null) {
                //interacting on a block within the town
            	Location location = town.getTownChestLocation().getBukkitLocation();
                if (location.equals(evt.getClickedBlock().getLocation())) {

                    //clicked on the town chest!!
                    if (town.getGroup().isMember(evt.getPlayer().getUniqueId())) {

                        if (town.isAtWar()) {
                            //player might have some rewards!
                            //scan for rewards
                            ItemStack reward = null;

                            for (ItemStack stack : evt.getPlayer().getInventory().getContents()) {
                                if (stack != null && InventoryManager.isChestRewardItem(stack)) {
                                    reward = stack;
                                    break;
                                }
                            }

                            if (reward != null) {
                                //remove
                                evt.getPlayer().getInventory().remove(reward);
                                evt.getPlayer().updateInventory();

                                ConquestTownsAPI.addRewards(town, TownMaterial.fromMaterial(reward.getType()), reward
                                        .getAmount());
                                evt.getPlayer().sendMessage(ChatColor.GREEN + "Stolen rewards have been successfully " +
                                        "deposited.");
                            }
                        }

                        if (town.getGroup().getMembers().get(evt.getPlayer().getUniqueId()).getWeight() >= PlayerRank
                                .MANAGER.getWeight()) {
                        	

                            //only managers and owners can open, but it's just a virtual view
                            Chest chest = (Chest) evt.getClickedBlock().getState();
                            new VirtualInventory(chest.getInventory().getContents(), evt.getPlayer());
                        } else {
                            evt.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to access the town chest.");
                        }
                       
                    } else if (town.isAtWar()) {
                        //player is not a member, and town is at war

                        if (town.isMemberOfWarTown(evt.getPlayer().getUniqueId())) {
                            //Steal some rewards

                            Map<TownMaterial, Integer> chest = town.getTownChestContents();

                            Random r = new Random(System.currentTimeMillis());

                            if (chest.isEmpty()) {
                                evt.getPlayer().sendMessage(ChatColor.RED + "This chest is empty.");
                            } else {
                                //chest is not empty

                                if (evt.getPlayer().getInventory().firstEmpty() == -1) {
                                    evt.getPlayer().sendMessage(ChatColor.RED + "You do not have enough " +
                                            "inventory space to raid this chest.");
                                    evt.setCancelled(true);
                                    return;
                                }

                                TownMaterial grab = chest.keySet().toArray(new TownMaterial[chest.size()])[r.nextInt
                                        (chest.size())];

                                int number = chest.get(grab);

                                int toTake = 0;

                                if (number <= GeneralConfig.minStackSteal) {
                                    //take all
                                    toTake = number;
                                } else {
                                    //generate a number
                                    int upperBound = GeneralConfig.maxStackSteal > number ? number : GeneralConfig.maxStackSteal;

                                    toTake = r.nextInt(upperBound) + GeneralConfig.minStackSteal;
                                }

                                town.removeFromTownChest(evt.getPlayer(), grab, toTake);

                                town.rewardsStolen();
                            }

                        } else {
                            evt.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to access the town chest.");
                        }

                    } else {
                        //player is not a member, and town is not at war
                        evt.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to access the town chest.");
                    }

                    evt.setCancelled(true);
                } else {
                    //not clicking on town chest, so run permissions as usual
                    if (!town.canDo(PermissionFlag.INTERACT, evt.getPlayer().getUniqueId())) {
                        evt.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to interact with this town.");
                        evt.setCancelled(true);
                        return;
                    }
                }

                /*
                    1) Have they clicked on the town chest
                    2) If no, then run permissions as usual

                    3) If yes
                        - If player belongs to town, then run permissions as usual
                            else if at war?
                                if member is of warring town then steal!
                            else deny permissions
                 */

            }
        }else if (evt.getAction().equals(Action.RIGHT_CLICK_BLOCK)){
        	 Town town = TownManager.getFromLocation(evt.getClickedBlock().getLocation());

             if (town != null) {
                 //interacting on a block within the town
             	Location location = town.getTownChestLocation().getBukkitLocation();
                 if (location.equals(evt.getClickedBlock().getLocation())) {
                	 evt.setCancelled(true);
                	 return;
                 }
             }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    private void breakBlock(BlockBreakEvent evt) {
        Town town = TownManager.getFromLocation(evt.getBlock().getLocation());
        if (town != null) {
            //run permissions
            if (!town.canDo(PermissionFlag.BUILD, evt.getPlayer().getUniqueId())) {
                evt.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to build in this town.");
                evt.setCancelled(true);
                return;
            } else if (town.isTooNearToChest(evt.getBlock().getLocation())) {
                evt.getPlayer().sendMessage(ChatColor.RED + "You cannot build so close to the town chest.");
                evt.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    private void placeBlock(BlockPlaceEvent evt) {
        //todo - town permission checks
        Town town = TownManager.getFromLocation(evt.getBlockPlaced().getLocation());
        if (town != null) {
            //run permissions

            if (town.isTooNearToChest(evt.getBlock().getLocation())) {
                evt.getPlayer().sendMessage(ChatColor.RED + "You cannot build so close to the town chest.");
                evt.setCancelled(true);
                return;
            }

            //is it TnT?
            if (evt.getBlockPlaced().getType().equals(Material.TNT)) {
                if (town.isMember(evt.getPlayer().getUniqueId())) {
                    evt.getPlayer().sendMessage(ChatColor.RED + "You cannot place TnT in your own town!");
                    evt.setCancelled(true);
                } else if (town.getMembers() < GeneralConfig.minPlayersRaiding) {
                    evt.getPlayer().sendMessage(ChatColor.RED + "This town does not have enough players online to be " +
                            "raided.");
                    evt.setCancelled(true);
                }
                return;
            } else if (!town.canDo(PermissionFlag.BUILD, evt.getPlayer().getUniqueId())) {
                evt.getPlayer().sendMessage(ChatColor.RED + "You do not have permission to build in this town.");
                evt.setCancelled(true);
                return;
            }
        }

        //now check town creation
        if (evt.getBlockPlaced().getType().equals(Material.CHEST)) {
            //now check it's our chest creation
            ItemStack placing = evt.getItemInHand();
            ItemMeta meta = placing.getItemMeta();
            if (meta.hasDisplayName() && ChatColor.stripColor(meta.getDisplayName()).endsWith("Vault")) {
                //this is a town chest!
                //get the town name
                String townName = ChatColor.stripColor(meta.getDisplayName().split(" ")[0].trim());
                if(evt.getBlock().getLocation().getY() >= 45){
                if (TownManager.createTown(evt.getPlayer(), evt.getBlockPlaced().getLocation(), townName)) {
                    //created successfully
                	for(Player online : Bukkit.getOnlinePlayers()){
                		online.sendMessage(ChatColor.GREEN + "Towns // " + ChatColor.WHITE + "A new town named " + townName + " has been created!");
                	}
                    evt.getPlayer().setItemInHand(null);
                } else {
                    evt.setCancelled(true);
                }
                }else{
                    evt.getPlayer().sendMessage(ChatColor.RED + "You must place your town chest above level 45!.");
                	evt.setCancelled(true);
                	
                }
            }
        }
    }

 
    @EventHandler
    private void chestBreak(BlockBreakEvent evt){
    	Block b = evt.getBlock();
    	if(b.getState() instanceof Chest){
    		Town town = TownManager.getFromLocation(b.getLocation());
    		if(town == null)
    			return;
    		evt.setCancelled(true);
    		;
    		
    	}
    }

    @EventHandler
    private void drop(PlayerDropItemEvent evt) {
        //Don't let players drop chest items
        if (InventoryManager.isChestRewardItem(evt.getItemDrop().getItemStack())) {
            evt.getPlayer().sendMessage(ChatColor.RED + "Town chest items cannot be dropped.");
            evt.setCancelled(true);
        }
    }
    @EventHandler
    private void onPlayerLogin(PlayerLoginEvent e){
    	Player p = e.getPlayer();
    	if(TownManager.getPlayerTown(p.getUniqueId())!= null){
    		Town t =  TownManager.getPlayerTown(p.getUniqueId());
    		TownManager.sendMessageToTown(t, ChatColor.WHITE + t.getTownName() + ChatColor.RED + "// " + ChatColor.WHITE + p.getName() + " has logged in!");
    	}
    	}
    


}
