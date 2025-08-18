# Jobs Configuration

This section covers everything you need to know about configuring jobs in JobsAdventure.

## Configuration Topics

### [Configuration Examples](configuration-examples.md)
Complete examples of job configurations with detailed explanations of each section.

### [Conditions System](conditions-system.md)
Learn how to set up requirements and conditions for job actions, including permissions, items, worlds, biomes, and more.

### [Level Up Actions](levelup-actions.md)
Configure customizable actions that trigger when players level up in a job, including rewards, effects, and notifications.

## Quick Overview

Each job in JobsAdventure is defined by a YAML configuration file located in the `plugins/JobsAdventure/jobs/` directory. A job configuration includes:

- **Basic Information**: Name, description, icon, and permissions
- **XP System**: Experience curves and leveling configuration
- **Actions**: Define what activities give XP (breaking blocks, killing mobs, etc.)
- **Conditions**: Set requirements for actions (permissions, tools, locations, etc.)
- **Level Up Actions**: Configure rewards and effects for leveling up
- **Rewards**: Link to reward systems and GUI configurations

## File Structure

```
plugins/JobsAdventure/
└── jobs/
    ├── miner.yml
    ├── farmer.yml
    ├── hunter.yml
    └── [custom_job].yml
```

Each `.yml` file represents a single job that players can join and progress in.

## Key Features

- **Flexible Action System**: Support for BREAK, PLACE, KILL, BREED, FISH, and more
- **Advanced Conditions**: Create complex requirements using AND/OR logic
- **Custom Integrations**: Native support for CustomCrops, CustomFishing, Nexo, ItemsAdder, MythicMobs, and MMOItems
- **Level Up Customization**: 100% customizable actions when players level up
- **Performance Optimized**: Efficient handling of large numbers of jobs and players

## Getting Started

1. Start with the [Configuration Examples](configuration-examples.md) to see complete job setups
2. Learn about the [Conditions System](conditions-system.md) to create requirements
3. Explore [Level Up Actions](levelup-actions.md) to reward player progression
4. Customize and create your own jobs based on your server's needs

## Need Help?

- Check the example jobs included with the plugin (miner.yml, farmer.yml, hunter.yml)
- Review the detailed documentation in each section
- Join our support Discord for assistance