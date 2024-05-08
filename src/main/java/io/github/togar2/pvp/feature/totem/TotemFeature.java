package io.github.togar2.pvp.feature.totem;

import io.github.togar2.pvp.feature.CombatFeature;
import net.minestom.server.entity.LivingEntity;
import net.minestom.server.entity.damage.DamageType;

public interface TotemFeature extends CombatFeature {
	/**
	 * Returns whether the entity is protected. May also apply (visual) effects.
	 *
	 * @param entity the entity to check for
	 * @param type the type of damage being done to the entity
	 * @return whether the entity is protected by a totem
	 */
	boolean tryProtect(LivingEntity entity, DamageType type);
}
