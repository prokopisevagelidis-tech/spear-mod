package dev.spearevolution;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Progression logic: kill handling, inventory synchronisation and the God Spear ban. */
public final class SpearManager {
    private static final SpearData DATA = new SpearData();

    private SpearManager() { }

    public static SpearData data() { return DATA; }

    public static void onServerStarted(MinecraftServer server) { DATA.load(server); }
    public static void onServerStopping(MinecraftServer server) { DATA.save(); }

    /** Called for every death. The killer must be a player; progression is server-side only. */
    public static void handleKill(LivingEntity victim, DamageSource source) {
        if (!(source.getEntity() instanceof ServerPlayer killer) || victim == killer || victim instanceof ArmorStand) return;
        SpearConfig cfg = SpearConfig.get();
        boolean victimIsPlayer = victim instanceof ServerPlayer;
        if (!victimIsPlayer && !cfg.countMobKills) return;

        // Read the weapon BEFORE upgrading, otherwise the upgrade swap could change what the killer is holding.
        SpearTier weapon = source.getDirectEntity() == killer ? ModItems.tierOf(killer.getMainHandItem()) : null;

        if (victimIsPlayer && weapon == SpearTier.GOD && cfg.banOnGodKill) banVictim(killer, (ServerPlayer) victim, cfg);

        boolean upgraded = DATA.registerKill(killer.getUUID(), cfg.killsPerUpgrade);
        if (upgraded) {
            SpearTier now = DATA.getTier(killer.getUUID());
            syncSpear(killer);
            killer.sendSystemMessage(Component.literal("Your spear evolved into the " + now.displayName() + "!").withStyle(ChatFormatting.GOLD));
            killer.level().playSound(null, killer.getX(), killer.getY(), killer.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            DATA.save();
        }
    }

    private static void banVictim(ServerPlayer killer, ServerPlayer victim, SpearConfig cfg) {
        MinecraftServer server = killer.level().getServer();
        String killerName = killer.getName().getString();
        String victimName = victim.getName().getString();
        SpearEvolution.LOGGER.info("[GOD SPEAR BAN] {} ({}) slew {} ({}) with the God Spear; banning victim.",
                killerName, killer.getUUID(), victimName, victim.getUUID());
        // Using the vanilla /ban command guarantees the entry is written to banned-players.json (persistent),
        // the victim is kicked, and the ban shows up in /banlist and can be lifted with /pardon.
        String reason = cfg.banReason.replace("\n", " ");
        server.execute(() -> {
            try {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                        "ban " + victimName + " " + reason + " (killed by " + killerName + ")");
            } catch (RuntimeException e) {
                SpearEvolution.LOGGER.error("God Spear ban of {} failed", victimName, e);
            }
        });
    }

    /**
     * Makes the player's inventory consistent with their stored tier: exactly one spear of their tier,
     * in the slot where their old spear was. Missing spears (dropped/lost/died) are re-issued.
     */
    public static void syncSpear(ServerPlayer player) {
        SpearTier tier = DATA.getTier(player.getUUID());
        Inventory inv = player.getInventory();
        boolean kept = false;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            SpearTier t = ModItems.tierOf(s);
            if (t == null) continue;
            if (!kept) {
                if (t != tier) inv.setItem(i, ModItems.create(tier));
                kept = true;
            } else {
                inv.setItem(i, ItemStack.EMPTY); // duplicates / stale tiers are removed
            }
        }
        if (!kept) {
            ItemStack fresh = ModItems.create(tier);
            if (!inv.add(fresh)) player.drop(fresh, false);
        }
    }

    /** Safety net run every second: enforces strictInventory (no tier skipping, spear restored if dropped). */
    public static void tick(MinecraftServer server) {
        if (!SpearConfig.get().strictInventory || server.getTickCount() % 20 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isAlive() || p.isSpectator() || p.hasDisconnected()) continue;
            try {
                if (needsSync(p)) syncSpear(p);
            } catch (RuntimeException e) {
                SpearEvolution.LOGGER.error("Spear sync failed for {}", p.getName().getString(), e);
            }
        }
    }

    private static boolean needsSync(ServerPlayer p) {
        SpearTier tier = DATA.getTier(p.getUUID());
        Inventory inv = p.getInventory();
        int count = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            SpearTier t = ModItems.tierOf(inv.getItem(i));
            if (t == null) continue;
            if (t != tier) return true;
            count++;
        }
        return count != 1;
    }
}
