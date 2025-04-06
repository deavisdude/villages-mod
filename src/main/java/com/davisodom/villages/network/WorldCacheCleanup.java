package com.davisodom.villages.network;

import com.davisodom.villages.Villages;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Handles cleanup of cached data when worlds are unloaded
 * to prevent memory leaks on long-running servers.
 */
@Mod.EventBusSubscriber(modid = Villages.MODID)
public class WorldCacheCleanup {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            LOGGER.debug("World unload detected, cleaning up caches for: {}", 
                serverLevel.dimension().location());
            
            // Clean up the caches for this world
            FindVillagesRequestPacket.clearCacheForLevel(serverLevel);
        }
    }
    
    /**
     * Called periodically to clean up expired caches,
     * providing an additional layer of protection against memory leaks.
     */
    @SubscribeEvent
    public static void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            // Only run this cleanup once every 5 minutes (6000 ticks)
            if (event.getServer().getTickCount() % 6000 == 0) {
                FindVillagesRequestPacket.cleanupExpiredCaches();
            }
        }
    }
}