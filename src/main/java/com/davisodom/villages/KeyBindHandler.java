package com.davisodom.villages;

import com.davisodom.villages.network.FindVillagesRequestPacket;
import com.davisodom.villages.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Villages.MODID, value = Dist.CLIENT)
public class KeyBindHandler {
    // Add cooldown mechanism (300ms default, reduced from 500ms)
    private static long lastRequestTime = 0;
    private static final long REQUEST_COOLDOWN = 300; // milliseconds
    
    // Add position caching to avoid redundant requests
    private static BlockPos lastRequestPosition = null;
    private static final int POSITION_THRESHOLD = 16; // Only request if player moved this many blocks
    
    // Track when we'll be able to make the next request
    private static long nextAvailableRequestTime = 0;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (KeyBindings.FIND_VILLAGES_KEY.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            Level level = minecraft.level;

            if (player != null && level != null) {
                long currentTime = System.currentTimeMillis();
                BlockPos currentPos = player.blockPosition();
                
                // Check if enough time has passed since last request
                boolean timeElapsed = (currentTime - lastRequestTime) > REQUEST_COOLDOWN;
                
                // Check if player has moved significantly
                boolean hasMoved = lastRequestPosition == null || 
                                   lastRequestPosition.distSqr(currentPos) > (POSITION_THRESHOLD * POSITION_THRESHOLD);
                
                // Only send request if cooldown passed and player moved enough
                if (timeElapsed && hasMoved) {
                    // Send request to server to find villages
                    NetworkHandler.sendToServer(new FindVillagesRequestPacket());
                    
                    // Update state
                    lastRequestTime = currentTime;
                    lastRequestPosition = currentPos;
                    nextAvailableRequestTime = currentTime + REQUEST_COOLDOWN;
                    
                    // Show searching message for immediate feedback
                    player.displayClientMessage(
                        Component.literal("Searching for villages...").withStyle(ChatFormatting.ITALIC),
                        false
                    );
                } else {
                    // Calculate remaining time
                    long remainingTime = Math.max(0, (nextAvailableRequestTime - currentTime) / 1000);
                    
                    if (!hasMoved) {
                        // Player hasn't moved enough
                        player.displayClientMessage(
                            Component.literal("Move at least " + POSITION_THRESHOLD + " blocks to search again.")
                                .withStyle(ChatFormatting.GRAY),
                            false
                        );
                    } else if (remainingTime > 0) {
                        // Still on cooldown
                        player.displayClientMessage(
                            Component.literal("Please wait before searching again.")
                                .withStyle(ChatFormatting.GRAY),
                            false
                        );
                    } else {
                        // Just a short cooldown, but don't annoy the player
                        player.displayClientMessage(
                            Component.literal("Searching too frequently. Wait a moment.")
                                .withStyle(ChatFormatting.GRAY),
                            false
                        );
                    }
                }
            }
        }
    }
}