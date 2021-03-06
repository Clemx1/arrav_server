package net.arrav.net.packet.in;

import io.netty.buffer.ByteBuf;
import net.arrav.net.packet.IncomingPacket;
import net.arrav.world.entity.actor.player.Player;

/**
 * This message sent from the client when the focus of the player's window changes.
 * @author Artem Batutin<artembatutin@gmail.com>
 */
public final class FocusChangePacket implements IncomingPacket {
	
	@Override
	public void handle(Player player, int opcode, int size, ByteBuf buf) {
		boolean focused = buf.get(false) == 1;
		player.screenFocus = focused;
	}
	
}
