package com.barbarajones.v2.village;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * The five jobs a Krave Villager can hold.
 *
 * <p>Each profession does three separate things and they are deliberately kept
 * apart so a designer can retune one without disturbing the others:
 *
 * <ol>
 *   <li>a <b>look</b> - one texture per profession, chosen by
 *       {@link #texture()}. There is exactly one entity type and one model;
 *       the profession is a synced entity data value, so an unemployed villager
 *       taking a job is a texture swap, not a re-spawn.
 *   <li>a <b>trade table</b> - see {@code VillageTrades}.
 *   <li>a <b>settlement contribution</b> - {@link #defencePerLevel()},
 *       {@link #productionPerLevel()} and {@link #attractionPerLevel()} are
 *       summed over every living resident and folded into the village's rating.
 *       This is why a village of five Guards has walls but no economy.
 * </ol>
 *
 * <p>Ordinals are persisted in entity NBT and sent over the wire. <b>Append new
 * professions at the end; never reorder these.</b>
 */
public enum KraveProfession {

    /**
     * Sells food and Krave, buys the raw stuff. The economy of the place.
     * Cream apron, cardboard visor, milk-carton satchel.
     */
    GROCER("grocer", 0, 4, 3, 0xE9B23C),

    /**
     * The mod's scientist: studies Krave, produces the most of it, and sells the
     * strange results. Purple lab coat, goggles pushed up on the box.
     */
    CEREALOGIST("cerealogist", 0, 9, 1, 0xB07CF0),

    /**
     * Raises the village's effective building count just by living in it, and
     * sells the blocks to raise it further. Tool belt, chocolate-stained gloves.
     */
    BUILDER("builder", 1, 2, 2, 0x8A5A2A),

    /**
     * The reason raids stop hurting. Contributes the bulk of the defence rating
     * and actively fights hostiles inside the claim. Red plate, box helm, spear.
     */
    GUARD("guard", 5, 0, 0, 0xC81E24),

    /**
     * Pulls newcomers in - the profession that makes a village grow rather than
     * merely exist. Runner's build, satchel of order slips, permanently late.
     */
    COURIER("courier", 1, 1, 6, 0x57B03A);

    private static final KraveProfession[] BY_ORDINAL = values();

    private final String id;
    private final int defencePerLevel;
    private final int productionPerLevel;
    private final int attractionPerLevel;
    private final int accentColor;
    private final ResourceLocation texture;

    KraveProfession(String id, int defencePerLevel, int productionPerLevel,
                    int attractionPerLevel, int accentColor) {
        this.id = id;
        this.defencePerLevel = defencePerLevel;
        this.productionPerLevel = productionPerLevel;
        this.attractionPerLevel = attractionPerLevel;
        this.accentColor = accentColor;
        this.texture = new ResourceLocation(BarbaraJonesMod.MODID,
                "textures/entity/krave_villager/" + id + ".png");
    }

    public String id() {
        return this.id;
    }

    /** Base contribution, multiplied by the villager's trade level (1..5). */
    public int defencePerLevel() {
        return this.defencePerLevel;
    }

    public int productionPerLevel() {
        return this.productionPerLevel;
    }

    public int attractionPerLevel() {
        return this.attractionPerLevel;
    }

    /** Packed 0xRRGGBB, used by the HUD and the trading screen headers. */
    public int accentColor() {
        return this.accentColor;
    }

    public ResourceLocation texture() {
        return this.texture;
    }

    public String translationKey() {
        return "village.barbarajones.profession." + this.id;
    }

    public Component displayName() {
        return Component.translatable(translationKey());
    }

    /** Flavour line shown under the name in the trading screen. */
    public Component tagline() {
        return Component.translatable(translationKey() + ".tagline");
    }

    public static KraveProfession byOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= BY_ORDINAL.length) {
            return GROCER;
        }
        return BY_ORDINAL[ordinal];
    }

    public static KraveProfession random(RandomSource random) {
        return BY_ORDINAL[random.nextInt(BY_ORDINAL.length)];
    }

    public static KraveProfession[] all() {
        return BY_ORDINAL.clone();
    }
}
