# 📖 GitBook Wiki Setup Guide

This guide explains how to configure and deploy the GitBook wiki for JobsAdventure.

## 🎯 Project Structure

The documentation is organized in the `docs/` folder with the following structure:

```
docs/
├── README.md              # Wiki homepage
├── SUMMARY.md             # GitBook table of contents
├── book.json              # Classic GitBook configuration
├── .gitbook.yaml          # Modern GitBook configuration
├── introduction/          # Introduction guides
├── installation/          # Installation guides
├── player-guide/          # Player guides
├── admin-guide/           # Administration guides
├── jobs-configuration/    # Job configuration
├── rewards/               # Reward system
├── integrations/          # Plugin integrations
├── reference/             # References and API
├── troubleshooting/       # Troubleshooting
└── appendix/              # Appendices and FAQ
```

## 🚀 Deployment Options

### Option 1: GitBook.com (Recommended)

1. **Create an account** on [GitBook.com](https://www.gitbook.com/)

2. **Create a new book**:
   - Title: "JobsAdventure Wiki"
   - Description: "Complete documentation for JobsAdventure"

3. **Connect your GitHub repository**:
   - Go to book settings
   - Connect your repository
   - Select the `docs/` folder as root

4. **Automatic configuration**:
   - GitBook will automatically detect the `.gitbook.yaml` file
   - Synchronization will happen automatically on each commit

### Option 2: GitBook CLI (Local)

1. **Install GitBook CLI**:
   ```bash
   npm install -g gitbook-cli
   ```

2. **Initialize the book**:
   ```bash
   cd docs/
   gitbook init
   ```

3. **Serve locally**:
   ```bash
   gitbook serve
   ```
   Accessible at: http://localhost:4000

4. **Generate static site**:
   ```bash
   gitbook build
   ```

### Option 3: GitHub Pages

1. **Create a `gh-pages` branch**:
   ```bash
   git checkout --orphan gh-pages
   ```

2. **Configure GitHub Actions**:
   Create `.github/workflows/gitbook.yml`:
   ```yaml
   name: Build and Deploy GitBook
   on:
     push:
       branches: [ main ]
   jobs:
     build:
       runs-on: ubuntu-latest
       steps:
       - uses: actions/checkout@v2
       - name: Setup Node.js
         uses: actions/setup-node@v2
         with:
           node-version: '16'
       - name: Install GitBook
         run: npm install -g gitbook-cli
       - name: Build GitBook
         run: |
           cd docs
           gitbook install
           gitbook build
       - name: Deploy to GitHub Pages
         uses: peaceiris/actions-gh-pages@v3
         with:
           github_token: ${{ secrets.GITHUB_TOKEN }}
           publish_dir: ./docs/_book
   ```

### Option 4: Custom Hosting

1. **Generate the site**:
   ```bash
   cd docs/
   gitbook build
   ```

2. **Upload the `_book/` folder** to your web server

## 🎨 Customization

### Theme and Style

Create `docs/styles/website.css`:
```css
/* Theme customization */
.book-summary {
    background: #f8f9fa;
}

.book-header {
    background: #2c3e50;
    color: white;
}

/* Code styling */
code {
    background: #f4f4f4;
    padding: 2px 4px;
    border-radius: 3px;
}

/* Table improvements */
table {
    border-collapse: collapse;
    width: 100%;
}

th, td {
    border: 1px solid #ddd;
    padding: 8px;
    text-align: left;
}

th {
    background-color: #f2f2f2;
}
```

### Logo and Favicon

1. Add your images to `docs/assets/`:
   - `logo.png` (for header)
   - `favicon.ico` (for browser tab)

2. Modify `book.json` to reference these files

### Additional Plugins

Add to `book.json` → `plugins`:
```json
{
  "plugins": [
    "anchor-navigation-ex",
    "advanced-emoji",
    "alerts",
    "include-codeblock",
    "mermaid-gb3",
    "video"
  ]
}
```

## 🔧 Maintenance

### Adding New Pages

1. **Create the markdown file** in the appropriate folder
2. **Add the entry** to `SUMMARY.md`:
   ```markdown
   * [New Page](folder/new-page.md)
   ```
3. **Commit and push** changes

### Updating Content

1. **Modify existing** markdown files
2. **Test locally** with `gitbook serve`
3. **Commit** changes
4. Deployment happens automatically

### Link Structure

Use relative links:
```markdown
[Other page](../admin-guide/commands.md)
[Same folder](configuration.md)
[Section](#section-title)
```

## 📊 Analytics and Monitoring

### Google Analytics

Add to `book.json`:
```json
{
  "pluginsConfig": {
    "ga": {
      "token": "UA-XXXXXXXX-X"
    }
  }
}
```

### GitBook Metrics

If using GitBook.com, analytics are automatically integrated.

## 🌍 Internationalization

To add other languages:

1. **Create folders**:
   ```
   docs/
   ├── en/          # English version (current)
   ├── fr/          # French version
   └── LANGS.md     # Language configuration
   ```

2. **Configure LANGS.md**:
   ```markdown
   # Languages
   
   * [English](en/)
   * [Français](fr/)
   ```

## 🔍 SEO and Referencing

### Metadata

Add to each page:
```markdown
---
description: Page description for search engines
keywords: jobsadventure, minecraft, plugin, jobs
---
```

### Sitemap

The `sitemap-general` plugin automatically generates an XML sitemap.

## 🛠️ Troubleshooting

### Build Errors

```bash
# Clear cache
gitbook install --clean

# Reinstall plugins
rm -rf node_modules/
gitbook install
```

### Synchronization Issues

- Check GitHub permissions
- Verify webhooks in settings
- Check build logs

### Performance

- Optimize images (WebP, compression)
- Limit number of plugins
- Use CDN for assets

## 📞 Support

- **GitBook Documentation**: [docs.gitbook.com](https://docs.gitbook.com/)
- **Community**: [community.gitbook.com](https://community.gitbook.com/)
- **GitHub Issues**: For project-specific issues

---

**Your JobsAdventure wiki is now ready! 📚✨**