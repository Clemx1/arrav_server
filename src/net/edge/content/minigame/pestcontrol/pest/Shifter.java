package net.edge.content.minigame.pestcontrol.pest;

import net.edge.world.entity.region.TraversalMap;
import net.edge.world.locale.Position;
import net.edge.util.rand.RandomUtils;
import net.edge.world.Graphic;
import net.edge.world.entity.actor.mob.Mob;

import java.util.Optional;

public class Shifter extends Pest {
	
	/**
	 * Creates a new {@link Mob}.
	 * @param id       the identification for this NPC.
	 * @param position the position of this character in the world.
	 */
	public Shifter(int id, Position position) {
		super(id, position);
	}
	
	@Override
	public void sequence(Mob knight) {
		//teleporting towards the knight.
		if((!getPosition().withinDistance(knight.getPosition(), 6) && getNewCombat().getLastAttacker() == null) || RandomUtils.inclusive(3) == 1) {
			Position delta = Position.delta(getPosition(), knight.getPosition());
			int x = RandomUtils.inclusive(delta.getX() < 0 ? -delta.getX() : delta.getX());
			int y = RandomUtils.inclusive(delta.getY() < 0 ? -delta.getY() : delta.getY());
			Position move = getPosition().move((delta.getX() < 0 ? -x : x) + RandomUtils.inclusive(3), (delta.getY() < 0 ? -y : y) + RandomUtils.inclusive(3));
			Optional<Position> destination = TraversalMap.getRandomTraversableTile(move, size());
			destination.ifPresent(this::move);
			graphic(new Graphic(308, 100));
			getNewCombat().reset();
		}
		
		if(!getNewCombat().isAttacking()) {
			if(getNewCombat().getLastAttacker().isPlayer())
				getNewCombat().attack(getNewCombat().getLastAttacker());
			else if(getPosition().withinDistance(knight.getPosition(), 6))
				getNewCombat().attack(knight);
		}
	}
	
	@Override
	public boolean aggressive() {
		return true;
	}
	
}
