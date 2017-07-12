package net.edge.net;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import net.edge.net.session.Session;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

/**
 * A {@link ChannelInboundHandlerAdapter} implementation that handles upstream messages from Netty.
 * @author Artem Batutin <artembatutin@gmail.com>
 */
@Sharable
public final class EdgevilleUpstreamHandler extends ChannelInboundHandlerAdapter {
	
	/**
	 * A default access level constructor to discourage external instantiation outside of the {@code io.luna.org} package.
	 */
	EdgevilleUpstreamHandler() { }
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
		e.printStackTrace();
	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		Session session = getSession(ctx);
		session.terminate();
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		Session session = getSession(ctx);
		session.handleUpstreamMessage(msg);
	}
	
	/**
	 * Gets the {@link Session} instance from the {@link ChannelHandlerContext}, and validates it to ensure it isn't {@code
	 * null}.
	 * @param ctx The channel handler context.
	 * @return The session instance.
	 */
	private Session getSession(ChannelHandlerContext ctx) {
		Session session = ctx.channel().attr(NetworkConstants.SESSION_KEY).get();
		return requireNonNull(session, "session == null");
	}
}