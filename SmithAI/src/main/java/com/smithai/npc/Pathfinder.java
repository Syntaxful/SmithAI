package com.smithai.npc;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Simple A* pathfinder for walkable terrain in a single world.
 * This is a starter implementation; it does not handle ladders, water, or complex jumps.
 */
public class Pathfinder {

    // Blocks that damage the NPC or otherwise cannot be safely walked through.
    private static final EnumSet<Material> HAZARDOUS = EnumSet.of(
        Material.LAVA, Material.FIRE, Material.SOUL_FIRE, Material.CACTUS,
        Material.SWEET_BERRY_BUSH, Material.WITHER_ROSE, Material.CAMPFIRE,
        Material.SOUL_CAMPFIRE, Material.MAGMA_BLOCK
    );

    // Blocks the NPC can swim through, but with a movement cost penalty.
    private static final EnumSet<Material> LIQUID_PASSABLE = EnumSet.of(Material.WATER);

    // Blocks that slow the NPC down.
    private static final EnumSet<Material> SLOW = EnumSet.of(Material.SOUL_SAND, Material.SOUL_SOIL, Material.COBWEB);

    private final World world;
    private final int maxNodes;
    private final double maxDistance;
    private final double stepHeight;
    private final double fallHeight;

    public Pathfinder(World world, int maxNodes, double maxDistance, double stepHeight, double fallHeight) {
        this.world = world;
        this.maxNodes = maxNodes;
        this.maxDistance = maxDistance;
        this.stepHeight = stepHeight;
        this.fallHeight = fallHeight;
    }

    public List<Location> findPath(Location start, Location goal) {
        if (start == null || goal == null || !start.getWorld().equals(goal.getWorld())) {
            return Collections.emptyList();
        }
        if (start.distanceSquared(goal) > maxDistance * maxDistance) {
            return Collections.emptyList();
        }

        Node startNode = new Node(floor(start));
        Node goalNode = new Node(floor(goal));

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Set<String> closed = new HashSet<>();
        Map<String, Node> allNodes = new HashMap<>();

        startNode.g = 0;
        startNode.h = heuristic(startNode, goalNode);
        startNode.f = startNode.h;
        open.add(startNode);
        allNodes.put(key(startNode), startNode);

        int nodesExamined = 0;
        while (!open.isEmpty() && nodesExamined < maxNodes) {
            Node current = open.poll();
            if (key(current).equals(key(goalNode))) {
                return reconstructPath(current);
            }
            if (closed.contains(key(current))) continue;
            closed.add(key(current));
            nodesExamined++;

            for (Node neighbor : neighbors(current)) {
                if (!isWalkable(neighbor)) continue;
                String nKey = key(neighbor);
                if (closed.contains(nKey)) continue;

                double moveCost = current.pos.distanceSquared(neighbor.pos) + costFor(current, neighbor);
                double tentativeG = current.g + moveCost;

                Node existing = allNodes.get(nKey);
                if (existing == null) {
                    neighbor.g = tentativeG;
                    neighbor.h = heuristic(neighbor, goalNode);
                    neighbor.f = neighbor.g + neighbor.h;
                    neighbor.parent = current;
                    open.add(neighbor);
                    allNodes.put(nKey, neighbor);
                } else if (tentativeG < existing.g) {
                    existing.g = tentativeG;
                    existing.f = existing.g + existing.h;
                    existing.parent = current;
                    open.remove(existing);
                    open.add(existing);
                }
            }
        }
        return Collections.emptyList();
    }

    private Vector floor(Location loc) {
        return new Vector(Math.floor(loc.getX()), Math.floor(loc.getY()), Math.floor(loc.getZ()));
    }

    private String key(Node node) {
        return key(node.pos);
    }

    private String key(Vector pos) {
        return pos.getBlockX() + "," + pos.getBlockY() + "," + pos.getBlockZ();
    }

    private double heuristic(Node a, Node b) {
        return a.pos.distanceSquared(b.pos);
    }

    private List<Location> reconstructPath(Node node) {
        List<Location> path = new ArrayList<>();
        while (node != null) {
            path.add(node.pos.toLocation(world).add(0.5, 0, 0.5));
            node = node.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private List<Node> neighbors(Node node) {
        List<Node> result = new ArrayList<>();
        Vector base = node.pos.clone();
        // Cardinal + diagonal directions at same level, one up, one down
        int[] dx = {1, -1, 0, 0, 1, 1, -1, -1};
        int[] dz = {0, 0, 1, -1, 1, -1, 1, -1};
        for (int i = 0; i < dx.length; i++) {
            if (dx[i] != 0 && dz[i] != 0) {
                // Diagonal moves require both adjacent cardinal cells to be clear.
                Vector corner1 = base.clone().add(new Vector(dx[i], 0, 0));
                Vector corner2 = base.clone().add(new Vector(0, 0, dz[i]));
                if (!isClear(corner1) || !isClear(corner2)) continue;
            }
            Vector same = base.clone().add(new Vector(dx[i], 0, dz[i]));
            Vector up = base.clone().add(new Vector(dx[i], 1, dz[i]));
            Vector down = base.clone().add(new Vector(dx[i], -1, dz[i]));
            result.add(new Node(same));
            result.add(new Node(up));
            result.add(new Node(down));
        }
        return result;
    }

    private boolean isClear(Vector pos) {
        Block feet = world.getBlockAt(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
        Block head = feet.getRelative(BlockFace.UP);
        return feet.isPassable() && head.isPassable() && !isHazardous(feet) && !isHazardous(head);
    }

    private boolean isWalkable(Node node) {
        Block feet = world.getBlockAt(node.pos.getBlockX(), node.pos.getBlockY(), node.pos.getBlockZ());
        Block head = feet.getRelative(BlockFace.UP);
        Block ground = feet.getRelative(BlockFace.DOWN);

        if (isHazardous(feet) || isHazardous(head) || isHazardous(ground)) return false;

        // Allow swimming through water if the head is clear.
        if (isLiquidPassable(feet) && head.isPassable()) return true;

        if (!feet.isPassable() || !head.isPassable()) return false;
        if (ground.isPassable()) {
            // allow falling up to fallHeight
            for (int i = 2; i <= fallHeight; i++) {
                Block below = feet.getRelative(0, -i, 0);
                if (!below.isPassable()) return true;
            }
            return false;
        }
        return true;
    }

    private double costFor(Node current, Node neighbor) {
        Block feet = world.getBlockAt(neighbor.pos.getBlockX(), neighbor.pos.getBlockY(), neighbor.pos.getBlockZ());
        Block ground = feet.getRelative(BlockFace.DOWN);
        double extra = 0.0;
        if (isLiquidPassable(feet)) extra += 2.0;
        if (SLOW.contains(ground.getType())) extra += 1.5;
        // Discourage falling unless it is the only route.
        double drop = current.pos.getY() - neighbor.pos.getY();
        if (drop > 0) extra += drop * 1.5;
        return extra;
    }

    private boolean isHazardous(Block block) {
        return HAZARDOUS.contains(block.getType());
    }

    private boolean isLiquidPassable(Block block) {
        return LIQUID_PASSABLE.contains(block.getType());
    }

    private static class Node {
        final Vector pos;
        Node parent;
        double g = Double.POSITIVE_INFINITY;
        double h = 0;
        double f = Double.POSITIVE_INFINITY;

        Node(Vector pos) {
            this.pos = pos;
        }
    }
}
