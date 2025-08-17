# 🎮 Guide du joueur - Premiers pas

Bienvenue dans JobsAdventure ! Ce guide vous apprendra tout ce qu'il faut savoir pour commencer votre aventure professionnelle sur Minecraft.

## 🤔 Qu'est-ce qu'un métier ?

Un **métier** dans JobsAdventure est une spécialisation qui vous permet de :
- **Gagner de l'expérience (XP)** en effectuant certaines actions
- **Monter de niveau** et débloquer de nouvelles capacités
- **Obtenir des récompenses** uniques basées sur votre progression
- **Participer à l'économie** du serveur de manière spécialisée

## 📋 Découvrir les métiers disponibles

Pour voir tous les métiers disponibles sur le serveur :
```
/jobs list
```

Vous verrez une liste comme celle-ci :
```
=== Métiers Disponibles ===
✓ Miner - Extraction de ressources souterraines
✗ Farmer - Agriculture et élevage  
✗ Hunter - Combat et survie
```

- ✓ = Métiers que vous avez déjà
- ✗ = Métiers disponibles

## 🎯 Votre premier métier

### Étape 1 : Choisir un métier
Nous recommandons de commencer par **Miner** car c'est le plus simple :
```
/jobs join miner
```

**Message de confirmation :**
```
✅ Vous avez rejoint le métier Miner !
```

### Étape 2 : Comprendre votre métier
Pour voir les détails de votre nouveau métier :
```
/jobs info miner
```

**Informations affichées :**
```
=== Miner ===
Description : Extraction de ressources souterraines
Niveau max : 100
Permission : jobsadventure.job.miner
Histoire :
  - Creusez profondément et trouvez des richesses !
  - Montez de niveau en minant minerais et pierres
  - XP bonus pour les matériaux rares
Types d'actions : BREAK, KILL
```

### Étape 3 : Commencer à gagner de l'XP
Maintenant, allez **miner des blocs** ! Chaque bloc miné vous donnera de l'XP :

- **Pierre** : 1 XP
- **Charbon** : 5 XP  
- **Fer** : 10 XP
- **Or** : 25 XP
- **Diamant** : 50 XP

**Messages XP que vous verrez :**
```
+1 XP (Miner)     [Pour la pierre]
+5 XP (Miner)     [Pour le charbon]
+50 XP (Miner)    [Pour le diamant]
```

## 📊 Suivre votre progression

### Vérifier vos statistiques
Pour voir votre niveau actuel et votre XP :
```
/jobs stats
```

**Exemple de sortie :**
```
=== Métiers de VotreNom ===
Miner - Niveau 3 (150/200 XP)
```

Cela signifie :
- Vous êtes **niveau 3** en Miner
- Vous avez **150 XP** sur les **200 nécessaires** pour le niveau 4

### Montée de niveau
Quand vous atteignez assez d'XP, vous montez de niveau :
```
🎉 Félicitations ! Vous avez atteint le niveau 4 dans le métier Miner !
```

### Voir le classement
Pour voir comment vous vous classez par rapport aux autres :
```
/jobs top miner
```

## 🎁 Système de récompenses

### Accéder aux récompenses
Pour voir les récompenses disponibles pour votre métier :
```
/jobs rewards open miner
```

Cela ouvre une **interface graphique** où vous pouvez :
- Voir toutes les récompenses disponibles
- Vérifier les prérequis
- Réclamer les récompenses débloquées

### Types de récompenses

#### Récompenses d'objets
- Outils améliorés
- Matériaux rares
- Objets spéciaux

#### Récompenses économiques
- Argent ajouté à votre compte
- Bonus économiques

#### Récompenses de commandes
- Téléportations spéciales
- Permissions temporaires
- Effets spéciaux

### Exemple de récompense débutant
**Starter Bonus** (Niveau 1) :
- 1x Pioche en pierre
- 10x Pain
- 50 pièces d'or

Pour la réclamer, cliquez dessus dans l'interface ou utilisez :
```
/jobs rewards claim miner starter_bonus
```

## 🏢 Avoir plusieurs métiers

### Limite de métiers
La plupart des serveurs permettent **2-3 métiers maximum** par joueur. Pour vérifier votre limite :
```
/jobs stats
```

### Ajouter un deuxième métier
Une fois à l'aise avec Miner, essayez un autre métier :
```
/jobs join farmer
```

### Gérer vos métiers
Pour quitter un métier :
```
/jobs leave farmer
```

**⚠️ Attention :** Quitter un métier vous fait perdre tout votre progrès dans ce métier !

## 🎯 Stratégies pour débuter

### 1. Commencez simple
- Choisissez **Miner** comme premier métier
- Minez dans vos activités normales
- Ne vous forcez pas, laissez l'XP venir naturellement

### 2. Explorez les prérequis
Certaines actions nécessitent des conditions :
- **Outils spécifiques** (ex: pioche en fer pour miner le charbon efficacement)
- **Niveau minimum** (ex: niveau 10 pour miner le fer)
- **Monde spécifique** (ex: diamants seulement dans l'overworld)
- **Heure du jour** (ex: certains bonus la nuit)

### 3. Optimisez votre équipement
- Utilisez les **meilleurs outils** pour votre niveau
- Réclamez les **récompenses d'outils** dès que possible
- Vérifiez les **enchantements recommandés**

### 4. Planifiez votre progression
- Regardez les **récompenses futures** pour vous motiver
- Fixez-vous des **objectifs de niveau** (ex: niveau 10, 25, 50)
- Variez les **activités** pour éviter l'ennui

## 💡 Conseils avancés

### Messages XP
Vous pouvez recevoir les messages XP de trois façons :
- **Chat** : Messages normaux dans le chat
- **Barre d'action** : Au-dessus de votre barre d'objets
- **Barre de boss** : Barre colorée en haut de l'écran

### Bonus XP temporaires
Parfois, les administrateurs activent des **événements bonus XP** :
```
🔥 BOOST XP ! +100% XP pendant 1 heure (Événement weekend)
```

### Conditions spéciales
Certaines actions donnent plus d'XP dans des conditions spéciales :
- **Profondeur** (plus profond = plus d'XP pour le mining)
- **Biome** (certains biomes donnent des bonus)
- **Heure** (bonus nocturnes pour certains métiers)
- **Météo** (bonus de pluie pour l'agriculture)

## ❓ Problèmes courants

### "Je ne gagne pas d'XP !"
✅ **Vérifications :**
1. Avez-vous rejoint le métier ? (`/jobs stats`)
2. Minez-vous les bons blocs ?
3. Respectez-vous les conditions (outils, niveau, etc.) ?
4. Le bloc était-il placé par un joueur ? (pas d'XP pour les blocs artificiels)

### "Je ne peux pas rejoindre un métier !"
✅ **Solutions :**
1. Vérifiez que vous avez la permission
2. Vérifiez que vous n'avez pas atteint la limite de métiers
3. Demandez à un administrateur

### "L'interface des récompenses ne s'ouvre pas !"
✅ **Solutions :**
1. Assurez-vous d'avoir rejoint le métier
2. Vérifiez que vous avez la permission `jobsadventure.rewards.use`
3. Essayez `/jobs rewards list` d'abord

## 📚 Commandes essentielles à retenir

| Commande | Description |
|:---|:---|
| `/jobs list` | Voir tous les métiers |
| `/jobs join <métier>` | Rejoindre un métier |
| `/jobs stats` | Voir votre progression |
| `/jobs info <métier>` | Détails d'un métier |
| `/jobs rewards open <métier>` | Interface des récompenses |
| `/jobs top <métier>` | Classement |
| `/jobs help` | Aide complète |

## 🚀 Prochaines étapes

Maintenant que vous maîtrisez les bases :

1. **Explorez** les autres guides :
   - [Rejoindre et quitter un métier](joining-leaving-jobs.md)
   - [Système de niveaux et XP](levels-and-xp.md)  
   - [Système de récompenses](rewards-system.md)

2. **Découvrez** les fonctionnalités avancées :
   - Intégrations avec d'autres plugins
   - Système de conditions complexes
   - Bonus et événements spéciaux

3. **Rejoignez** la communauté pour partager vos expériences !

---

**Bon jeu et bonne progression dans vos métiers ! 🎉**