package com.barbarajones.content.extra;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Registry for the one entity this content pack needs - the recliner seat.
 *
 * <p>It deliberately does NOT go through {@code ModEntities}: the seat is an
 * implementation detail of a single block, it has no spawn egg, no attributes
 * and no gameplay of its own, and keeping it here means the whole extra-content
 * pack is self-contained. Forge's {@link RegisterEvent} is the supported way to
 * register outside a DeferredRegister.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ExtraRegistry {

    /** Tiny, unsummonable, never written to disk - see {@link SeatEntity}. */
    public static final EntityType<SeatEntity> RECLINER_SEAT = EntityType.Builder
            .<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
            .sized(0.01F, 0.01F)
            .clientTrackingRange(8)
            .updateInterval(20)
            .noSummon()
            .noSave()
            .build("recliner_seat");

    private ExtraRegistry() { }

    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.ENTITY_TYPES,
                new ResourceLocation(BarbaraJonesMod.MODID, "recliner_seat"),
                () -> RECLINER_SEAT);
    }
}
