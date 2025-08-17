# JobsAdventure

A professional-grade Minecraft jobs plugin with extensive features, integrations, and comprehensive customization options.

## 🌟 Features

- **🏗️ Unlimited Jobs**: Create custom jobs with unique actions, requirements, and rewards
- **📈 Advanced XP System**: Mathematical curves, level caps, and permission-based multipliers
- **🎁 Comprehensive Rewards**: Items, commands, economy integration with interactive GUI
- **🔧 External Integrations**: Nexo, ItemsAdder, CustomCrops, CustomFishing, MythicMobs, MMOItems
- **📊 PlaceholderAPI Support**: Extensive placeholders for leaderboards and statistics
- **🛡️ Anti-Exploit Protection**: Block tracking system to prevent XP farming
- **⚡ Performance Optimized**: Async operations, caching, and concurrent data structures
- **🎛️ Highly Configurable**: YAML-based configuration with hot reloading

## 📋 Requirements

- **Minecraft Server**: 1.17+ (Paper recommended)
- **Java**: 17+
- **Dependencies**: None (PlaceholderAPI optional but recommended)

## 🚀 Quick Start

1. Download the JobsAdventure plugin JAR file
2. Place it in your server's `plugins` folder
3. Install PlaceholderAPI (optional but recommended)
4. Start your server
5. Configure jobs in the `plugins/JobsAdventure/jobs/` folder
6. Use `/jobs reload` to apply configuration changes

## 📚 Documentation

This repository contains comprehensive documentation for JobsAdventure:

### 📖 Main Documentation
- **[📘 Complete WIKI](WIKI.md)** - Main documentation covering all aspects of the plugin
- **[⚙️ Configuration Guide](CONFIGURATION_GUIDE.md)** - Detailed configuration reference
- **[💻 Commands & Permissions](COMMANDS_AND_PERMISSIONS.md)** - Complete command reference
- **[🔌 API Documentation](API_DOCUMENTATION.md)** - Developer API and integration guide
- **[📈 Placeholders Guide](placeholders_guide.md)** - PlaceholderAPI integration reference

### 🎯 Quick Reference

| Document | Description | Target Audience |
|----------|-------------|-----------------|
| [WIKI.md](WIKI.md) | Complete plugin overview and setup guide | Server owners, admins |
| [CONFIGURATION_GUIDE.md](CONFIGURATION_GUIDE.md) | Detailed configuration with examples | Server admins, advanced users |
| [COMMANDS_AND_PERMISSIONS.md](COMMANDS_AND_PERMISSIONS.md) | Command usage and permission setup | Server admins, moderators |
| [API_DOCUMENTATION.md](API_DOCUMENTATION.md) | Developer API and custom integrations | Plugin developers |
| [placeholders_guide.md](placeholders_guide.md) | Placeholder usage for displays | Server owners using PlaceholderAPI |

## 🎮 For Players

### Basic Commands
```bash
/jobs list                          # View available jobs
/jobs join <job>                    # Join a job
/jobs info                          # Check your progress
/jobs rewards open <job>            # View and claim rewards
/jobs stats                         # View detailed statistics
```

### Getting Started
1. Use `/jobs list` to see available jobs
2. Join a job with `/jobs join <job>`
3. Start performing job-related activities to gain XP
4. Check your progress with `/jobs info`
5. Claim rewards with `/jobs rewards open <job>`

## 🛠️ For Server Administrators

### Essential Admin Commands
```bash
/jobs reload                        # Reload configurations
/jobs debug <on|off>               # Toggle debug mode
/jobs admin setlevel <player> <job> <level>  # Set player level
/jobs xpbonus give <player> <multiplier> <duration>  # Give XP bonuses
```

### Quick Setup
1. **Configure Jobs**: Edit files in `plugins/JobsAdventure/jobs/`
2. **Set Permissions**: Configure job and multiplier permissions
3. **Customize Rewards**: Set up rewards in `plugins/JobsAdventure/rewards/`
4. **Test Configuration**: Use `/jobs reload` and test with players

### Management Tips
- Use `/jobs xpbonus give * 2.0 2h "Event"` for server-wide events
- Monitor system health with `/jobs xpbonus info`
- Enable debug mode temporarily for troubleshooting
- Regular configuration backups recommended

## 👨‍💻 For Developers

### API Access
```java
// Get plugin instance
JobsAdventure plugin = JobsAdventure.getInstance();

// Access core managers
JobManager jobManager = plugin.getJobManager();
RewardManager rewardManager = plugin.getRewardManager();

// Player operations
PlayerJobData playerData = jobManager.getPlayerData(player);
double xp = playerData.getXp("miner");
int level = playerData.getLevel("miner");
```

### Custom Integration
```java
// Listen to JobsAdventure events
@EventHandler
public void onPlayerLevelUp(PlayerLevelUpEvent event) {
    Player player = event.getPlayer();
    Job job = event.getJob();
    int newLevel = event.getNewLevel();
    // Custom logic here
}
```

### Extending Functionality
- **Custom Conditions**: Extend `AbstractCondition` for complex requirements
- **Custom Actions**: Register new action types for specific integrations
- **Custom Placeholders**: Create additional PlaceholderAPI expansions
- **Event Integration**: Hook into external plugin events

## 🔧 Configuration Examples

### Basic Job Configuration
```yaml
# jobs/miner.yml
name: "&6Miner"
description: "Extract valuable resources from the earth"
icon: "DIAMOND_PICKAXE"
max-level: 100
xp-curve: "mining_balanced"

actions:
  break:
    stone:
      target: "STONE"
      xp: 1.0
    diamond_ore:
      target: "DIAMOND_ORE"
      xp: 20.0
```

### Reward Configuration
```yaml
# rewards/miner_rewards.yml
rewards:
  level_5_tools:
    name: "&6Starter Tools"
    required-level: 5
    items:
      - material: "IRON_PICKAXE"
        name: "&6Miner's Pickaxe"
        enchantments:
          EFFICIENCY: 2
```

### XP Multiplier Setup
```yaml
# config.yml
xp-multipliers:
  "jobsadventure.multiplier.vip": 1.5
  "jobsadventure.multiplier.premium": 2.0
  "jobsadventure.multiplier.admin": 10.0
```

## 🎨 Placeholders Examples

### Individual Job Stats
```
%jobsadventure_miner_player_level%          # Player's miner level
%jobsadventure_miner_player_xp%             # Player's miner XP
%jobsadventure_miner_player_rank%           # Player's rank in miner leaderboard
```

### Leaderboards
```
%jobsadventure_miner_leaderboard_1_name%    # #1 miner player name
%jobsadventure_miner_leaderboard_1_level%   # #1 miner player level
%jobsglobal_totallevels_1_name%             # #1 player by total levels
```

### Usage in Signs/Holograms
```
&6&l=== TOP MINERS ===
&e#1: &f%jobsadventure_miner_leaderboard_1_name%
&7Level %jobsadventure_miner_leaderboard_1_level%

Your Rank: &a%jobsadventure_miner_player_rank%
Your Level: &a%jobsadventure_miner_player_level%
```

## 🔗 Integration Support

### Supported Plugins
- **PlaceholderAPI** - Extensive placeholder support
- **Nexo** - Custom block integration
- **ItemsAdder** - Custom items and blocks
- **CustomCrops** - Advanced farming system
- **CustomFishing** - Custom fishing integration
- **MythicMobs** - Custom mob rewards
- **MMOItems** - Advanced item system
- **Vault** - Economy integration (planned)

### Integration Benefits
- **Automatic Detection**: Integrations activate automatically when plugins are detected
- **Enhanced Features**: Additional functionality when integrated plugins are present
- **Seamless Experience**: Native support for custom content from integrated plugins

## 📈 Performance Features

- **Async Operations**: Non-blocking database and file operations
- **Smart Caching**: Intelligent caching for placeholders and leaderboards
- **Concurrent Data Structures**: Thread-safe operations for high-performance servers
- **Batch Processing**: Efficient bulk operations for large player bases
- **Memory Management**: Automatic cleanup and optimization features

## 🛡️ Security Features

- **Permission-Based Access**: Granular permissions for all features
- **Input Validation**: Comprehensive validation of all user inputs
- **Anti-Exploit Protection**: Built-in systems to prevent XP farming
- **Safe Command Execution**: Secure command processing for rewards
- **Configuration Validation**: Automatic validation with helpful error messages

## 🎯 Use Cases

### Survival Servers
- Traditional job progression with mining, farming, hunting
- Economy integration for job-based income
- Rank-based permissions and rewards

### Skyblock Servers
- Resource generation through job activities
- Island-specific job bonuses
- Challenge-based reward systems

### RPG Servers
- Class-like job system with unique abilities
- Quest integration through condition system
- Level-based content unlocking

### Prison Servers
- Mine-based progression system
- Rank-up integration through job levels
- Prestige systems with job resets

## 📞 Support & Resources

### Getting Help
- **Documentation**: Start with the [Complete WIKI](WIKI.md)
- **Configuration Issues**: Check the [Configuration Guide](CONFIGURATION_GUIDE.md)
- **Commands**: Reference the [Commands & Permissions](COMMANDS_AND_PERMISSIONS.md) guide
- **Development**: See the [API Documentation](API_DOCUMENTATION.md)

### Best Practices
- Always test configuration changes in a development environment
- Use debug mode to troubleshoot issues
- Regular backups of configuration and player data
- Monitor performance metrics for optimization opportunities

### Common Solutions
- **Jobs not working**: Check permissions and job configuration
- **XP not awarding**: Verify action configurations and conditions
- **Placeholders not showing**: Ensure PlaceholderAPI is installed and registered
- **Performance issues**: Review caching settings and cleanup intervals

## 📋 Feature Roadmap

### Planned Features
- **Database Support**: MySQL/PostgreSQL integration for large servers
- **Web Interface**: Web-based statistics and administration panel
- **Quest Integration**: Advanced quest system with job requirements
- **Multi-Server Sync**: Cross-server job progression synchronization
- **Advanced Analytics**: Detailed statistics and reporting system
- **Mobile Companion**: Mobile app for server statistics

### Contributing
While this is a private plugin, feedback and suggestions are welcome for improving the documentation and feature set.

---

## 📄 Documentation Index

| File | Purpose | Audience |
|------|---------|----------|
| `README.md` | Overview and quick start guide | Everyone |
| `WIKI.md` | Complete documentation and features | Server owners |
| `CONFIGURATION_GUIDE.md` | Detailed configuration reference | Administrators |
| `COMMANDS_AND_PERMISSIONS.md` | Command usage and permissions | Staff members |
| `API_DOCUMENTATION.md` | Developer API and integration | Developers |
| `placeholders_guide.md` | PlaceholderAPI integration | Content creators |

---

**JobsAdventure** - Transform your Minecraft server with a professional jobs system that scales from small communities to large networks. Built for performance, designed for flexibility, and crafted for the ultimate player experience.

*Version 1.0+ - Built with modern Minecraft server development practices*