package com.winthier.perm;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.perm.ExtraRank;
import com.cavetale.core.perm.Perm;
import com.cavetale.core.perm.Rank;
import com.cavetale.core.perm.StaffRank;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class ToggleRankCommand extends AbstractCommand<PermPlugin> {
    private static final String DISABLED_SUFFIX = "-disabled";

    protected ToggleRankCommand(final PermPlugin plugin) {
        super(plugin, "togglerank");
    }

    @Override
    protected void onEnable() {
        rootNode.denyTabCompletion()
            .description("Toggle ranks on/off")
            .playerCaller(this::toggleRank);
    }

    private void toggleRank(Player player) {
        final List<Rank> ranks = new ArrayList<>();
        for (var it : StaffRank.values()) {
            ranks.add(it);
        }
        ranks.add(ExtraRank.BUILDER);
        ranks.add(ExtraRank.HEAD_BUILDER);
        // Toggle off
        final UUID uuid = player.getUniqueId();
        final List<Rank> hasRanks = new ArrayList<>();
        for (Rank rank : ranks) {
            if (Perm.get().isInAssignedGroup(uuid, rank.getKey())) {
                hasRanks.add(rank);
            }
        }
        if (!hasRanks.isEmpty()) {
            for (Rank rank : hasRanks) {
                Perm.get().removeGroup(uuid, rank.getKey());
                Perm.get().addGroup(uuid, rank.getKey() + DISABLED_SUFFIX);
            }
            player.sendMessage(text("Rank(s) toggled off: " + hasRanks, YELLOW));
            return;
        }
        // Toggle on
        final List<Rank> hasDisabledRanks = new ArrayList<>();
        for (Rank rank : ranks) {
            if (Perm.get().isInAssignedGroup(uuid, rank.getKey() + DISABLED_SUFFIX)) {
                hasDisabledRanks.add(rank);
            }
        }
        if (!hasDisabledRanks.isEmpty()) {
            for (Rank rank : hasDisabledRanks) {
                Perm.get().removeGroup(uuid, rank.getKey() + DISABLED_SUFFIX);
                Perm.get().addGroup(uuid, rank.getKey());
            }
            player.sendMessage(text("Rank(s) toggled on: " + hasDisabledRanks, GREEN));
            return;
        }
        throw new CommandWarn("You do not have any ranks to toggle");
    }
}
