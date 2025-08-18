# 🚀 Release Guide for JobsAdventure

## 📋 How to Create a Release

### 🎯 Automatic Release (Recommended)

1. **Create a Git Tag**:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. **GitHub Actions will automatically**:
   - ✅ Build the project with Maven
   - ✅ Update version numbers
   - ✅ Create the JAR file
   - ✅ Generate release notes
   - ✅ Create GitHub Release
   - ✅ Upload the JAR as an asset

### 🎛️ Manual Release

If you need to create a release manually:

1. **Go to GitHub Repository**
2. **Click "Releases"** → **"Create a new release"**
3. **Choose a tag**: `v1.0.0` (create new tag)
4. **Title**: `🚀 JobsAdventure v1.0.0`
5. **Use the auto-generated description** or customize
6. **Upload the JAR file** from `target/` folder
7. **Publish release**

### 📝 Version Naming Convention

- **Major releases**: `v1.0.0`, `v2.0.0`
- **Minor releases**: `v1.1.0`, `v1.2.0`
- **Patch releases**: `v1.0.1`, `v1.0.2`
- **Pre-releases**: `v1.0.0-beta.1`, `v1.0.0-rc.1`

### 🔧 Local Build for Testing

```bash
# Build locally
mvn clean package

# The JAR will be in target/ folder
ls target/JobsAdventure-*.jar
```

### 🎨 Release Notes Template

```markdown
# 🚀 JobsAdventure v1.0.0

## ✨ What's New
- 🆕 **New Feature**: Description
- 🔧 **Improvement**: Description  
- 🐛 **Bug Fix**: Description

## 🔧 Technical Details
- **Compatibility**: Folia 1.21+, Paper 1.13+, Spigot 1.13+
- **Java**: OpenJDK 21
- **Dependencies**: PlaceholderAPI (recommended)

## 📦 Installation
1. Download `JobsAdventure-v1.0.0.jar`
2. Place in `/plugins` folder
3. Restart server
4. Enjoy! 🎉

## 🔗 Links
- [📚 Full Documentation](README.md) #
- [🐛 Report Issues](../../issues)
- [💡 Discussions](../../discussions)
```

### 🚨 Important Notes

- **Always test** releases in a development environment first
- **Version tags** must start with `v` (e.g., `v1.0.0`)
- **JAR files** are automatically named `JobsAdventure-v{version}.jar`
- **Release notes** are auto-generated but can be customized
- **Artifacts** are kept for 90 days on GitHub