package dev.spearevolution.mixin;

import dev.spearevolution.GodSpearAbility;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts the swap-hand key (default F) on the server. When the player sneaks with the God Spear in hand the key
 * activates Divine Smite instead of swapping items. Server-authoritative: no client code or custom packet needed.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void spearEvolution$onPlayerAction(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (packet.getAction() != ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND) return;
        MinecraftServer server = this.player.level().getServer();
        // The vanilla method re-queues itself onto the main thread; only act once we are on it.
        if (server == null || !server.isSameThread()) return;
        if (GodSpearAbility.tryActivate(this.player)) ci.cancel();
    }
}
