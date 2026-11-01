package dev.spearevolution;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A spear. Damage/attack speed live in attribute modifiers; right-click performs the tier's lunge dash. */
public class SpearItem extends Item {
    private static final int LUNGE_COOLDOWN_TICKS = 30;
    private static final Map<UUID, Long> LUNGE_READY_AT = new HashMap<>();

    private final SpearTier tier;

    public SpearItem(Item.Properties properties, SpearTier tier) {
        super(properties);
        this.tier = tier;
    }

    public SpearTier tier() { return tier; }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (tier.lunge() <= 0) return InteractionResult.PASS;
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS; // client: play the swing; the server decides the actual dash
        }
        long now = serverLevel.getGameTime();
        Long readyAt = LUNGE_READY_AT.get(sp.getUUID());
        if (readyAt != null && now < readyAt) return InteractionResult.FAIL;
        LUNGE_READY_AT.put(sp.getUUID(), now + LUNGE_COOLDOWN_TICKS);

        Vec3 look = sp.getLookAngle();
        Vec3 dir = new Vec3(look.x, Math.max(look.y * 0.5, 0.08), look.z).normalize();
        double power = 0.35 + 0.30 * tier.lunge();
        sp.setDeltaMovement(sp.getDeltaMovement().add(dir.scale(power)));
        sp.hurtMarked = true; // makes the server push the new velocity to the client
        serverLevel.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.2F);
        return InteractionResult.SUCCESS;
    }
}
