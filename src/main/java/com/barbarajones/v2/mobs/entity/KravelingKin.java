package com.barbarajones.v2.mobs.entity;

/**
 * Marker implemented by every hostile member of the Kraveling family
 * (Kraveling, Krispbone, Loomweaver, Soggy - NOT The Mascot itself). Lets
 * {@code MascotBuffAuraGoal} find "nearby Kravelings" generically instead of
 * hard-coding one class, so a sixth family member added later gets buffed for
 * free just by implementing this.
 */
public interface KravelingKin {
}
