package com.davisodom.villages;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BlueprintSelectionStorage {
    private static final Map<UUID, BlueprintSelection> selections = new ConcurrentHashMap<>();

    public static void setSelection(UUID playerId, BlueprintSelection selection) {
        selections.put(playerId, selection);
    }

    public static BlueprintSelection getSelection(UUID playerId) {
        return selections.get(playerId);
    }
}
