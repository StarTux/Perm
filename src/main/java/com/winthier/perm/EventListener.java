package com.winthier.perm;

import com.cavetale.core.event.connect.ConnectMessageEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginEnableEvent;
import static com.winthier.perm.PermPlugin.CHANNEL;
import static com.winthier.perm.PermPlugin.REFRESH;

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
}
