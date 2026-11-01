package dev.spearevolution;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModItems {
    private static final Map<SpearTier, SpearItem> ITEMS = new EnumMap<>(SpearTier.class);

    private ModItems() { }

    public static void register() {
        for (SpearTier tier : SpearTier.values()) {
            Identifier id = Identifier.fromNamespaceAndPath(SpearEvolution.MOD_ID, tier.itemId());
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
            Item.Properties props = new Item.Properties()
                    .setId(key)
                    .stacksTo(1)
                    .component(DataComponents.ATTRIBUTE_MODIFIERS, attributes(tier))
                    .component(DataComponents.LORE, lore(tier));
            if (tier == SpearTier.GOD) {
                props.rarity(Rarity.EPIC).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            } else if (tier == SpearTier.NETHERITE) {
                props.rarity(Rarity.RARE);
            }
            SpearItem item = new SpearItem(props, tier);
            Registry.register(BuiltInRegistries.ITEM, key, item);
            ITEMS.put(tier, item);
        }
    }

    public static SpearItem get(SpearTier tier) { return ITEMS.get(tier); }
    public static ItemStack create(SpearTier tier) { return new ItemStack(ITEMS.get(tier)); }

    /** Returns the tier of a spear stack, or null if the stack is not one of ours. */
    public static SpearTier tierOf(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof SpearItem s ? s.tier() : null;
    }

    public static boolean isGod(ItemStack stack) { return tierOf(stack) == SpearTier.GOD; }

    private static ItemAttributeModifiers attributes(SpearTier tier) {
        // The player's own base attack damage is 1.0, so the modifier is (total - 1).
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, tier.totalDamage() - 1.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -2.5, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    private static Component line(String text, ChatFormatting color) {
        return Component.literal(text).setStyle(Style.EMPTY.withColor(color).withItalic(false));
    }

    private static String num(double d) { return d == Math.floor(d) ? Long.toString((long) d) : Double.toString(d); }

    private static ItemLore lore(SpearTier tier) {
        List<Component> l = new ArrayList<>();
        l.add(line("Tier " + (tier.index() + 1) + " / " + SpearTier.values().length, ChatFormatting.DARK_GRAY));
        l.add(line("Sharpness " + num(tier.sharpness()) + "   Lunge " + num(tier.lunge()), ChatFormatting.GRAY));
        l.add(line("+2 hearts bonus damage", ChatFormatting.BLUE));
        if (tier.breaksShields()) l.add(line("Breaks shields on impact", ChatFormatting.RED));
        if (tier == SpearTier.GOD) {
            l.add(line("Sneak + swap-hand: charge Divine Smite", ChatFormatting.GOLD));
            l.add(line("Slaying a player bans them", ChatFormatting.DARK_RED));
        }
        return new ItemLore(l);
    }
}
