# Database Migration Guide

This guide explains how to migrate UniverseJobs data from YML files to a database system.

## Supported Databases

- **SQLite** (Default) - File-based database, good for smaller servers
- **MySQL** - Network database, recommended for larger servers or multiple server networks

## Configuration

### Enable Database Storage

1. Edit your `config.yml` file
2. Set `database.enabled` to `true`
3. Configure your database type and connection details

### SQLite Configuration
```yaml
database:
  enabled: true
  type: "sqlite"
  prefix: "universejobs_"
  pool:
    min-connections: 2
    max-connections: 10
```

### MySQL Configuration
```yaml
database:
  enabled: true
  type: "mysql"
  host: "localhost"
  port: 3306
  database: "universejobs"
  username: "your_username"
  password: "your_password"
  prefix: "universejobs_"
  pool:
    min-connections: 2
    max-connections: 10
    connection-timeout-ms: 30000
    validation-interval-ms: 300000
```

## Migration Process

### Automatic Migration

1. Configure database settings in `config.yml`
2. Restart the server
3. The plugin will automatically detect YML files and migrate them
4. A backup will be created in the `migration-backup` folder
5. Migration status is logged to console

### Manual Migration

You can also trigger migration manually using commands:

```
/jobs database migrate
```

## Database Management Commands

All database commands require `universejobs.admin` permission:

- `/jobs database health` - Check database connection health
- `/jobs database stats` - View performance statistics  
- `/jobs database test` - Run integration tests
- `/jobs database test performance` - Run performance benchmark
- `/jobs database migrate` - Manually trigger data migration

## Database Schema

### Tables Created

- `{prefix}player_data` - Player job progress (XP, levels)
- `{prefix}reward_claims` - Reward claim history
- `{prefix}job_stats` - Job-specific statistics

### Performance Features

- Connection pooling with HikariCP
- Async operations to prevent server lag
- Intelligent caching system
- Batch operations for better performance

## Backup and Recovery

### Automatic Backups

Before migration, the plugin automatically creates a backup in:
`plugins/UniverseJobs/migration-backup/data-{timestamp}/`

### Manual Backup

To backup your database data, you can:

1. For SQLite: Copy the `data.db` file
2. For MySQL: Use `mysqldump` to create a database backup

## Troubleshooting

### Common Issues

1. **Connection Failed**
   - Check database credentials
   - Verify database server is running
   - Check firewall settings

2. **Migration Failed**
   - Check YML file permissions
   - Verify database write permissions
   - Review server logs for detailed errors

3. **Performance Issues**
   - Adjust connection pool settings
   - Monitor database server resources
   - Use `/jobs database stats` to check performance

### Rollback to File System

To rollback to YML files:

1. Set `database.enabled` to `false`
2. Restore YML files from backup
3. Restart the server

## Performance Optimization

### Recommended Settings

For small servers (< 50 players):
```yaml
pool:
  min-connections: 2
  max-connections: 5
```

For medium servers (50-200 players):
```yaml
pool:
  min-connections: 3
  max-connections: 10
```

For large servers (200+ players):
```yaml
pool:
  min-connections: 5
  max-connections: 20
```

### MySQL Optimization

For MySQL, consider these additional optimizations:

1. Enable query caching
2. Optimize `innodb_buffer_pool_size`
3. Use SSD storage for better I/O performance
4. Monitor slow query log

## Data Integrity

The plugin ensures data integrity by:

- Using database transactions
- Validating data before migration
- Creating backups before changes
- Implementing retry mechanisms for failed operations

## Support

If you encounter issues during migration:

1. Check the server console for error messages
2. Use `/jobs database health` to verify database connection
3. Review the migration backup files
4. Report issues with full error logs