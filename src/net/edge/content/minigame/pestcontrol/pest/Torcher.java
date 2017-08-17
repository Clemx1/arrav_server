package net.edge.content.minigame.pestcontrol.pest;

import net.edge.util.rand.RandomUtils;
import net.edge.world.entity.actor.mob.Mob;
import net.edge.world.entity.region.TraversalMap;
import net.edge.world.locale.Position;

import java.util.Optional;

public class Torcher extends Pest {
	
	/**
	 * Creates a new {@link Mob}.
	 * @param id       the identification for this NPC.
	 * @param position the position of this character in the world.
	 */
	public Torcher(int id, Position position) {
		super(id, position);
		getAttr().get("master_archery").set(true);
	}
	
	@Override
	public void sequence(Mob knight) {
		if(getNewCombat().getLastAttacker() != null && getNewCombat().getLastAttacker().isPlayer()) {
			getNewCombat().attack(getNewCombat().getLastAttacker());
		} else if(!getPosition().withinDistance(knight.getPosition(), 15) && !getNewCombat().isAttacking()) {
			Position delta = Position.delta(getPosition(), knight.getPosition());
			int x = RandomUtils.inclusive(delta.getX() < 0 ? -delta.getX() : delta.getX());
			int y = RandomUtils.inclusive(delta.getY() < 0 ? -delta.getY() : delta.getY());
			Position move = getPosition().move((delta.getX() < 0 ? -x : x) + RandomUtils.inclusive(3), (delta.getY() < 0 ? -y : y) + RandomUtils.inclusive(3));
			Optional<Position> destination = TraversalMap.getRandomTraversableTile(move, size());
			destination.ifPresent(d -> getMovementQueue().smartWalk(d));
		}
	}
	
	@Override
	public boolean aggressive() {
		return true;
	}
	
	@Override
	public boolean ranged() {
		return true;
	}
}
