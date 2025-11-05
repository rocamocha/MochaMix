# Loot Sparkle JSON Structures Documentation

This document outlines the JSON configuration structures used in the Loot Sparkle mod for defining trial phases, spawn configurations, and challenges.

## Phase Lists (`phase_lists/*/phase*.json`)

Phase lists define the sequence of phases for a trial sparkle. Each phase list contains an array of phases that execute in order.

### Structure
```json
{
    "phases": [
        {
            "type": "emitter|burst|boss|challenge|puzzle",
            "rate": number,           // (emitter only) seconds between spawns
            "bonus": number,          // (emitter only) bonus seconds added when mob killed
            "sources": ["string"],    // source file names (without .json extension)
            "duration": {
                "value": number,       // duration in seconds
                "type": "limit|timer"  // limit = max time, timer = countdown
            }
        }
    ]
}
```

### Phase Types
- **emitter**: Continuously spawns mobs at regular intervals
- **burst**: Spawns all mobs at once (not yet implemented)
- **boss**: Single powerful mob spawn (not yet implemented)
- **challenge**: Target practice or skill-based challenges
- **puzzle**: Logic puzzles (not yet implemented)

### Example
```json
{
    "phases": [
        {
            "type": "emitter",
            "rate": 5,
            "bonus": 5,
            "sources": ["zombie_horde", "skeleton_crew"],
            "duration": {
                "value": 60,
                "type": "limit"
            }
        }
    ]
}
```

## Combat Phase Sources (`phase_sources/combat/*.json`)

Combat phase sources define weighted spawn tables for mob encounters with varying equipment and attributes.

### Structure
```json
{
    "spawns": [
        {
            "mobId": "minecraft:entity_id",
            "weight": number,         // spawn probability weight (higher = more common)
            "count": number,          // number of this mob to spawn
            "armor": {                // optional armor equipment
                "helmet": "minecraft:item_id",
                "chestplate": "minecraft:item_id",
                "leggings": "minecraft:item_id",
                "boots": "minecraft:item_id"
            },
            "attributes": {           // optional attribute modifiers
                "max_health": number,
                "movement_speed": number,
                "attack_damage": number,
                "armor": number,
                "knockback_resistance": number,
                "scale": number
            },
            "name": "string"          // optional custom name
        }
    ],
    "rolls": number               // number of spawn selections to make
}
```

### Attributes
- `max_health`: Base health points
- `movement_speed`: Movement speed multiplier
- `attack_damage`: Attack damage value
- `armor`: Armor protection value
- `knockback_resistance`: Resistance to knockback (0.0-1.0)
- `scale`: Size multiplier

### Example
```json
{
    "spawns": [
        {
            "mobId": "minecraft:zombie",
            "weight": 10,
            "count": 2,
            "attributes": {
                "max_health": 25.0,
                "scale": 1.0
            }
        }
    ],
    "rolls": 3
}
```

## Challenge Phase Sources (`phase_sources/challenge/*.json`)

Challenge phase sources define skill-based challenges like target practice.

### Structure
```json
{
    "challenges": [
        {
            "type": "target",        // challenge type
            "camo": boolean,         // whether targets are camouflaged
            "count": number          // number of targets to hit
        }
    ]
}
```

### Challenge Types
- **target**: Hit falling block targets

### Example
```json
{
    "challenges": [
        {
            "type": "target",
            "camo": false,
            "count": 3
        }
    ]
}
```

## Puzzle Phase Sources (`phase_sources/puzzle/*.json`)

Puzzle phase sources define logic puzzles (currently not implemented).

### Structure
```json
{
    "puzzles": [
        {
            // Puzzle configuration (TBD)
        }
    ]
}
```

## File Organization

```
data/loot-sparkle/
├── phase_lists/           # Trial phase sequences
│   ├── 6_cursed/
│   ├── 7_blighted/
│   └── 8_doomed/
├── phase_sources/         # Phase content definitions
│   ├── combat/           # Mob spawn configurations
│   ├── challenge/        # Skill challenges
│   └── puzzle/           # Logic puzzles
└── ...
```

## Notes

- All mob IDs must be valid Minecraft entity identifiers
- All item IDs must be valid Minecraft item identifiers
- Attribute values should be reasonable for gameplay balance
- Phase sources are referenced by filename without extension
- Weight values determine spawn probability (higher = more likely)</content>
<parameter name="filePath">c:\Users\rocam\Projects\MochaMix\docs\loot-sparkle\JSON_Structures.md