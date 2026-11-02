package dev.masterattractor.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.masterattractor.attraction.AttractionManager;
import dev.masterattractor.config.Config;
import dev.masterattractor.network.SyncStatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Locale;

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
				.then(Commands.argument("value", DoubleArgumentType.doubleArg(1.0, 2048.0))
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
			.then(Commands.literal("animation")
				.requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
				.then(Commands.argument("mode", StringArgumentType.word())
					.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
						Arrays.stream(Config.AnimationMode.values()).map(Enum::name), builder))
					.executes(ctx -> {
						String modeStr = StringArgumentType.getString(ctx, "mode");
						try {
							Config.animationMode = Config.AnimationMode.valueOf(modeStr.toUpperCase(Locale.ROOT));
							Config.save();
							ctx.getSource().sendSuccess(() -> Component.translatable("text.masterattractor.animation_set", Config.animationMode.name()), true);
							return 1;
						} catch (IllegalArgumentException e) {
							ctx.getSource().sendFailure(Component.literal("Invalid animation mode. Valid: " + Arrays.toString(Config.AnimationMode.values())));
							return 0;
						}
					})
				)
			)
			.then(Commands.literal("trail")
				.requires(source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()))
				.then(Commands.argument("type", StringArgumentType.word())
					.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
						Arrays.stream(Config.ParticleTrail.values()).map(Enum::name), builder))
					.executes(ctx -> {
						String trailStr = StringArgumentType.getString(ctx, "type");
						try {
							Config.particleTrail = Config.ParticleTrail.valueOf(trailStr.toUpperCase(Locale.ROOT));
							Config.save();
							ctx.getSource().sendSuccess(() -> Component.translatable("text.masterattractor.trail_set", Config.particleTrail.name()), true);
							return 1;
						} catch (IllegalArgumentException e) {
							ctx.getSource().sendFailure(Component.literal("Invalid trail type. Valid: " + Arrays.toString(Config.ParticleTrail.values())));
							return 0;
						}
					})
				)
			)
		);
	}
}