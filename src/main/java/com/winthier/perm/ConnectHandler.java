package com.winthier.perm;

import com.winthier.connect.Connect;
import com.winthier.connect.event.ConnectMessageEvent;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@RequiredArgsConstructor
public final class ConnectHandler implements Listener {
    public static final String CHANNEL = "perm";
    public static final String REFRESH = "refresh";
    private final PermPlugin plugin;

    public ConnectHandler enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        return this;
    }

    @EventHandler
    void onConnectMessage(ConnectMessageEvent event) {
        if (!CHANNEL.equals(event.getMessage().getChannel())) return;
        String payload = event.getMessage().getPayload();
        if (REFRESH.equals(payload)) {
            plugin.getLogger().info("Connect refresh signal received");
            plugin.refreshPermissions();
        }
    }

    public void broadcastRefresh() {
        Connect.getInstance().broadcast(CHANNEL, REFRESH);
    }
}
