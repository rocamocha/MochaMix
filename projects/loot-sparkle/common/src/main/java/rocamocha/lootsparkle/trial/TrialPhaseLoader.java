package rocamocha.lootsparkle.trial;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import rocamocha.lootsparkle.sparkle.SparkleTier;
import rocamocha.lootsparkle.sparkle.SparkleCategory;

/**
 * Handles loading trial phase configurations from datapack JSON files
 */
public class TrialPhaseLoader {
    private static final String PHASE_SOURCES_PATH = "loot-sparkle:phase_sources/";

    /**
     * Loads trial phases for a specific tier
     */
    public static List<TrialPhase> loadTrialPhases(SparkleTier tier, ResourceManager resourceManager) {
        List<TrialPhase> phases = new ArrayList<>();

        if (tier.getCategory() != SparkleCategory.TRIAL) {
            return phases; // Only trial tiers have phases
        }

        try {
            // Load all phase list files for this tier
            List<PhaseList> phaseLists = loadPhaseLists(tier, resourceManager);
            if (phaseLists.isEmpty()) {
                return getFallbackPhases(tier);
            }

            // Randomly select one phase list
            java.util.Random random = new java.util.Random();
            PhaseList selectedList = phaseLists.get(random.nextInt(phaseLists.size()));

            // Load all phases from the selected phase list
            for (Phase phase : selectedList.getPhases()) {
                // Randomly select a source from the phase
                String selectedSource = phase.getSources().get(
                    random.nextInt(phase.getSources().size())
                );

                if ("challenge".equals(phase.getType())) {
                    // Load challenge source
                    List<Challenge> challenges = loadChallengeSource(selectedSource, resourceManager);
                    if (challenges != null && !challenges.isEmpty()) {
                        phases.add(new TrialPhase(
                            phase.getType(),
                            challenges,
                            phase.getDuration()
                        ));
                    }
                } else {
                    // Load the phase source for combat phases
                    PhaseSource phaseSource = loadPhaseSource(selectedSource, resourceManager);
                    if (phaseSource != null) {
                        phases.add(new TrialPhase(
                            phase.getType(),
                            phaseSource.getSpawns(),
                            phaseSource.getRolls(),
                            phase.getDuration(),
                            phase.getRate(),
                            phase.getBonus()
                        ));
                    }
                }
            }

        } catch (Exception e) {
            // Fallback to default phases
            phases.addAll(getFallbackPhases(tier));
        }

        return phases;
    }

    /**
     * Loads all phase lists for a tier
     */
    private static List<PhaseList> loadPhaseLists(SparkleTier tier, ResourceManager resourceManager) {
        List<PhaseList> phaseLists = new ArrayList<>();
        String tierPath = "phase_lists/" + tier.getNumberedName();

        try {
            // Get all JSON files in the tier directory
            var resources = resourceManager.findResources(tierPath, path -> path.getPath().endsWith(".json"));
            
            for (var entry : resources.entrySet()) {
                try {
                    var resource = entry.getValue();
                    var reader = new InputStreamReader(resource.getInputStream());
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                    PhaseList phaseList = parsePhaseList(json);
                    if (phaseList != null) {
                        phaseLists.add(phaseList);
                    }

                    reader.close();
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }

        return phaseLists;
    }

    /**
     * Parses a PhaseList from JSON
     */
    private static PhaseList parsePhaseList(JsonObject json) {
        if (!json.has("phases")) {
            return null;
        }

        JsonArray phasesArray = json.getAsJsonArray("phases");
        List<Phase> phases = new ArrayList<>();

        for (JsonElement phaseElement : phasesArray) {
            JsonObject phaseObj = phaseElement.getAsJsonObject();
            Phase phase = parsePhase(phaseObj);
            if (phase != null) {
                phases.add(phase);
            }
        }

        return new PhaseList(phases);
    }

    /**
     * Parses a Phase from JSON
     */
    private static Phase parsePhase(JsonObject json) {
        String type = json.get("type").getAsString();
        JsonArray sourcesArray = json.getAsJsonArray("sources");
        List<String> sources = new ArrayList<>();
        for (JsonElement source : sourcesArray) {
            sources.add(source.getAsString());
        }

        Duration duration = null;
        if (json.has("duration")) {
            JsonObject durationObj = json.getAsJsonObject("duration");
            int value = durationObj.get("value").getAsInt();
            String durationType = durationObj.get("type").getAsString();
            duration = new Duration(value, durationType);
        }

        Integer rate = json.has("rate") ? json.get("rate").getAsInt() : 5;
        Integer bonus = json.has("bonus") ? json.get("bonus").getAsInt() : 0;

        return new Phase(type, sources, duration, rate, bonus);
    }

    /**
     * Loads a PhaseSource from JSON
     */
    private static PhaseSource loadPhaseSource(String sourceName, ResourceManager resourceManager) {
        Identifier resourceId = Identifier.of("loot-sparkle", PHASE_SOURCES_PATH.substring("loot-sparkle:".length()) + "combat/" + sourceName + ".json");

        try {
            var resource = resourceManager.getResource(resourceId);
            if (resource.isPresent()) {
                var reader = new InputStreamReader(resource.get().getInputStream());
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                List<SpawnEntry> spawns = parseSpawns(json.getAsJsonArray("spawns"));
                Rolls rolls = parseRolls(json.get("rolls"));

                reader.close();
                return new PhaseSource(spawns, rolls);
            }
        } catch (Exception e) {
        }

        return null;
    }

    /**
     * Loads a Challenge source from JSON
     */
    private static List<Challenge> loadChallengeSource(String sourceName, ResourceManager resourceManager) {
        Identifier resourceId = Identifier.of("loot-sparkle", PHASE_SOURCES_PATH.substring("loot-sparkle:".length()) + "challenge/" + sourceName + ".json");

        try {
            var resource = resourceManager.getResource(resourceId);
            if (resource.isPresent()) {
                var reader = new InputStreamReader(resource.get().getInputStream());
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                List<Challenge> challenges = parseChallenges(json.getAsJsonArray("challenges"));

                reader.close();
                return challenges;
            } else {
            }
        } catch (Exception e) {
        }

        return null;
    }

    /**
     * Parses challenges array from JSON
     */
    private static List<Challenge> parseChallenges(JsonArray challengesArray) {
        List<Challenge> challenges = new ArrayList<>();

        for (JsonElement challengeElement : challengesArray) {
            JsonObject challengeObj = challengeElement.getAsJsonObject();
            Challenge challenge = parseChallenge(challengeObj);
            if (challenge != null) {
                challenges.add(challenge);
            }
        }

        return challenges;
    }

    /**
     * Parses a Challenge from JSON
     */
    private static Challenge parseChallenge(JsonObject json) {
        String type = json.get("type").getAsString();
        boolean camo = json.has("camo") && json.get("camo").getAsBoolean();
        int count = json.get("count").getAsInt();

        return new Challenge(type, camo, count);
    }

    /**
     * Parses spawns array from JSON
     */
    private static List<SpawnEntry> parseSpawns(JsonArray spawnsArray) {
        List<SpawnEntry> spawns = new ArrayList<>();

        for (JsonElement spawnElement : spawnsArray) {
            JsonObject spawnObj = spawnElement.getAsJsonObject();
            SpawnEntry spawn = parseSpawnEntry(spawnObj);
            if (spawn != null) {
                spawns.add(spawn);
            }
        }

        return spawns;
    }

    /**
     * Parses a SpawnEntry from JSON
     */
    private static SpawnEntry parseSpawnEntry(JsonObject json) {
        String mobId = json.get("mobId").getAsString();

        Map<String, String> armor = null;
        if (json.has("armor")) {
            armor = new java.util.HashMap<>();
            JsonObject armorObj = json.getAsJsonObject("armor");
            for (Map.Entry<String, JsonElement> entry : armorObj.entrySet()) {
                armor.put(entry.getKey(), entry.getValue().getAsString());
            }
        }

        Map<String, Object> attributes = null;
        if (json.has("attributes")) {
            attributes = new java.util.HashMap<>();
            JsonObject attrObj = json.getAsJsonObject("attributes");
            for (Map.Entry<String, JsonElement> entry : attrObj.entrySet()) {
                // For now, assume all attributes are numbers
                attributes.put(entry.getKey(), entry.getValue().getAsDouble());
            }
        }

        String name = json.has("name") ? json.get("name").getAsString() : null;
        boolean boss = json.has("boss") && json.get("boss").getAsBoolean();
        Integer weight = json.has("weight") ? json.get("weight").getAsInt() : null;
        Count count = json.has("count") ? parseCount(json.get("count")) : new Count(1);

        return new SpawnEntry(mobId, armor, attributes, name, boss, weight, count);
    }

    /**
     * Parses a Count from JSON
     */
    private static Count parseCount(JsonElement element) {
        if (element.isJsonPrimitive()) {
            return new Count(element.getAsInt());
        } else {
            JsonObject obj = element.getAsJsonObject();
            int min = obj.get("min").getAsInt();
            int max = obj.get("max").getAsInt();
            return new Count(min, max);
        }
    }

    /**
     * Parses Rolls from JSON
     */
    private static Rolls parseRolls(JsonElement element) {
        if (element == null) {
            return new Rolls(1);
        }
        if (element.isJsonPrimitive()) {
            return new Rolls(element.getAsInt());
        } else {
            JsonObject obj = element.getAsJsonObject();
            int min = obj.get("min").getAsInt();
            int max = obj.get("max").getAsInt();
            return new Rolls(min, max);
        }
    }

    /**
     * Gets fallback phases when datapack loading fails
     */
    private static List<TrialPhase> getFallbackPhases(SparkleTier tier) {
        List<TrialPhase> phases = new ArrayList<>();

        // Create fallback spawns
        List<SpawnEntry> spawns = new ArrayList<>();
        spawns.add(new SpawnEntry("minecraft:zombie", null, null, null, false, 10, new Count(1)));
        spawns.add(new SpawnEntry("minecraft:skeleton", null, null, null, false, 8, new Count(1)));
        spawns.add(new SpawnEntry("minecraft:spider", null, null, null, false, 6, new Count(1)));

        Rolls rolls = new Rolls(1, 5);
        // Use generic fallback duration - in practice, the JSON should load properly
        Duration duration = new Duration(30, "advance");

        switch (tier) {
            case CURSED, BLIGHTED -> phases.add(new TrialPhase("emitter", spawns, rolls, duration, 3, 2));
            case DOOMED -> phases.add(new TrialPhase("burst", spawns, rolls, duration, null, 2));
            default -> {} // No phases for non-trial tiers
        }

        return phases;
    }
}
