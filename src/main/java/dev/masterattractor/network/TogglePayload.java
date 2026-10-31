package dev.masterattractor.network;

import dev.masterattractor.MasterAttractor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record TogglePayload() implements CustomPacketPayload {
	public static final TogglePayload INSTANCE = new TogglePayload();
	public static final Type<TogglePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MasterAttractor.MOD_ID, "toggle"));
	public static final StreamCodec<ByteBuf, TogglePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
