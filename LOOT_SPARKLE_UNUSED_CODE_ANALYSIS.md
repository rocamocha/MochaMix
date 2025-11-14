# Loot-Sparkle Unused/Deprecated Code Analysis

## Executive Summary
This analysis identifies unused, duplicate, empty, and deprecated code in the loot-sparkle project that could be candidates for removal. The analysis was performed on 2025-11-13 and examined all 40 Java source files and associated resource files in the project.

## Findings

### 1. DUPLICATE FILE (High Priority)
**File:** `TreasureCompassItem.java` exists in TWO locations
- `/projects/loot-sparkle/common/src/main/java/rocamocha/lootsparkle/TreasureCompassItem.java` (OLD)
- `/projects/loot-sparkle/common/src/main/java/rocamocha/lootsparkle/item/TreasureCompassItem.java` (CURRENT)

**Reasoning:** 
- The file in the old package `rocamocha.lootsparkle` is not imported anywhere in the codebase
- All imports reference `rocamocha.lootsparkle.item.TreasureCompassItem`
- The files are identical duplicates (188 lines each with same content)
- This is likely a refactoring artifact where the file was moved to the `item` package but the old version was not deleted
- Verified with: `grep -r "rocamocha.lootsparkle.TreasureCompassItem" --include="*.java"` returns 0 results

**Recommendation:** Remove `/projects/loot-sparkle/common/src/main/java/rocamocha/lootsparkle/TreasureCompassItem.java`

---

### 2. EMPTY FILES (High Priority)

#### A. Empty Java File
**File:** `AnvilRepairMixin.java`
- **Location:** `/projects/loot-sparkle/common/src/main/java/rocamocha/lootsparkle/mixin/AnvilRepairMixin.java`
- **Size:** 0 bytes (completely empty)

**Reasoning:**
- File exists in the mixin directory but contains no code
- Not registered in `lootsparkle.mixins.json` configuration
- The mixins.json only includes `PlayerMovementMixin` in the client mixins array
- Likely a placeholder file that was created but never implemented
- No actual mixin functionality exists

**Recommendation:** Remove entirely

#### B. Empty JSON Tag Files
**Files:**
- `/projects/loot-sparkle/common/src/main/resources/data/loot-sparkle/tags/items/treasure_compass_enchantable.json` (0 bytes)
- `/projects/loot-sparkle/common/src/main/resources/data/minecraft/tags/items/enchantable_durability.json` (0 bytes)

**Reasoning:**
- These are completely empty files with 0 bytes of content
- Similar functionality exists in other tag files that DO have proper JSON content:
  - `enchantable/treasure_compass.json` - properly defines treasure compass as enchantable
  - `enchantable/compass.json` - adds treasure compass to compass enchantable category
  - `enchantable/durability.json` - adds treasure compass to durability enchantable category
- Empty JSON files serve no purpose and may cause parsing errors or warnings
- The treasure compass enchantability is already handled by the non-empty files

**Recommendation:** Remove both empty JSON files

---

### 3. DEPRECATED METHOD (Medium Priority)
**Method:** `getSparkleLootTable()` in `LootTableIntegration.java`
- **Location:** Lines 246-261
- **Annotation:** Explicitly marked `@Deprecated` with JavaDoc comment "Use generateLootForSparkle with tier instead"

**Reasoning:**
- Has zero call sites in the entire codebase (verified with `grep -r "getSparkleLootTable(" --include="*.java"`)
- Superseded by `generateLootForSparkle()` which is the current implementation used throughout
- The deprecated comment explicitly points to the replacement method
- Contains outdated logic that loads from a single common tier file instead of tier-specific logic
- Keeping deprecated unused methods increases maintenance burden

**Recommendation:** Remove the deprecated method (lines 242-261)

---

### 4. UNUSED CLASS WITH CRITICAL BUG DISCOVERY (High Priority)
**File:** `EnchantmentBootstrap.java`
- **Location:** `/projects/loot-sparkle/common/src/main/java/rocamocha/lootsparkle/enchantment/EnchantmentBootstrap.java`

**Reasoning:**
- This class has ZERO call sites - no imports, no method calls anywhere in the codebase
- There is a similar class `EnchantmentsBootstrap.java` (note the 's') that IS actively used
- `EnchantmentsBootstrap` is called from `EnchantmentsMixin.java` line 22
- Appears to be a duplicate/old version of the bootstrap class that was created during development

**CRITICAL FINDING - BUG DISCOVERED:** 
This reveals an actual BUG in the code - Shimmerseek enchantment is defined but NOT being registered!

Comparison:
- `EnchantmentBootstrap.java` registers: **Soul Sight, Fairy Dust, Shimmerseek** (3 enchantments)
- `EnchantmentsBootstrap.java` registers: **Soul Sight, Fairy Dust** (2 enchantments - MISSING Shimmerseek!)

Evidence of bug:
- `ShimmerseekEnchantment.java` class exists and defines the enchantment
- `ShimmerseekWeightModifier.java` class exists and is used to modify sparkle tier weights based on Shimmerseek
- `shimmerseek.json` data file exists in enchantment definitions
- Multiple references to Shimmerseek in `LootSparkle.java` (lines 241-298 for showing weights)
- But `EnchantmentsBootstrap.java` (the active one) doesn't register it!

**Recommendation:** 
1. First, FIX THE BUG by adding Shimmerseek registration to `EnchantmentsBootstrap.java`
2. Then, remove the unused `EnchantmentBootstrap.java` file

---

### 5. UNUSED RECIPE FILE (Low Priority)
**File:** `treasure_compass_repair.json`
- **Location:** `/projects/loot-sparkle/common/src/main/resources/data/loot-sparkle/recipes/treasure_compass_repair.json`
- **Content:** Defines a recipe type `"loot-sparkle:treasure_compass_repair"`

**Reasoning:**
- No custom recipe serializer or recipe type is registered in Java code
- Search results: `grep -rn "RecipeSerializer|RecipeType|treasure_compass_repair" --include="*.java"` returns 0 Java results
- The actual repair mechanic is implemented directly in `TreasureCompassItem.use()` method (lines 79-152), not via the recipe system
- The repair is handled programmatically when player right-clicks with compass in main hand and repair material in off-hand
- This recipe file would fail to load as the recipe type `loot-sparkle:treasure_compass_repair` doesn't exist and isn't registered
- Minecraft would generate warnings/errors trying to deserialize this unknown recipe type

**Recommendation:** Remove the recipe file as repair is handled programmatically

---

### 6. POTENTIALLY UNUSED CLASS (Low Priority - Needs Verification)

**Class:** `MobEntry.java`
- **Location:** `/projects/loot-sparkle/common/src/main/java/rocamocha/lootsparkle/trial/MobEntry.java`
- **Size:** 40 lines

**Reasoning:**
- No instantiations (`new MobEntry`) found in codebase
- Not imported anywhere except in its own package declaration
- Was likely part of older trial system design that has been refactored
- Has been replaced by `SpawnEntry` class which IS actively used throughout the trial system
- Contains fields: mobId, weight, count, emit (similar to SpawnEntry but simpler)

**CAUTION:**
- Could potentially be used by JSON deserialization (Gson can create objects without explicit constructor calls)
- However, all trial phase JSON files use `SpawnEntry` format, not `MobEntry` format
- Examined phase_sources JSON files and they all use SpawnEntry structure

**Recommendation:** Can likely be removed, but recommend verifying no hidden JSON deserialization usage first. If unsure, mark as deprecated first and remove in next version.

---

### 7. CLASSES VERIFIED AS USED (Not Candidates for Removal)

The following classes were investigated and confirmed to be ACTIVELY USED:

**Class:** `Phase.java` (Not to be confused with `TrialPhase`)
- **Location:** `/projects/loot-sparkle/common/src/main/java/rocamocha/lootsparkle/trial/Phase.java`
- **Status:** ACTIVELY USED - Do NOT remove
- **Usage:** Used by `TrialPhaseLoader.parsePhase()` to load phase configurations from JSON (line 162)
- **Purpose:** Represents intermediate phase data structure loaded from phase_lists JSON files before being converted to TrialPhase objects

**Class:** `PhaseList.java`
- **Location:** `/projects/loot-sparkle/common/src/main/java/rocamocha/lootsparkle/trial/PhaseList.java`
- **Status:** ACTIVELY USED - Do NOT remove
- **Usage:** Used by `TrialPhaseLoader.loadPhaseLists()` (line 89) and `parsePhaseList()` (line 121)
- **Purpose:** Represents phase list configurations loaded from JSON datapack files

**Class:** `PhaseSource.java`
- **Location:** `/projects/loot-sparkle/common/src/main/java/rocamocha/lootsparkle/trial/PhaseSource.java`
- **Status:** ACTIVELY USED - Do NOT remove
- **Usage:** Used by `TrialPhaseLoader.loadPhaseSource()` (line 168) 
- **Purpose:** Represents combat phase sources loaded from phase_sources/combat JSON files

All trial-related classes (Challenge, Count, Duration, Rolls, SpawnEntry, TargetFallingBlockEntity, TrialPhase, TrialPhaseLoader) are part of the hostile sparkle trial mechanics system and are actively used.

---

## Summary of Removal Candidates

### High Priority (Should Remove)
1. ✅ **Duplicate TreasureCompassItem.java** in old package location
   - `/projects/loot-sparkle/common/src/main/java/rocamocha/lootsparkle/TreasureCompassItem.java`
   
2. ✅ **Empty AnvilRepairMixin.java** file
   - `/projects/loot-sparkle/common/src/main/java/rocamocha/lootsparkle/mixin/AnvilRepairMixin.java`
   
3. ✅ **Empty JSON tag files** (2 files)
   - `/projects/loot-sparkle/common/src/main/resources/data/loot-sparkle/tags/items/treasure_compass_enchantable.json`
   - `/projects/loot-sparkle/common/src/main/resources/data/minecraft/tags/items/enchantable_durability.json`

### Medium Priority (Should Remove After Bug Fix)
4. ⚠️ **EnchantmentBootstrap.java** - BUT FIRST fix Shimmerseek registration bug in EnchantmentsBootstrap.java!
   - `/projects/loot-sparkle/common/src/main/java/rocamocha/lootsparkle/enchantment/EnchantmentBootstrap.java`
   
5. ✅ **Deprecated getSparkleLootTable() method** in LootTableIntegration.java (lines 242-261)

### Low Priority (Can Remove)
6. ✅ **treasure_compass_repair.json** recipe file
   - `/projects/loot-sparkle/common/src/main/resources/data/loot-sparkle/recipes/treasure_compass_repair.json`
   
7. ⚠️ **MobEntry.java** class (verify no JSON deserialization usage first)
   - `/projects/loot-sparkle/common/src/main/java/rocamocha/lootsparkle/trial/MobEntry.java`

### NOT Unused (Keep - Verified Active Usage)
- ✓ Phase.java - Used in trial phase loading from JSON
- ✓ PhaseList.java - Used in trial phase loading from JSON
- ✓ PhaseSource.java - Used in combat phase loading from JSON
- ✓ All other trial-related classes - Part of hostile sparkle trial mechanics

---

## Critical Bug Found

**Shimmerseek Enchantment Not Being Registered**

The Shimmerseek enchantment is defined throughout the codebase but is NOT being registered in the active bootstrap class:

**Evidence:**
- ✓ `ShimmerseekEnchantment.java` exists and defines the enchantment builder
- ✓ `ShimmerseekWeightModifier.java` exists and modifies sparkle tier weights
- ✓ `shimmerseek.json` data file exists with enchantment definition
- ✓ Multiple code references in `LootSparkle.java` (lines 241-298)
- ✗ `EnchantmentsBootstrap.java` (active) does NOT register Shimmerseek
- ✓ `EnchantmentBootstrap.java` (unused) DOES register Shimmerseek

**Impact:**
- Shimmerseek enchantment would not be available in-game
- Players cannot obtain the enchantment even though it's fully implemented
- Weight modification system wouldn't work as intended
- Commands referencing Shimmerseek would fail

**Fix Required:**
Add Shimmerseek registration to `EnchantmentsBootstrap.java` (the active file) before removing `EnchantmentBootstrap.java`

Add after line 38 in `EnchantmentsBootstrap.java`:
```java
// Register Shimmerseek enchantment
var shimmerseekKey = RegistryKey.of(RegistryKeys.ENCHANTMENT, Identifier.of("loot-sparkle", "shimmerseek"));
var shimmerseekBuilder = ShimmerseekEnchantment.builder(itemLookup);
registry.register(shimmerseekKey, shimmerseekBuilder.build(shimmerseekKey.getValue()));
```

---

## Statistics

**Total Java files examined:** 40
**Total resource files examined:** ~45

**Findings:**
- **Duplicate files:** 1 (TreasureCompassItem.java)
- **Empty files:** 3 (1 Java, 2 JSON)
- **Deprecated methods:** 1 (getSparkleLootTable)
- **Unused classes:** 2-3 (EnchantmentBootstrap, MobEntry, possibly more)
- **Unused resource files:** 1 (treasure_compass_repair.json)
- **Critical bugs discovered:** 1 (Shimmerseek not registered)

**Total removal candidates:** 7-8 files/methods
**Total estimated lines of code that can be removed:** ~250-300 lines

---

## Methodology

This analysis was performed using the following techniques:

1. **File System Analysis:** Listed all Java and resource files in the project
2. **Duplicate Detection:** Identified duplicate filenames and compared content
3. **Empty File Detection:** Found files with 0 bytes
4. **Usage Analysis:** Searched for imports, instantiations, and method calls using grep
5. **Deprecation Search:** Searched for @Deprecated annotations
6. **Configuration Verification:** Checked registration in mixin configs and JSON files
7. **Cross-Reference Verification:** Verified actual usage patterns across the codebase
8. **JSON Datapack Analysis:** Examined resource files for usage patterns

---

## Recommendations

1. **Immediate Actions (High Priority):**
   - Remove duplicate TreasureCompassItem.java from old location
   - Remove empty AnvilRepairMixin.java
   - Remove empty JSON tag files

2. **After Bug Fix (Medium Priority):**
   - Fix Shimmerseek registration bug in EnchantmentsBootstrap.java
   - Remove unused EnchantmentBootstrap.java
   - Remove deprecated getSparkleLootTable() method

3. **Optional Cleanup (Low Priority):**
   - Remove treasure_compass_repair.json recipe file
   - Verify and remove MobEntry.java if truly unused

4. **Testing After Removal:**
   - Verify mod compiles successfully
   - Test treasure compass functionality
   - Test all three enchantments (Soul Sight, Fairy Dust, Shimmerseek)
   - Test trial sparkle phases to ensure Phase/PhaseList/PhaseSource work correctly
   - Run any existing unit tests

---

## Notes

- This analysis was conducted as a read-only scan with no modifications made
- All findings were verified through code inspection and usage pattern analysis
- The Shimmerseek bug was an unexpected but valuable discovery during this analysis
- Some classes that initially appeared unused (Phase, PhaseList, PhaseSource) were verified as actively used through deeper analysis
- This report provides clear reasoning for each candidate to aid in decision-making

---

**Analysis Date:** 2025-11-13
**Analyzer:** GitHub Copilot
**Project:** loot-sparkle (part of MochaMix repository)
**Branch:** copilot/identify-unused-code
