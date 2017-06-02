package net.edge.content.skill.prayer;

import net.edge.task.Task;
import net.edge.content.skill.SkillData;
import net.edge.content.skill.action.impl.DestructionSkillAction;
import net.edge.world.Animation;
import net.edge.world.node.entity.player.Player;
import net.edge.world.node.item.Item;
import net.edge.world.object.ObjectNode;

import java.util.Optional;

public final class PrayerBoneAltar extends DestructionSkillAction {
	
	private final Bone bone;
	
	public PrayerBoneAltar(Player player, int itemId, ObjectNode object) {
		super(player, Optional.of(object.getGlobalPos()));
		this.bone = Bone.getBone(itemId).orElse(null);
	}
	
	public static boolean produce(Player player, int itemId, ObjectNode object) {
		if(object.getId() != 409) {
			return false;
		}
		Optional<Bone> bone = Bone.getBone(itemId);
		
		if(!bone.isPresent()) {
			return false;
		}
		
		PrayerBoneAltar altarAction = new PrayerBoneAltar(player, itemId, object);
		altarAction.start();
		return true;
	}
	
	@Override
	public boolean init() {
		return true;
	}
	
	@Override
	public boolean canExecute() {
		return true;
	}
	
	@Override
	public Item destructItem() {
		return new Item(bone.getId());
	}
	
	@Override
	public void onDestruct(Task t, boolean success) {
		if(success) {
			getPlayer().animation(new Animation(713));
			getPlayer().getMessages().sendLocalGraphic(624, position.get(), 0);
			getPlayer().message("You offer the " + bone + " to the gods... they seem pleased.");
		}
	}
	
	@Override
	public int delay() {
		return 6;
	}
	
	@Override
	public boolean instant() {
		return true;
	}
	
	@Override
	public double experience() {
		return (bone.getExperience() * 2);
	}
	
	@Override
	public SkillData skill() {
		return SkillData.PRAYER;
	}
}