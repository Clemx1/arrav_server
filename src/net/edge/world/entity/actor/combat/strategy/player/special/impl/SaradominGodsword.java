package net.edge.world.entity.actor.combat.strategy.player.special.impl;

import net.edge.content.skill.Skills;
import net.edge.world.Animation;
import net.edge.world.Graphic;
import net.edge.world.entity.actor.Actor;
import net.edge.world.entity.actor.combat.attack.AttackModifier;
import net.edge.world.entity.actor.combat.attack.FightType;
import net.edge.world.entity.actor.combat.hit.Hit;
import net.edge.world.entity.actor.combat.strategy.player.PlayerMeleeStrategy;
import net.edge.world.entity.actor.combat.weapon.WeaponInterface;
import net.edge.world.entity.actor.player.Player;

import java.util.Optional;

/**
 * @author <a href="http://www.rune-server.org/members/stand+up/">Stand Up</a>
 * @since 2-9-2017.
 */
public final class SaradominGodsword extends PlayerMeleeStrategy {
	
	private static final Graphic GRAPHIC = new Graphic(1220);
	private static final Animation ANIMATION = new Animation(7071, Animation.AnimationPriority.HIGH);
	private static final AttackModifier MODIFIER = new AttackModifier().accuracy(0.1).damage(0.8);

	@Override
	public void start(Player attacker, Actor defender, Hit[] hits) {
		super.start(attacker, defender, hits);
		attacker.graphic(GRAPHIC);
	}

	@Override
	public void attack(Player player, Actor defender, Hit h) {
		super.attack(player, defender, h);
		if(h.isAccurate()) {
			int hitpoints = h.getDamage() / 2;//50%
			int prayer = h.getDamage() / 3;//25%
			player.getSkills()[Skills.HITPOINTS].increaseLevel(hitpoints < 100 ? 100 : hitpoints, 990);
			player.getSkills()[Skills.PRAYER].increaseLevel(prayer < 5 ? 5 : prayer / 10, 99);
		}
	}

	@Override
	public void finishOutgoing(Player attacker, Actor defender) {
		WeaponInterface.setStrategy(attacker);
	}

	@Override
	public int getAttackDelay(Player attacker, Actor defender, FightType fightType) {
		return 4;
	}

	@Override
	public Animation getAttackAnimation(Player attacker, Actor defender) {
		return ANIMATION;
	}

	@Override
	public Optional<AttackModifier> getModifier(Player attacker) {
		return Optional.of(MODIFIER);
	}

}