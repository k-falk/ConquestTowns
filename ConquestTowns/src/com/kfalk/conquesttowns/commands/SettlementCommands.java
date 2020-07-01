package com.kfalk.conquesttowns.commands;

import com.kfalk.conquesttowns.database.SettlementManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;




public class SettlementCommands implements CommandExecutor {

    private static final String[] help = new String[]{ChatColor.RED + "Conquest Towns // " + ChatColor.DARK_GRAY + "--- " + ChatColor.RED + "Basic Help " +
            ChatColor.DARK_GRAY + "---",
            ChatColor.YELLOW + "  /settlement create",
            ChatColor.YELLOW + "  /settlement disband",
            ChatColor.YELLOW + "  /settlement invite [player]",
            ChatColor.YELLOW + "  /settlement kick [player]",
            ChatColor.YELLOW + "  /settlement info"};

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {

        if (command.getName().equalsIgnoreCase("settlement")) {

            if (commandSender instanceof Player) {
                Player p = (Player) commandSender;

                if (args.length == 1 && args[0].equalsIgnoreCase("create")) {
                    SettlementManager.createSettlement(p);
                    return true;
                } else if (args.length == 1 && args[0].equalsIgnoreCase("disband")) {
                    SettlementManager.disbandSettlement(p, false);
                    return true;
                } else if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
                    SettlementManager.inviteToSettlement(p, args[1]);
                    return true;
                } else if (args.length == 2 && args[0].equalsIgnoreCase("kick")) {
                    SettlementManager.removeMember(p, args[1]);
                    return true;
                } else if (args.length == 1 && args[0].equalsIgnoreCase("info")) {
                    SettlementManager.showInfo(p);
                    return true;
                } else if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
                    SettlementManager.acceptSettlementInvite(p, args[1]);
                    return true;
                } else if (args.length == 1 && args[0].equalsIgnoreCase("leave")) {
                    SettlementManager.leaveOwnSettlement(p);
                    return true;
                } else {
                    //show help
                    p.sendMessage(help);
                    return true;
                }

            } else {
                commandSender.sendMessage(ChatColor.RED + "'/settlement' commands must be executed in-game.");
            }

            return true;
        }

        return false;
    }
}
