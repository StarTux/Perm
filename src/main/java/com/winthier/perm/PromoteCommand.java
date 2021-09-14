package com.winthier.perm;

import com.winthier.perm.rank.PlayerRank;
import com.winthier.playercache.PlayerCache;
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
        final PlayerRank playerRank = args.length >= 2
            ? PlayerRank.ofKey(args[1])
            : PlayerRank.MEMBER;
        if (playerRank == null) {
            sender.sendMessage(Component.text("Rank not found: " + args[1], NamedTextColor.RED));
            return true;
        }
        if (playerRank.has(player.uuid)) {
            sender.sendMessage(Component.text(player.name + " already has rank " + playerRank.displayName, NamedTextColor.RED));
        }
        if (!playerRank.promote(player.uuid)) {
            sender.sendMessage(Component.text("Promotion failed!", NamedTextColor.RED));
            return true;
        }
        sender.sendMessage(Component.text(player.name + " promoted to " + playerRank.displayName, NamedTextColor.YELLOW));
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
