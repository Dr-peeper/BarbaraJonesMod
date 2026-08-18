package com.barbarajones.content;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;

/**
 * Liquid chocolate - hot, burns most things, and is the only thing in the
 * Krave Kosmos that turns Cayden Super Saiyan (see CaydenCobb.tick() and
 * EventHandler's fluid-damage handler).
 */
public final class ModFluids {

    // ForgeRegistries.FLUID_TYPES.get() resolves eagerly and can be null this
    // early in mod construction (crashed on launch: NPE in getRegistryKey()
    // because "reg" was null). The ResourceKey-based overload resolves lazily
    // instead, which is the same pattern DeferredRegister uses internally for
    // vanilla registries and is safe to call this early.
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, BarbaraJonesMod.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, BarbaraJonesMod.MODID);

    private ModFluids() { }

    private static final ResourceLocation STILL_TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "block/chocolate_still");
    private static final ResourceLocation FLOWING_TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "block/chocolate_flow");

    public static final RegistryObject<FluidType> CHOCOLATE_TYPE = FLUID_TYPES.register("chocolate",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.barbarajones.chocolate")
                    .canSwim(false)
                    .canDrown(false)
                    .canExtinguish(false)
                    .canConvertToSource(false)
                    .density(1400)
                    .viscosity(1400)
                    .temperature(1000)
                    .lightLevel(6)) {
                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override
                        public ResourceLocation getStillTexture() {
                            return STILL_TEXTURE;
                        }

                        @Override
                        public ResourceLocation getFlowingTexture() {
                            return FLOWING_TEXTURE;
                        }

                        @Override
                        public int getTintColor() {
                            return 0xFF3A2412;
                        }
                    });
                }
            });

    public static final RegistryObject<FlowingFluid> CHOCOLATE =
            FLUIDS.register("chocolate", () -> new ForgeFlowingFluid.Source(chocolateProperties()));
    public static final RegistryObject<FlowingFluid> CHOCOLATE_FLOWING =
            FLUIDS.register("flowing_chocolate", () -> new ForgeFlowingFluid.Flowing(chocolateProperties()));

    private static ForgeFlowingFluid.Properties chocolateProperties() {
        return new ForgeFlowingFluid.Properties(CHOCOLATE_TYPE, CHOCOLATE, CHOCOLATE_FLOWING)
                .bucket(() -> ModItems.CHOCOLATE_BUCKET.get())
                .block(() -> ModBlocks.CHOCOLATE_BLOCK.get())
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2)
                .tickRate(20)
                .explosionResistance(100.0F);
    }
}
