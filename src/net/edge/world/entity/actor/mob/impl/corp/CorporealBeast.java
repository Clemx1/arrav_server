package net.edge.world.entity.actor.mob.impl.corp;

import net.edge.task.Task;
import net.edge.content.skill.summoning.familiar.Familiar;
import net.edge.world.locale.Position;
import net.edge.world.World;
import net.edge.world.entity.EntityState;
import net.edge.world.entity.actor.Actor;
import net.edge.world.entity.actor.mob.Mob;
import net.edge.world.entity.actor.mob.strategy.impl.CorporealBeastStrategy;
import net.edge.world.entity.actor.player.Player;
import net.edge.world.locale.area.AreaManager;

import java.util.Collection;
import java.util.Optional;

/**
 * The class which represents a single corporeal beast boss with it's dark cores.
 * @author <a href="http://www.rune-server.org/members/stand+up/">Stand Up</a>
 */
public final class CorporealBeast extends Mob {
	
	/**
	 * Determines if the corporeal beast task is running for this {@link CorporealBeast}.
	 */
	private Optional<CorporealBeastTask> task = Optional.empty();
	
	/**
	 * The dark energy core this corporeal beast has summoned.
	 */
	private final DarkEnergyCore darkEnergyCore = new DarkEnergyCore(this, new Position(0, 0));
	
	/**
	 * Constructs a new {@link CorporealBeast}.
	 */
	public CorporealBeast() {
		super(8133, new Position(2986, 4381, 2));
		this.setStrategy(Optional.of(new CorporealBeastStrategy(this)));
	}
	
	@Override
	public Mob create() {
		return new CorporealBeast();
	}
	
	@Override
	public void appendDeath() {
		if(!darkEnergyCore.isDead() && darkEnergyCore.getState() == EntityState.ACTIVE) {
			darkEnergyCore.appendDeath();
		}
		
		this.task.ifPresent(t -> {
			t.setRunning(false);
			task = Optional.empty();
		});
		super.appendDeath();
	}
	
	/**
	 * Attempts to devour the players familiar.
	 * @param victim the victim being attacked.
	 * @return {@code true} if the familiar was devoured, {@code false} otherwise.
	 */
	public boolean devourFamiliar(Actor victim) {
		if(victim.isMob()) {
			return false;
		}
		
		Player player = victim.toPlayer();
		
		if(!player.getFamiliar().isPresent()) {
			return false;
		}
		
		Familiar familiar = player.getFamiliar().get();
		
		int hitpoints = (familiar.getCurrentHealth() * 10) / 4;
		
		this.healEntity(hitpoints);
		
		familiar.dismiss(player, false);
		
		player.message("@red@Your familiar was devoured by the Corporeal beast, healing itself effectively...");
		return true;
	}
	
	public Optional<CorporealBeastTask> getTask() {
		return task;
	}
	
	public void setTask() {
		if(task.isPresent()) {
			return;
		}
		this.task = Optional.of(new CorporealBeastTask(this));
		World.get().submit(task.get());
	}
	
	public DarkEnergyCore getDarkEnergyCore() {
		return darkEnergyCore;
	}
	
	/**
	 * The task which is responsible for summoning dark energy cores and replenishing
	 * the corporeal beast when there are no players in the room anymore.
	 * @author <a href="http://www.rune-server.org/members/stand+up/">Stand Up</a>
	 */
	private static final class CorporealBeastTask extends Task {
		
		/**
		 * The corporeal beast this task is for.
		 */
		private final CorporealBeast beast;
		
		/**
		 * Constructs a new {@link CorporealBeastTask}.
		 * @param beast {@link #beast}.
		 */
		CorporealBeastTask(CorporealBeast beast) {
			super(1, false);
			this.beast = beast;
		}
		
		@Override
		protected void execute() {
			beast.getRegion().ifPresent(r -> {
				if(r.getPlayers().isEmpty() || r.getPlayers().stream().noneMatch(player -> AreaManager.get().inArea(player.getPosition(), "CORPOREAL_BEAST"))) {
					beast.healEntity(beast.getMaxHealth());//this is capped.
					beast.task.ifPresent(t -> {
						t.setRunning(false);
						beast.task = Optional.empty();
					});
					
					this.cancel();
					return;
				}
			});
			
			if(beast.isDead() || !beast.task.isPresent()) {
				this.cancel();
				return;
			}
		}
	}
}