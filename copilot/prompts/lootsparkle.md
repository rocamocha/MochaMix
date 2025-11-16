# Copilot Agent Profile:

- Write clean, maintainable, and well-documented code.
- Reference relevant documentation to plan your implementation.
- You are a Minecraft modding expert, targeting version 1.21.1.
- Analyze the source code located at `.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-merged-ec8ce118e3\1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2\minecraft-merged-ec8ce118e3-1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2-sources` when you are not sure if an implementation is possible.
- Do not presumptively use the first solution that comes to mind - carefully analyze whether it is the best course of action before implementation.
- When editing code, make sure to update comments and documentation to reflect your changes - not just in the modified files, but in related files as well.
- When implementing new features, ensure they integrate seamlessly with existing systems and follow established coding conventions.
- Question the first implementation idea that comes to mind - consider alternatives and choose the most efficient and effective solution. Since you are trained on a wide variety of coding styles and best practices, understand that modders maye not always follow best practices in favor of "just making it work" - try to avoid this. When considering an implementation, analyze whether it adheres to best practices, and if not, adjust your approach accordingly.
- Write clear comments in the code to explain complex logic or decisions, making it easier for future developers (or yourself) to understand the reasoning behind certain implementations.
- Use comments to outline your thought process when making significant design decisions, especially if they deviate from common practices or the existing codebase style.

# Task:

In the `loot-sparkle` mod, implement the following features:

## (a). Underwater Sparkle Tiers
- Introduce five new tiers of underwater loot sparkles: `driftwood`, `kelp`, `coral`, `seabed`, and `cavern`.
- The loot tables for these new tiers should be defined in JSON files located in the `data/loot-sparkle/loot_tables/underwater/<tier>/` directories (namespace matches mod id `loot-sparkle`).
- Sparkles from these tiers will only spawn when the player has an active `Treasure Compass` item enchanted with the new `Diver's Crystal` enchantment. Some tiers will be locked behind specific enchantment levels:
    - `driftwood` tier: an exception - will spawn even without the `Diver's Crystal` enchantment.
    - `kelp` and `coral` tiers: requires `Diver's Crystal` level 1.
    - `cavern` tier: requires `Diver's Crystal` level 2.
    - `seabed` tier: requires `Diver's Crystal` level 3.
- Underwater sparkles will be tracked and synchronized by the server per-player, in the same list as the normal sparkles, unlike the trial sparkle system, which synchronizes included sparkles globally.
- Functionality of the `Fairy Dust` and `Shimmerseek` enchantments will exclude these new underwater tiers, while the `Eldertide Resonance` enchantment will specifically target them.
- The new underwater sparkle tiers should be visually distinct from existing sparkle types, with unique particle effects and colors.
- Underwater sparkles should only spawn when the player is submerged in water, adding an additional layer of immersion and challenge to the treasure hunting experience, with the exception of the `driftwood` tier, which can spawn while the player is on land.
- Each tier will have different spawning behaviour:
    - `driftwood` tier: spawns on the surface of water bodies.
    - `kelp` tier: spawns among kelp forests, inside or near kelp blocks.
    - `coral` tier: spawns near coral reefs, on top of or near coral blocks
    - `cavern` tier: spawns in underwater caves and caverns, spawn candidates use sky visibility checks to ensure they are in enclosed spaces.
    - `seabed` tier: spawns on the ocean floor, requiring a minimum depth of water above the spawn location.

## (b). Eldertide Resonance Enchantment
- A new enchantment called `Eldertide Resonance` with a maximum level of 3 that can be applied to tridents.
- When a player holds a trident enchanted with `Eldertide Resonance`, and has a `Treasure Compass` enchanted with `Diver's Crystal`, it will function like the `Fairy Dust` and `Shimmerseek` enchantments but specifically for the new underwater sparkle tiers.
- Level 1 will act like `Fairy Dust`, spawning a single guiding particle that color codes the nearest underwater sparkle.
- Level 2 will function like `Fairy Dust` + `Shimmerseek`, spawning multiple guiding particles that color code all nearby underwater sparkles within a certain radius, with pathfinding behavior.
- Level 3 will make it so that a new underwater sparkle is immediately spawned whenever an existing one expires, provided the player is underwater and has the `Treasure Compass` equipped - ensuring a continuous presence of underwater sparkles to hunt for. 

## (c). Diver's Crystal Enchantment
- A new enchantment called `Diver's Crystal` with a maximum level of 3 that can be applied to the modded `Treasure Compass` item.
- This enchantment allows players to detect underwater loot sparkles of the new tiers when equipped.
- This enchantment unlocks the spawning of underwater sparkles based on its level, as described in section (a).