package com.davisodom.villages;

import com.davisodom.villages.network.FindVillagesRequestPacket;
import com.davisodom.villages.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Villages.MODID, value = Dist.CLIENT)
public class KeyBindHandler {
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (KeyBindings.FIND_VILLAGES_KEY.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            Level level = minecraft.level;

            if (player != null && level != null) {
                // Send request to server to find villages
                NetworkHandler.sendToServer(new FindVillagesRequestPacket());
            }
        }
    }
}