package dev.spearevolution;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Server configuration, stored at config/spear_evolution.json. Reloadable with /spear reload. */
public final class SpearConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static SpearConfig current = new SpearConfig();

    /** Ban the victim when the God Spear kills a player. */
    public boolean banOnGodKill = true;
    public String banReason = "Slain by the God Spear";
    /** Count kills of non-player entities toward progression. */
    public boolean countMobKills = true;
    /** Kills needed per tier upgrade (requirement: 1). */
    public int killsPerUpgrade = 1;
    /** Keep exactly one spear of the player's own tier in the inventory (no skipping tiers, no duplicates). */
    public boolean strictInventory = true;
    public int godAbilityCooldownSeconds = 30;
    /** How long an activated smite stays charged waiting for a hit. */
    public int godAbilityWindowSeconds = 20;
    /** Extra damage of the smite, in hearts (4 hearts = 8 HP). */
    public double godAbilityExtraHearts = 4.0;
    /** How long a shield stays disabled after being broken by a Netherite/God spear. */
    public int shieldDisableSeconds = 10;
    /** If true the shield item is destroyed instead of just disabled. */
    public boolean shieldDestroy = false;

    public static SpearConfig get() { return current; }

    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("spear_evolution.json"); }

    /** Loads the config, writing defaults if absent. On parse errors the previous values are kept. Returns true on success. */
    public static boolean load() {
        Path p = path();
        try {
            if (Files.notExists(p)) {
                current = new SpearConfig();
                save(p);
                return true;
            }
            try (Reader r = Files.newBufferedReader(p)) {
                SpearConfig loaded = GSON.fromJson(r, SpearConfig.class);
                if (loaded == null) throw new IOException("config file is empty");
                loaded.sanitize();
                current = loaded;
            }
            save(p); // rewrite so newly added options appear in the file
            return true;
        } catch (Exception e) {
            SpearEvolution.LOGGER.error("Could not load {}; keeping previous settings", p, e);
            return false;
        }
    }

    private static void save(Path p) throws IOException {
        Files.createDirectories(p.getParent());
        try (Writer w = Files.newBufferedWriter(p)) { GSON.toJson(current, w); }
    }

    private void sanitize() {
        killsPerUpgrade = Math.max(1, killsPerUpgrade);
        godAbilityCooldownSeconds = Math.max(0, godAbilityCooldownSeconds);
        godAbilityWindowSeconds = Math.max(1, godAbilityWindowSeconds);
        godAbilityExtraHearts = Math.max(0, godAbilityExtraHearts);
        shieldDisableSeconds = Math.max(1, shieldDisableSeconds);
        if (banReason == null || banReason.isBlank()) banReason = "Slain by the God Spear";
    }
}
