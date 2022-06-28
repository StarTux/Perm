package com.winthier.perm;

import com.cavetale.core.event.connect.ConnectMessageEvent;
import com.cavetale.core.event.hud.PlayerHudEvent;
import com.cavetale.core.event.hud.PlayerHudPriority;
import com.cavetale.mytems.item.font.Glyph;
import com.winthier.perm.sql.SQLPlayerLevel;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginEnableEvent;
import static com.cavetale.core.font.Unicode.tiny;
import static com.winthier.perm.PermPlugin.CHANNEL;
import static com.winthier.perm.PermPlugin.REFRESH;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

@RequiredArgsConstructor
public final class EventListener implements Listener {
    private final PermPlugin plugin;

    @EventHandler
    public void onPluginEnable(final PluginEnableEvent event) {
        if (plugin.vaultEnabled) return;
        if (!event.getPlugin().getName().equals("Vault")) return;
        plugin.tryToLoadVault();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerLogin(final PlayerLoginEvent event) {
        plugin.setupPlayerPerms(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerQuit(final PlayerQuitEvent event) {
        plugin.resetPlayerPerms(event.getPlayer());
    }

    @EventHandler
    private void onConnectMessage(ConnectMessageEvent event) {
        if (CHANNEL.equals(event.getChannel()) && REFRESH.equals(event.getPayload())) {
            plugin.getLogger().info("Connect refresh signal received");
            plugin.refreshPermissionsAsync();
        }
    }

    @EventHandler
    private void onPlayerHud(PlayerHudEvent event) {
        SQLPlayerLevel row = plugin.cache.playerLevels.get(event.getPlayer().getUniqueId());
        if (row == null || row.getUpdated().getTime() < System.currentTimeMillis() - 10000L) return;
        event.bossbar(PlayerHudPriority.HIGH,
                      join(noSeparators(), text(tiny("tier"), GRAY), Glyph.toComponent("" + row.getLevel())),
                      BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_10,
                      (float) row.getProgress() / (float) row.getLevel());
    }
}
