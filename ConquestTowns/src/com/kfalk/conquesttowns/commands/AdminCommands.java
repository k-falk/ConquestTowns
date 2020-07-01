package com.kfalk.conquesttowns.commands;

import com.kfalk.conquesttowns.ConquestTowns;
import com.kfalk.conquesttowns.data.PageGen;
import com.kfalk.conquesttowns.data.Town;
import com.kfalk.conquesttowns.data.TownMaterial;
import com.kfalk.conquesttowns.database.GeneralConfig;
import com.kfalk.conquesttowns.database.TownManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permission;

import java.util.Arrays;


public class AdminCommands implements CommandExecutor {

    private static final String[] help = new String[]{
            ChatColor.YELLOW + "  /towna destroy <town>",
            ChatColor.YELLOW + "  /towna upgrade <town>",
            ChatColor.YELLOW + "  /towna owner <town> <player>",
            ChatColor.YELLOW + "  /towna invite <town> <player>",
            ChatColor.YELLOW + "  /towna kick <town> <player>",
            ChatColor.YELLOW + "  /towna broadcast <town> <message>",
            ChatColor.YELLOW + "  /towna war <town1> <town2>",
            ChatColor.YELLOW + "  /towna peace <town>"};

    private final PageGen pageGen;

    public AdminCommands() {
        String[] perms = new String[]{"destroy", "upgrade", "owner", "invite", "kick", "broadcast", "war", "peace"};

        String base = "ctowns.admin.";

        for (String s : perms) {
            Bukkit.getPluginManager().addPermission(new Permission(base + s));
            ConquestTowns.logger.info("Registered permission [" + base + s + "]");
        }


        pageGen = new PageGen(Arrays.asList(help), 8);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {

        if (command.getName().equalsIgnoreCase("towna")) {

            if (sender instanceof Player) {
            	try{
                Player p = (Player) sender;

                if (args.length == 2 && args[0].equalsIgnoreCase("destroy")) {

                    if (!p.hasPermission("ctowns.admin.destroy") && !p.isOp()) {
                        p.sendMessage(ChatColor.RED + "You do not have permission to do this.");
                        return true;
                    }

                    Town town = TownManager.getTownByName(args[1]);
                    if (town == null) {
                        p.sendMessage(ChatColor.RED + "There is no town by this name.");
                        return true;
                    }

                    TownManager.destroyTown(town);

                    p.sendMessage(ChatColor.GREEN + "Town '" + town.getTownName() + "' has been destroyed.");

                    return true;
                }else if(args.length == 2 && args[0].equalsIgnoreCase("give")){
                	   if (!p.hasPermission("ctowns.admin.upgrade") && !p.isOp()) {
                           p.sendMessage(ChatColor.RED + "You do not have permission to do this.");
                           return true;
                       }
                	   
                	TownMaterial tm = TownMaterial.valueOf(args[1]);
                	ItemStack reward = new ItemStack(tm.getMaterial(), 64);
					ItemMeta im =reward.getItemMeta();
					im.setDisplayName(TownMaterial.fromMaterial(reward.getType()).getAlias());
					reward.setItemMeta(im);
					p.getInventory().addItem(reward);
                    p.sendMessage(ChatColor.RED + "Recieved 64 " + tm.getAlias());
                	return true;
                }
                else if (args.length == 2 && args[0].equalsIgnoreCase("upgrade")) {

                    if (!p.hasPermission("ctowns.admin.upgrade") && !p.isOp()) {
                        p.sendMessage(ChatColor.RED + "You do not have permission to do this.");
                        return true;
                    }

                    Town town = TownManager.getTownByName(args[1]);
                    if (town == null) {
                        p.sendMessage(ChatColor.RED + "There is no town by this name.");
                        return true;
                    }

                    town.forceUpgrade(p);

                    return true;
                } else if (args.length == 3 && args[0].equalsIgnoreCase("owner")) {

                    if (!p.hasPermission("ctowns.admin.owner") && !p.isOp()) {
                        p.sendMessage(ChatColor.RED + "You do not have permission to do this.");
                        return true;
                    }

                    Town town = TownManager.getTownByName(args[1]);
                    if (town == null) {
                        p.sendMessage(ChatColor.RED + "There is no town by this name.");
                        return true;
                    }

                    town.forceOwner(args[2], p);

                    return true;
                } else if (args.length == 3 && args[0].equalsIgnoreCase("invite")) {

                    if (!p.hasPermission("ctowns.admin.invite") && !p.isOp()) {
                        p.sendMessage(ChatColor.RED + "You do not have permission to do this.");
                        return true;
                    }

                    Town town = TownManager.getTownByName(args[1]);
                    if (town == null) {
                        p.sendMessage(ChatColor.RED + "There is no town by this name.");
                        return true;
                    }

                    TownManager.adminInvite(town, p, args[2]);

                    return true;
                } else if (args.length == 3 && args[0].equalsIgnoreCase("kick")) {

                    if (!p.hasPermission("ctowns.admin.kick") && !p.isOp()) {
                        p.sendMessage(ChatColor.RED + "You do not have permission to do this.");
                        return true;
                    }

                    Town town = TownManager.getTownByName(args[1]);
                    if (town == null) {
                        p.sendMessage(ChatColor.RED + "There is no town by this name.");
                        return true;
                    }

                    TownManager.adminRemoveMember(town, p, args[2]);

                    return true;
                } else if (args.length >= 3 && args[0].equalsIgnoreCase("broadcast")) {

                    if (!p.hasPermission("ctowns.admin.broadcast") && !p.isOp()) {
                        p.sendMessage(ChatColor.RED + "You do not have permission to do this.");
                        return true;
                    }

                    Town town = TownManager.getTownByName(args[1]);
                    if (town == null) {
                        p.sendMessage(ChatColor.RED + "There is no town by this name.");
                        return true;
                    }

                    //build message
                    StringBuilder msg = new StringBuilder();

                    for (int i = 2; i < args.length; i++) {
                        msg.append(args[i]).append(" ");
                    }

                    //remove trailing space
                    msg.setLength(msg.length() - 1);

                    town.broadcastMessage(p.getName(), msg.toString());

                    p.sendMessage(ChatColor.GREEN + "Your message was broadcast successfully.");

                    return true;
                } else if (args.length == 3 && args[0].equalsIgnoreCase("war")) {

                    if (!p.hasPermission("ctowns.admin.war") && !p.isOp()) {
                        p.sendMessage(ChatColor.RED + "You do not have permission to do this.");
                        return true;
                    }

                    Town town = TownManager.getTownByName(args[1]),
                            town2 = TownManager.getTownByName(args[2]);

                    if (town == null) {
                        p.sendMessage(ChatColor.RED + "There is no town by this name. [" + args[1] + "]");
                        return true;
                    } else if (town2 == null) {
                        p.sendMessage(ChatColor.RED + "There is no town by this name. [" + args[2] + "]");
                        return true;
                    }

                    if (town.isAtWar() || town2.isAtWar()) {
                        p.sendMessage("One, or both of these towns are already at war.");
                    } else if (town.getMembers() < GeneralConfig.warMembersNeeded || town2.getMembers() <
                            GeneralConfig.warMembersNeeded) {
                        p.sendMessage("One, or both of these towns do not have enough players online.");
                    } else {
                        //war
                        town.wageWar(town2.getTownName(), false);
                        town2.wageWar(town.getTownName(), false);
                        p.sendMessage(ChatColor.RED + "Towns were sent to war successfully.");
                        return true;
                    }

                    return true;
                } else if (args.length == 2 && args[0].equalsIgnoreCase("peace")) {

                    if (!p.hasPermission("ctowns.admin.peace") && !p.isOp()) {
                        p.sendMessage(ChatColor.RED + "You do not have permission to do this.");
                        return true;
                    }

                    Town town = TownManager.getTownByName(args[1]);

                    if (town == null) {
                        p.sendMessage(ChatColor.RED + "There is no town by this name.");
                    } else if (!town.isAtWar()) {
                        p.sendMessage(ChatColor.RED + "This town is not at war.");
                    } else {
                        town.forceSetPeace();
                        p.sendMessage(ChatColor.GREEN + "Towns were set to peace mode successfully.");
                    }

                    return true;
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

                }

                p.sendMessage(ChatColor.YELLOW + "/towna help <page>");
            	}catch(Exception e){
                	e.printStackTrace();
                }

            } else {
                sender.sendMessage(ChatColor.RED + "/towna commands must be executed in-game.");
            }
            return true;
        }
        
        
        return false;
    }

}
