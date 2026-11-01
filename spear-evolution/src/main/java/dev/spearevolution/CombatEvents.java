package dev.spearevolution;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Wires combat events: shield breaking (Netherite + God), God smite, progression and ban on death. */
public final class CombatEvents {
    private CombatEvents() { }

    public static void register() {
        // Runs before vanilla block calculation so a broken shield no longer reduces the hit.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            try { breakShield(entity, source); }
            catch (RuntimeException e) { SpearEvolution.LOGGER.error("Shield break failed", e); }
            return true;
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) ->
                GodSpearAbility.onDamaged(entity, source, taken));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            try {
                GodSpearAbility.onDeath(entity);
                SpearManager.handleKill(entity, source);
            } catch (RuntimeException e) {
                SpearEvolution.LOGGER.error("Kill handling failed", e);
            }
        });
    }

    private static void breakShield(LivingEntity target, DamageSource source) {
        if (!source.is(DamageTypes.PLAYER_ATTACK)) return;
        if (!(source.getEntity() instanceof ServerPlayer attacker) || source.getDirectEntity() != attacker) return;
        SpearTier tier = ModItems.tierOf(attacker.getMainHandItem());
        if (tier == null || !tier.breaksShields() || !target.isBlocking()) return;

        InteractionHand hand = target.getUsedItemHand();
        ItemStack shield = target.getUseItem();
        SpearConfig cfg = SpearConfig.get();
        if (cfg.shieldDestroy) {
            target.setItemInHand(hand, ItemStack.EMPTY);
        } else if (target instanceof Player p) {
            p.getCooldowns().addCooldown(shield, cfg.shieldDisableSeconds * 20);
        }
        target.stopUsingItem();
        if (target.level() instanceof ServerLevel level) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
