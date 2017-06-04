package net.edge.content.skill.summoning.familiar.impl;

import net.edge.util.rand.RandomUtils;
import net.edge.content.dialogue.impl.NpcDialogue;
import net.edge.content.skill.summoning.familiar.Familiar;
import net.edge.content.skill.summoning.familiar.FamiliarAbility;
import net.edge.content.skill.summoning.familiar.impl.forager.ForagerPassiveAbility;
import net.edge.content.skill.summoning.familiar.passive.PassiveAbility;
import net.edge.content.skill.summoning.familiar.passive.impl.PeriodicalAbility;
import net.edge.content.skill.summoning.specials.SummoningData;
import net.edge.world.node.entity.npc.Npc;
import net.edge.world.node.entity.player.Player;
import net.edge.world.node.item.Item;
import net.edge.world.node.item.ItemNodeManager;
import net.edge.world.node.item.ItemNodeStatic;
import net.edge.world.node.region.Region;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents the Albino rat familiar.
 * @author <a href="http://www.rune-server.org/members/stand+up/">Stand Up</a>
 */
public final class AlbinoRat extends Familiar {
	
	/**
	 * Constructs a new {@link AlbinoRat}.
	 */
	public AlbinoRat() {
		super(SummoningData.ALBINO_RAT);
	}
	
	private final ForagerPassiveAbility ability = new ForagerPassiveAbility(1985);
	
	@Override
	public FamiliarAbility getAbilityType() {
		return ability;
	}
	
	private final PeriodicalAbility passiveAbility = new PeriodicalAbility(350, t -> {
		if(!t.getFamiliar().isPresent()) {
			return;
		}
		if(ThreadLocalRandom.current().nextInt(100) < 40) {
			t.getFamiliar().get().forceChat("Rawrgh, mai cheese!");
			return;
		}
		ItemNodeStatic eggs = new ItemNodeStatic(new Item(1985), t.getFamiliar().get().getPosition());
		ItemNodeManager.register(eggs);
	});
	
	@Override
	public Optional<PassiveAbility> getPassiveAbility() {
		return Optional.of(passiveAbility);
	}
	
	@Override
	public boolean isCombatic() {
		return true;
	}
	
	@Override
	public void interact(Player player, Npc npc, int id) {
		if(id == 1) {
			player.getDialogueBuilder().append(RandomUtils.random(RANDOM_DIALOGUE));
		}
	}
	
	private final NpcDialogue[] RANDOM_DIALOGUE = new NpcDialogue[]{new NpcDialogue(getId(), "Hey boss, we doing to do anything wicked today?"), new NpcDialogue(getId(), "You know, boss, I don't think you're totally into", "this whole 'evil' thing."), new NpcDialogue(getId(), "Hey boss, can we go and loot something now?")};
}
