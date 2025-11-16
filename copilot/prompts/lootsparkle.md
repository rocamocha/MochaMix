# Copilot Agent Profile:

- Write clean, maintainable, and well-documented code.
- Reference relevant documentation to plan your implementation.
- You are a Minecraft modding expert, targeting version 1.21.1.
- Analyze the source code located at `.gradle\loom-cache\minecraftMaven\net\minecraft\minecraft-merged-ec8ce118e3\1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2\minecraft-merged-ec8ce118e3-1.21.1-net.fabricmc.yarn.1_21_1.1.21.1+build.3-v2-sources` when you are not sure if an implementation is possible.
- Do not presumptively use the first solution that comes to mind - carefully analyze whether it is the best course of action before implementation.
- When editing code, make sure to update comments and documentation to reflect your changes - not just in the modified files, but in related files as well.
- When implementing new features, ensure they integrate seamlessly with existing systems and follow established coding conventions.

# Task:

In the `loot-sparkle` mod, implement the following features:

## (a). Underwater Sparkle Tiers
- Introduce four new tiers of underwater loot sparkles: `driftwood`, `kelp`, `coral`, `seabed`, and `cavern`.
- The loot tables for these new tiers should be defined in JSON files located in the `data/loot_sparkle/loot_tables/underwater/<tier>/` directories.
- Sparkles from these tiers will only spawn when the player has an active `Treasure Compass` item enchanted with the new `Diver's Crystal` enchantment.
- Underwater sparkles will be tracked and synchronized by the server per-player, in the same list as the normal sparkles, unlike the trial sparkle system, which synchronizes included sparkles globally.
- Functionality of the `Shimmerseek` enchantment will exclude these new underwater tiers.
- The new underwater sparkle tiers should be visually distinct from existing sparkle types, with unique particle effects and colors.

## (b). Eldertide Resonance Enchantment
1. A new enchantment called `Eldertide Resonance` with a maximum level of 3 that can be applied to tridents.
2. When a player holds a trident enchanted with `Eldertide Resonance`, and has a `Treasure Compass` enchanted with `Diver's Crystal`, it will function like the `Shimmerseek` enchantment but specifically for the new underwater sparkle tiers.

## (c). Diver's Crystal Enchantment
1. A new enchantment called `Diver's Crystal` with a maximum level of 3 that can be applied to the modded `Treasure Compass` item.
2. This enchantment allows players to detect and spawn underwater loot sparkles of the new tiers when equipped.