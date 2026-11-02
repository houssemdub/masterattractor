package dev.masterattractor.mixin;

import dev.masterattractor.attraction.AttractionManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity {
	protected ItemEntityMixin(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void onTickHead(CallbackInfo ci) {
		if (AttractionManager.isItemBeingAttracted(this.getUUID()) || (this.level().isClientSide() && this.isNoGravity())) {
			this.noPhysics = true;
			this.setNoGravity(true);
		}
	}

	@Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
	private void onMoveTowardsClosestSpace(double x, double y, double z, CallbackInfo ci) {
		if (AttractionManager.isItemBeingAttracted(this.getUUID()) || this.isNoGravity()) {
			ci.cancel();
		}
	}

	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
	private void onBeforeMove(CallbackInfo ci) {
		if (AttractionManager.isItemBeingAttracted(this.getUUID()) || (this.level().isClientSide() && this.isNoGravity())) {
			this.noPhysics = true;
		}
	}
}
