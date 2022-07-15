package com.winthier.perm.event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

/**
 * Called whenever a player's permissions have been updated, for any
 * reason.
 * @deprecated
 * Use com.cavetale.core.event.perm.PlayerPermissionUpdateEvent
 *
 * PermPlugin will call this on every player refresh if something
 * changed, but avoid if this is the initial setup as it will usually
 * coincide with PlayerJoin or PluginEnable anyway.
 *
 */
@Getter @Deprecated
public final class PlayerPermissionUpdateEvent extends PlayerEvent {
    @NonNull private Map<String, Boolean> oldPermissions;
    @NonNull private Map<String, Boolean> newPermissions;
    /**
     * Required by Event.
     */
    @Getter private static HandlerList handlerList = new HandlerList();

    /**
     * Required by Event.
     */
    @Override public HandlerList getHandlers() {
        return handlerList;
    }

    public PlayerPermissionUpdateEvent(final Player player, final Map<String, Boolean> oldPerms, final Map<String, Boolean> newPerms) {
        super(player);
        this.oldPermissions = oldPerms;
        this.newPermissions = newPerms;
    }

    /**
     * Check if and how a permission changed.
     * @param permission the permission
     * @return true if it turned true, false if it turned false, null
     * if it never changed.
     */
    public Boolean getPermissionChange(String permission) {
        Boolean a = oldPermissions.get(permission);
        Boolean b = newPermissions.get(permission);
        if (b == Boolean.TRUE) {
            return a != Boolean.TRUE ? true : null;
        } else { // b is false or null
            return a == Boolean.TRUE ? false : null;
        }
    }

    public Map<String, Boolean> getPermissionChanges() {
        Set<String> names = new HashSet<>();
        names.addAll(oldPermissions.keySet());
        names.addAll(newPermissions.keySet());
        Map<String, Boolean> result = new HashMap<>();
        for (String name : names) {
            Boolean change = getPermissionChange(name);
            if (change != null) result.put(name, change);
        }
        return result;
    }

    public void call() {
        Bukkit.getPluginManager().callEvent(this);
    }
}
