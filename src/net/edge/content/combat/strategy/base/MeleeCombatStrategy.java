package net.edge.content.combat.strategy.base;

import net.edge.content.combat.CombatHit;
import net.edge.content.combat.CombatType;
import net.edge.content.combat.strategy.CombatStrategy;
import net.edge.content.combat.weapon.WeaponInterface;
import net.edge.content.minigame.MinigameHandler;
import net.edge.world.entity.actor.Actor;
import net.edge.world.Animation;
import net.edge.world.entity.actor.mob.Mob;
import net.edge.world.entity.actor.player.Player;

public final class MeleeCombatStrategy implements CombatStrategy {
	
	@Override
	public boolean canOutgoingAttack(Actor character, Actor victim) {
		if(character.isMob()) {
			return true;
		}
		Player player = (Player) character;
		
		if(!MinigameHandler.execute(player, m -> m.canHit(player, victim, CombatType.MELEE))) {
			return false;
		}
		
		player.getCombat().setCombatType(CombatType.MELEE);
		return true;
	}
	
	@Override
	public CombatHit outgoingAttack(Actor character, Actor victim) {
		if(character.isMob()) {
			Mob mob = (Mob) character;
			mob.animation(new Animation(mob.getDefinition().getAttackAnimation()));
		} else if(character.isPlayer()) {
			Player player = (Player) character;
			if(player.getWeaponAnimation() != null && !player.getFightType().isAnimationPrioritized()) {
				player.animation(new Animation(player.getWeaponAnimation().getAttacking()[player.getFightType().getStyle().ordinal()], Animation.AnimationPriority.HIGH));
			} else {
				player.animation(new Animation(player.getFightType().getAnimation(), Animation.AnimationPriority.HIGH));
			}
		}
		return new CombatHit(character, victim, 1, CombatType.MELEE, true);
	}
	
	@Override
	public int attackDelay(Actor character) {
		return character.getAttackDelay();
	}
	
	@Override
	public int attackDistance(Actor character) {
		if(character.isMob())
			return 1;
		if(character.toPlayer().getWeapon() == WeaponInterface.HALBERD)
			return 2;
		return 1;
	}
	
	@Override
	public int[] getMobs() {
		return new int[]{6261};
	}
	
}
