package dev.masterattractor.network;

import dev.masterattractor.MasterAttractor;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SyncStatePayload(boolean enabled) implements CustomPacketPayload {
	public static final Type<SyncStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(MasterAttractor.MOD_ID, "sync_state"));
	public static final StreamCodec<ByteBuf, SyncStatePayload> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL,
		SyncStatePayload::enabled,
		SyncStatePayload::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
