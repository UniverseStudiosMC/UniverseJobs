# 🚀 Quick Installation

This guide will help you install JobsAdventure in less than 5 minutes!

## 📋 Prerequisites

Before starting, make sure you have:

- **Minecraft Server**: Paper 1.13+ / Spigot 1.13+ / Bukkit 1.13+ (or Folia 1.21+ for maximum performance)
- **Java**: OpenJDK 21 (recommended) or compatible version
- **PlaceholderAPI**: Required for all features
- **RAM**: Minimum 2GB, recommended 4GB+ for large servers

## 📥 Download

1. **Download the latest version** of JobsAdventure from:
   - GitHub Releases: [JobsAdventure Releases](https://github.com/ax-dev/JobsAdventure/releases)
   - SpigotMC: [SpigotMC Page](https://spigotmc.org/)

2. **Download PlaceholderAPI** if you don't have it already:
   - [PlaceholderAPI on SpigotMC](https://www.spigotmc.org/resources/placeholderapi.6245/)

## 🔧 Installation

### Step 1: Install Plugins
1. **Place** the `JobsAdventure-v1.0.jar` file in your `/plugins` folder
2. **Place** the `PlaceholderAPI.jar` file in your `/plugins` folder
3. **Restart** your server

### Step 2: Verification
1. Check in the console that JobsAdventure loaded correctly:
   ```
   [INFO] JobsAdventure has been successfully enabled!
   [INFO] Configuration loaded successfully
   [INFO] Jobs loaded successfully
   [INFO] PlaceholderAPI integration initialized successfully
   ```

2. Test in-game with the command:
   ```
   /jobs list
   ```

## ✅ Default Configuration

JobsAdventure comes with **3 pre-configured jobs** ready to use:

### ⛏️ Miner
- **Max Level**: 100
- **Actions**: Stone mining, ores, underground combat
- **Rewards**: Enhanced tools, economic bonuses
- **Specialties**: Depth bonuses, rare ore multipliers

### 🌾 Farmer  
- **Max Level**: 75
- **Actions**: Agriculture, livestock, food production
- **Rewards**: Rare seeds, farming tools
- **Specialties**: Seasonal bonuses, breeding multipliers

### 🏹 Hunter
- **Max Level**: 80
- **Actions**: Combat, taming, survival
- **Rewards**: Specialized weapons, rare items
- **Specialties**: MythicMobs integration, rare creature bonuses

## 🎮 Quick Test

1. **Join a job**:
   ```
   /jobs join miner
   ```

2. **Mine some blocks** of stone or coal

3. **Check your stats**:
   ```
   /jobs stats
   ```

4. **Open rewards interface**:
   ```
   /jobs rewards open miner
   ```

## 🔧 Optional Plugins (recommended)

For a complete experience, install these plugins:

| Plugin | Functionality | Priority |
|:---|:---|:---:|
| **Vault** | Economy and permissions | 🔴 High |
| **MythicMobs** | Custom creatures | 🟡 Medium |
| **CustomCrops** | Advanced farming | 🟡 Medium |
| **CustomFishing** | Custom fishing | 🟢 Low |
| **Nexo/ItemsAdder** | Custom items | 🟢 Low |
| **MMOItems** | Specialized tools | 🟢 Low |

## 🎯 Next Steps

Now that JobsAdventure is installed:

1. **Explore** the [default jobs](../player-guide/getting-started.md)
2. **Configure** your [first custom jobs](../jobs-configuration/creating-jobs.md)
3. **Discover** the [reward system](../rewards/reward-configuration.md)
4. **Learn** the [admin commands](../admin-guide/admin-commands.md)

## ❗ Common Issues

### Plugin won't load
- ✅ Check you're using Java 21+
- ✅ Make sure PlaceholderAPI is installed
- ✅ Check the [logs](../troubleshooting/logs-debugging.md) for details

### Commands don't work
- ✅ Check [permissions](../reference/permissions.md)
- ✅ Restart the server after installation

### No XP gained
- ✅ Check you've joined a job with `/jobs join <job>`
- ✅ See the [troubleshooting guide](../troubleshooting/common-issues.md)

## 🆘 Need Help?

- 📚 [Complete FAQ](../appendix/faq.md)
- 🔧 [Troubleshooting Guide](../troubleshooting/common-issues.md)
- 💬 [Support and Community](../appendix/support.md)

---

**Congratulations! JobsAdventure is now installed and ready to transform your server! 🎉**