package com.winthier.perm;

import com.winthier.generic_events.PlayerHasPermissionEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginEnableEvent;

@RequiredArgsConstructor
public final class EventListener implements Listener {
    private final PermPlugin plugin;

    @EventHandler
    public void onPluginEnable(final PluginEnableEvent event) {
        if (plugin.vaultEnabled) return;
        if (!event.getPlugin().getName().equals("Vault")) return;
        VaultPerm vaultPerm = new VaultPerm(plugin);
        vaultPerm.register();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(final PlayerLoginEvent event) {
        plugin.setupPlayerPerms(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(final PlayerQuitEvent event) {
        plugin.resetPlayerPerms(event.getPlayer());
    }

    @EventHandler
    public void onPlayerHasPermission(final PlayerHasPermissionEvent event) {
        boolean has = plugin.playerHasPerm(event.getPlayerId(),
                                           event.getPermission());
        event.setPermitted(has);
    }
}
