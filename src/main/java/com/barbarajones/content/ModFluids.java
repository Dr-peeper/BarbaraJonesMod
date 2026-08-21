package com.barbarajones.content;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
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
import org.joml.Vector3f;

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
                    // Real lava's own values (Forge's ForgeMod.LAVA_TYPE) - density
                    // and viscosity are what actually make wading through it feel
                    // slow, not a MobEffect; motionScale is the third piece of
                    // that, and was never set at all before (defaulted to water's
                    // free-flowing feel despite the high density/viscosity).
                    .density(3000)
                    .viscosity(6000)
                    .motionScale(0.0023000000569969416D)
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

                        // Deliberately NO getOverlayTexture() override - real lava doesn't
                        // have one either (only water does; check vanilla's own assets,
                        // there is no lava equivalent of underwater.png). Lava's actual
                        // "can't see anything" effect is 100% the fog below, not a HUD
                        // overlay layered on top of it - copying that exactly here instead
                        // of the water-style overlay this used to also apply.

                        /** Thick brown murk instead of the default fog color - "hard to see when submerged." */
                        @Override
                        public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level,
                                                       int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor) {
                            return new Vector3f(0.22F, 0.12F, 0.06F);
                        }

                        /** Vision cut down to a few blocks, same as lava - not just tinted, genuinely hard to see. */
                        @Override
                        public void modifyFogRender(Camera camera, net.minecraft.client.renderer.FogRenderer.FogMode mode,
                                                    float renderDistance, float partialTick, float nearDistance,
                                                    float farDistance, com.mojang.blaze3d.shaders.FogShape shape) {
                            com.mojang.blaze3d.systems.RenderSystem.setShaderFogStart(0.0F);
                            com.mojang.blaze3d.systems.RenderSystem.setShaderFogEnd(4.0F);
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
