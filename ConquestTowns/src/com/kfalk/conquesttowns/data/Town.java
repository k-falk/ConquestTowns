package com.kfalk.conquesttowns.data;

import com.kfalk.conquesttowns.ConquestTowns;
import com.kfalk.conquesttowns.api.ConquestTownsAPI;
import com.kfalk.conquesttowns.database.GeneralConfig;
import com.kfalk.conquesttowns.database.InventoryManager;
import com.kfalk.conquesttowns.database.TownManager;
import com.kfalk.conquesttowns.permissions.PermissionFlag;
import com.kfalk.conquesttowns.permissions.PermissionSetting;
import com.kfalk.conquesttowns.terrain.TownTerrain;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;


public class Town {

    //This will hold the town owner, and all members
    private final RankedGroup group;

    private TownRank townRank;

    private Loc townChest;

    private long lastWar;

    private final String name;
    private final UUID owner;

    private Map<PermissionFlag, PermissionSetting> permissions = new HashMap<PermissionFlag, PermissionSetting>();

    private String warring = null;

    //used for quick checks, always the same!
    private double minXGLOBAL, maxXGLOBAL, minZGLOBAL, maxZGLOBAL;

    private double minXCURRENT, maxXCURRENT, minZCURRENT, maxZCURRENT;



    /*
            Each town defined in it's own folder, which is a UUID of the owners name

            File inside the town folder:
                - members.yml
                    -> member UUID's, manager UUID's
                - config.yml
                    -> General stuff , chest location, war cooldown data, town rank, permissions
         */

        /*

            --- FLAG BASED PERMISSION SYSTEM ---
            <OWNER ONLY> <OWNER & MANAGERS> <ALL>

            - inviting players to the town
            - kicking players from the town
            - interacting with items inside the town
            - building inside the town
            - accessing the rewards chest
            - declaring war

         */

    public Town(UUID owner) {
        this.owner = owner;

        final File localRoot = new File(ConquestTowns.towns_root + File.separator + owner.toString()),
                membersFile = new File(localRoot + File.separator + "members.yml"),
                configFile = new File(localRoot + File.separator + "config.yml");

        /*
            Assume town is already created and this is for loading!
         */
        localRoot.mkdir();

        FileConfiguration io = YamlConfiguration.loadConfiguration(membersFile);

        this.group = new RankedGroup(owner);

        for (String s : io.getStringList("managers")) {
            this.group.addMember(UUID.fromString(s), PlayerRank.MANAGER);
        }

        for (String s : io.getStringList("members")) {
            this.group.addMember(UUID.fromString(s), PlayerRank.DEFAULT);
        }

        this.group.registerCallback(new Runnable() {
            @Override
            public void run() {
                FileConfiguration io = YamlConfiguration.loadConfiguration(membersFile);

                List<String> members = new ArrayList<String>();
                List<String> managers = new ArrayList<String>();

                for (Map.Entry<UUID, PlayerRank> e : group.getMembers().entrySet()) {
                    if (e.getValue().equals(PlayerRank.DEFAULT)) {
                        members.add(e.getKey().toString());
                    } else if (e.getValue().equals(PlayerRank.MANAGER)) {
                        managers.add(e.getKey().toString());
                    }
                }

                //now save all to members file
                io.set("managers", managers);
                io.set("members", members);
                try {
                    io.save(membersFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        //now config file
        io = YamlConfiguration.loadConfiguration(configFile);
        townChest = new Loc(io.getString("chest.location"));
        townRank = TownRank.valueOf(io.getString("town.rank").toUpperCase());
        lastWar = io.getLong("town.war.last");
        name = io.getString("town.name");
        
        final int radius = TownRank.KINGDOM.getRadius() + GeneralConfig.additionalSpacing;

        Loc loc = this.getTownChestLocation();

        minXGLOBAL = loc.getX() - radius;
        minZGLOBAL = loc.getZ() - radius;
        maxXGLOBAL = loc.getX() + radius;
        maxZGLOBAL = loc.getZ() + radius;

        minXCURRENT = loc.getX() - townRank.getRadius();
        minZCURRENT = loc.getZ() - townRank.getRadius();
        maxXCURRENT = loc.getX() + townRank.getRadius();
        maxZCURRENT = loc.getZ() + townRank.getRadius();

        //load permissions
        for (PermissionFlag flag : PermissionFlag.values()) {
            permissions.put(flag, PermissionSetting.parse(io.getString("permissions." + flag.toString())));
        }
    }

    public long getLastWar() {
        return lastWar;
    }

    private WarInstance warInstance;

    public void rewardsStolen() {
        if (this.warInstance != null) {
            this.warInstance.rewardsStolen(this);
        }
    }

    public void wageWar(String town, boolean initiate) {
        this.warring = town;
        this.lastWar = System.currentTimeMillis();

        final File localRoot = new File(ConquestTowns.towns_root + File.separator + owner.toString()),
                configFile = new File(localRoot + File.separator + "config.yml");

        FileConfiguration io = YamlConfiguration.loadConfiguration(localRoot);
        io.set("town.war.last", lastWar);
        try {
            io.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //broadcast!!
       /* if (initiate) {
            broadcastMessage(ChatColor.RED + "You have waged war upon " + town + "!");
        } else {
            broadcastMessage(ChatColor.RED + town + " has waged war against you!");
        }*/

        this.warInstance = new WarInstance(this, TownManager.getTownByName(town));
    }

    public void forceSetPeace(){
        warInstance.endWar();
        setPeace();
    }

    public void setPeace() {
        this.warring = null;
        this.warInstance = null;
    }

    public boolean isAtWar() {
        return warring != null;
    }

    public String getWarringWith() {
        return warring;
    }

    @SuppressWarnings("deprecation")
	public void forceOwner(String newOwner, Player p) {
        OfflinePlayer on = Bukkit.getPlayer(newOwner);

        if (on == null) {
            on = Bukkit.getOfflinePlayer(newOwner);

            if (on == null) {
                p.sendMessage(ChatColor.RED + "No player by that name was found.");
                return;
            }
        }
        //make owner default
        group.setRank(owner, PlayerRank.DEFAULT);

        //make new player owner
        group.setRank(on.getUniqueId(), PlayerRank.OWNER);


        //will save automatically due to callback
    }

    public void removeFromTownChest(Player p, TownMaterial material, int take) {
        Map<TownMaterial, Integer> contents = getTownChestContents();
        int am = contents.get(material);

        if (take >= am) {
            take = am;
            contents.remove(material);
        } else {
            contents.put(material, am - take);
        }

        //now remove this from the chest, easier to remove from map, and then refill chest
        ItemStack[] populate = new ItemStack[9 * 3];
        int index = 0;
        for (Map.Entry<TownMaterial, Integer> e : contents.entrySet()) {
            populate[index] = ConquestTownsAPI.getRewardStack(e.getKey(), e.getValue());
            index++;
        }

        Block block = townChest.getBukkitLocation().getBlock();

        Chest chest = (Chest) block.getState();

        chest.getInventory().setContents(populate);
        chest.update();

        //give player items
        ItemStack stack = ConquestTownsAPI.getRewardStack(material, take);
        p.getInventory().addItem(stack);
        p.updateInventory();

        p.sendMessage(ChatColor.GREEN + "You have stolen rewards from the chest!");

        InventoryManager.collateChestInventory(this);
    }

    public boolean isTooNearToChest(Location l) {
        return l.distance(townChest.getBukkitLocation()) <= GeneralConfig.vaultBuildDenyRadius;
    }

    public Map<TownMaterial, Integer> getTownChestContents() {

        InventoryManager.collateChestInventory(this);

        Block block = townChest.getBukkitLocation().getBlock();

        Map<TownMaterial, Integer> map = new HashMap<TownMaterial, Integer>();

        if (block.getState() instanceof Chest) {

            Chest chest = (Chest) block.getState();

            for (ItemStack stack : chest.getInventory().getContents()) {
                if (stack == null) {
                    continue;
                } else {
                    TownMaterial material = TownMaterial.fromMaterial(stack.getType());
                    if (material == null) {
                        ConquestTowns.logger.severe("Found unrecognised item in town chest for town [" + name + "]!!");
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

            return map;

        } else {
            ConquestTowns.logger.severe("Chest was not found at required location for town [" + name + "]!");
            return null;
        }
    }

    public void forceUpgrade(Player p) {

        if (townRank.equals(TownRank.KINGDOM)) {
            p.sendMessage(ChatColor.RED + "This town cannot be upgraded further.");
            return;
        }

        TownRank next = townRank.next();

        //now remove borders
        TownTerrain.setBorder(this, Material.AIR, 1);

        //upgrade rank
        townRank = next;
        
        //change regions
        Loc loc = this.getTownChestLocation();


        this.minXCURRENT = loc.getX() - townRank.getRadius();
        this.minZCURRENT = loc.getZ() - townRank.getRadius();
        this.maxXCURRENT = loc.getX() + townRank.getRadius();
        //re-erect borders
        TownTerrain.setBorder(this, Material.OBSIDIAN, 1);

        //update config
        File localRoot = new File(ConquestTowns.towns_root + File.separator + owner.toString()),
                configFile = new File(localRoot + File.separator + "config.yml");

        FileConfiguration io = YamlConfiguration.loadConfiguration(configFile);
        io.set("town.rank", townRank.toString().toLowerCase());
        try {
            io.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        broadcastMessage(null, ChatColor.GREEN + "Your town has been upgraded and is now classed as a " + ChatColor.YELLOW + townRank.toString());

        p.sendMessage(ChatColor.GREEN + "Town '" + name + "' has been upgraded to a " + next.toString());
    }

    //player who is executing the command
    public void upgrade(Player p) {

        if (!canDo(PermissionFlag.UPGRADE, p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "You do not have permission to upgrade this town.");
            return;
        }

        if (townRank.equals(TownRank.KINGDOM)) {
            p.sendMessage(ChatColor.RED + "Your town cannot be upgraded any further.");
        } else {
            TownRank next = townRank.next();

            Map<TownMaterial, Integer> required = GeneralConfig.upgradeRequirements.get(townRank);
            Map<TownMaterial, Integer> chestContents = getTownChestContents();

            //can we upgrade?
            Map<TownMaterial, Integer> missing = new HashMap<TownMaterial, Integer>();

            for (Map.Entry<TownMaterial, Integer> e : required.entrySet()) {
                if (chestContents.containsKey(e.getKey())) {

                    int inChest = chestContents.get(e.getKey());

                    if (inChest < e.getValue()) {
                        //need more!
                        missing.put(e.getKey(), e.getValue() - inChest);
                    }

                } else {
                    //none of this in chest
                    missing.put(e.getKey(), e.getValue());
                }
            }

            if (missing.isEmpty()) {
                //upgrade town, and remove stuff from chest!

                Chest chest = (Chest) townChest.getBukkitLocation().getBlock().getState();

                //first remove the stuff from the chest, take this out of the required map
                for (Map.Entry<TownMaterial, Integer> e : required.entrySet()) {
                    removeN(chest.getInventory(), e.getKey().getMaterial(), e.getValue());
                }

                chest.update();

                //now remove borders
                TownTerrain.setBorder(this, Material.AIR, 1);

                //upgrade rank
                townRank = next;
                Loc loc = this.getTownChestLocation();


                this.minXCURRENT = loc.getX() - townRank.getRadius();
                this.minZCURRENT = loc.getZ() - townRank.getRadius();
                this.maxXCURRENT = loc.getX() + townRank.getRadius();
                this.maxZCURRENT = loc.getZ() + townRank.getRadius();
                
                //re-erect borders
                TownTerrain.setBorder(this, Material.OBSIDIAN, 1);

                //update config
                File localRoot = new File(ConquestTowns.towns_root + File.separator + owner.toString()),
                        configFile = new File(localRoot + File.separator + "config.yml");

                FileConfiguration io = YamlConfiguration.loadConfiguration(configFile);
                io.set("town.rank", townRank.toString().toLowerCase());
                try {
                    io.save(configFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                broadcastMessage(null, ChatColor.GREEN + "Your town has been upgraded and is now classed as a " + ChatColor.YELLOW + townRank.toString());

            } else {
                //we are short!
                p.sendMessage(ChatColor.RED + "You do not have the required material to upgrade your town.");
                for (Map.Entry<TownMaterial, Integer> e : missing.entrySet()) {
                    p.sendMessage(ChatColor.RED + "Need " + ChatColor.WHITE + e.getValue() + ChatColor.RED + " more " + ChatColor.YELLOW + e.getKey().getAlias());
                }
                return;
            }
        }
    }

    private void removeN(Inventory inventory, Material material, int n) {
        Iterator<ItemStack> it = inventory.iterator();

        while (it.hasNext()) {
            ItemStack stack = it.next();
            if (stack == null) {
                continue;
            } else if (stack.getType().equals(material)) {

                int amount = stack.getAmount();

                if (n > amount) {
                    //remove the whole stack, and subtract from N
                    it.remove();
                    n -= amount;
                } else {
                    //only remove the difference
                    int leave = amount - n;
                    stack.setAmount(leave);
                    return;
                }

            }
        }
    }

    public TownRank getTownRank() {
        return townRank;
    }

    public boolean isMember(UUID uuid) {
        return group.isMember(uuid);
    }

    public boolean isMemberOfWarTown(UUID uuid) {
        return TownManager.getTownByName(warring).isMember(uuid);
    }

    public Loc getTownChestLocation() {
        return townChest;
    }

    public RankedGroup getGroup() {
        return group;
    }

    public String getTownName() {
        return name;
    }

    public boolean canDo(PermissionFlag action, UUID uuid) {
        PermissionSetting setting = permissions.get(action);

        int weight = isMember(uuid) ? group.getMembers().get(uuid).getWeight() : 0;

        return weight >= setting.getWeight();

        //0 is all, 3 is owner
        //anyone can do action if they have equal or higher weight
    }

    public boolean isInside(Location l) {
        return l.getBlockX() >= minXCURRENT && l.getBlockX() <= maxXCURRENT && l.getBlockZ() >= minZCURRENT && l.getBlockZ() <= maxZCURRENT;
    }

    public boolean isInsideMAXBounds(Location l) {
        return l.getBlockX() >= minXGLOBAL && l.getBlockX() <= maxXGLOBAL && l.getBlockZ() >= minZGLOBAL && l.getBlockZ() <= maxZGLOBAL;
    }

    public void broadcastMessage(String sender, String message) {
        String sendMSG;
        if(sender != null) {
             sendMSG = name + ChatColor.DARK_GRAY + " | " + ChatColor.GREEN + sender + ChatColor.RED + " // " + ChatColor.RESET + message;
        } else {
            sendMSG = name + ChatColor.RED + " // " + ChatColor.RESET + message;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isMember(p.getUniqueId())) {
                //<Town> | <Player> //
                p.sendMessage(sendMSG);
            }
        }
    }

    public UUID getOwner() {
        return owner;
    }

    public int getMembers() {
        return group.getMembers().size();
    }

    public Map<PermissionFlag, PermissionSetting> getPermissions() {
        return permissions;
    }

    public String getFormattedMemberList() {
        StringBuilder members = new StringBuilder();
        for (UUID uuid : group.getMembers().keySet()) {
        	if(getGroup().getMembers().get(uuid).getWeight() == PlayerRank
                    .MANAGER.getWeight()){
                members.append(ChatColor.ITALIC).append(Bukkit.getOfflinePlayer(uuid).getName()).append(", ");

        	}else if(getGroup().getMembers().get(uuid).getWeight() == PlayerRank
                    .OWNER.getWeight()){
             members.append(ChatColor.BOLD).append(Bukkit.getOfflinePlayer(uuid).getName()).append(", ");

        	}else{
            members.append(Bukkit.getOfflinePlayer(uuid).getName()).append(", ");
        }}
        	
        members.setLength(members.length() - 2);
        return members.toString();
    }

    public void printInfo(Player p) {
        p.sendMessage(ChatColor.GREEN + "----- " + name + " [INFO] -----");
        p.sendMessage("X: " + ((int) townChest.getX()) + " Y: " + ((int) townChest.getY()) + " Z: " + ((int) townChest.getZ()));
        p.sendMessage(ChatColor.YELLOW + "Town Rank: " + ChatColor.RESET + townRank.toString());
        p.sendMessage(ChatColor.YELLOW + "Members: " + ChatColor.RESET + getFormattedMemberList());
    }

    public void updatePermissions(PermissionFlag flag, PermissionSetting setting) {
        permissions.put(flag, setting);

        final File localRoot = new File(ConquestTowns.towns_root + File.separator + owner.toString()),
                configFile = new File(localRoot + File.separator + "config.yml");

        FileConfiguration io = YamlConfiguration.loadConfiguration(configFile);
        io.set("permissions." + flag.toString(), setting.toString());
        try {
            io.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getOnlinePlayers() {
        int count = 0;
        for (Player on : Bukkit.getOnlinePlayers()) {
            if (group.isMember(on.getUniqueId())) {
                count++;
            }
        }
        return count;
    }

}
