package com.davisodom.villages.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

public class DirectionHelper {
    private static final String[] CARDINAL_DIRECTIONS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

    public static String getCardinalDirection(double yaw) {
        // Convert Minecraft's yaw to 0-360 degrees
        yaw = (yaw + 360) % 360;
        // Each cardinal direction takes up 45 degrees
        int index = (int) Math.round(yaw / 45.0) % 8;
        return CARDINAL_DIRECTIONS[index];
    }

    public static String getDirectionTowards(Player player, BlockPos target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        // Convert to 0-360 range
        angle = (angle + 360) % 360;
        int index = (int) Math.round(angle / 45.0) % 8;
        return CARDINAL_DIRECTIONS[index];
    }

    public static double getDistance(Player player, BlockPos target) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static Component formatVillageInfo(Player player, BlockPos villageCenter) {
        String playerFacing = getCardinalDirection(player.getYRot());
        String directionToVillage = getDirectionTowards(player, villageCenter);
        int distance = (int) getDistance(player, villageCenter);
        
        return Component.literal(String.format(
            "Village at (x=%d, z=%d) - %d blocks %s (you're facing %s)",
            villageCenter.getX(), villageCenter.getZ(),
            distance, directionToVillage, playerFacing
        ));
    }
}