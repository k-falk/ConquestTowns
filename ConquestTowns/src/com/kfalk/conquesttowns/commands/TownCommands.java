package com.kfalk.conquesttowns.commands;

import com.kfalk.conquesttowns.data.*;
import com.kfalk.conquesttowns.database.GeneralConfig;
import com.kfalk.conquesttowns.database.SettlementManager;
import com.kfalk.conquesttowns.database.TownManager;
import com.kfalk.conquesttowns.database.WarManager;
import com.kfalk.conquesttowns.permissions.PermissionFlag;
import com.kfalk.conquesttowns.permissions.PermissionSetting;
import com.kfalk.conquesttowns.terrain.TownTerrain;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;


public class TownCommands implements CommandExecutor {

    // [] denotes optional parameters

    private static final String[] help = new String[]{
    		//Member commands
    		ChatColor.YELLOW + "  /town join <town-name>" + ChatColor.DARK_GRAY + " - Join a town after you are invited",
            ChatColor.YELLOW + "  /town members [town-name]" + ChatColor.DARK_GRAY + " - Get the members of a town",
            ChatColor.YELLOW + "  /town who <player>" + ChatColor.DARK_GRAY + " - Get the town of a player",
        	ChatColor.YELLOW + "  /town leave" + ChatColor.DARK_GRAY + " - Leave your town",
            ChatColor.YELLOW + "  /town list" + ChatColor.DARK_GRAY + " - List the towns of the server",
            ChatColor.YELLOW + "  /town location" + ChatColor.DARK_GRAY + " - Get the location of your town",

            
            ChatColor.YELLOW + "  /town border true/false" + ChatColor.DARK_GRAY + " - Sets Town border",
            ChatColor.YELLOW + "  /town create <name>" + ChatColor.DARK_GRAY + " - Creates town with given name",
            ChatColor.YELLOW + "  /town invite <player>" + ChatColor.DARK_GRAY + " - Invite a player to your town",
            ChatColor.YELLOW + "  /town kick <player>" + ChatColor.DARK_GRAY + " - Kick player from your town",
            ChatColor.YELLOW + "  /town setrank <player> MANAGER/MEMBER" + ChatColor.DARK_GRAY + " - Set the rank of a player to member or manager",
            ChatColor.YELLOW + "  /town destroy" + ChatColor.DARK_GRAY + " - Destroy your town",
            ChatColor.YELLOW + "  /town upgrade" + ChatColor.DARK_GRAY + " - Upgrade your town",
            ChatColor.YELLOW + "  /town online [town-name]" + ChatColor.DARK_GRAY + " - Get the online members of a town",
            ChatColor.YELLOW + "  /town ranks" + ChatColor.DARK_GRAY + " - Get the ranks of the towns on the server",
            ChatColor.YELLOW + "  /town upgrades <TOWN/METROPOLIS/KINGDOM>" + ChatColor.DARK_GRAY + " - Get the required materials for each upgrade",
            ChatColor.YELLOW + "  /town war <town-name>" + ChatColor.DARK_GRAY + " - Declare war on a town",
            ChatColor.YELLOW + "  /town info <town-name>" + ChatColor.DARK_GRAY + " - Get the Location, Rank and Members of a town",
            ChatColor.YELLOW + "  /town broadcast <message>" + ChatColor.DARK_GRAY + " - Broadcast a message to your town",
            //only owner can run the permissions command
            ChatColor.YELLOW + "  /town permissions" + ChatColor.DARK_GRAY + " - Get the permissions of each rank",
            ChatColor.YELLOW + "  /town permissions <flag> <rank>" + ChatColor.DARK_GRAY + " - Set the permissions of each rank"};

    private Map<UUID, Long> destroyConfirm = new HashMap<UUID, Long>();
    private Map<UUID, Long> leaveConfirm = new HashMap<UUID, Long>();
    
    private final PageGen pageGen;

    public TownCommands() {
        pageGen = new PageGen(Arrays.asList(help), 8);
    }

    @SuppressWarnings("deprecation")
	@Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if (command.getName().equalsIgnoreCase("town")) {

            if (commandSender instanceof Player) {
                Player p = (Player) commandSender;
                try{
                if (args.length == 2 && args[0].equalsIgnoreCase("create")) {

                    TownManager.tryGiveTownChest(p, args[1]);
                    
                    return true;
                } else if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {

                    TownManager.inviteToTown(p, args[1]);

                    return true;
                } else if (args.length == 2 && args[0].equalsIgnoreCase("kick")) {

                    TownManager.removeMember(p, args[1]);

                    return true;
                } else if (args.length == 2 && args[0].equalsIgnoreCase("join")) {

                    TownManager.acceptTownInvite(p, args[1]);

                    return true;
                }else if(args.length >= 1 && args[0].equalsIgnoreCase("leave")){
                	if(TownManager.getPlayerTown(p.getUniqueId())== null){
                        p.sendMessage(ChatColor.RED + "You do not belong to a town.");
                	}else{                		
                		p.sendMessage(ChatColor.RED + "Are you sure you'd like to leave " + TownManager.getPlayerTown(p.getUniqueId()) + " ?");
                		p.sendMessage(ChatColor.RED + "Type " + ChatColor.YELLOW + "/town leave confirm" + ChatColor.RED + " to confirm. ");
                		leaveConfirm.put(p.getUniqueId(),System.currentTimeMillis());
                		

                	}
                }
                	if(args.length==2 && args[1].equalsIgnoreCase("confirm")){
                if(leaveConfirm.containsKey(p.getUniqueId())){
                		long t = leaveConfirm.get(p.getUniqueId());
                        if (System.currentTimeMillis() - t > 30000) {
                        p.sendMessage(ChatColor.RED + "More than 30 seconds has passed! Town leave command invalidated.");
                		leaveConfirm.remove(p.getUniqueId());
                	}else {
                        //destroy!
                		leaveConfirm.remove(p.getUniqueId());
                        TownManager.memberLeave(p);
                    }
                	
                }
                } else if (args.length == 3 && args[0].equalsIgnoreCase("setrank")) {

                    Town belong = TownManager.getPlayerTown(p.getUniqueId());

                    if (belong == null) {
                        p.sendMessage(ChatColor.RED + "You do not belong to a town.");
                    } else if (!belong.getOwner().equals(p.getUniqueId())) {
                        p.sendMessage(ChatColor.RED + "Only the owner of a town may promote members.");
                    } else {

                        boolean promote = true;

                        if (args[2].equalsIgnoreCase("manager")) {
                            promote = true;
                        } else if (args[2].equalsIgnoreCase("member")) {
                            promote = false;
                        } else {
                            p.sendMessage(ChatColor.RED + "Invalid rank, select either MEMBER or MANAGER");
                            return true;
                        }

                        //check for player existance
                        OfflinePlayer player = Bukkit.getPlayer(args[1]);

                        if (player == null) {
                            //try get offline
                            player = Bukkit.getOfflinePlayer(args[1]);
                        }

                        if (player == null) {
                            p.sendMessage(ChatColor.RED + "No player by the name '" + args[1] + "' could be found.");
                            return true;
                        }

                        //player is not null, but does he belong to our town
                        Town targetTown = TownManager.getPlayerTown(player.getUniqueId());

                        if (targetTown == null || !targetTown.getTownName().equalsIgnoreCase(belong.getTownName())) {
                            p.sendMessage(ChatColor.RED + "'" + player.getName() + "' is not a member of your town.");
                            return true;
                        }

                        PlayerRank rank = belong.getGroup().getMembers().get(player.getUniqueId());

                        if (rank.equals(PlayerRank.DEFAULT) && !promote) {
                            p.sendMessage(ChatColor.RED + "This player is already a member.");
                        } else if (rank.equals(PlayerRank.MANAGER) && promote) {
                            p.sendMessage(ChatColor.RED + "This player is already a manager.");
                        } else {
                            //set rank
                            belong.getGroup().setRank(player.getUniqueId(), promote ? PlayerRank.MANAGER : PlayerRank.DEFAULT);

                            if (promote) {
                                p.sendMessage(player.getName() + ChatColor.GREEN + " was promoted to manager rank.");
                            } else {
                                p.sendMessage(player.getName() + ChatColor.RED + " was demoted to member rank.");
                            }
                        }

                    }

                    return true;
                } else if (args.length == 1 && args[0].equalsIgnoreCase("destroy")) {

                    Town belong = TownManager.getPlayerTown(p.getUniqueId());

                    if (belong == null) {
                        p.sendMessage(ChatColor.RED + "You do not belong to a town.");
                    } else {
                        if (belong.canDo(PermissionFlag.DESTROY, p.getUniqueId())) {

                            if (!destroyConfirm.containsKey(p.getUniqueId())) {
                                p.sendMessage(ChatColor.RED + "WARNING - Are you sure you want to destroy your town!?");
                                p.sendMessage(ChatColor.RED + "If so, run this command again. You will be re-prompted for confirmation after 30s.");

                                destroyConfirm.put(p.getUniqueId(), System.currentTimeMillis());
                            } else {
                                long t = destroyConfirm.remove(p.getUniqueId());

                                //has 60 seconds passed
                                if (System.currentTimeMillis() - t > 30000) {
                                    //invalidate command
                                    p.sendMessage(ChatColor.RED + "More than 30 seconds has passed! Destroy command invalidated.");
                                } else {
                                    //destroy!

                                    TownManager.destroyTown(belong);

                                    p.sendMessage(ChatColor.GREEN + "Your town has been destroyed.");
                                }
                            }

                        } else {
                            p.sendMessage(ChatColor.RED + "You do not have permission to destroy this town.");
                        }
                    }

                    return true;
                } else if (args.length == 1 && args[0].equalsIgnoreCase("upgrade")) {

                    Town belong = TownManager.getPlayerTown(p.getUniqueId());

                    if (belong == null) {
                        p.sendMessage(ChatColor.RED + "You do not belong to a town.");
                    } else {
                        belong.upgrade(p);
                    }

                    return true;
                } else if (args.length == 1 && args[0].equalsIgnoreCase("list")) {

                    if (TownManager.towns.isEmpty()) {
                        p.sendMessage(ChatColor.RED + "There are no established towns at the moment.");
                        return true;
                    }

                    StringBuilder list = new StringBuilder();
                    for (Town town : TownManager.towns.values()) {
                        list.append(town.getTownName()).append(", ");
                    }
                    list.setLength(list.length() - 2);

                    p.sendMessage(ChatColor.YELLOW + "Towns: " + ChatColor.RESET + list.toString());

                    return true;
                } else if (args.length >= 1 && args[0].equalsIgnoreCase("members")) {

                    if (args.length == 1) {
                        Town belong = TownManager.getPlayerTown(p.getUniqueId());
                        if (belong == null) {
                            p.sendMessage(ChatColor.RED + "You are not a member of a town.");
                            return true;
                        } else {
                            p.sendMessage(ChatColor.YELLOW + "Town Members (" + belong.getMembers() + "): " + ChatColor.RESET + belong.getFormattedMemberList());
                        }
                    } else if (args.length == 2) {

                        Town town = TownManager.getTownByName(args[1]);

                        if (town == null) {
                            p.sendMessage(ChatColor.RED + "There is no town by this name.");
                        } else {
                            //show members
                            p.sendMessage(ChatColor.YELLOW + "Town Members (" + town.getMembers() + "): " + ChatColor.RESET + town.getFormattedMemberList());
                        }

                    } else {
                        p.sendMessage(help);
                    }

                    return true;
                }  else if(args.length>= 1 && args[0].equalsIgnoreCase("location")){
                
                	Town town = null;
                	String string = "";
                	if(args.length == 1){
                		if(TownManager.getPlayerTown(p.getUniqueId())!=null){
                		town = TownManager.getPlayerTown(p.getUniqueId());
                		Location loc = TownManager.getTownChest(town).getLocation();
                		string = ChatColor.RED + "" + (int)loc.getX() + ", " + (int)loc.getY() + ", " + (int)loc.getZ();
                		}else{
                            p.sendMessage(ChatColor.RED + "You are not currently in a town.");

                		}
                	}
                	p.sendMessage(ChatColor.GREEN + town.getTownName() + " Location: " + ChatColor.WHITE + string);
                }
            else if(args.length>= 1 && args[0].equalsIgnoreCase("online")){
                
                	Town town = null;
                	String string = "";
                	if(args.length == 1){
                		if(TownManager.getPlayerTown(p.getUniqueId())!=null){
                		town = TownManager.getPlayerTown(p.getUniqueId());
                		
                		}
                	}else if(args.length > 1){
                		town = TownManager.getTownByName(args[1]);
                	}
                	for(Player online : TownManager.getOnlineMembers(town)){
                		string += online.getName() + ", ";
                	}
                	p.sendMessage(ChatColor.GREEN + "Online Members: " + ChatColor.WHITE + string);
                }
                
                else if (args.length == 1 && args[0].equalsIgnoreCase("ranks")) {

                    StringBuilder builder = new StringBuilder();


                    for (Town town : TownManager.towns.values()) {
                        builder.append(town.getTownName()).append("[").append(ChatColor.AQUA).append(town.getTownRank().toString().charAt(0)).append
                                (ChatColor.RESET).append("], ");
                    }

                    builder.setLength(builder.length() - 2);

                    p.sendMessage(ChatColor.YELLOW + "S - Settlement, F - Faction, T - Town, M - Metropolis, K - Kingdom");
                    p.sendMessage(builder.toString());

                    return true;
                } else if (args.length == 2 && args[0].equalsIgnoreCase("upgrades")) {

                    TownRank rank = null;

                    try {
                        rank = TownRank.valueOf(args[1].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        p.sendMessage(ChatColor.RED + "Unrecognised town rank! Settlement, Faction, Town, Metropolis and Kingdom are valid ranks.");
                        return true;
                    }

                    if (rank == null) {
                        p.sendMessage(ChatColor.RED + "Unrecognised town rank! Settlement, Faction, Town, Metropolis and Kingdom are valid ranks.");
                    } else {
                        p.sendMessage(ChatColor.LIGHT_PURPLE + "Required materials for upgrade to rank [" + rank.toString() + "].");
                        for (Map.Entry<TownMaterial, Integer> e : GeneralConfig.upgradeRequirements.get(rank).entrySet()) {
                            p.sendMessage(e.getKey() + " -> " + e.getValue() + " " + ChatColor.YELLOW + e.getKey().getAlias());
                        }
                    }

                    return true;
                } else if (args.length == 2 && args[0].equalsIgnoreCase("war")) {

                    Town belong = TownManager.getPlayerTown(p.getUniqueId());
                    if (belong == null) {
                        p.sendMessage(ChatColor.RED + "You are not a member of a town.");
                        return true;
                    } else if (!belong.canDo(PermissionFlag.WAR, p.getUniqueId())) {
                        p.sendMessage(ChatColor.RED + "You do not have permission to do this.");
                        return true;
                    } else {
                        WarManager.wageWar(p, belong, args[1]);
                    }

                    return true;
                } else if (args.length == 2 && args[0].equalsIgnoreCase("who")) {

                    UUID target = null;

                    OfflinePlayer on = Bukkit.getPlayer(args[1]);

                    if (on != null) {
                        target = on.getUniqueId();
                    } else {
                        //try offline lookup
                        on = Bukkit.getOfflinePlayer(args[1]);
                        if (on != null) {
                            target = on.getUniqueId();
                        } else {
                            p.sendMessage(ChatColor.RED + "No player by that name was found!");
                            return true;
                        }
                    }

                    for (RankedGroup group : SettlementManager.settlements) {
                        if (group.isMember(target)) {
                            //player belongs to a settlement!
                            p.sendMessage(ChatColor.YELLOW + on.getName() + ChatColor.RESET + " is in a settlement with " + ChatColor.GREEN + group
                                    .getMembers().size() + ChatColor.RESET + " members.");
                            return true;
                        }
                    }

                    //they aren't in a settlement
                    for (Town town : TownManager.towns.values()) {
                        if (town.isMember(target)) {
                            //they are a town member

                            PlayerRank rank = town.getGroup().getMembers().get(target);

                            p.sendMessage(ChatColor.YELLOW + on.getName() + ChatColor.RESET + " is in the " + ChatColor.YELLOW + town.getTownRank().toString
                                    () + ChatColor.RESET + " of " + ChatColor.YELLOW + town.getTownName() + ChatColor.RESET + " as a " + ChatColor.GREEN +
                                    rank.toString());

                            return true;
                        }
                    }

                    p.sendMessage(ChatColor.RED + on.getName() + " is not a member of any settlement or town.");

                    return true;
                } else if (args.length == 2 && args[0].equalsIgnoreCase("info")) {

                    Town town = TownManager.getTownByName(args[1]);

                    if (town == null) {
                        p.sendMessage(ChatColor.RED + "There is no town by this name.");
                    } else {
                        town.printInfo(p);
                    }

                    return true;
                } else if (args.length == 2 && args[0].equalsIgnoreCase("border")) {

                    Town town = TownManager.getPlayerTown(p.getUniqueId());

                    if (town == null || !town.getOwner().equals(p.getUniqueId())) {
                        p.sendMessage(ChatColor.RED + "You must own a town to do this.");
                    } else {
                        boolean raise = true;

                        if (args[1].equalsIgnoreCase("true")) {
                            raise = true;
                            p.sendMessage(ChatColor.GREEN + "Town Border raised successfully.");
                        } else if (args[1].equalsIgnoreCase("false")) {
                            raise = false;
                            p.sendMessage(ChatColor.GREEN + "Town Border lowered successfully.");
                        } else {
                            p.sendMessage(ChatColor.RED + "Invalid value! Must be true/false.");
                            return true;
                        }

                        TownTerrain.setBorder(town, raise ? Material.STONE : Material.AIR, 1);
                    }

                    return true;
                } else if (args.length >= 2 && args[0].equalsIgnoreCase("broadcast")) {

                    Town belong = TownManager.getPlayerTown(p.getUniqueId());

                    if (belong == null) {
                        p.sendMessage(ChatColor.RED + "You do not belong to a town.");
                    } else {
                        if (belong.canDo(PermissionFlag.BROADCAST, p.getUniqueId())) {

                            //build message
                            StringBuilder msg = new StringBuilder();

                            for (int i = 1; i < args.length; i++) {
                                msg.append(args[i]).append(" ");
                            }

                            //remove trailing space
                            msg.setLength(msg.length() - 1);

                            belong.broadcastMessage(p.getName(), ChatColor.translateAlternateColorCodes('&', msg.toString()));

                        } else {
                            p.sendMessage(ChatColor.RED + "You do not have permission to broadcast.");
                        }
                    }

                    return true;
                } else if (args.length == 1 && args[0].equalsIgnoreCase("permissions")) {

                    Town belong = TownManager.getPlayerTown(p.getUniqueId());

                    if (belong == null) {
                        p.sendMessage(ChatColor.RED + "You are not a member of a town.");
                        return true;
                    } else if (!belong.getOwner().equals(p.getUniqueId())) {
                        p.sendMessage(ChatColor.RED + "You do not have permission for this.");
                        return true;
                    }

                    StringBuilder b = new StringBuilder();

                    b.append(ChatColor.YELLOW).append("Permission Flags: ").append(ChatColor.RESET);

                    for (PermissionFlag flag : PermissionFlag.values()) {
                        b.append(flag.toString()).append(", ");
                    }

                    b.setLength(b.length() - 2);

                    p.sendMessage(b.toString());

                    b.setLength(0);

                    b.append(ChatColor.YELLOW).append("Permission Settings: ").append(ChatColor.RESET);

                    for (PermissionSetting setting : PermissionSetting.values()) {
                        b.append(setting.toString()).append(", ");
                    }

                    b.setLength(b.length() - 2);

                    p.sendMessage(b.toString());

                    p.sendMessage(ChatColor.GREEN + "----- Current Permissions -----");

                    List<String> keys = new ArrayList<String>();
                    for (PermissionFlag flag : belong.getPermissions().keySet()) {
                        //normalise
                        keys.add(flag.toString().substring(0, 1) + flag.toString().substring(1).toLowerCase());
                    }

                    //Alphabetical order
                    Collections.sort(keys);

                    for (String flag : keys) {
                        if (flag.equalsIgnoreCase("destroy") || flag.equalsIgnoreCase("interact")) {
                            p.sendMessage(ChatColor.RED + "LOCKED - " + ChatColor.GRAY + flag + " -> " + belong.getPermissions().get(PermissionFlag
                                    .valueOf(flag.toUpperCase())));
                        } else {
                            p.sendMessage(flag + " -> " + ChatColor.YELLOW + belong.getPermissions().get(PermissionFlag.valueOf(flag.toUpperCase())));
                        }
                    }

                    return true;
                } else if (args.length == 3 && args[0].equalsIgnoreCase("permissions")) {

                    Town belong = TownManager.getPlayerTown(p.getUniqueId());

                    if (belong == null) {
                        p.sendMessage(ChatColor.RED + "You are not a member of a town.");
                        return true;
                    } else if (!belong.getOwner().equals(p.getUniqueId())) {
                        p.sendMessage(ChatColor.RED + "You do not have permission for this.");
                        return true;
                    }


                    PermissionFlag flag = PermissionFlag.valueOf(args[1].toUpperCase());

                    if (flag == null) {
                        p.sendMessage(ChatColor.RED + "Invalid permission flag.");
                        return true;
                    }

                    if (flag.equals(PermissionFlag.DESTROY) || flag.equals(PermissionFlag.INTERACT)) {
                        p.sendMessage(ChatColor.RED + "This flag cannot be changed.");
                        return true;
                    }

                    PermissionSetting setting = PermissionSetting.valueOf(args[2].toUpperCase());

                    if (setting == null) {
                        p.sendMessage(ChatColor.RED + "invalid permission setting.");
                        return true;
                    }

                    belong.updatePermissions(flag, setting);

                    p.sendMessage(ChatColor.GREEN + "Permissions updated successfully.");

                } else if (args.length >= 1 && args[0].equalsIgnoreCase("help")) {

                    int page = 1;

                    if (args.length == 2) {
                        try {
                            page = Integer.parseInt(args[1]);
                        } catch (NumberFormatException e) {
                            p.sendMessage(ChatColor.RED + "Invalid page number, must be an integer! Defaulting to page 1.");
                        }
                    }
                    pageGen.sendPage(p, page);

                } else {
                    //show help
                	pageGen.sendPage(p, 1);
                	return true;
                }

            }catch(Exception e){
            	pageGen.sendPage(p, 1);
            }
            }
                else {
                commandSender.sendMessage(ChatColor.RED + "'/town' commands must be executed in-game.");
            }

            return true;
        }

        return false;
    }
}

