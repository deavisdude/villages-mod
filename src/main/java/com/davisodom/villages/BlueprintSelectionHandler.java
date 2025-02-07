package com.davisodom.villages;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BlueprintSelectionHandler {
    // Temporarily hold the first corner per player
    private static final ConcurrentHashMap<UUID, BlockPos> firstCornerMap = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!Config.enableBlueprintSaving)
            return;
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        // Only work if holding a wooden axe
        ItemStack held = player.getMainHandItem();
        if (held.getItem() != Items.WOODEN_AXE)
            return;
        BlockPos pos = event.getPos();
        firstCornerMap.put(player.getUUID(), pos);
        player.sendSystemMessage(
            Component.literal(ChatFormatting.GREEN + "First corner recorded at: " + pos.toShortString())
        );
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!Config.enableBlueprintSaving)
            return;
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;
        // Only work if holding a wooden axe
        ItemStack held = player.getMainHandItem();
        if (held.getItem() != Items.WOODEN_AXE)
            return;

        BlockPos first = firstCornerMap.get(player.getUUID());
        if (first == null) {
            player.sendSystemMessage(
                Component.literal(ChatFormatting.RED + "First corner not set. Use left click with wooden axe to record first corner.")
            );
        } else {
            BlockPos pos = event.getPos();
            player.sendSystemMessage(
                Component.literal(ChatFormatting.GREEN + "Second corner recorded at: " + pos.toShortString() +
                    ". Blueprint selection complete: " + first.toShortString() + " -> " + pos.toShortString())
            );
            BlueprintSelection selection = new BlueprintSelection(first, pos);
            BlueprintSelectionStorage.setSelection(player.getUUID(), selection);
            firstCornerMap.remove(player.getUUID());
        }
    }
}
