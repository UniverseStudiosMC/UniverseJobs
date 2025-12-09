# Installation

## Requirements

| Requirement | Version |
|-------------|---------|
| Java | 21 or higher |
| Server | Paper 1.13+ or Folia |
| Vault | Latest (for economy) |
| PlaceholderAPI | Optional |

## Installation Steps

### 1. Download

Download the latest version from [GitHub Releases](https://github.com/UniverseStudiosMC/UniverseJobs/releases/latest).

### 2. Install

1. Stop your server
2. Place `UniverseJobs-x.x.x.jar` in your `plugins/` folder
3. Start your server

### 3. First Launch

On first launch, UniverseJobs will create:

```
plugins/UniverseJobs/
├── config.yml              # Main configuration
├── jobs/                   # Job definitions
│   ├── miner.yml
│   ├── farmer.yml
│   ├── hunter.yml
│   ├── lumberjack.yml
│   ├── brewer.yml
│   ├── repairer.yml
│   └── explorer.yml
├── rewards/                # Reward definitions
│   ├── miner_rewards.yml
│   ├── farmer_rewards.yml
│   ├── hunter_rewards.yml
│   └── lumberjack_rewards.yml
├── menus/                  # GUI configurations
│   ├── main-menu.yml
│   ├── job-menu.yml
│   ├── actions-menu.yml
│   ├── rankings-menu.yml
│   └── boost-manager.yml
├── gui/                    # Reward GUI configurations
├── xp-curves/              # XP progression curves
│   └── default.yml
├── languages/              # Translations
│   ├── en_US.yml
│   └── fr_FR.yml
└── data/                   # Player data (if using file storage)
```

### 4. Configure Database (Optional)

By default, UniverseJobs uses SQLite. For larger servers, configure MySQL in `config.yml`:

```yaml
database:
  type: "mysql"
  mysql:
    host: "localhost"
    port: 3306
    database: "universejobs"
    username: "user"
    password: "password"
  prefix: "universejobs_"
  pool:
    min-connections: 2
    max-connections: 10
```

### 5. Verify Installation

Run `/jobs` to open the main menu. If everything is working, you'll see the jobs GUI.

## Migrating from JobsReborn

UniverseJobs includes an automatic migration tool:

```
/jobs admin migration start
```

This will:
1. Detect JobsReborn data
2. Create a backup
3. Convert player data
4. Generate a migration report

## Updating

1. Stop your server
2. Replace the old JAR with the new one
3. Start your server

Configuration files are preserved during updates. New options will be added with default values.

## Troubleshooting

### Plugin doesn't load

- Check Java version: `java -version` (must be 21+)
- Check server console for errors
- Ensure Vault is installed

### Economy not working

- Install Vault
- Install an economy plugin (EssentialsX, CMI, etc.)
- Restart the server

### Placeholders not working

- Install PlaceholderAPI
- Run `/papi reload`
