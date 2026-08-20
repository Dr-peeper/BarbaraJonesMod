package com.barbarajones.v2.bonds;

import com.barbarajones.entity.CaydenCobb;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;

/**
 * A Cayden who fell out of an over-stuffed Krave Cloning Box (see
 * {@link KraveCloningBoxItem}) instead of being born the normal way, which for
 * this mod's normal way already involves a login handler and a tamed
 * companion, so "normal" was never on the table.
 *
 * <p>Deliberately its own {@code EntityType} rather than a flag on
 * {@code CaydenCobb}: {@link CanonicalGuardEvents} enforces "exactly one
 * original Cayden Cobb" by comparing {@code entity.getClass() ==
 * CaydenCobb.class} exactly, so anything that is a {@code BredCaydenCobb}
 * (a distinct subclass) is automatically exempt from that check and free to
 * exist in as many copies as the player is willing to keep feeding boxes to
 * make. He otherwise IS a real Cayden - same feeding, same ascension ladder,
 * same combat, same housing claim - inherited wholesale rather than
 * reimplemented, because {@code CaydenCobb} was not ours to edit and did not
 * need to be: everything this needs is already public on it.
 *
 * <p><b>Known collision, not fixed here:</b> {@code CaydenCobb.die()} banks
 * ascension progress into a static, owner-UUID-keyed map
 * ({@code ASCENSION_LEGACY}) for the next respawn to pick up, shared by every
 * {@code CaydenCobb} instance including this subclass. If a bred Cayden and
 * the canonical Cayden share an owner and both happen to be dead at once, the
 * second one to die overwrites the first one's banked legacy. See the module
 * doc for the one-line fix ({@code ASCENSION_LEGACY} keyed by entity UUID, or
 * a protected no-op hook this class could override) - it was left alone
 * rather than worked around, because CaydenCobb.java was explicitly off
 * limits to edit.
 */
public class BredCaydenCobb extends CaydenCobb {

    public BredCaydenCobb(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    /** Identical to {@link CaydenCobb#createAttributes()} - a clone is not built different, just born different. */
    public static AttributeSupplier.Builder createAttributes() {
        return CaydenCobb.createAttributes();
    }
}
