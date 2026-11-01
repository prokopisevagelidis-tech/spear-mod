package dev.spearevolution;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * God Spear "Divine Smite". Fully server-authoritative:
 *  - Activation: hold the God Spear, sneak, press the swap-hand key (intercepted by a mixin on the server).
 *  - The next successful hit on ANOTHER PLAYER strikes lightning and deals extra damage; then a cooldown starts.
 *  - State is mirrored to the client through action-bar messages sent by the server (no client code required).
 */
public final class GodSpearAbility {
    private static final Map<UUID, Integer> ACTIVE_UNTIL = new HashMap<>();   // server tick
    private static final Map<UUID, Integer> COOLDOWN_UNTIL = new HashMap<>(); // server tick
    private static boolean applyingExtraDamage = false;

    private GodSpearAbility() { }

    private static int now(ServerPlayer p) { return p.level().getServer().getTickCount(); }

    private static void bar(ServerPlayer p, String text, ChatFormatting color) {
        p.displayClientMessage(Component.literal(text).withStyle(color), true);
    }

    private static int secondsLeft(int untilTick, int now) { return (int) Math.ceil((untilTick - now) / 20.0); }

    /** Returns true if the swap-hand packet must be cancelled (i.e. the key press was consumed by the ability). */
    public static boolean tryActivate(ServerPlayer p) {
        if (!p.isShiftKeyDown() || !ModItems.isGod(p.getMainHandItem())) return false;
        int now = now(p);
        Integer cd = COOLDOWN_UNTIL.get(p.getUUID());
        if (cd != null && cd > now) {
            bar(p, "Divine Smite on cooldown: " + secondsLeft(cd, now) + "s", ChatFormatting.RED);
            return true;
        }
        Integer active = ACTIVE_UNTIL.get(p.getUUID());
        if (active != null && active > now) {
            bar(p, "Divine Smite is already charged", ChatFormatting.YELLOW);
            return true;
        }
        ACTIVE_UNTIL.put(p.getUUID(), now + SpearConfig.get().godAbilityWindowSeconds * 20);
        bar(p, "Divine Smite charged - strike a player!", ChatFormatting.GOLD);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.5F);
        return true;
    }

    /** Called after any damage; triggers the smite if the attacker is charged and hit another player. */
    public static void onDamaged(LivingEntity target, DamageSource source, float damageTaken) {
        if (applyingExtraDamage || damageTaken <= 0) return;
        if (!(source.getEntity() instanceof ServerPlayer attacker) || source.getDirectEntity() != attacker) return;
        if (!(target instanceof ServerPlayer victim) || victim == attacker) return;
        if (!ModItems.isGod(attacker.getMainHandItem())) return;

        int now = now(attacker);
        Integer active = ACTIVE_UNTIL.get(attacker.getUUID());
        if (active == null || active <= now) return;
        Integer cd = COOLDOWN_UNTIL.get(attacker.getUUID());
        if (cd != null && cd > now) return;

        ACTIVE_UNTIL.remove(attacker.getUUID());
        SpearConfig cfg = SpearConfig.get();
        COOLDOWN_UNTIL.put(attacker.getUUID(), now + cfg.godAbilityCooldownSeconds * 20);

        ServerLevel level = victim.level();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt != null) {
            bolt.setPos(victim.getX(), victim.getY(), victim.getZ());
            bolt.setVisualOnly(true); // the extra damage below is applied by us, with the attacker credited
            level.addFreshEntity(bolt);
        }
        applyingExtraDamage = true;
        try {
            victim.invulnerableTime = 0; // let the bonus bypass the hit-immunity window of the normal hit
            victim.hurtServer(level, source, (float) (cfg.godAbilityExtraHearts * 2.0));
        } catch (RuntimeException e) {
            SpearEvolution.LOGGER.error("Divine Smite damage failed", e);
        } finally {
            applyingExtraDamage = false;
        }
        bar(attacker, "Divine Smite! Cooldown " + cfg.godAbilityCooldownSeconds + "s", ChatFormatting.GOLD);
    }

    public static void onDeath(LivingEntity entity) {
        if (entity instanceof ServerPlayer p) ACTIVE_UNTIL.remove(p.getUUID());
    }

    /** Every 10 ticks: refresh the cooldown/charge readout for players holding the God Spear and expire stale charges. */
    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        if (now % 10 != 0) return;
        COOLDOWN_UNTIL.values().removeIf(t -> t <= now - 200);
        Iterator<Map.Entry<UUID, Integer>> it = ACTIVE_UNTIL.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null || e.getValue() <= now || !ModItems.isGod(p.getMainHandItem())) {
                it.remove();
                if (p != null) bar(p, "Divine Smite faded", ChatFormatting.GRAY);
            }
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!ModItems.isGod(p.getMainHandItem())) continue;
            Integer active = ACTIVE_UNTIL.get(p.getUUID());
            Integer cd = COOLDOWN_UNTIL.get(p.getUUID());
            if (active != null) bar(p, "Divine Smite charged (" + secondsLeft(active, now) + "s)", ChatFormatting.GOLD);
            else if (cd != null && cd > now) bar(p, "Divine Smite cooldown: " + secondsLeft(cd, now) + "s", ChatFormatting.RED);
        }
    }

    public static void clear(UUID id) { ACTIVE_UNTIL.remove(id); COOLDOWN_UNTIL.remove(id); }
}
