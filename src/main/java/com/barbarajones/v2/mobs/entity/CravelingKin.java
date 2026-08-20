package com.barbarajones.v2.mobs.entity;

/**
 * Marker implemented by every hostile member of the Craveling family
 * (Craveling, Krispbone, Loomweaver, Soggy - NOT The Mascot itself). Lets
 * {@code MascotBuffAuraGoal} find "nearby Cravelings" generically instead of
 * hard-coding one class, so a sixth family member added later gets buffed for
 * free just by implementing this.
 */
public interface CravelingKin {
}
