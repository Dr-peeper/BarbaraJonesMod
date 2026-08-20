package com.barbarajones.v2.quests.client;

import net.minecraft.client.Minecraft;

/**
 * Client-only entry points, kept in their own class so the item can reach them
 * through {@code DistExecutor} without a dedicated-server class-load ever touching a
 * {@code net.minecraft.client} type.
 */
public final class QuestScreens {

    private QuestScreens() {
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new QuestTreeScreen());
    }
}
