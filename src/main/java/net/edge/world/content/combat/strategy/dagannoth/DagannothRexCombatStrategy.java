package net.edge.world.content.combat.strategy.dagannoth;

import net.edge.world.content.combat.CombatSessionData;
import net.edge.world.content.combat.CombatType;
import net.edge.world.content.combat.strategy.CombatStrategy;
import net.edge.world.node.entity.EntityNode;

import java.util.Arrays;
import java.util.Objects;

public final class DagannothRexCombatStrategy implements CombatStrategy {

	@Override
	public boolean canOutgoingAttack(EntityNode character, EntityNode victim) {
		return character.isNpc() && victim.isPlayer();
	}
	
	@Override
	public void incomingAttack(EntityNode character, EntityNode attacker, CombatSessionData data) {
		if(data.getType().equals(CombatType.RANGED) || data.getType().equals(CombatType.MELEE)) {
			attacker.toPlayer().message("Your attacks are completely blocked...");
			Arrays.stream(data.getHits()).filter(Objects::nonNull).forEach(h -> h.setAccurate(false));
			return;
		}
	}

	@Override
	public CombatSessionData outgoingAttack(EntityNode character, EntityNode victim) {
		return new CombatSessionData(character, victim, 1, CombatType.MELEE, true);
	}

	@Override
	public int attackDelay(EntityNode character) {
		return character.getAttackSpeed();
	}

	@Override
	public int attackDistance(EntityNode character) {
		return 1;
	}

	@Override
	public int[] getNpcs() {
		return new int[]{2883};
	}

}