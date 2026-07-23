package com.smithai.npc;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SmithNPC {

    private final UUID id;
    private final Entity entity;
    private final String name;
    private Player following = null;

    public SmithNPC(UUID id, Entity entity, String name) {
        this.id = id;
        this.entity = entity;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public Entity getEntity() {
        return entity;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return entity != null && !entity.isDead() ? entity.getLocation() : null;
    }

    public void remove() {
        if (entity != null && !entity.isDead()) {
            entity.remove();
        }
    }

    public void sendMessage(Player player, String message) {
        if (player != null && player.isOnline()) {
            player.sendMessage("§b[" + name + "] §f" + message);
        }
    }

    public void sendMessageToAll(String message) {
        org.bukkit.Bukkit.broadcastMessage("§b[" + name + "] §f" + message);
    }

    public void lookAt(Location target) {
        Location loc = getLocation();
        if (loc != null && target != null && target.getWorld().equals(loc.getWorld())) {
            loc.setDirection(target.clone().subtract(loc).toVector());
            entity.teleport(loc);
        }
    }

    public void teleport(Location location) {
        if (entity != null && !entity.isDead()) {
            entity.teleport(location);
        }
    }

    public void follow(Player player) {
        this.following = player;
    }

    public void stay() {
        this.following = null;
    }

    public boolean isFollowing() {
        return following != null && following.isOnline();
    }

    public Player getFollowing() {
        return following;
    }

    public void tick(double followDistance) {
        if (following == null || !following.isOnline()) {
            return;
        }
        Location target = following.getLocation();
        Location current = getLocation();
        if (current == null || !current.getWorld().equals(target.getWorld())) {
            return;
        }
        if (current.distanceSquared(target) > followDistance * followDistance) {
            lookAt(target);
            // Simple teleport follow; a full implementation would use pathfinding.
            Location moveTo = target.clone().subtract(current.toVector().subtract(target.toVector()).normalize().multiply(followDistance));
            moveTo.setY(target.getY());
            teleport(moveTo);
        }
    }
}
