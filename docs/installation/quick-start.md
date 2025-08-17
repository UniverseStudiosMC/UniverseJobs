# 🚀 Installation rapide

Ce guide vous permettra d'installer JobsAdventure en moins de 5 minutes !

## 📋 Prérequis

Avant de commencer, assurez-vous d'avoir :

- **Serveur Minecraft** : Paper 1.13+ / Spigot 1.13+ / Bukkit 1.13+ (ou Folia 1.21+ pour les performances maximales)
- **Java** : OpenJDK 21 (recommandé) ou version compatible
- **PlaceholderAPI** : Obligatoire pour toutes les fonctionnalités
- **RAM** : Minimum 2GB, recommandé 4GB+ pour les gros serveurs

## 📥 Téléchargement

1. Téléchargez la dernière version de JobsAdventure depuis :
   - GitHub Releases : [JobsAdventure Releases](https://github.com/ax-dev/JobsAdventure/releases)
   - SpigotMC : [Page SpigotMC](https://spigotmc.org/)

2. Téléchargez **PlaceholderAPI** si vous ne l'avez pas déjà :
   - [PlaceholderAPI sur SpigotMC](https://www.spigotmc.org/resources/placeholderapi.6245/)

## 🔧 Installation

### Étape 1 : Installation des plugins
1. **Placez** le fichier `JobsAdventure-v1.0.jar` dans votre dossier `/plugins`
2. **Placez** le fichier `PlaceholderAPI.jar` dans votre dossier `/plugins`
3. **Redémarrez** votre serveur

### Étape 2 : Vérification
1. Vérifiez dans la console que JobsAdventure s'est chargé correctement :
   ```
   [INFO] JobsAdventure a été activé avec succès !
   [INFO] Configuration chargée avec succès
   [INFO] Métiers chargés avec succès
   [INFO] PlaceholderAPI integration initialized successfully
   ```

2. Testez en jeu avec la commande :
   ```
   /jobs list
   ```

## ✅ Configuration par défaut

JobsAdventure est livré avec **3 métiers pré-configurés** prêts à l'emploi :

### ⛏️ Mineur (Miner)
- **Niveau max** : 100
- **Actions** : Minage de pierres, minerais, combat souterrain
- **Récompenses** : Outils améliorés, bonus économiques
- **Spécialités** : Bonus de profondeur, multiplicateurs de minerais rares

### 🌾 Fermier (Farmer)  
- **Niveau max** : 75
- **Actions** : Agriculture, élevage, production alimentaire
- **Récompenses** : Graines rares, outils agricoles
- **Spécialités** : Bonus saisonniers, multiplicateurs d'élevage

### 🏹 Chasseur (Hunter)
- **Niveau max** : 80
- **Actions** : Combat, apprivoisement, survie
- **Récompenses** : Armes spécialisées, objets rares
- **Spécialités** : Intégration MythicMobs, bonus de créatures rares

## 🎮 Test rapide

1. **Rejoignez un métier** :
   ```
   /jobs join miner
   ```

2. **Minez quelques blocs** de pierre ou de charbon

3. **Vérifiez vos stats** :
   ```
   /jobs stats
   ```

4. **Ouvrez l'interface des récompenses** :
   ```
   /jobs rewards open miner
   ```

## 🔧 Plugins optionnels (recommandés)

Pour une expérience complète, installez ces plugins :

| Plugin | Fonctionnalité | Priorité |
|:---|:---|:---:|
| **Vault** | Économie et permissions | 🔴 Haute |
| **MythicMobs** | Créatures personnalisées | 🟡 Moyenne |
| **CustomCrops** | Agriculture avancée | 🟡 Moyenne |
| **CustomFishing** | Pêche personnalisée | 🟢 Basse |
| **Nexo/ItemsAdder** | Objets personnalisés | 🟢 Basse |
| **MMOItems** | Outils spécialisés | 🟢 Basse |

## 🎯 Prochaines étapes

Maintenant que JobsAdventure est installé :

1. **Explorez** les [métiers par défaut](../player-guide/getting-started.md)
2. **Configurez** vos [premiers métiers personnalisés](../jobs-configuration/creating-jobs.md)
3. **Découvrez** le [système de récompenses](../rewards/reward-configuration.md)
4. **Apprenez** les [commandes d'administration](../admin-guide/admin-commands.md)

## ❗ Problèmes courants

### Plugin ne se charge pas
- ✅ Vérifiez que vous utilisez Java 21+
- ✅ Assurez-vous que PlaceholderAPI est installé
- ✅ Consultez les [logs](../troubleshooting/logs-debugging.md) pour plus de détails

### Commandes ne fonctionnent pas
- ✅ Vérifiez les [permissions](../reference/permissions.md)
- ✅ Redémarrez le serveur après l'installation

### Pas d'XP gagné
- ✅ Vérifiez que vous avez rejoint un métier avec `/jobs join <métier>`
- ✅ Consultez le [guide de dépannage](../troubleshooting/common-issues.md)

## 🆘 Besoin d'aide ?

- 📚 [FAQ complète](../appendix/faq.md)
- 🔧 [Guide de dépannage](../troubleshooting/common-issues.md)
- 💬 [Support et communauté](../appendix/support.md)

---

**Félicitations ! JobsAdventure est maintenant installé et prêt à transformer votre serveur ! 🎉**