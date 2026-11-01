package dev.spearevolution;

import java.util.Locale;

/**
 * The ten spear tiers. This enum is deliberately free of Minecraft imports so the balance table can be unit-tested.
 *
 * Damage model: total attack damage = baseDamage + 4 (the required "+2 hearts") + sharpness bonus.
 * Sharpness bonus uses the vanilla formula 0.5 * level + 0.5 but accepts fractional levels (5.5) and levels above
 * the vanilla cap (VIII), which the enchantment registry cannot express, so it is baked into the item's attribute.
 */
public enum SpearTier {
    WOODEN   ("wooden",    1.0, 0.0, 0.0),
    COPPER   ("copper",    2.0, 1.0, 0.5),
    IRON     ("iron",      3.0, 2.0, 1.0),
    GOLD     ("gold",      3.5, 3.0, 1.5),
    LAPIS    ("lapis",     3.5, 4.0, 2.0),
    REDSTONE ("redstone",  3.5, 5.0, 2.5),
    EMERALD  ("emerald",   4.0, 5.0, 3.0),
    DIAMOND  ("diamond",   4.5, 5.0, 3.0),
    NETHERITE("netherite", 5.0, 5.5, 3.5),
    GOD      ("god",       5.0, 8.0, 4.0);

    /** The mandatory extra damage over each spear's base damage: 2 hearts = 4 HP. */
    public static final double BONUS_DAMAGE = 4.0;

    private final String key;
    private final double baseDamage;
    private final double sharpness;
    private final double lunge;

    SpearTier(String key, double baseDamage, double sharpness, double lunge) {
        this.key = key;
        this.baseDamage = baseDamage;
        this.sharpness = sharpness;
        this.lunge = lunge;
    }

    public String key() { return key; }
    public String itemId() { return key + "_spear"; }
    public String displayName() { return key.substring(0, 1).toUpperCase(Locale.ROOT) + key.substring(1) + " Spear"; }
    public double baseDamage() { return baseDamage; }
    public double sharpness() { return sharpness; }
    public double lunge() { return lunge; }
    public int index() { return ordinal(); }

    public double sharpnessBonus() { return sharpness <= 0 ? 0 : 0.5 * sharpness + 0.5; }
    public double totalDamage() { return baseDamage + BONUS_DAMAGE + sharpnessBonus(); }

    public boolean breaksShields() { return this == NETHERITE || this == GOD; }
    public boolean isMax() { return this == GOD; }
    public SpearTier next() { return isMax() ? this : values()[ordinal() + 1]; }

    public static SpearTier byIndex(int i) {
        SpearTier[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, i))];
    }

    /** Accepts a tier name ("god") or a 1-based number ("10"). Returns null when unknown. */
    public static SpearTier parse(String s) {
        if (s == null) return null;
        String t = s.trim().toLowerCase(Locale.ROOT).replace("_spear", "");
        for (SpearTier tier : values()) if (tier.key.equals(t)) return tier;
        try {
            int n = Integer.parseInt(t);
            if (n >= 1 && n <= values().length) return values()[n - 1];
        } catch (NumberFormatException ignored) { }
        return null;
    }
}
