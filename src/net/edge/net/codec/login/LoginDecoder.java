package net.edge.net.codec.login;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.Attribute;
import net.edge.net.NetworkConstants;
import net.edge.net.codec.crypto.IsaacRandom;
import net.edge.net.packet.PacketUtils;
import net.edge.net.session.LoginSession;
import net.edge.net.session.Session;
import net.edge.util.TextUtils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;

import static com.google.common.base.Preconditions.checkState;

/**
 * A {@link ByteToMessageDecoder} implementation that decodes the entire login protocol in states.
 * @author lare96 <http://github.org/lare96>
 */
public final class LoginDecoder extends ByteToMessageDecoder {
	
	/**
	 * A cryptographically secure random number generator.
	 */
	private static final Random RANDOM = new SecureRandom();
	
	/**
	 * The current state of decoding the protocol.
	 */
	private State state = State.HANDSHAKE;
	
	/**
	 * The size of the last portion of the protocol.
	 */
	private int rsaBlockSize;
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		switch(state) {
			case HANDSHAKE:
				Attribute<Session> attribute = ctx.channel().attr(NetworkConstants.SESSION_KEY);
				attribute.set(new LoginSession(ctx.channel()));
				decodeHandshake(ctx, in, out);
				state = State.LOGIN_TYPE;
				break;
			case LOGIN_TYPE:
				decodeLoginType(ctx, in, out);
				state = State.RSA_BLOCK;
				break;
			case RSA_BLOCK:
				decodeRsaBlock(ctx, in, out);
				break;
		}
	}
	
	/**
	 * Decodes the handshake portion of the login protocol.
	 * @param ctx The channel handler context.
	 * @param in  The data that is being decoded.
	 * @param out The list of decoded messages.
	 * @throws Exception If any exceptions occur while decoding this portion of the protocol.
	 */
	private void decodeHandshake(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if(in.readableBytes() >= 2) {
			int opcode = in.readUnsignedByte();
			@SuppressWarnings("unused") int nameHash = in.readUnsignedByte();
			checkState(opcode == 14, "id != 14");
			
			ByteBuf buf = ctx.alloc().buffer(17);
			buf.writeLong(0);
			buf.writeByte(0);
			buf.writeLong(RANDOM.nextLong());
			ctx.writeAndFlush(buf, ctx.voidPromise());
		}
	}
	
	/**
	 * Decodes the portion of the login protocol where the login type and RSA block size are determined.
	 * @param ctx The channel handler context.
	 * @param in  The data that is being decoded.
	 * @param out The list of decoded messages.
	 * @throws Exception If any exceptions occur while decoding this portion of the protocol.
	 */
	private void decodeLoginType(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if(in.readableBytes() >= 2) {
			int loginType = in.readUnsignedByte();
			checkState(loginType == 16 || loginType == 18, "loginType != 16 or 18");
			rsaBlockSize = in.readUnsignedByte();
			checkState((rsaBlockSize - 40) > 0, "(rsaBlockSize - 40) <= 0");
		}
	}
	
	/**
	 * Decodes the RSA portion of the login protocol.
	 * @param ctx The channel handler context.
	 * @param in  The data that is being decoded.
	 * @param out The list of decoded messages.
	 */
	private void decodeRsaBlock(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
		if(in.readableBytes() >= rsaBlockSize) {
			int magicId = in.readUnsignedByte();
			checkState(magicId == 255, "magicId != 255");
			int build = in.readUnsignedShort();
			@SuppressWarnings("unused") int memoryVersion = in.readUnsignedByte();
			
			for(int i = 0; i < 9; i++) {
				in.readInt();
			}
			
			int expectedSize = in.readUnsignedByte();
			checkState(expectedSize == (rsaBlockSize - 41), "expectedSize != (rsaBlockSize - 41) -> " + rsaBlockSize);

			byte[] rsaBytes = new byte[rsaBlockSize - 41];
			in.readBytes(rsaBytes);
			byte[] rsaData = new BigInteger(rsaBytes).toByteArray();
			ByteBuf rsaBuffer = Unpooled.wrappedBuffer(rsaData);
			try {
				int rsaOpcode = rsaBuffer.readUnsignedByte();
				checkState(rsaOpcode == 10, "rsaOpcode != 10");

				long clientHalf = rsaBuffer.readLong();
				long serverHalf = rsaBuffer.readLong();
				int[] isaacSeed = {(int) (clientHalf >> 32), (int) clientHalf, (int) (serverHalf >> 32), (int) serverHalf};
				IsaacRandom decryptor = new IsaacRandom(isaacSeed);
				for (int i = 0; i < isaacSeed.length; i++) {
					isaacSeed[i] += 50;
				}
				IsaacRandom encryptor = new IsaacRandom(isaacSeed);

				@SuppressWarnings("unused") int uid = rsaBuffer.readInt();
				String username = PacketUtils.getCString(rsaBuffer).toLowerCase().trim();
				String password = PacketUtils.getCString(rsaBuffer).toLowerCase();
				long usernameHash = TextUtils.nameToHash(username);
				out.add(new LoginRequest(username, usernameHash, password, build, encryptor, decryptor, ctx.channel().pipeline()));
			} finally {
				if (rsaBuffer.isReadable()) {
					rsaBuffer.release();
				}
			}
		}
	}
	
	/**
	 * An enumerated type whose elements represent the various stages of the login protocol.
	 */
	private enum State {
		HANDSHAKE,
		LOGIN_TYPE,
		RSA_BLOCK
	}
}