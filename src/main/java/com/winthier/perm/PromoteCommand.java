package com.winthier.perm;

import com.winthier.playercache.PlayerCache;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

@RequiredArgsConstructor
public final class PromoteCommand implements CommandExecutor {
    private final PermPlugin plugin;

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length != 1) return false;
        String playerName = args[0];
        UUID playerUuid = PlayerCache.uuidForName(playerName);
        if (playerUuid == null) {
            sender.sendMessage("Player not found: " + playerName);
            return true;
        }
        playerName = PlayerCache.nameForUuid(playerUuid);
        String fromGroupArg = "friendly";
        String toGroupArg = "member";
        SQLGroup fromGroup = plugin.cache.findGroup(fromGroupArg);
        if (fromGroup == null) {
            sender.sendMessage("Group not found: " + fromGroupArg);
            return true;
        }
        SQLGroup toGroup = plugin.cache.findGroup(toGroupArg);
        if (toGroup == null) {
            sender.sendMessage("Group not found: " + toGroupArg);
            return true;
        }
        List<String> groups = plugin.findPlayerGroups(playerUuid);
        if (!groups.contains(fromGroup.getKey())) {
            sender.sendMessage(playerName + " not in group " + fromGroup.getDisplayName());
            return true;
        }
        if (groups.contains(toGroup.getKey())) {
            sender.sendMessage(playerName + " already in group " + toGroup.getDisplayName());
            return true;
        }
        plugin.db.find(SQLMember.class)
            .eq("member", playerUuid)
            .eq("group", fromGroup.getKey())
            .delete();
        plugin.db.insert(new SQLMember(playerUuid, toGroup.getKey()));
        plugin.updateVersion();
        plugin.refreshPermissions();
        sender.sendMessage(playerName + " promoted to " + toGroup.getDisplayName());
        return true;
    }
}
