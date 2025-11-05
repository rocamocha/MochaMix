# Logical Loadouts Data Structures Documentation

This document outlines the data structures and concepts used in the Logical Loadouts mod for managing player equipment configurations.

## Loadout Data Structure

### Loadout Class
The core data structure representing a complete equipment configuration.

#### Properties
- **id**: UUID - Unique identifier for the loadout
- **name**: String - Display name (max 32 characters)
- **lastModified**: Long - Timestamp of last modification

#### Inventory Slots
- **hotbar[9]**: ItemStack[] - Hotbar slots 0-8
- **mainInventory[27]**: ItemStack[] - Main inventory slots 9-35
- **armor[4]**: ItemStack[] - Armor slots (boots, leggings, chestplate, helmet)
- **offhand[1]**: ItemStack[] - Offhand slot

#### Metadata
- **metadata**: Map<String, String> - Key-value pairs for additional data

### Loadout Types

#### Global Loadouts
- **Purpose**: Cross-world and cross-server persistent loadouts
- **Storage**: Client-side local storage
- **Slots**: Fixed 3 slots (indices 0-2)
- **Availability**: Always accessible regardless of server
- **Use Case**: Favorite loadouts that work everywhere

#### Local Loadouts
- **Purpose**: Client-side only loadouts for single-player or specific worlds
- **Storage**: Client-side local storage
- **Slots**: Unlimited (dynamically allocated)
- **Availability**: Only on the client that created them
- **Use Case**: World-specific or experimental loadouts

#### Server Loadouts
- **Purpose**: Server-managed loadouts with admin controls
- **Storage**: Server-side persistent storage
- **Slots**: Unlimited (per player limits configurable)
- **Availability**: Only on servers with the mod installed
- **Use Case**: Server-specific equipment sets, admin-provided loadouts

#### Server-Shared Loadouts
- **Purpose**: Loadouts available to all players on a server
- **Storage**: Server-side in `world/logical-loadouts/server/` directory
- **Slots**: Unlimited
- **Availability**: All players on the server
- **Use Case**: Starter kits, event equipment, admin templates

## Storage Architecture

### Client-Side Storage
```
.config/logical-loadouts/
├── global/
│   ├── slot_0.nbt
│   ├── slot_1.nbt
│   └── slot_2.nbt
└── local/
    ├── loadout_uuid_1.nbt
    ├── loadout_uuid_2.nbt
    └── ...
```

### Server-Side Storage
```
world/logical-loadouts/
├── players/
│   └── player_uuid/
│       ├── loadout_uuid_1.nbt
│       ├── loadout_uuid_2.nbt
│       └── ...
└── server/
    ├── shared_loadout_1.nbt
    ├── shared_loadout_2.nbt
    └── ...
```

## Network Protocol

### Packet Types

#### Client → Server
- **LoadoutCreatePacket**: Create new loadout
- **LoadoutUpdatePacket**: Update existing loadout
- **LoadoutDeletePacket**: Delete loadout
- **LoadoutApplyPacket**: Apply loadout to player
- **LoadoutListRequestPacket**: Request loadout list

#### Server → Client
- **LoadoutListResponsePacket**: Send loadout list
- **LoadoutDataPacket**: Send loadout data
- **LoadoutOperationResultPacket**: Operation success/failure
- **ServerLoadoutsPacket**: Send server-shared loadouts

## Configuration Options

### Server Configuration (Future)
```properties
# Maximum loadouts per player (default: unlimited)
max-loadouts-per-player=10

# Banned items (comma-separated item IDs)
banned-items=minecraft:bedrock,minecraft:barrier

# Allow server-shared loadouts
allow-server-shared=true

# Require permission for loadout operations
require-permission=false
```

### Client Configuration (Future)
```properties
# Enable auto-save on inventory change
auto-save=false

# Show loadout names in tooltips
show-names-in-tooltip=true

# Keybinding conflicts resolution
keybind-conflict-mode=warn
```

## Loadout Validation

### Item Restrictions
- **Banned Items**: Configurable list of items that cannot be stored
- **Stack Limits**: Respects Minecraft's stack size limits
- **Durability**: Preserves item damage and enchantments
- **NBT Data**: Maintains all item metadata

### Inventory Constraints
- **Slot Validation**: Ensures items go to correct inventory sections
- **Space Requirements**: Validates sufficient inventory space before application
- **Item Conflicts**: Handles armor slot conflicts and tool incompatibilities

## Loadout Operations

### Creation
1. Validate loadout name
2. Capture current inventory state
3. Generate UUID
4. Store in appropriate location
5. Update client/server caches

### Application
1. Validate player permissions
2. Check inventory space
3. Backup current inventory (optional)
4. Clear target slots
5. Place loadout items
6. Handle overflow items

### Synchronization
1. Client requests loadout list
2. Server sends available loadouts
3. Client caches loadout data
4. Changes sync bidirectionally
5. Conflict resolution for concurrent edits

## GUI Components

### Loadout Selection Screen
- **Loadout List**: Scrollable list of available loadouts
- **Loadout Preview**: Shows equipment layout
- **Quick Actions**: Apply, edit, delete buttons
- **Search/Filter**: Find loadouts by name or contents

### Loadout Editor
- **Inventory Grid**: Visual representation of equipment
- **Slot Management**: Drag and drop item placement
- **Name Editor**: Rename loadouts
- **Metadata Editor**: Add custom properties

### Keybinding Interface
- **Slot Assignment**: Map loadouts to hotkeys
- **Conflict Detection**: Warn about key conflicts
- **Quick Switch**: Instant loadout application

## Error Handling

### Common Error Conditions
- **Insufficient Space**: Not enough inventory slots for loadout
- **Permission Denied**: Player lacks server permissions
- **Loadout Not Found**: Referenced loadout doesn't exist
- **Corrupted Data**: Invalid NBT data in storage files
- **Network Timeout**: Connection issues during sync

### Recovery Mechanisms
- **Data Backup**: Automatic backup before destructive operations
- **Rollback**: Restore previous state on failure
- **Validation**: Pre-operation checks prevent errors
- **Logging**: Detailed error logging for debugging

## Performance Considerations

### Memory Management
- **Lazy Loading**: Loadouts loaded on demand
- **Cache Limits**: Bounded caches prevent memory leaks
- **Garbage Collection**: Proper cleanup of unused objects

### Network Optimization
- **Compression**: NBT compression for storage
- **Delta Sync**: Only send changed data
- **Batch Operations**: Group multiple operations

### Storage Efficiency
- **Sparse Storage**: Only store non-empty slots
- **Metadata Compression**: Efficient key-value storage
- **Indexing**: Fast lookup by UUID and name</content>
<parameter name="filePath">c:\Users\rocam\Projects\MochaMix\docs\logical-loadouts\Data_Structures.md