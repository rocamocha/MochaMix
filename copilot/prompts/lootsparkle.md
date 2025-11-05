You are a Minecraft modding expert, targeting version 1.21.1.
Reference relevant documentation to plan your implementation.
Analyze the source code when you are not sure if an implementation is possible.
Do not presumptively use the first solution that comes to mind - carefully analyze whether it is the best course of action before implementation.

Let's restructure the spawn phases for hostile sparkles to be sourced differently. I would like for each hostile sparkle tiers' combat phases to be sourced from its own subdirectory, in a new directory in the datapack named `phase_lists`.

Each tiers' subdirectory will contain `json` files with a schema that outlines the following information:

At least 1 entry in `phases`, with inner fields for:
- `sources` - takes multiple strings, pointing to `json` files in `phase_sources`.
- `type` - a string value, can be `emitter`, `burst`, `boss`, `challenge`, or `puzzle` -- represents the spawning logic that will be used for the phase. For now, we will focus on `emitter`, `burst`, and `boss`. Let's leave `challenge` and `puzzle` just as a scaffolding in the actual code.
- `duration` - has two inner fields: `value` - an integer representing real time seconds; and `type` - a string determining what happens at the end of the duration, can be `advance`, `limit`, `survive`.

```json
{
    "phases": [
        {
            "type": "emitter",
            "sources": [
                "zombie_horde",
                "skeleton_crew"
            ],
            "duration": {
                "value": 30,
                "type": "advance"
            }
        }
    ]
}
```

---

The sources `json` will contain a parent `spawns` with entries containing fields for:

- `mobId`
- `armor` (optional) - with possible fields for each armor slot, accepting item IDs for mod compat.
- `attributes` (optional) - accepts minecraft mob attributes such as health, scale, movement speed, armor, armor toughness, attack damage, attack knockback, etc.
- `name` (optional) - a name to be displayed above the mob like with a nametag
- `boss` a boolean value - when true, the mobs health will show up as a boss bar
- `weight` (optional) - when the entry has this field, it will be considered as a candidate for the randomized spawning for the phase; when this field is not present, the entry will be included as a guaranteed spawn for the phase
- `count` (optional) - determines the number of these that will spawn; accepts either an integer for a fixed number, or an entry that has `min` and `max` fields; if the field is not present, defaults to 1

Alongside `spawns` will be `rolls` which can accept an integer or an entry with `min` and `max` fields.