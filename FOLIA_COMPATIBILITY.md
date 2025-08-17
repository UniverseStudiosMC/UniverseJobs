# JobsAdventure - Folia Compatibility Guide

JobsAdventure is now **fully compatible** with Folia, Paper, and Spigot servers!

## 🌟 **What is Folia?**

Folia is a fork of Paper that introduces **regionised multithreading** to Minecraft servers. Instead of using a single main thread, Folia divides the world into regions that can tick in parallel, dramatically improving performance for servers with many players.

## ✅ **Compatibility Features Implemented**

### 🔧 **1. Scheduler Compatibility**
- **FoliaLib Integration**: Uses FoliaLib for cross-platform scheduler compatibility
- **Region-Aware Scheduling**: Tasks are executed in the correct region context
- **Entity-Specific Tasks**: Player-related tasks run on the entity's owning region
- **Async Operations**: Background tasks (saving, cleanup) run asynchronously

### 🌍 **2. Multi-Platform Support**
- **Auto-Detection**: Automatically detects Folia, Paper, or Spigot
- **Unified API**: Same plugin works on all three platforms
- **Fallback Support**: Graceful degradation for older server versions

### 📡 **3. Event Handling**
- **Thread-Safe Events**: All events are handled in the correct region context
- **Player Actions**: Block break/place events run on player's region
- **Entity Interactions**: Mob kills, breeding, taming run on entity's region
- **Async Safety**: No race conditions or thread conflicts

### 💾 **4. Data Management**
- **Async Loading**: Player data loads asynchronously on join
- **Async Saving**: Player data saves asynchronously on quit
- **Thread-Safe Storage**: ConcurrentHashMap usage for multi-threaded access
- **Auto-Save**: Periodic saving works correctly on all platforms

### 🎯 **5. XP & Rewards System**
- **Region-Aware XP**: XP calculations happen in the correct thread
- **Safe Boss Bars**: Boss bars are managed per-player region
- **Action Bar Messages**: Action bar messages respect region boundaries
- **Async Rewards**: Reward processing is thread-safe

## 🚀 **Platform Detection**

The plugin automatically detects your server platform:

```java
// Get platform information
FoliaCompatibilityManager foliaManager = plugin.getFoliaManager();

if (foliaManager.isFolia()) {
    // Running on Folia - regionised multithreading enabled
} else if (foliaManager.isPaper()) {
    // Running on Paper - single thread with Folia compatibility layer
} else {
    // Running on Spigot/Bukkit - single thread with Folia compatibility layer
}
```

## ⚙️ **Configuration**

### **plugin.yml Declaration**
```yaml
folia-supported: true
```

### **Language Support**
All features work with the multilingual system (fr_FR, en_US, etc.) on all platforms.

### **Performance Settings**
No special configuration needed! The plugin automatically optimizes for your platform.

## 🔧 **Technical Implementation**

### **Scheduler Replacement**
**Before (Bukkit Scheduler):**
```java
Bukkit.getScheduler().runTaskLater(plugin, task, delay);
```

**After (Folia Compatible):**
```java
foliaManager.runLater(task, delay);
```

### **Region-Aware Operations**
```java
// Run task on player's region
foliaManager.runAtEntity(player, () -> {
    // Player-specific operations
});

// Run task on location's region  
foliaManager.runAtLocation(location, () -> {
    // Location-specific operations
});
```

### **Async Teleportation**
```java
// Folia-safe teleportation
foliaManager.teleportPlayerSafely(player, destination)
    .thenAccept(success -> {
        if (success) {
            // Teleportation successful
        }
    });
```

## 📊 **Performance Benefits on Folia**

### **Before (Single Thread)**
- ⚠️ All operations on one thread
- ⚠️ Performance bottlenecks with many players
- ⚠️ TPS drops under heavy load

### **After (Multi-Threaded Regions)**
- ✅ Operations distributed across regions
- ✅ Better performance with many players
- ✅ Stable TPS under heavy load
- ✅ Automatic load balancing

## 🛡️ **Thread Safety Measures**

1. **ConcurrentHashMap**: All player data storage
2. **Atomic Operations**: XP calculations and level updates
3. **Region Context**: All entity/player operations in correct thread
4. **Async Processing**: File I/O and database operations
5. **Lock-Free Design**: No deadlocks or race conditions

## 🧪 **Testing**

The plugin has been tested on:
- ✅ **Folia 1.21.x** - Full regionised multithreading
- ✅ **Paper 1.21.x** - Single thread with compatibility layer
- ✅ **Spigot 1.21.x** - Legacy compatibility mode

## 🚨 **Migration Notes**

### **From Older Versions**
- ✅ **Automatic**: No configuration changes needed
- ✅ **Data Safe**: All existing data remains compatible
- ✅ **Plugin Compatible**: Works with existing plugin configurations

### **Server Migration**
- ✅ **Paper → Folia**: Drop-in replacement
- ✅ **Spigot → Folia**: No changes needed
- ✅ **Folia → Paper**: Backwards compatible

## 📈 **Recommended Folia Configuration**

### **For 200-300 Players:**
```yaml
global-config:
  threaded-regions:
    threads: 12  # Adjust based on your CPU cores (80% max)
```

### **Thread Allocation Guidelines:**
- **Netty I/O**: ~4 threads
- **Chunk System**: ~3 threads  
- **Chunk Workers**: ~2 threads (if world pre-generated)
- **Tick Threads**: Remaining cores (up to 80% total)

## 🤝 **Support**

If you experience any issues with Folia compatibility:

1. **Check Platform**: Verify the plugin detects your platform correctly
2. **Review Logs**: Look for Folia-specific initialization messages
3. **Test Cross-Platform**: Try the same configuration on Paper to isolate issues
4. **Report Issues**: Include platform information and server logs

## 🔮 **Future Compatibility**

The plugin is designed to remain compatible with future versions of:
- **Folia**: As the regionised multithreading evolves
- **Paper**: When Folia APIs are integrated upstream
- **Spigot**: Continued legacy support

---

**JobsAdventure** - Ready for the future of Minecraft server performance! 🚀