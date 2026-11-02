package dev.masterattractor.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import dev.masterattractor.attraction.AttractionManager;
import dev.masterattractor.config.Config;
import dev.masterattractor.network.SyncStatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class AttractorCommand {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("attractor")
			.then(Commands.literal("toggle")
				.executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					boolean enabled = AttractionManager.togglePlayer(player);
					ServerPlayNetworking.send(player, new SyncStatePayload(enabled));
					Component status = Component.translatable(enabled ? "text.masterattractor.activated" : "text.masterattractor.deactivated");
					ctx.getSource().sendSuccess(() -> Component.translatable("text.masterattractor.prefix").append(status), false);
					return 1;
				})
			)
			.then(Commands.literal("reload")
				.requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
				.executes(ctx -> {
					Config.load();
					ctx.getSource().sendSuccess(() -> Component.translatable("text.masterattractor.reloaded"), true);
					return 1;
				})
			)
			.then(Commands.literal("radius")
				.requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
				.then(Commands.argument("value", DoubleArgumentType.doubleArg(1.0, 48.0))
					.executes(ctx -> {
						double val = DoubleArgumentType.getDouble(ctx, "value");
						Config.radius = val;
						Config.save();
						ctx.getSource().sendSuccess(() -> Component.translatable("text.masterattractor.radius_set", val), true);
						return 1;
					})
				)
			)
			.then(Commands.literal("strength")
				.requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
				.then(Commands.argument("value", DoubleArgumentType.doubleArg(0.1, 10.0))
					.executes(ctx -> {
						double val = DoubleArgumentType.getDouble(ctx, "value");
						Config.strength = val;
						Config.save();
						ctx.getSource().sendSuccess(() -> Component.translatable("text.masterattractor.strength_set", val), true);
						return 1;
					})
				)
			)
		);
	}
}
