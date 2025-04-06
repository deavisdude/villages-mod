package com.davisodom.villages;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final String KEY_CATEGORY_VILLAGES = "key.category.villages";
    public static final String KEY_FIND_VILLAGES = "key.villages.find_villages";
    
    public static final KeyMapping FIND_VILLAGES_KEY = new KeyMapping(
        KEY_FIND_VILLAGES,
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V, // V key
        KEY_CATEGORY_VILLAGES
    );
}