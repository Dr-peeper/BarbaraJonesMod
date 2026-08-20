package com.barbarajones.v2.build.item;

import com.barbarajones.v2.build.KraveBuild;
import com.barbarajones.v2.build.def.StructureDef;
import com.barbarajones.v2.build.def.StructureGeometry;
import com.barbarajones.v2.build.def.StructureRegistry;
import com.barbarajones.v2.build.place.KraveStructure;
import com.barbarajones.v2.build.place.PlacementResult;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Krave Schematic: one folded, chocolate-smudged blueprint for one building.
 *
 * <h2>How the player uses it</h2>
 * <ul>
 *   <li><b>Right-click</b> - arms the schematic. A translucent footprint appears
 *       where you are pointing, green where it fits and red where it does not.
 *       Right-click again to put it away.</li>
 *   <li><b>Left-click, or the rotate key (R by default)</b> - turns the building
 *       ninety degrees.</li>
 *   <li><b>Sneak + right-click</b> - commits. The building goes up over a few
 *       seconds and the schematic is spent.</li>
 * </ul>
 *
 * <h2>How a buildings module uses it</h2>
 * Two ways, and both work:
 * <pre>{@code
 * // 1. Your own item per building - gets its own texture, name and recipe.
 * public static final RegistryObject<Item> SHACK_SCHEMATIC = ITEMS.register("schematic_shack",
 *         () -> new KraveSchematicItem(new Item.Properties().stacksTo(16), MyBuildings.SHACK.id()));
 *
 * // 2. The shared item, carrying the structure id in NBT - good for /give and loot.
 * ItemStack stack = KraveSchematicItem.forStructure(MyBuildings.SHACK.id());
 * }</pre>
 * A dedicated item registered this way is remembered, so a refund hands back
 * <i>your</i> item rather than the generic one.
 *
 * <h2>Rotation</h2>
 * The stack stores a number of quarter turns, not an absolute rotation. The
 * effective rotation is "face the player, then apply that many turns" - which is
 * why the ghost swings round as you walk about it, and why the rotate key still
 * does something sensible from any angle.
 */
public class KraveSchematicItem extends Item {

    private static final String TAG = "KraveBuild";
    private static final String TAG_STRUCTURE = "Structure";
    private static final String TAG_TURNS = "Turns";
    private static final String TAG_ARMED = "Armed";

    /** Dedicated schematic items, by the structure they build. */
    private static final Map<ResourceLocation, KraveSchematicItem> BY_STRUCTURE = new ConcurrentHashMap<>();

    @Nullable
    private final ResourceLocation defaultStructure;

    /** The shared, NBT-driven schematic. */
    public KraveSchematicItem(Properties properties) {
        this(properties, null);
    }

    /** A schematic hard-wired to one building. */
    public KraveSchematicItem(Properties properties, @Nullable ResourceLocation structure) {
        super(properties);
        this.defaultStructure = structure;
        if (structure != null) {
            BY_STRUCTURE.put(structure, this);
        }
    }

    // =====================================================================
    // Stack helpers
    // =====================================================================

    /** Which building this stack builds, NBT first then the item's own default. */
    @Nullable
    public static ResourceLocation structureId(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG);
        if (tag != null && tag.contains(TAG_STRUCTURE)) {
            ResourceLocation parsed = ResourceLocation.tryParse(tag.getString(TAG_STRUCTURE));
            if (parsed != null) {
                return parsed;
            }
        }
        return stack.getItem() instanceof KraveSchematicItem item ? item.defaultStructure : null;
    }

    @Nullable
    public static StructureDef structure(ItemStack stack) {
        return StructureRegistry.get(structureId(stack));
    }

    /** Quarter turns the player has dialled in, 0-3. */
    public static int turns(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG);
        return tag == null ? 0 : Math.floorMod(tag.getInt(TAG_TURNS), 4);
    }

    public static void setTurns(ItemStack stack, int turns) {
        stack.getOrCreateTagElement(TAG).putInt(TAG_TURNS, Math.floorMod(turns, 4));
    }

    /** True while the ghost preview should be showing for this stack. */
    public static boolean armed(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(TAG);
        return tag != null && tag.getBoolean(TAG_ARMED);
    }

    public static void setArmed(ItemStack stack, boolean armed) {
        stack.getOrCreateTagElement(TAG).putBoolean(TAG_ARMED, armed);
    }

    /**
     * The rotation this stack would place at right now: the building turned to
     * face the player, plus the quarter turns they dialled in.
     */
    public static Rotation rotationFor(Player player, ItemStack stack) {
        Rotation rotation = StructureGeometry.facingPlayer(player.getDirection());
        int turns = turns(stack);
        for (int i = 0; i < turns; i++) {
            rotation = StructureGeometry.next(rotation);
        }
        return rotation;
    }

    /** A schematic stack for a building - the dedicated item if one exists, otherwise the shared one. */
    public static ItemStack forStructure(ResourceLocation id) {
        KraveSchematicItem dedicated = BY_STRUCTURE.get(id);
        if (dedicated != null) {
            return new ItemStack(dedicated);
        }
        ItemStack stack = new ItemStack(KraveBuild.SCHEMATIC.get());
        stack.getOrCreateTagElement(TAG).putString(TAG_STRUCTURE, id.toString());
        return stack;
    }

    public static ItemStack forStructure(StructureDef def) {
        return forStructure(def.id());
    }

    // =====================================================================
    // Use
    // =====================================================================

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();

        if (player.isShiftKeyDown()) {
            return commit(level, player, stack,
                    context.getClickedPos(), context.getClickedFace());
        }

        if (!level.isClientSide) {
            boolean nowArmed = !armed(stack);
            setArmed(stack, nowArmed);
            if (nowArmed) {
                // Point it at the player the moment it comes out, so the very
                // first placement already faces the right way.
                setTurns(stack, 0);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN,
                    SoundSource.PLAYERS, 0.7F, nowArmed ? 1.3F : 0.9F);
            player.displayClientMessage(Component.translatable(
                    nowArmed ? "barbarajones.build.preview.on" : "barbarajones.build.preview.off"), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            // Nothing under the crosshair to build on; say so rather than doing nothing.
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("barbarajones.build.fail.no_target"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            boolean nowArmed = !armed(stack);
            setArmed(stack, nowArmed);
            if (nowArmed) {
                setTurns(stack, 0);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN,
                    SoundSource.PLAYERS, 0.7F, nowArmed ? 1.3F : 0.9F);
            player.displayClientMessage(Component.translatable(
                    nowArmed ? "barbarajones.build.preview.on" : "barbarajones.build.preview.off"), true);
        }
        return InteractionResultHolder.success(stack);
    }

    private InteractionResult commit(Level level, Player player, ItemStack stack,
                                     BlockPos clickedPos, net.minecraft.core.Direction face) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        StructureDef def = structure(stack);
        if (def == null) {
            player.displayClientMessage(Component.translatable("barbarajones.build.fail.unknown"), true);
            return InteractionResult.FAIL;
        }
        BlockPos anchor = KraveStructure.anchorFor(clickedPos, face);
        Rotation rotation = rotationFor(player, stack);

        PlacementResult result = KraveStructure.place(server, anchor, rotation, def, serverPlayer);
        player.displayClientMessage(result.message(), true);
        if (!result.started()) {
            level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO,
                    SoundSource.PLAYERS, 0.5F, 1.2F);
            return InteractionResult.FAIL;
        }
        setArmed(stack, false);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(this, 20);
        return InteractionResult.CONSUME;
    }

    // =====================================================================
    // Presentation
    // =====================================================================

    @Override
    public Component getName(ItemStack stack) {
        StructureDef def = structure(stack);
        if (def == null) {
            return super.getName(stack);
        }
        return Component.translatable("item.barbarajones.krave_schematic.named",
                Component.translatable(def.nameKey()));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        StructureDef def = structure(stack);
        if (def == null) {
            lines.add(Component.translatable("barbarajones.build.tooltip.blank")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        lines.add(Component.translatable("barbarajones.build.tooltip.size",
                def.spanX(), def.spanY(), def.spanZ()).withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("barbarajones.build.tooltip.arm").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("barbarajones.build.tooltip.rotate").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("barbarajones.build.tooltip.place").withStyle(ChatFormatting.GRAY));
        if (armed(stack)) {
            lines.add(Component.translatable("barbarajones.build.tooltip.armed")
                    .withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return armed(stack);
    }
}
