package com.smithai.npc;

import com.smithai.SmithAIPlugin;
import com.smithai.util.BlockCompat;
import com.smithai.util.MaterialCompat;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * A Smith_AI NPC. When Citizens is available the NPC is a player model and
 * Citizens' Navigator handles movement; otherwise velocity-based movement is used.
 */
public class SmithNPC {

    private final UUID id;
    private final Entity entity;
    private final String name;
    private final Object citizensHandle;
    private final NPCInventory inventory;

    private Player following = null;
    private Location moveTarget = null;
    private boolean pathfinding = false;

    private Location taskLookTarget = null;
    private long taskLookUntil = 0;

    private List<Location> path = Collections.emptyList();
    private int pathIndex = 0;
    private long lastPathRecalc = 0;
    private long lastLookTime = 0;
    private long lastIdleLookTime = 0;
    private long lastTargetRefresh = 0;

    private Location lastStuckCheckLocation = null;
    private long lastStuckCheckTime = 0;
    private int stuckTicks = 0;
    private long lastBridgeTime = 0;

    private static final long LOOK_INTERVAL_MS = 200;
    private static final long TARGET_REFRESH_MS = 1000;
    private static final long STUCK_CHECK_MS = 1000;
    private static final long BRIDGE_COOLDOWN_MS = 250;
    private static final double STUCK_DISTANCE_SQ = 0.12 * 0.12;
    private static final int STUCK_THRESHOLD = 4;
    private static final double LEASH_DISTANCE_SQ = 48 * 48;
    private static final double IDLE_LOOK_RADIUS = 8.0;
    private static final double IDLE_LOOK_RADIUS_SQ = IDLE_LOOK_RADIUS * IDLE_LOOK_RADIUS;

    private static final Material[] BRIDGE_MATERIALS = initBridgeMaterials();

    private static Material[] initBridgeMaterials() {
        Material[] candidates = {
            MaterialCompat.get("COBBLESTONE"), MaterialCompat.get("STONE"), MaterialCompat.get("DIRT"),
            MaterialCompat.get("OAK_PLANKS", "WOOD"), MaterialCompat.get("SPRUCE_PLANKS", "WOOD", "SPRUCE_WOOD"),
            MaterialCompat.get("BIRCH_PLANKS", "BIRCH_WOOD"), MaterialCompat.get("JUNGLE_PLANKS", "JUNGLE_WOOD"),
            MaterialCompat.get("ACACIA_PLANKS", "ACACIA_WOOD"), MaterialCompat.get("DARK_OAK_PLANKS", "DARK_OAK_WOOD"),
            MaterialCompat.get("MANGROVE_PLANKS"), MaterialCompat.get("CRIMSON_PLANKS", "CRIMSON_WOOD"),
            MaterialCompat.get("WARPED_PLANKS", "WARPED_WOOD"), MaterialCompat.get("NETHERRACK")
        };
        return java.util.Arrays.stream(candidates).filter(m -> m != null).toArray(Material[]::new);
    }

    public SmithNPC(UUID id, Entity entity, String name, NPCInventory inventory) {
        this.id = id;
        this.entity = entity;
        this.name = name;
        this.citizensHandle = PlayerModelHelper.getCitizensHandle(entity);
        this.inventory = inventory;
    }

    public SmithNPC(UUID id, Entity entity, String name) {
        this(id, entity, name, entity instanceof Player ? new NPCInventory((Player) entity) : new NPCInventory());
    }

    public UUID getId() { return id; }
    public Entity getEntity() { return entity; }
    public String getName() { return name; }
    public boolean isCitizens() { return citizensHandle != null; }

    public Location getLocation() {
        return entity != null && !entity.isDead() ? entity.getLocation() : null;
    }

    public void remove() {
        if (entity == null || entity.isDead()) return;
        PlayerModelHelper.destroyCitizensNpc(entity);
    }

    public boolean hasInventory() { return inventory != null; }

    public NPCInventory getInventory() {
        return inventory;
    }

    public org.bukkit.inventory.Inventory getBukkitInventory() {
        return entity instanceof Player ? ((Player) entity).getInventory() : null;
    }

    public void sendMessage(Player player, String message) {
        if (player != null && player.isOnline()) player.sendMessage("§b[" + name + "] §f" + message);
    }

    public void sendMessageToAll(String message) {
        org.bukkit.Bukkit.broadcastMessage("§b[" + name + "] §f" + message);
    }

    public void lookAt(Location target) {
        long now = System.currentTimeMillis();
        if (now - lastLookTime < LOOK_INTERVAL_MS) return;
        lastLookTime = now;
        Location loc = getLocation();
        if (loc == null || target == null || !sameWorld(loc, target)) return;
        if (citizensHandle != null) {
            PlayerModelHelper.faceLocation(citizensHandle, target);
            return;
        }
        Vector direction = target.clone().subtract(loc).toVector();
        if (direction.lengthSquared() < 0.0001) return;
        loc.setDirection(direction);
        entity.teleport(loc);
    }

    public void teleport(Location location) {
        if (entity != null && !entity.isDead()) entity.teleport(location);
    }

    public void follow(Player player) {
        this.following = player;
        this.moveTarget = null;
        this.pathfinding = false;
        if (citizensHandle != null) {
            PlayerModelHelper.setNavigatorSpeed(citizensHandle, 1.0f);
            PlayerModelHelper.followEntity(citizensHandle, player);
        }
    }

    public void stay() {
        this.following = null;
        this.moveTarget = null;
        this.pathfinding = false;
        this.path = Collections.emptyList();
        this.pathIndex = 0;
        if (citizensHandle != null) {
            PlayerModelHelper.cancelNavigation(citizensHandle);
        }
        setSprinting(false);
        setSneaking(false);
    }

    public boolean isFollowing() { return following != null && following.isOnline(); }
    public Player getFollowing() { return following; }

    public void setMoveTarget(Location target) {
        Location current = getLocation();
        if (current != null && target != null && !sameWorld(current, target)) {
            teleport(target);
            return;
        }
        this.moveTarget = target != null ? target.clone() : null;
        this.pathfinding = target != null;
        this.path = Collections.emptyList();
        this.pathIndex = 0;
        if (citizensHandle != null) {
            PlayerModelHelper.setNavigatorSpeed(citizensHandle, 1.0f);
            PlayerModelHelper.navigateTo(citizensHandle, target);
        } else {
            recalculatePath();
        }
    }

    public Location getMoveTarget() { return moveTarget; }
    public boolean isPathfinding() { return pathfinding; }

    public void cancelPathfinding() {
        this.moveTarget = null;
        this.pathfinding = false;
        this.path = Collections.emptyList();
        this.pathIndex = 0;
        if (citizensHandle != null) PlayerModelHelper.cancelNavigation(citizensHandle);
        setSprinting(false);
        setSneaking(false);
    }

    public void setTaskLookTarget(Location target, long durationMs) {
        this.taskLookTarget = target != null ? target.clone() : null;
        this.taskLookUntil = target != null ? System.currentTimeMillis() + durationMs : 0;
    }

    public boolean isDoingTask() { return System.currentTimeMillis() < taskLookUntil; }

    public void tick(double followDistance) {
        if (entity == null || entity.isDead()) return;
        if (citizensHandle != null) {
            tickCitizens(followDistance);
        } else {
            tickVelocity(followDistance);
        }
    }

    private void tickCitizens(double followDistance) {
        long now = System.currentTimeMillis();

        if (isDoingTask() && taskLookTarget != null) {
            lookAt(taskLookTarget);
            setSneaking(true);
            return;
        }

        if (following != null && following.isOnline()) {
            Location target = following.getLocation();
            Location current = getLocation();
            if (current != null && sameWorld(current, target)) {
                double distSq = current.distanceSquared(target);
                if (distSq > LEASH_DISTANCE_SQ) {
                    teleport(target);
                    return;
                }
                if (distSq > followDistance * followDistance) {
                    if (now - lastTargetRefresh > TARGET_REFRESH_MS) {
                        PlayerModelHelper.setNavigatorSpeed(citizensHandle, distSq > 64 ? 1.5f : 1.0f);
                        PlayerModelHelper.followEntity(citizensHandle, following);
                        lastTargetRefresh = now;
                    }
                    setSneaking(false);
                    return;
                }
            }
            PlayerModelHelper.cancelNavigation(citizensHandle);
            setSneaking(false);
            lookAtNearestPlayer();
            return;
        }

        if (pathfinding && moveTarget != null) {
            Location current = getLocation();
            if (current != null && sameWorld(current, moveTarget)) {
                if (current.distanceSquared(moveTarget) <= 2.5 * 2.5) {
                    cancelPathfinding();
                } else if (now - lastTargetRefresh > TARGET_REFRESH_MS) {
                    PlayerModelHelper.setNavigatorSpeed(citizensHandle, current.distanceSquared(moveTarget) > 64 ? 1.5f : 1.0f);
                    PlayerModelHelper.navigateTo(citizensHandle, moveTarget);
                    lastTargetRefresh = now;
                }
            } else {
                cancelPathfinding();
            }
            return;
        }

        setSneaking(false);
        lookAtNearestPlayer();
    }

    private void tickVelocity(double followDistance) {
        if (isDoingTask() && taskLookTarget != null) {
            lookAt(taskLookTarget);
            setSneaking(true);
            return;
        }
        if (following != null && following.isOnline()) {
            tickVelocityFollow(followDistance);
            return;
        }
        if (pathfinding && moveTarget != null) {
            tickVelocityPathfinding();
            return;
        }
        setSneaking(false);
        lookAtNearestPlayer();
    }

    private void tickVelocityFollow(double followDistance) {
        Location target = following.getLocation();
        Location current = getLocation();
        if (current == null) return;
        if (!sameWorld(current, target)) { teleport(target); return; }
        double distSq = current.distanceSquared(target);
        if (distSq <= followDistance * followDistance) {
            setSprinting(false);
            setSneaking(false);
            lookAt(target);
            return;
        }
        if (distSq > LEASH_DISTANCE_SQ) { teleport(target); setSprinting(false); return; }
        double speed = distSq > 64 ? 0.45 : distSq > 16 ? 0.35 : 0.25;
        setSprinting(speed >= 0.35);
        moveToward(target, speed);
        lookAt(target);
    }

    private void tickVelocityPathfinding() {
        Location current = getLocation();
        if (current == null || moveTarget == null || !sameWorld(current, moveTarget)) { cancelPathfinding(); return; }
        if (current.distanceSquared(moveTarget) <= 6.25) { cancelPathfinding(); return; }
        if (path.isEmpty() || pathIndex >= path.size() || (System.currentTimeMillis() - lastPathRecalc) > 3000) {
            recalculatePath();
        }
        Location waypoint = path.isEmpty() || pathIndex >= path.size() ? moveTarget : path.get(pathIndex);
        if (current.distanceSquared(waypoint) <= 2.25) {
            pathIndex++;
            waypoint = path.isEmpty() || pathIndex >= path.size() ? moveTarget : path.get(pathIndex);
        }
        double speed = current.distanceSquared(waypoint) > 16 ? 0.35 : 0.25;
        moveToward(waypoint, speed);
        lookAt(waypoint);
        checkStuck(current);
    }

    private void checkStuck(Location current) {
        long now = System.currentTimeMillis();
        if (now - lastStuckCheckTime <= STUCK_CHECK_MS) return;
        if (lastStuckCheckLocation != null && current.distanceSquared(lastStuckCheckLocation) <= STUCK_DISTANCE_SQ) {
            stuckTicks++;
            if (stuckTicks >= STUCK_THRESHOLD) {
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

    private void recalculatePath() {
        Location current = getLocation();
        if (current == null || moveTarget == null || !sameWorld(current, moveTarget)) {
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
        Vector offset = target.toVector().subtract(current.toVector());
        double flatLen = Math.sqrt(offset.getX() * offset.getX() + offset.getZ() * offset.getZ());
        if (flatLen < 0.0001) return;
        Vector direction = offset.clone().normalize();
        Vector velocity = direction.multiply(speed).setY(entity.getVelocity().getY());

        Block blockAhead = current.clone().add(direction.getX(), 0, direction.getZ()).getBlock();
        Block blockAboveAhead = blockAhead.getRelative(0, 1, 0);
        Block blockFeet = current.getBlock();
        Block blockGround = current.clone().add(0, -1, 0).getBlock();
        boolean inWater = blockFeet.getType() == Material.WATER || blockFeet.getType() == MaterialCompat.get("STATIONARY_WATER");
        boolean climbing = isClimbable(blockAhead) || isClimbable(blockFeet);

        if (climbing) velocity.setY(0.3);
        else if (!BlockCompat.isPassable(blockAhead) && BlockCompat.isPassable(blockAboveAhead) && BlockCompat.isPassable(blockFeet)) velocity.setY(0.45);
        else if (inWater) velocity.setY(0.15);
        else if (!BlockCompat.isPassable(blockGround)) velocity.setY(Math.min(0.0, velocity.getY()));

        entity.setVelocity(velocity);
        tryBridge(current, direction);
        setSneaking(BlockCompat.isPassable(blockGround) && !inWater && !climbing);
    }

    private void setSprinting(boolean sprinting) {
        if (entity instanceof Player) ((Player) entity).setSprinting(sprinting);
    }

    private void setSneaking(boolean sneaking) {
        if (entity instanceof Player) ((Player) entity).setSneaking(sneaking);
    }

    private boolean isClimbable(Block block) {
        Material type = block.getType();
        return type == MaterialCompat.get("LADDER") || type == MaterialCompat.get("VINE")
            || type == MaterialCompat.get("TWISTING_VINES") || type == MaterialCompat.get("WEEPING_VINES");
    }

    private boolean isLiquidPassable(Block block) {
        Material type = block.getType();
        return type == Material.WATER || type == MaterialCompat.get("STATIONARY_WATER");
    }

    private void tryBridge(Location current, Vector direction) {
        long now = System.currentTimeMillis();
        if (now - lastBridgeTime < BRIDGE_COOLDOWN_MS) return;
        if (direction.lengthSquared() < 0.0001) return;
        Vector horizontal = direction.clone().setY(0);
        if (horizontal.lengthSquared() < 0.0001) return;
        horizontal.normalize();
        Material mat = findBridgeMaterial();
        if (mat == null) return;
        Location ahead = current.clone().add(horizontal);
        Block aheadGround = ahead.clone().add(0, -1, 0).getBlock();
        if (BlockCompat.isPassable(aheadGround) && !isLiquidPassable(aheadGround)) {
            placeBlockAt(aheadGround.getLocation(), mat);
            lastBridgeTime = now;
            return;
        }
        Block currentGround = current.clone().add(0, -1, 0).getBlock();
        if (BlockCompat.isPassable(currentGround) && !isLiquidPassable(currentGround)) {
            placeBlockAt(currentGround.getLocation(), mat);
            lastBridgeTime = now;
        }
    }

    private Material findBridgeMaterial() {
        if (inventory != null) {
            for (Material mat : BRIDGE_MATERIALS) if (inventory.contains(mat)) return mat;
        }
        return Material.COBBLESTONE;
    }

    private void placeBlockAt(Location loc, Material mat) {
        Block block = loc.getBlock();
        if (!BlockCompat.isAir(block)) return;
        block.setType(mat);
        if (inventory != null) inventory.remove(mat, 1);
    }

    public void jump() {
        if (entity != null && !entity.isDead()) entity.setVelocity(entity.getVelocity().setY(0.5));
    }

    private void lookAtNearestPlayer() {
        long now = System.currentTimeMillis();
        if (now - lastIdleLookTime < 1000) return;
        lastIdleLookTime = now;
        Location current = getLocation();
        if (current == null) return;
        World world = current.getWorld();
        Player nearest = null;
        double best = IDLE_LOOK_RADIUS_SQ;
        for (Player p : world.getPlayers()) {
            if (p == entity || p.isDead()) continue;
            double d = current.distanceSquared(p.getLocation());
            if (d < best) { best = d; nearest = p; }
        }
        if (nearest != null) lookAt(nearest.getLocation().add(0, 1.5, 0));
    }

    private static boolean sameWorld(Location a, Location b) {
        return a != null && b != null && a.getWorld().equals(b.getWorld());
    }
}
