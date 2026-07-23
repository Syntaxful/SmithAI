package com.smithai.npc;

import com.smithai.SmithAIPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class SmithNPC {

    private final UUID id;
    private final Entity entity;
    private final String name;
    private Player following = null;
    private Location moveTarget = null;
    private boolean pathfinding = false;
    private long pathStartTime = 0;
    private long lastMoveTime = 0;
    private List<Location> path = Collections.emptyList();
    private int pathIndex = 0;
    private long lastPathRecalc = 0;

    private Location lastStuckCheckLocation = null;
    private long lastStuckCheckTime = 0;
    private int stuckTicks = 0;
    private static final int STUCK_TICK_THRESHOLD = 3;
    private static final double STUCK_DISTANCE_SQ = 0.15 * 0.15;

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

    public boolean hasInventory() {
        return entity instanceof Player;
    }

    public org.bukkit.inventory.Inventory getInventory() {
        if (entity instanceof Player) {
            return ((Player) entity).getInventory();
        }
        return null;
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
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).setAI(false);
            }
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
        this.moveTarget = null;
        this.pathfinding = false;
    }

    public void stay() {
        this.following = null;
        this.moveTarget = null;
        this.pathfinding = false;
    }

    public boolean isFollowing() {
        return following != null && following.isOnline();
    }

    public Player getFollowing() {
        return following;
    }

    public void setMoveTarget(Location target) {
        Location current = getLocation();
        if (current != null && target != null && !current.getWorld().equals(target.getWorld())) {
            teleport(target);
            return;
        }
        this.moveTarget = target != null ? target.clone() : null;
        this.pathfinding = target != null;
        this.pathStartTime = System.currentTimeMillis();
        this.lastMoveTime = System.currentTimeMillis();
        this.path = Collections.emptyList();
        this.pathIndex = 0;
        recalculatePath();
    }

    public Location getMoveTarget() {
        return moveTarget;
    }

    public boolean isPathfinding() {
        return pathfinding;
    }

    public void cancelPathfinding() {
        this.moveTarget = null;
        this.pathfinding = false;
        this.path = Collections.emptyList();
        this.pathIndex = 0;
    }

    public void tick(double followDistance) {
        if (following != null && following.isOnline()) {
            tickFollow(followDistance);
            return;
        }
        if (pathfinding && moveTarget != null) {
            tickPathfinding();
        }
    }

    private void tickFollow(double followDistance) {
        Location target = following.getLocation();
        Location current = getLocation();
        if (current == null) {
            return;
        }
        if (!current.getWorld().equals(target.getWorld())) {
            teleport(target);
            return;
        }
        double distSq = current.distanceSquared(target);
        if (distSq <= followDistance * followDistance) {
            lookAt(target);
            return;
        }
        if (distSq > 64 * 64) {
            teleport(target);
            return;
        }
        double speed = distSq > 8 * 8 ? 0.45 : distSq > 4 * 4 ? 0.35 : 0.25;
        moveToward(target, speed);
        lookAt(target);
    }

    private void tickPathfinding() {
        Location current = getLocation();
        if (current == null || moveTarget == null || !current.getWorld().equals(moveTarget.getWorld())) {
            cancelPathfinding();
            return;
        }
        if (current.distanceSquared(moveTarget) <= 2.5 * 2.5) {
            cancelPathfinding();
            return;
        }

        // Recalculate path periodically if stale or empty
        if (path.isEmpty() || pathIndex >= path.size() || (System.currentTimeMillis() - lastPathRecalc) > 3000) {
            recalculatePath();
            if (path.isEmpty()) {
                // Fallback to direct movement if no path found
                long now = System.currentTimeMillis();
                if (now - lastMoveTime > 150) {
                    moveToward(moveTarget, 0.25);
                    lastMoveTime = now;
                }
                lookAt(moveTarget);
                return;
            }
        }

        Location waypoint = path.get(pathIndex);
        if (current.distanceSquared(waypoint) <= 1.5 * 1.5) {
            pathIndex++;
            if (pathIndex >= path.size()) {
                // Reached end of path, recalculate for remaining distance
                recalculatePath();
            }
            if (pathIndex < path.size()) {
                waypoint = path.get(pathIndex);
            } else {
                waypoint = moveTarget;
            }
        }

        long now = System.currentTimeMillis();
        if (now - lastMoveTime > 150) {
            double speed = current.distanceSquared(waypoint) > 4 * 4 ? 0.35 : 0.25;
            moveToward(waypoint, speed);
            lastMoveTime = now;
        }
        lookAt(waypoint);

        // Stuck detection: if the NPC hasn't moved in a few checks, recalculate and try to jump.
        if (now - lastStuckCheckTime > 1000) {
            if (lastStuckCheckLocation != null && current.distanceSquared(lastStuckCheckLocation) <= STUCK_DISTANCE_SQ) {
                stuckTicks++;
                if (stuckTicks >= STUCK_TICK_THRESHOLD) {
                    recalculatePath();
                    jump();
                    stuckTicks = 0;
                }
            } else {
                stuckTicks = 0;
            }
            lastStuckCheckLocation = current.clone();
            lastStuckCheckTime = now;
        }
    }

    private void recalculatePath() {
        Location current = getLocation();
        if (current == null || moveTarget == null || !current.getWorld().equals(moveTarget.getWorld())) {
            return;
        }
        SmithAIPlugin plugin = SmithAIPlugin.getInstance();
        if (plugin == null) {
            path = Collections.emptyList();
            return;
        }
        Pathfinder pf = new Pathfinder(current.getWorld(), 2000, 64.0, 1.0, 3.0);
        path = pf.findPath(current, moveTarget);
        pathIndex = 0;
        lastPathRecalc = System.currentTimeMillis();
    }

    private void moveToward(Location target, double speed) {
        if (entity == null || entity.isDead()) return;
        Location current = getLocation();
        if (current == null) return;

        Vector direction = target.toVector().subtract(current.toVector()).normalize();
        Vector velocity = direction.multiply(speed).setY(entity.getVelocity().getY());

        Block blockAhead = current.clone().add(direction.getX(), 0, direction.getZ()).getBlock();
        Block blockAboveAhead = blockAhead.getRelative(0, 1, 0);
        Block blockFeet = current.getBlock();
        Block blockGround = current.clone().add(0, -1, 0).getBlock();
        boolean inWater = blockFeet.getType() == Material.WATER;
        boolean climbing = isClimbable(blockAhead) || isClimbable(blockFeet);

        // Simple step-up / jump logic
        if (climbing) {
            velocity.setY(0.3);
        } else if (!blockAhead.isPassable() && blockAboveAhead.isPassable() && blockFeet.isPassable()) {
            velocity.setY(0.45);
        } else if (inWater) {
            // Swim upward to stay at the surface and move through water.
            velocity.setY(0.15);
        } else if (blockGround.isPassable() && blockFeet.isPassable()) {
            // falling, keep gravity
        } else {
            velocity.setY(Math.min(0.0, velocity.getY()));
        }

        entity.setVelocity(velocity);
    }

    private boolean isClimbable(Block block) {
        Material type = block.getType();
        return type == Material.LADDER || type == Material.VINE || type == Material.TWISTING_VINES || type == Material.WEEPING_VINES;
    }

    public void jump() {
        if (entity != null && !entity.isDead()) {
            entity.setVelocity(entity.getVelocity().setY(0.5));
        }
    }
}
