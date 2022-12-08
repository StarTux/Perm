package com.winthier.perm;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.event.player.PluginPlayerEvent;
import com.cavetale.core.font.Emoji;
import com.cavetale.core.font.GlyphPolicy;
import com.cavetale.mytems.item.font.Glyph;
import com.winthier.perm.sql.SQLLevel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import static com.cavetale.core.font.Unicode.tiny;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.newline;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.JoinConfiguration.separator;
import static net.kyori.adventure.text.event.ClickEvent.runCommand;
import static net.kyori.adventure.text.event.HoverEvent.showText;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class TierCommand extends AbstractCommand<PermPlugin> {
    protected TierCommand(final PermPlugin plugin) {
        super(plugin, "tier");
    }

    @Override
    protected void onEnable() {
        rootNode.denyTabCompletion()
            .description("View tier list")
            .playerCaller(this::tier);
    }

    private void tier(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        book.editMeta(m -> {
                if (!(m instanceof BookMeta meta)) return;
                List<Component> pages = new ArrayList<>();
                int playerLevel = plugin.getPlayerLevel(player.getUniqueId());
                int playerProgress = plugin.getPlayerLevelProgress(player.getUniqueId());
                pages.add(join(noSeparators(),
                               player.displayName(),
                               newline(),
                               newline(),
                               text(tiny("your tier "), DARK_GRAY),
                               Glyph.toComponent("" + playerLevel),
                               newline(),
                               newline(),
                               text(tiny("progress "), DARK_GRAY),
                               text(playerProgress, DARK_BLUE),
                               text("/", DARK_GRAY),
                               text(playerLevel, DARK_BLUE),
                               newline(),
                               newline(),
                               (text("Tier up by completing tutorials and quests")
                                .clickEvent(runCommand("/tut"))
                                .hoverEvent(showText(text("/tut", GREEN))))));
                Map<Integer, List<SQLLevel>> tierMap = new TreeMap<>();
                for (SQLLevel row : plugin.cache.levels) {
                    if (row.getDescription() == null) continue;
                    tierMap.computeIfAbsent(row.getLevel(), i -> new ArrayList<>()).add(row);
                }
                List<Integer> levelList = new ArrayList<>(tierMap.keySet());
                Collections.sort(levelList);
                for (int level : levelList) {
                    List<SQLLevel> rows = tierMap.get(level);
                    rows.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
                    List<Component> descriptions = new ArrayList<>(rows.size());
                    for (SQLLevel row : rows) {
                        descriptions.add(Emoji.replaceText(row.getDescription(), GlyphPolicy.HIDDEN, false).asComponent());
                    }
                    pages.add(join(noSeparators(),
                                   text(tiny("tier "), DARK_GRAY),
                                   Glyph.toComponent("" + level),
                                   newline(),
                                   newline(),
                                   join(separator(newline()), descriptions)));
                }
                meta.pages(pages);
                meta.author(text("Cavetale"));
                meta.title(text("Tier"));
            });
        player.closeInventory();
        player.openBook(book);
        PluginPlayerEvent.Name.USE_TIER.call(plugin, player);
    }
}
