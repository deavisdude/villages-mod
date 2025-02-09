package com.davisodom.villages;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BlueprintSelectionHandler {
    private static final ConcurrentHashMap<UUID, BlockPos> firstCornerMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> lastInteractionTime = new ConcurrentHashMap<>();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long INTERACTION_COOLDOWN_MS = 1000; // 1 second cooldown

    public BlueprintSelectionHandler() {
        LOGGER.info("BlueprintSelectionHandler registered successfully");
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!Config.enableBlueprintSaving || event.isCanceled())
            return;
            
        if (event.getLevel().isClientSide())
            return;
            
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
            
        ItemStack held = player.getMainHandItem();
        if (held.getItem() != Items.WOODEN_AXE)
            return;

        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastInteractionTime.get(playerId);
        
        // Check cooldown
        if (lastTime != null && currentTime - lastTime < INTERACTION_COOLDOWN_MS) {
            event.setResult(Result.DENY);
            event.setCanceled(true);
            return;
        }

        BlockPos pos = event.getPos();
        firstCornerMap.put(playerId, pos);
        lastInteractionTime.put(playerId, currentTime);
        
        player.sendSystemMessage(
            Component.literal(ChatFormatting.GREEN + "First corner recorded at: " + pos.toShortString())
        );
        
        event.setResult(Result.DENY);
        event.setCanceled(true);
        LOGGER.debug("Left click corner selection processed at: " + pos.toShortString());
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!Config.enableBlueprintSaving || event.isCanceled())
            return;
            
        if (event.getLevel().isClientSide())
            return;
            
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
            
        ItemStack held = player.getMainHandItem();
        if (held.getItem() != Items.WOODEN_AXE)
            return;

        UUID playerId = player.getUUID();
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastInteractionTime.get(playerId);
        
        // Check cooldown
        if (lastTime != null && currentTime - lastTime < INTERACTION_COOLDOWN_MS) {
            event.setResult(Result.DENY);
            event.setCanceled(true);
            return;
        }

        BlockPos first = firstCornerMap.get(playerId);
        if (first == null) {
            player.sendSystemMessage(
                Component.literal(ChatFormatting.RED + "First corner not set. Use left click with wooden axe to record first corner.")
            );
        } else {
            BlockPos pos = event.getPos();
            lastInteractionTime.put(playerId, currentTime);
            
            player.sendSystemMessage(
                Component.literal(ChatFormatting.GREEN + "Second corner recorded at: " + pos.toShortString() +
                    ". Blueprint selection complete: " + first.toShortString() + " -> " + pos.toShortString())
            );
            BlueprintSelection selection = new BlueprintSelection(first, pos);
            BlueprintSelectionStorage.setSelection(playerId, selection);
        }
        
        event.setResult(Result.DENY);
        event.setCanceled(true);
        LOGGER.debug("Right click corner selection processed at: " + event.getPos().toShortString());
    }

    public static void clearPlayerSelections(UUID playerId) {
        firstCornerMap.remove(playerId);
        lastInteractionTime.remove(playerId);
        BlueprintSelectionStorage.clearSelection(playerId);
    }
}
