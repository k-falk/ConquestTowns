package com.kfalk.conquesttowns.database;

import com.kfalk.conquesttowns.ConquestTowns;
import com.kfalk.conquesttowns.data.*;
import com.kfalk.conquesttowns.misc.FileUtils;
import com.kfalk.conquesttowns.permissions.PermissionFlag;
import com.kfalk.conquesttowns.permissions.PermissionSetting;
import com.kfalk.conquesttowns.terrain.TownTerrain;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class TownManager {

    private static final File townRoot = new File(ConquestTowns.root + File.separator + "Towns");
    public static ArrayList<UUID> townCreators = new ArrayList<UUID>();
    
    //hold all towns
    public static Map<UUID, Town> towns = new HashMap<UUID, Town>();

    private static ItemStack townChestItem;

    public static void init() {
        townRoot.mkdir();

        for (File dir : townRoot.listFiles()) {
            if (dir.isDirectory()) {
                //only process directories
                UUID uuid = UUID.fromString(dir.getName());
                towns.put(uuid, new Town(uuid));
            }
        }

        ConquestTowns.logger.info("Loaded [" + towns.size() + "] towns.");

        townChestItem = new ItemStack(Material.CHEST);
        ItemMeta meta = townChestItem.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Create a Town");
        meta.setLore(Arrays.asList(ChatColor.RESET + "" + ChatColor.BLUE + "Place this to create a town."));
        townChestItem.setItemMeta(meta);

        /*
            Each town defined in it's own folder, which is a UUID of the owners name

            File inside the town folder:
                - members.yml
                    -> member UUID's, manager UUID's, rank
                - config.yml
                    -> General stuff , chest location, war cooldown data
                - chest.dat - Serialised chest data contents
         */

        /*

            War-Cooldown: <long> ~ time after which this town can be at war again

            --- FLAG BASED PERMISSION SYSTEM ---
            <OWNER ONLY> <OWNER & MANAGERS> <ALL>

            - inviting players to the town
            - kicking players from the town
            - interacting with items inside the town
            - building inside the town
            - accessing the rewards chest
            - declaring war

         */
    }


    public static void destroyTown(Town town) {
        //send message to players
        town.broadcastMessage(null, ChatColor.RED + "This town has been destroyed!");

        //lower borders
        TownTerrain.setBorder(town, Material.AIR, 1);

        //remove from map
        towns.remove(town.getOwner());

        //now delete all files
        File localRoot = new File(ConquestTowns.towns_root + File.separator + town.getOwner().toString());
        FileUtils.deleteRecursive(localRoot);
    }

    public static Town getTownByName(String name) {
        for (Town t : towns.values()) {
            if (t.getTownName().equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

    public static Town getPlayerTown(UUID uuid) {
        for (Town town : towns.values()) {
            if (town.isMember(uuid)) {
                return town;
            }
        }
        return null;
    }

    public static Town getFromLocation(Location l) {
        for (Town town : towns.values()) {
            if (town.isInside(l)) {
                return town;
            }
        }
        return null;
    }

    public static boolean isNameUnique(String townName) {
        for (Town town : towns.values()) {
            if (town.getTownName().equalsIgnoreCase(townName)) {
                return false;
            }
        }
        return true;
    }

    public static void tryGiveTownChest(Player p, String name) {

        if (!isNameUnique(name)) {
            p.sendMessage(ChatColor.RED + "There is already a town by this name!");
            return;
        }

        UUID uuid = p.getUniqueId();

        File localRoot = new File(ConquestTowns.towns_root + File.separator + uuid.toString());
        if (localRoot.exists()) {
            p.sendMessage(ChatColor.RED + "You are already the owner of a town!");
            return;
        }

        //check if they belong to a town
        for (Town t : towns.values()) {
            if (t.isMember(uuid)) {
                p.sendMessage(ChatColor.RED + "You must leave your town before starting your own.");
                return;
            }
        }

        boolean pass = false;
        for (RankedGroup group : SettlementManager.settlements) {
            if (group.getOwner().equals(uuid)) {
                pass = true;
                break;
            }
        }

        if (!pass) {
            p.sendMessage(ChatColor.RED + "You must own a settlement to create a town.");
            return;
        }

        ItemStack chestItem = new ItemStack(townChestItem);
        ItemMeta meta = chestItem.getItemMeta();
        meta.setDisplayName(name + " Vault");
        /*List<String> lore = meta.getLore();
        lore.add(ChatColor.RESET + "Name:" + name);
        meta.setLore(lore);*/
        chestItem.setItemMeta(meta);
        
        if (p.getInventory().contains(chestItem)) {
            p.getInventory().remove(chestItem);
        }else if(townCreators.contains(p.getUniqueId())){
            p.sendMessage(ChatColor.GREEN + "You may not create more than one town at once.");
            return;
        }
        if (p.getInventory().firstEmpty() != -1) {
        	
            p.getInventory().addItem(chestItem);
            townCreators.add(p.getUniqueId());
            p.sendMessage(ChatColor.GREEN + "Place this chest to create your town.");
        }else {
            p.sendMessage(ChatColor.RED + "Your inventory is full!");

        }
       }
        
        
    

    public static boolean canPlace(Location l) {
        //use this method to check if a town can be placed, considering potential intersection with other towns future expansion

        //radius of each town is considered as the kingdom size (max rank) plus the additional spacing
        final int radius = TownRank.KINGDOM.getRadius() + GeneralConfig.additionalSpacing;

        for (Town t : towns.values()) {
            Loc loc = t.getTownChestLocation();

            double minX = loc.getX() - radius, minZ = loc.getZ() - radius,
                    maxX = loc.getX() + radius, maxZ = loc.getZ() + radius;

            //now check our town chest is not within this region
            if (l.getX() >= minX && l.getX() <= maxX && l.getZ() >= minZ && l.getZ() <= maxZ) {
                //overlap, so reject!
                return false;
            } else {
                //carry on and check the next town...
                continue;
            }
        }

        return true;
    }

    //TODO - this will be invoked by placing a chest, take all money and carry out checks at this point (Recieving the chest is free)
    public static boolean createTown(Player owner, Location chestPlaced, String name) {

        UUID uuid = owner.getUniqueId();

        File localRoot = new File(ConquestTowns.towns_root + File.separator + uuid.toString());
        if (localRoot.exists()) {
            owner.sendMessage(ChatColor.RED + "You are already the owner of a town!");
            return false;
        }

        if (!isNameUnique(name)) {
            owner.sendMessage(ChatColor.RED + "There is already a town by this name!");
            return false;
        }
        
        //check if they belong to a town
        for (Town t : towns.values()) {
            if (t.isMember(uuid)) {
                owner.sendMessage(ChatColor.RED + "You must leave your town before starting your own.");
                return false;
            }
        }

        //check if they belong to a settlement
        for (RankedGroup group : SettlementManager.settlements) {
            if (group.isMember(owner.getUniqueId())) {
                //they are in this settlement

                if (group.getOwner().equals(uuid)) {
                    //This is the owner, try and create the town!

                    if (group.getMembers().size() >= GeneralConfig.settlementMembersNeeded) {
                        //they have enough members

                        //but, do they have enough money?
                        if (ConquestTowns.economy.has(owner, GeneralConfig.townCreationCost)) {
                            //yes they do

                            //is the location valid?
                            if (canPlace(chestPlaced)) {

                                //take the money and create the town
                                if (ConquestTowns.economy.withdrawPlayer(owner, GeneralConfig.townCreationCost).transactionSuccess()) {

                                    //create it!

                                    //lets create the folders and files first, then add to our cache
                                    localRoot.mkdir();

                                    File membersFile = new File(localRoot + File.separator + "members.yml"),
                                            configFile = new File(localRoot + File.separator + "config.yml");

                                    try {
                                        membersFile.createNewFile();
                                        configFile.createNewFile();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    FileConfiguration io = YamlConfiguration.loadConfiguration(membersFile);

                                    for (Map.Entry<UUID, PlayerRank> e : group.getMembers().entrySet()) {
                                        io.set(e.getKey().toString(), e.getValue().toString());
                                    }

                                    try {
                                        io.save(membersFile);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    io = YamlConfiguration.loadConfiguration(configFile);

                                    io.set("chest.location", new Loc(chestPlaced).toString());
                                    io.set("town.rank", TownRank.FACTION.toString());
                                    io.set("town.war.last", 0);
                                    io.set("town.name", name);

                                    //default permissions
                                    io.set("permissions." + PermissionFlag.BUILD.toString(), PermissionSetting.MEMBER.toString());
                                    io.set("permissions." + PermissionFlag.BROADCAST.toString(), PermissionSetting.MEMBER.toString());
                                    io.set("permissions." + PermissionFlag.INTERACT.toString(), PermissionSetting.ALL.toString());
                                    io.set("permissions." + PermissionFlag.INVITE.toString(), PermissionSetting.MANAGER.toString());
                                    io.set("permissions." + PermissionFlag.KICK.toString(), PermissionSetting.MANAGER.toString());
                                    io.set("permissions." + PermissionFlag.WAR.toString(), PermissionSetting.OWNER.toString());
                                    io.set("permissions." + PermissionFlag.PROMOTE.toString(), PermissionSetting.OWNER.toString());
                                    io.set("permissions." + PermissionFlag.UPGRADE.toString(), PermissionSetting.OWNER.toString());
                                    io.set("permissions." + PermissionFlag.DESTROY.toString(), PermissionSetting.OWNER.toString());

                                    try {
                                        io.save(configFile);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                    //now delete the old settlement, in QUIET mode
                                    SettlementManager.disbandSettlement(owner, true);
                                    	
                                    //and now add to town manager cache
                                    Town town = new Town(uuid);
                                    TownManager.towns.put(uuid, town);

                                    //raise borders
                                    TownTerrain.setBorder(town, Material.STONE, 1);

                                    //notify player
                                    owner.sendMessage(ChatColor.GREEN + "Your town was created successfully for $" + GeneralConfig.townCreationCost);

                                    //remove from creation list
                                    if(townCreators.contains(owner.getUniqueId()))
                                    	townCreators.remove(owner.getUniqueId());
                                    
                                    return true;

                                } else {
                                    owner.sendMessage(ChatColor.RED + "Unknown transaction failure! Town creation cancelled.");
                                    return false;
                                }

                            } else {
                                owner.sendMessage(ChatColor.RED + "You are too close to another town! Please place your town elsewhere.");
                                return false;
                            }

                        } else {
                            owner.sendMessage(ChatColor.RED + "You need at least $" + GeneralConfig.townCreationCost + " to create a town.");
                        }

                    } else {
                        owner.sendMessage(ChatColor.RED + "You need at least " + GeneralConfig.settlementMembersNeeded + " players in your settlement to " +
                                "create a town.");
                    }

                } else {
                    owner.sendMessage(ChatColor.RED + "You must own a settlement to create a town.");
                }
            }
        }
        return false;
    }

    /*
        Invitation stuff
     */

    private static List<PlayerInvite> invites = new ArrayList<PlayerInvite>();

    public static void adminRemoveMember(Town town, Player p, String targetName) {
        RankedGroup group = town.getGroup();

        OfflinePlayer on = Bukkit.getPlayer(targetName);

        UUID target = null;

        if (on != null) {
            //this player is online
            target = on.getUniqueId();
        } else {
            //player is offline
            on = Bukkit.getOfflinePlayer(targetName);

            if (on != null) {
                target = on.getUniqueId();
            } else {
                p.sendMessage(ChatColor.RED + "No player by this name was found.");
                return;
            }
        }

        if (target.equals(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "You cannot remove yourself from the town.");
            p.sendMessage(ChatColor.RED + "/town destroy " + ChatColor.WHITE + "to destroy your town.");
            return;
        }

        if (group.isMember(target)) {
            group.removeMember(target);

            p.sendMessage(ChatColor.GREEN + on.getName() + " was removed from your town successfully.");
        } else {
            p.sendMessage(ChatColor.RED + "That player is not a member of your town.");
        }
    }

    public static void removeMember(Player p, String member) {

        Town belong = getPlayerTown(p.getUniqueId());

        if (belong == null) {
            p.sendMessage(ChatColor.RED + "You do not belong to a town.");
            return;
        } else if (!belong.canDo(PermissionFlag.KICK, p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "You do not have permission to do this.");
            return;
        }

        RankedGroup group = belong.getGroup();

        OfflinePlayer on = Bukkit.getPlayer(member);

        UUID target = null;

        if (on != null) {
            //this player is online
            target = on.getUniqueId();
        } else {
            //player is offline
            on = Bukkit.getOfflinePlayer(member);

            if (on != null) {
                target = on.getUniqueId();
            } else {
                p.sendMessage(ChatColor.RED + "No player by this name was found.");
                return;
            }
        }

        if (target.equals(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "You cannot remove yourself from the town.");
            p.sendMessage(ChatColor.RED + "/town destroy " + ChatColor.WHITE + "to destroy your town.");
            return;
        }

        if (group.isMember(target)) {
            group.removeMember(target);

            p.sendMessage(ChatColor.GREEN + on.getName() + " was removed from your town successfully.");
        } else {
            p.sendMessage(ChatColor.RED + "That player is not a member of your town.");
        }
    }
    public static void memberLeave(Player p) {

        Town belong = getPlayerTown(p.getUniqueId());

        if (belong == null) {
            p.sendMessage(ChatColor.RED + "You do not belong to a town.");
            return;
        } 

        RankedGroup group = belong.getGroup();
        if(group.getOwner().equals(p.getUniqueId())){
        	p.sendMessage(ChatColor.GREEN + "You cannot leave as the owner. You must disband your town or find a replacement!");
        }else if (group.isMember(p.getUniqueId())) {
            group.removeMember(p.getUniqueId());
            p.sendMessage(ChatColor.GREEN + "You have left " + belong.getTownName() + " .");
            sendMessageToTown(belong, ChatColor.GREEN + p.getName() + " has left " + belong.getTownName() + " .");
        }
    }

    public static void adminInvite(Town town, Player p, String targetName) {
        Player other = Bukkit.getPlayer(targetName);

        RankedGroup group = town.getGroup();

        if (other == null) {
            p.sendMessage(ChatColor.RED + "That player is not online.");
            return;
        }

        UUID target = other.getUniqueId();

        if (group.isMember(target)) {
            p.sendMessage(ChatColor.RED + "That player is already a member of your town.");
            return;
        }

        /*Town ofInvitee = TownManager.getPlayerTown(target);
        if (ofInvitee != null) {
            p.sendMessage(ChatColor.RED + "That player already belongs to a town!");
            return;
        }*/

        for (PlayerInvite inv : invites) {
            if (inv.getTarget().equals(target) && inv.getGroup().getOwner().equals(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "An invitation has already been sent to this player.");
                return;
            }
        }

        invites.add(new PlayerInvite(target, group, 120));

        p.sendMessage(ChatColor.GREEN + "Invitation sent successfully.");

        other.sendMessage(ChatColor.GREEN + "You have been invited to join the town - " + town.getTownName());
        other.sendMessage(ChatColor.GREEN + "/town join " + town.getTownName() + ChatColor.WHITE + " to join.");
        other.sendMessage(ChatColor.RED + "This invitation expires in 2 minutes.");
    }

    public static void inviteToTown(Player p, String targetName) {

        Town belong = getPlayerTown(p.getUniqueId());

        if (belong == null) {
            p.sendMessage(ChatColor.RED + "You do not belong to a town.");
            return;
        } else if (!belong.canDo(PermissionFlag.INVITE, p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "You do not have permission to do this.");
            return;
        }

        Player other = Bukkit.getPlayer(targetName);

        RankedGroup group = belong.getGroup();

        if (other == null) {
            p.sendMessage(ChatColor.RED + "That player is not online.");
            return;
        }

        UUID target = other.getUniqueId();

        if (group.isMember(target)) {
            p.sendMessage(ChatColor.RED + "That player is already a member of your town.");
            return;
        }

        /*Town ofInvitee = TownManager.getPlayerTown(target);
        if (ofInvitee != null) {
            p.sendMessage(ChatColor.RED + "That player already belongs to a town!");
            return;
        }*/

        for (PlayerInvite inv : invites) {
            if (inv.getTarget().equals(target) && inv.getGroup().getOwner().equals(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "You have already sent an invitation to this player.");
                return;
            }
        }

        invites.add(new PlayerInvite(target, group, 120));

        p.sendMessage(ChatColor.GREEN + "Invitation sent successfully.");

        other.sendMessage(ChatColor.GREEN + "You have been invited to join the town - " + belong.getTownName());
        other.sendMessage(ChatColor.GREEN + "/town join " + belong.getTownName() + ChatColor.WHITE + " to join.");
        other.sendMessage(ChatColor.RED + "This invitation expires in 2 minutes.");
    }

    public static void acceptTownInvite(Player p, String name) {
        Iterator<PlayerInvite> it = invites.iterator();

        if (getTownByName(name) == null) {
            p.sendMessage(ChatColor.RED + "There is no town by this name.");
            return;
        }

        for (RankedGroup group : SettlementManager.settlements) {
            if (group.isMember(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "You must leave your settlement before joining a town.");
                return;
            }
        }

        //are they in a settlement?
        for (Town town : towns.values()) {
            RankedGroup group = town.getGroup();
            if (group.isMember(p.getUniqueId())) {
                p.sendMessage(ChatColor.RED + "You must leave your current town before joining another.");
                return;
            }
        }

        while (it.hasNext()) {
            PlayerInvite inv = it.next();

            if (inv.getTarget().equals(p.getUniqueId())) {
                //could be a match
                if (inv.hasExpired()) {
                    p.sendMessage(ChatColor.RED + "This invitation has expired.");
                } else {

                    //lets check the groups match
                    for (Town town : towns.values()) {
                        if (town.getGroup().equals(inv.getGroup())) {
                            //groups match!
                            //add to town
                            inv.getGroup().addMember(p.getUniqueId());

                            //player is saved via callback message in RankedGroup.java
                            p.sendMessage(ChatColor.GREEN + "You have joined " + name + " successfully.");
                            sendMessageToTown(getTownByName(name),ChatColor.WHITE+ name + ChatColor.RED +  " //// " + ChatColor.WHITE + " Welcome " + p.getName() + "  to the town!");
                            it.remove();
                            return;
                        }
                    }

                }
                return;
            }
        }

    }
	public static void sendMessageToTown(Town town, String message){
		for(Player player : getOnlineMembers(town)){
				player.sendMessage(message);
		  }
	}
	 public static ArrayList<Player> getOnlineMembers(Town town){
		 ArrayList<Player> onlineMembers = new ArrayList<Player>();
		 for (Player p : Bukkit.getOnlinePlayers()) {
		      if (town.isMember(p.getUniqueId())) {
		    	  onlineMembers.add(p);
		      }
		    }
		 return onlineMembers;

		  }
    public static Town getTownFromChest(Location location) {

        final String rep = new Loc(location).toString();

        for (Town t : towns.values()) {
            if (t.getTownChestLocation().toString().equalsIgnoreCase(rep)) {
                return t;
            }
        }
    

        return null;
    }
    public static Chest getTownChest(Town t){
   	 Location loc = t.getTownChestLocation().getBukkitLocation();
   	 Block b = loc.getBlock();
   	 if(b.getState() instanceof Chest){
			Chest chest = (Chest) b.getState();
			return chest;

   	 }
   	 return null;
    }
}
