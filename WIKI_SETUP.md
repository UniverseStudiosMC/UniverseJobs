# 📖 Guide de configuration du Wiki GitBook

Ce guide vous explique comment configurer et déployer le wiki GitBook de JobsAdventure.

## 🎯 Structure du projet

La documentation est organisée dans le dossier `docs/` avec la structure suivante :

```
docs/
├── README.md              # Page d'accueil du wiki
├── SUMMARY.md             # Table des matières GitBook
├── book.json              # Configuration GitBook classique
├── .gitbook.yaml          # Configuration GitBook moderne
├── introduction/          # Guides d'introduction
├── installation/          # Guides d'installation
├── player-guide/          # Guides pour les joueurs
├── admin-guide/           # Guides d'administration
├── jobs-configuration/    # Configuration des métiers
├── rewards/               # Système de récompenses
├── integrations/          # Intégrations de plugins
├── reference/             # Références et API
├── troubleshooting/       # Dépannage
└── appendix/              # Annexes et FAQ
```

## 🚀 Options de déploiement

### Option 1 : GitBook.com (Recommandé)

1. **Créez un compte** sur [GitBook.com](https://www.gitbook.com/)

2. **Créez un nouveau livre** :
   - Titre : "JobsAdventure Wiki"
   - Description : "Documentation complète pour JobsAdventure"

3. **Connectez votre repository GitHub** :
   - Allez dans les paramètres du livre
   - Connectez votre repository
   - Sélectionnez le dossier `docs/` comme racine

4. **Configuration automatique** :
   - GitBook détectera automatiquement le fichier `.gitbook.yaml`
   - La synchronisation se fera automatiquement à chaque commit

### Option 2 : GitBook CLI (Local)

1. **Installez GitBook CLI** :
   ```bash
   npm install -g gitbook-cli
   ```

2. **Initialisez le livre** :
   ```bash
   cd docs/
   gitbook init
   ```

3. **Servez localement** :
   ```bash
   gitbook serve
   ```
   Accessible à : http://localhost:4000

4. **Générez le site statique** :
   ```bash
   gitbook build
   ```

### Option 3 : GitHub Pages

1. **Créez une branche `gh-pages`** :
   ```bash
   git checkout --orphan gh-pages
   ```

2. **Configurez GitHub Actions** :
   Créez `.github/workflows/gitbook.yml` :
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

### Option 4 : Hébergement personnalisé

1. **Générez le site** :
   ```bash
   cd docs/
   gitbook build
   ```

2. **Uploadez le dossier `_book/`** sur votre serveur web

## 🎨 Personnalisation

### Thème et style

Créez `docs/styles/website.css` :
```css
/* Personnalisation du thème */
.book-summary {
    background: #f8f9fa;
}

.book-header {
    background: #2c3e50;
    color: white;
}

/* Style pour les codes */
code {
    background: #f4f4f4;
    padding: 2px 4px;
    border-radius: 3px;
}

/* Amélioration des tableaux */
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

### Logo et favicon

1. Ajoutez vos images dans `docs/assets/` :
   - `logo.png` (pour le header)
   - `favicon.ico` (pour l'onglet du navigateur)

2. Modifiez `book.json` pour référencer ces fichiers

### Plugins supplémentaires

Ajoutez dans `book.json` → `plugins` :
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

### Ajout de nouvelles pages

1. **Créez le fichier markdown** dans le bon dossier
2. **Ajoutez l'entrée** dans `SUMMARY.md` :
   ```markdown
   * [Nouvelle page](dossier/nouvelle-page.md)
   ```
3. **Commitez et pushez** les changements

### Mise à jour du contenu

1. **Modifiez les fichiers** markdown existants
2. **Testez localement** avec `gitbook serve`
3. **Commitez** les changements
4. Le déploiement se fait automatiquement

### Structure des liens

Utilisez des liens relatifs :
```markdown
[Autre page](../admin-guide/commands.md)
[Même dossier](configuration.md)
[Section](#titre-de-section)
```

## 📊 Analytics et monitoring

### Google Analytics

Ajoutez dans `book.json` :
```json
{
  "pluginsConfig": {
    "ga": {
      "token": "UA-XXXXXXXX-X"
    }
  }
}
```

### Métriques GitBook

Si vous utilisez GitBook.com, les analytics sont intégrées automatiquement.

## 🌍 Internationalisation

Pour ajouter d'autres langues :

1. **Créez les dossiers** :
   ```
   docs/
   ├── fr/          # Version française (actuelle)
   ├── en/          # Version anglaise
   └── LANGS.md     # Configuration des langues
   ```

2. **Configurez LANGS.md** :
   ```markdown
   # Languages
   
   * [Français](fr/)
   * [English](en/)
   ```

## 🔍 SEO et référencement

### Métadonnées

Ajoutez dans chaque page :
```markdown
---
description: Description de la page pour les moteurs de recherche
keywords: jobsadventure, minecraft, plugin, métiers
---
```

### Sitemap

Le plugin `sitemap-general` génère automatiquement un sitemap XML.

## 🛠️ Dépannage

### Erreurs de build

```bash
# Nettoyage du cache
gitbook install --clean

# Réinstallation des plugins
rm -rf node_modules/
gitbook install
```

### Problèmes de synchronisation

- Vérifiez les permissions GitHub
- Contrôlez les webhooks dans les paramètres
- Vérifiez les logs de build

### Performance

- Optimisez les images (WebP, compression)
- Limitez le nombre de plugins
- Utilisez un CDN pour les assets

## 📞 Support

- **Documentation GitBook** : [docs.gitbook.com](https://docs.gitbook.com/)
- **Community** : [community.gitbook.com](https://community.gitbook.com/)
- **GitHub Issues** : Pour les problèmes spécifiques au projet

---

**Votre wiki JobsAdventure est maintenant prêt ! 📚✨**