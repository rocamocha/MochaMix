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

In the `loot-sparkle` mod, implement the following features / changes:

## Trial Sparkles
- Create a new curse enchantment called "Curse of Treasure" that can be applied to the modded `Treasure Compass` item.
- Trial Sparkle spawn candidates should check first within a 48-block radius for players with the "Curse of Treasure" enchantment.
- When no players with the enchantment are found within range, the Trial Sparkle should not spawn at all.
- Trial Sparkles' auto-activation range should be reduced to 8 blocks.
- Auto-activation should be configurable via the mod's config file, with the default setting being enabled.