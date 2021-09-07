package com.winthier.perm;

import com.winthier.playercache.PlayerCache;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

@RequiredArgsConstructor
public final class PromoteCommand implements TabExecutor {
    private final PermPlugin plugin;

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length != 1 && args.length != 2) return false;
        PlayerCache player = PlayerCache.forName(args[0]);
        if (player == null) {
            sender.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
            return true;
        }
        final String fromGroupArg;
        final String toGroupArg;
        if (args.length == 2) {
            toGroupArg = args[1];
            int index = PlayerRank.KEYS.indexOf(args[1]);
            if (index < 0) {
                sender.sendMessage(Component.text("Rank not found: " + toGroupArg, NamedTextColor.RED));
                return true;
            }
            if (index == 0) {
                sender.sendMessage(Component.text("Cannot promote to: " + toGroupArg, NamedTextColor.RED));
                return true;
            }
            fromGroupArg = PlayerRank.KEYS.get(index - 1);
        } else {
            fromGroupArg = "friendly";
            toGroupArg = "member";
        }
        SQLGroup fromGroup = plugin.cache.findGroup(fromGroupArg);
        if (fromGroup == null) {
            sender.sendMessage(Component.text("Group not found: " + fromGroupArg, NamedTextColor.RED));
            return true;
        }
        SQLGroup toGroup = plugin.cache.findGroup(toGroupArg);
        if (toGroup == null) {
            sender.sendMessage(Component.text("Group not found: " + toGroupArg, NamedTextColor.RED));
            return true;
        }
        List<String> groups = plugin.findPlayerGroups(player.uuid);
        if (!groups.contains(fromGroup.getKey())) {
            sender.sendMessage(Component.text(player.name + " not in group " + fromGroup.getDisplayName(), NamedTextColor.RED));
            return true;
        }
        if (groups.contains(toGroup.getKey())) {
            sender.sendMessage(Component.text(player.name + " already in group " + toGroup.getDisplayName(), NamedTextColor.RED));
            return true;
        }
        plugin.db.find(SQLMember.class)
            .eq("member", player.uuid)
            .eq("group", fromGroup.getKey())
            .delete();
        plugin.db.insert(new SQLMember(player.uuid, toGroup.getKey()));
        plugin.updateVersion();
        plugin.refreshPermissions();
        sender.sendMessage(Component.text(player.name + " promoted to " + toGroup.getDisplayName(), NamedTextColor.YELLOW));
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length == 1) return null;
        if (args.length == 2) {
            return PlayerRank.KEYS.subList(1, PlayerRank.KEYS.size()).stream()
                .filter(a -> a.contains(args[1]))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
