package com.barbarajones.entity.barbara;

import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.entity.Daniel;
import com.barbarajones.entity.MomCobb;
import com.barbarajones.entity.Nugget;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Friend-or-foe rules for everything Barbara exhales.
 *
 * <p>This class exists so there is exactly one place that decides who eats a
 * cloud. Rule one of this mod is that Cayden Cobb does not die, and Barbara
 * throws area effects for a living - so the crew check lives here, alone, and
 * every ability routes through it rather than writing its own filter.
 */
public final class SmokeTargets {

    private SmokeTargets() { }

    /**
     * The people Barbara would never smoke out. Cayden heads the list and is
     * never removable from it - no ability may ever pass him to a damage or
     * debuff call, whatever state she is in.
     */
    public static boolean isCrew(Entity e) {
        return e instanceof CaydenCobb
                || e instanceof BarbaraJones
                || e instanceof Nugget
                || e instanceof Daniel
                || e instanceof MomCobb
                || e instanceof AbstractVillager
                || (e instanceof TamableAnimal tame && tame.isTame());
    }

    /**
     * Whether Barbara is willing to hit this thing right now. Hostiles always
     * qualify; players only while she has genuinely lost it with them; anything
     * else only if it is already the thing she is swinging at.
     *
     * @param barbara may be null for a projectile that outlived its owner, in
     *                which case only outright hostiles are fair game
     */
    public static boolean isFoe(@Nullable BarbaraJones barbara, Entity e) {
        if (!(e instanceof LivingEntity victim) || !victim.isAlive() || isCrew(e)) {
            return false;
        }
        if (e instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
            return barbara != null && barbara.isMadAtPlayers();
        }
        if (victim instanceof Enemy) {
            return true;
        }
        return barbara != null && victim == barbara.getTarget();
    }

    /** Everything she would hit inside a sphere, nearest-first order not guaranteed. */
    public static List<LivingEntity> foesWithin(BarbaraJones barbara, Vec3 centre, double radius) {
        AABB box = new AABB(centre.x - radius, centre.y - radius, centre.z - radius,
                centre.x + radius, centre.y + radius, centre.z + radius);
        List<LivingEntity> out = new ArrayList<>();
        double r2 = radius * radius;
        for (LivingEntity e : barbara.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (e != barbara && isFoe(barbara, e) && e.distanceToSqr(centre) <= r2) {
                out.add(e);
            }
        }
        return out;
    }

    /** Her side of the fight, for the abilities that help instead of hurt. */
    public static List<LivingEntity> crewWithin(BarbaraJones barbara, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        double r2 = radius * radius;
        for (LivingEntity e : barbara.level().getEntitiesOfClass(LivingEntity.class,
                barbara.getBoundingBox().inflate(radius))) {
            if (e == barbara || e.distanceToSqr(barbara.position()) > r2) {
                continue;
            }
            boolean player = e instanceof Player p && !p.isSpectator();
            if (player || isCrew(e)) {
                out.add(e);
            }
        }
        return out;
    }
}
