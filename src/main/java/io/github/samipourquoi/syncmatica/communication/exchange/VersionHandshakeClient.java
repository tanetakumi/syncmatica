package io.github.samipourquoi.syncmatica.communication.exchange;

import org.apache.logging.log4j.LogManager;

import io.github.samipourquoi.syncmatica.Context;
import io.github.samipourquoi.syncmatica.ServerPlacement;
import io.github.samipourquoi.syncmatica.Syncmatica;
import io.github.samipourquoi.syncmatica.communication.ExchangeTarget;
import io.github.samipourquoi.syncmatica.communication.FeatureSet;
import io.github.samipourquoi.syncmatica.communication.PacketType;
import io.github.samipourquoi.syncmatica.litematica.LitematicManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class VersionHandshakeClient extends AbstractExchange {
	
	private String partnerVersion;
	
	public VersionHandshakeClient(ExchangeTarget partner, Context con) {
		super(partner, con);
	}

	@Override
	public boolean checkPacket(Identifier id, PacketByteBuf packetBuf) {
		return id.equals(PacketType.CONFIRM_USER.IDENTIFIER)
				||id.equals(PacketType.REGISTER_VERSION.IDENTIFIER)
				||id.equals(PacketType.FEATURE_REQUEST.IDENTIFIER);
	}

	@Override
	public void handle(Identifier id, PacketByteBuf packetBuf) {
		if (id.equals(PacketType.REGISTER_VERSION.IDENTIFIER)) {
			String partnerVersion = packetBuf.readString(32767);
			if (!getContext().checkPartnerVersion(partnerVersion)) {
				// any further packets are risky so no further packets should get send
				LogManager.getLogger(VersionHandshakeClient.class).info("Denying syncmatica join due to outdated server with local version {} and server version {}", Syncmatica.VERSION, partnerVersion);
				close(false);
			} else if (id.equals(PacketType.REGISTER_VERSION.IDENTIFIER)) {
				this.partnerVersion = partnerVersion;
				FeatureSet fs = FeatureSet.fromVersionString(partnerVersion);
				if (fs == null) {
					getPartner().sendPacket(PacketType.FEATURE_REQUEST.IDENTIFIER, new PacketByteBuf(Unpooled.buffer()));
				} else {
					PacketByteBuf newBuf = new PacketByteBuf(Unpooled.buffer());
					newBuf.writeString(Syncmatica.VERSION);
					getPartner().sendPacket(PacketType.REGISTER_VERSION.IDENTIFIER, newBuf);
				}
			} else if (id.equals(PacketType.FEATURE_REQUEST.IDENTIFIER)) {
				PacketByteBuf newBuf = new PacketByteBuf(Unpooled.buffer());
				newBuf.writeString(Syncmatica.VERSION);
			}
		} else
		if (id.equals(PacketType.CONFIRM_USER.IDENTIFIER)) {
			int placementCount = packetBuf.readInt();
			for (int i =0; i<placementCount; i++) {
				ServerPlacement p = getManager().receiveMetaData(packetBuf);
				getContext().getSyncmaticManager().addPlacement(p);
			}
			LogManager.getLogger(VersionHandshakeClient.class).info("Joining syncmatica server with local version {} and server version {}", Syncmatica.VERSION, partnerVersion);
			LitematicManager.getInstance().commitLoad();
			getContext().startup();
		}
	}

	@Override
	public void init() {}

	@Override
	protected void sendCancelPacket() {}

}
