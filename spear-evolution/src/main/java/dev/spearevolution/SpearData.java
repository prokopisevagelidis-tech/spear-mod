package dev.spearevolution;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player progression, saved in <world>/spear_evolution/players.json.
 * Keyed by UUID, so progression survives disconnects, restarts, deaths, and dropped/lost spears.
 */
public final class SpearData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final class Entry { int tier; int kills; int progress; }

    private final Map<UUID, Entry> entries = new HashMap<>();
    private Path file;
    private boolean dirty;

    public synchronized void load(MinecraftServer server) {
        entries.clear();
        file = server.getWorldPath(LevelResource.ROOT).resolve("spear_evolution").resolve("players.json");
        if (Files.notExists(file)) return;
        try (Reader r = Files.newBufferedReader(file)) {
            JsonElement root = JsonParser.parseReader(r);
            if (!root.isJsonObject()) return;
            JsonObject players = root.getAsJsonObject().getAsJsonObject("players");
            if (players == null) return;
            for (Map.Entry<String, JsonElement> e : players.entrySet()) {
                try {
                    UUID id = UUID.fromString(e.getKey());
                    Entry en = GSON.fromJson(e.getValue(), Entry.class);
                    en.tier = SpearTier.byIndex(en.tier).index();
                    entries.put(id, en);
                } catch (RuntimeException bad) {
                    SpearEvolution.LOGGER.warn("Skipping corrupt spear entry {}", e.getKey(), bad);
                }
            }
            SpearEvolution.LOGGER.info("Loaded spear progression for {} players", entries.size());
        } catch (IOException | RuntimeException e) {
            SpearEvolution.LOGGER.error("Failed to read {}; a backup copy is kept next to it", file, e);
            try { Files.copy(file, file.resolveSibling("players.json.corrupt"), StandardCopyOption.REPLACE_EXISTING); }
            catch (IOException ignored) { }
        }
    }

    public synchronized void save() {
        if (file == null || !dirty) return;
        try {
            Files.createDirectories(file.getParent());
            JsonObject players = new JsonObject();
            for (Map.Entry<UUID, Entry> e : entries.entrySet()) players.add(e.getKey().toString(), GSON.toJsonTree(e.getValue()));
            JsonObject root = new JsonObject();
            root.addProperty("version", 1);
            root.add("players", players);
            Path tmp = file.resolveSibling("players.json.tmp");
            try (Writer w = Files.newBufferedWriter(tmp)) { GSON.toJson(root, w); }
            try { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException ex) { Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING); }
            dirty = false;
        } catch (IOException e) {
            SpearEvolution.LOGGER.error("Failed to save spear progression to {}", file, e);
        }
    }

    private Entry entry(UUID id) { return entries.computeIfAbsent(id, k -> new Entry()); }

    public synchronized SpearTier getTier(UUID id) { Entry e = entries.get(id); return e == null ? SpearTier.WOODEN : SpearTier.byIndex(e.tier); }
    public synchronized int getKills(UUID id) { Entry e = entries.get(id); return e == null ? 0 : e.kills; }

    public synchronized void setTier(UUID id, SpearTier tier) { Entry e = entry(id); e.tier = tier.index(); e.progress = 0; dirty = true; }
    public synchronized void reset(UUID id) { entries.remove(id); dirty = true; }

    /** Registers one kill. Returns true when the player advanced to the next tier (exactly one tier at a time). */
    public synchronized boolean registerKill(UUID id, int killsPerUpgrade) {
        Entry e = entry(id);
        e.kills++;
        dirty = true;
        if (SpearTier.byIndex(e.tier).isMax()) return false;
        if (++e.progress >= killsPerUpgrade) { e.tier++; e.progress = 0; return true; }
        return false;
    }
}
