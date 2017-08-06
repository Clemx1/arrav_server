package net.edge.world.entity.actor;

import net.edge.net.packet.out.SendConfig;
import net.edge.task.Task;
import net.edge.world.World;

/**
 * The parent class that handles the death process for all characters.
 * @param <T> the type of actor the death process is being executed for.
 * @author lare96 <http://github.com/lare96>
 */
public abstract class ActorDeath<T extends Actor> extends Task {
	
	/**
	 * The actor who has died and needs the death process.
	 */
	private final T actor;
	
	/**
	 * The counter that will determine which part of the death process we are
	 * on.
	 */
	private int counter;
	
	/**
	 * Creates a new {@link ActorDeath}.
	 * @param actor the actor who has died and needs the death process.
	 */
	public ActorDeath(T actor) {
		super(1, true);
		this.actor = actor;
	}
	
	/**
	 * The part of the death process where the actor is prepared for the
	 * rest of the death process.
	 */
	public abstract void preDeath();
	
	/**
	 * The main part of the death process where the killer is found for the
	 * actor.
	 */
	public abstract void death();
	
	/**
	 * The last part of the death process where the actor is reset.
	 */
	public abstract void postDeath();
	
	@Override
	public final void execute() {
		switch(counter++) {
			case 0:
				getActor().setDead(true);
				getActor().getPoisonDamage().set(0);
				getActor().getMovementQueue().reset();
				getActor().unfreeze();
				getActor().unStun();
				if(getActor().isPlayer()) {
					getActor().toPlayer().out(new SendConfig(174, 0));
				}
				break;
			case 1:
				preDeath();
				break;
			case 5:
				death();
				break;
			case 6:
				postDeath();
				if(getActor().isPlayer()) {
					getActor().setDead(false);
				}
				this.cancel();
				break;
		}
	}
	
	@Override
	public final void onException(Exception e) {
		e.printStackTrace();
		if(getActor().isPlayer()) {
			World.get().queueLogout(getActor().toPlayer());
		} else if(getActor().isMob()) {
			World.get().getMobs().remove(getActor().toMob());
		}
	}
	
	public T getActor() {
		return actor;
	}
	
	@Override
	public int getCounter() {
		return counter;
	}
	
	public void setCounter(int counter) {
		this.counter = counter;
	}
}