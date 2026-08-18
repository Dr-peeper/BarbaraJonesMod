package com.barbarajones.client;

import com.barbarajones.net.PacketApocalypse;

import net.minecraft.client.Minecraft;

/** Client-only entry points, kept off the dedicated server via DistExecutor. */
public final class ClientPacketHandler {

    private ClientPacketHandler() { }

    public static void handleApocalypse(PacketApocalypse msg) {
        ApocalypseClient.handle(msg);
    }

    public static void openQuestBook() {
        Minecraft.getInstance().setScreen(new QuestBookScreen());
    }

    public static void openRecipeBook() {
        Minecraft.getInstance().setScreen(new RecipeBookScreen());
    }

    public static void handleCaydenStatus(com.barbarajones.net.PacketCaydenStatus msg) {
        CaydenHudClient.accept(msg);
    }

    public static void handleKraveHit(com.barbarajones.net.PacketKraveHit msg) {
        KraveHitClient.accept();
    }

    public static void openManual() {
        Minecraft.getInstance().setScreen(new KraveManualScreen());
    }
}
