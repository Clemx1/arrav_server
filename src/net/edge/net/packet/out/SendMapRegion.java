package net.edge.net.packet.out;

import io.netty.buffer.ByteBuf;
import net.edge.world.locale.Position;
import net.edge.net.codec.ByteTransform;
import net.edge.net.codec.GameBuffer;
import net.edge.net.packet.OutgoingPacket;
import net.edge.world.entity.actor.player.Player;

public final class SendMapRegion implements OutgoingPacket {
	private final Position  position;
	
	public SendMapRegion(Position position) {
		this.position = position;
	}
	
	@Override
	public ByteBuf write(Player player, GameBuffer msg) {
		msg.message(73);
		msg.putShort(position.getRegionX() + 6, ByteTransform.A);
		msg.putShort(position.getRegionY() + 6);
		return msg.getBuffer();
	}
}
