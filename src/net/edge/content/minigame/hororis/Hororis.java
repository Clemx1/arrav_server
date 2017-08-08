package net.edge.content.minigame.hororis;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.edge.content.minigame.Minigame;
import net.edge.content.skill.Skills;
import net.edge.net.packet.out.SendFade;
import net.edge.net.packet.out.SendGraphic;
import net.edge.util.rand.RandomUtils;
import net.edge.world.Animation;
import net.edge.world.Hit;
import net.edge.world.World;
import net.edge.world.entity.actor.mob.Mob;
import net.edge.world.entity.actor.mob.impl.DefaultMob;
import net.edge.world.entity.actor.mob.impl.SkeletalHorror;
import net.edge.world.entity.actor.player.Player;
import net.edge.world.entity.item.GroundItem;
import net.edge.world.entity.item.GroundItemPolicy;
import net.edge.world.entity.item.GroundItemStatic;
import net.edge.world.entity.item.Item;
import net.edge.world.entity.region.Region;
import net.edge.world.locale.Position;
import net.edge.world.object.GameObject;

import java.util.Optional;

import static net.edge.content.achievements.Achievement.HORRORIFIC;

/**
 * A custom "Horroris" minigame.
 * @author Artem Batutin <artembatutin@gmail.com>
 */
public class Hororis extends Minigame {
	
	/**
	 * The skeletal horror instance.
	 */
	private final SkeletalHorror horror;
	
	/**
	 * Skeleton minions.
	 */
	private static final ObjectList<Mob> minions = new ObjectArrayList<>();
	
	/**
	 * The ground bones.
	 */
	private static final ObjectList<GroundItem> bones = new ObjectArrayList<>();
	
	/**
	 * The players in the ring.
	 */
	private static final ObjectList<Player> players = new ObjectArrayList<>();
	
	public Hororis(SkeletalHorror horror) {
		super("Hororis", MinigameSafety.SAFE, MinigameType.NORMAL);
		this.horror = horror;
	}
	
	@Override
	public void onLogin(Player player) {
	
	}
	
	@Override
	public void onLogout(Player player) {
		players.remove(player);
		player.getAttr().get("ignoredAggressionLevel").set(false);
		player.out(new SendFade(20, 100, 160));
		player.task(2, pl -> pl.move(new Position(3367, 3513)));
		player.setMinigame(Optional.empty());
	}
	
	@Override
	public void onEnter(Player player) {
		players.add(player);
		player.getAttr().get("ignoredAggressionLevel").set(true);
		player.out(new SendFade(20, 100, 160));
		player.task(2, pl -> pl.move(new Position(3380, 3513)));
		player.setMinigame(this);
	}
	
	@Override
	public boolean onFirstClickNpc(Player player, Mob mob) {
		if(mob.getId() == 9181 || mob.getId() == 9182 || mob.getId() == 9183) {
			player.getSkills()[Skills.PRAYER].increaseExperience(300);
			World.get().getMobs().remove(mob);
			return true;
		}
		player.message("You cannot interact with this mob in here!");
		return false;
	}
	
	@Override
	public boolean onFirstClickObject(Player player, GameObject object) {
		if(object.getId() == 86382) {
			onLogout(player);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean contains(Player player) {
		return players.contains(player);
	}
	
	@Override
	public Position deathPosition(Player player) {
		return new Position(3380, 3513);
	}
	
	public void over() {
		for(Mob m : minions) {
			World.get().getMobs().remove(m);
		}
		minions.clear();
		Region reg = horror.getRegion();
		for(GroundItem b : bones) {
			reg.unregister(b);
		}
		bones.clear();
		for(Player p : players) {
			p.out(new SendFade(20, 100, 160));
			p.task(2, pl -> pl.move(new Position(3367, 3513)));
			p.getSkills()[Skills.PRAYER].increaseExperience(400);
			p.getAttr().get("ignoredAggressionLevel").set(false);
			p.message("The skeletal horror didn't stand a chance! Good job.");
			HORRORIFIC.inc(p);
		}
		players.clear();
	}
	
	public void drop(int part) {
		Mob mob = new DefaultMob(part, getRandom());
		World.get().getMobs().add(mob);
		minions.add(mob);
		if(players.size() > 0) {
			Player p = RandomUtils.random(players);
			if(p != null)
				mob.getCombat().attack(p);
		}
	}
	
	public void bones() {
		for(int i = 0; i < 5; i++) {
			Position pos = getRandom();
			GroundItemStatic item = new GroundItemStatic(new Item(526), pos, GroundItemPolicy.TIMEOUT);
			for(Player p : players) {
				p.out(new SendGraphic(520, pos, 0));
				if(p.getPosition().same(pos)) {
					p.damage(new Hit(RandomUtils.inclusive(20, 100), Hit.HitType.DISEASE, Hit.HitIcon.MAGIC));
				}
			}
			horror.getRegion().register(item);
			bones.add(item);
		}
	}
	
	public void minion() {
		Mob mob = new DefaultMob(RandomUtils.inclusive(10629, 10641), getRandom());
		World.get().getMobs().add(mob);
		minions.add(mob);
		mob.animation(new Animation(9598));
	}
	
	private Position getRandom() {
		return new Position(3382 + RandomUtils.inclusive(10), 3511 + RandomUtils.inclusive(10));
	}
	
	public void message(String message) {
		for(Player p : players) {
			p.message(message);
		}
	}
	
}