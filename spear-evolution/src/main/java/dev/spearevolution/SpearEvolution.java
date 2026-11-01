package dev.spearevolution;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpearEvolution implements ModInitializer {
    public static final String MOD_ID = "spear_evolution";
    public static final Logger LOGGER = LoggerFactory.getLogger("Spear Evolution");

    @Override
    public void onInitialize() {
        SpearConfig.load();
        ModItems.register();
        CombatEvents.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> SpearCommand.register(dispatcher));

        ServerLifecycleEvents.SERVER_STARTED.register(SpearManager::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(SpearManager::onServerStopping);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> SpearManager.syncSpear(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            GodSpearAbility.onDeath(handler.getPlayer()); // drops a pending charge; the cooldown intentionally survives logout
            SpearManager.data().save();
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> SpearManager.syncSpear(newPlayer));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            SpearManager.tick(server);
            GodSpearAbility.tick(server);
        });
        LOGGER.info("Spear Evolution loaded: {} spear tiers registered", SpearTier.values().length);
    }
}
