package com.barbarajones.v2.mayor;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

import java.util.List;

/**
 * A permit, a kit, or a charter: the piece of paper the player hands Barbara to
 * commission one project.
 *
 * <p>One item per {@link ProjectKind}, registered in a loop from
 * {@code KraveMayor}, so adding a project adds its item with no further edit.
 * Right-click Barbara with one and the project joins her queue; the permit is
 * spent at that moment, not when the building goes up, because the queue is
 * where the commitment is.
 *
 * <p><b>Names and tooltips are literals, not translation keys.</b> The mod's
 * language file is owned by another part of the project and this module is not
 * allowed to edit it, so a translated name here would ship as
 * {@code item.barbarajones.permit_house} on every player's screen. A literal is
 * the honest option: it is right in English today and it is a
 * search-and-replace away from being translatable the day somebody wants to
 * translate it. See the module report for the lang keys this would need.
 */
public class MayorPermitItem extends Item {

    private final ProjectKind kind;

    public MayorPermitItem(Properties properties, ProjectKind kind) {
        super(properties);
        this.kind = kind;
    }

    /** The project a stack commissions, or null if the stack is not a permit. */
    @Nullable
    public static ProjectKind kindOf(ItemStack stack) {
        return stack.getItem() instanceof MayorPermitItem permit ? permit.kind : null;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal(this.kind.title());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines,
                                TooltipFlag flag) {
        lines.add(Component.literal(this.kind.flavour()).withStyle(ChatFormatting.GRAY));

        MayorRank required = MayorRank.byIndex(this.kind.minRank());
        lines.add(Component.literal("Needs: " + required.title())
                .withStyle(ChatFormatting.DARK_GRAY));

        lines.add(Component.literal("She'll want:").withStyle(ChatFormatting.DARK_GRAY));
        for (ProjectKind.Material material : this.kind.materials()) {
            lines.add(Component.literal("  " + material.count() + "x ")
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .append(material.displayName().copy().withStyle(ChatFormatting.DARK_GRAY)));
        }

        if (this.kind.residents() > 0) {
            lines.add(Component.literal("Moves in: " + this.kind.residents()
                            + (this.kind.residents() == 1 ? " resident" : " residents"))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        lines.add(Component.literal("Hand it to Barbara.").withStyle(ChatFormatting.YELLOW));
    }
}
