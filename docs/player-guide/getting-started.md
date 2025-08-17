# 🎮 Player Guide - Getting Started

Welcome to JobsAdventure! This guide will teach you everything you need to know to start your professional adventure in Minecraft.

## 🤔 What is a Job?

A **job** in JobsAdventure is a specialization that allows you to:
- **Gain experience (XP)** by performing certain actions
- **Level up** and unlock new abilities
- **Obtain unique rewards** based on your progression
- **Participate in the server economy** in a specialized way

## 📋 Discover Available Jobs

To see all jobs available on the server:
```
/jobs list
```

You'll see a list like this:
```
=== Available Jobs ===
✓ Miner - Underground resource extraction
✗ Farmer - Agriculture and livestock  
✗ Hunter - Combat and survival
```

- ✓ = Jobs you already have
- ✗ = Availabthe jobs

## 🎯 Your First Job

### Step 1: Choose a Job
We recommend starting with **Miner** as it's the simplest:
```
/jobs join miner
```

**Confirmation message:**
```
✅ You have joined the Miner job!
```

### Step 2: Understand Your Job
To see details about your new job:
```
/jobs info miner
```

**Information displayed:**
```
=== Miner ===
Description: Underground resource extraction
Max Level: 100
Permission: jobsadventure.job.miner
Lore:
  - Dig deep and find riches!
  - Level up by mining ores and stones
  - Bonus XP for rare materials
Action Types: BREAK, KILL
```

### Step 3: Start Gaining XP
Now go **mine blocks**! Each mined block will give you XP:

- **Stone**: 1 XP
- **Coal**: 5 XP  
- **Iron**: 10 XP
- **Gold**: 25 XP
- **Diamond**: 50 XP

**XP messages you'll see:**
```
+1 XP (Miner)     [For stone]
+5 XP (Miner)     [For coal]
+50 XP (Miner)    [For diamond]
```

## 📊 Track Your Progress

### Check Your Statistics
To see your current level and XP:
```
/jobs stats
```

**Example output:**
```
=== YourName's Jobs ===
Miner - Level 3 (150/200 XP)
```

This means:
- You are **level 3** in Miner
- You have **150 XP** out of **200 needed** for level 4

### Level Up
When you reach enough XP, you level up:
```
🎉 Congratulations! You have reached level 4 in the Miner job!
```

### See Rankings
To see how you rank against others:
```
/jobs top miner
```

## 🎁 Reward System

### Access Rewards
To see available rewards for your job:
```
/jobs rewards open miner
```

This opens a **graphical interface** where you can:
- See all available rewards
- Check requirements
- Claim unlocked rewards

### Types of Rewards

#### Item Rewards
- Enhanced tools
- Rare materials
- Special items

#### Economic Rewards
- Money added to your account
- Economic bonuses

#### Command Rewards
- Special teleportations
- Temporary permissions
- Special effects

### Example Beginner Reward
**Starter Bonus** (Level 1):
- 1x Stone Pickaxe
- 10x Bread
- 50 gold coins

To claim it, click on it in the interface or use:
```
/jobs rewards claim miner starter_bonus
```

## 🏢 Having Multiple Jobs

### Job Limit
Most servers allow **2-3 jobs maximum** per player. To check your limit:
```
/jobs stats
```

### Add a Second Job
Once comfortable with Miner, try another job:
```
/jobs join farmer
```

### Manage Your Jobs
To leave a job:
```
/jobs leave farmer
```

**⚠️ Warning:** Leaving a job makes you lose all progress in that job!

## 🎯 Beginner Strategies

### 1. Start Simple
- Choose **Miner** as your first job
- Mine during your normal activities
- Don't force it, let XP come naturally

### 2. Explore Requirements
Some actions require conditions:
- **Specific tools** (e.g., iron pickaxe to mine coal efficiently)
- **Minimum level** (e.g., level 10 to mine iron)
- **Specific world** (e.g., diamonds only in overworld)
- **Time of day** (e.g., certain bonuses at night)

### 3. Optimize Your Equipment
- Use the **best tools** for your level
- Claim **tool rewards** as soon as possible
- Check **recommended enchantments**

### 4. Plan Your Progression
- Look at **future rewards** for motivation
- Set **level goals** (e.g., level 10, 25, 50)
- Vary **activities** to avoid boredom

## 💡 Advanced Tips

### XP Messages
You can receive XP messages in three ways:
- **Chat**: Normal messages in chat
- **Action Bar**: Above your hotbar
- **Boss Bar**: Colored bar at top of screen

### Temporary XP Bonuses
Sometimes administrators activate **bonus XP events**:
```
🔥 XP BOOST! +100% XP for 1 hour (Weekend event)
```

### Special Conditions
Some actions give more XP under special conditions:
- **Depth** (deeper = more XP for mining)
- **Biome** (certain biomes give bonuses)
- **Time** (night bonuses for certain jobs)
- **Weather** (rain bonuses for farming)

## ❓ Common Problems

### "I'm not gaining XP!"
✅ **Checks:**
1. Have you joined the job? (`/jobs stats`)
2. Are you mining the right blocks?
3. Do you meet the conditions (tools, level, etc.)?
4. Was the block placed by a player? (no XP for artificial blocks)

### "I can't join a job!"
✅ **Solutions:**
1. Check you have permission
2. Check you haven't reached the job limit
3. Ask an administrator

### "The rewards interface won't open!"
✅ **Solutions:**
1. Make sure you've joined the job
2. Check you have permission `jobsadventure.rewards.use`
3. Try `/jobs rewards list` first

## 📚 Essential Commands to Remember

| Command | Description |
|:---|:---|
| `/jobs list` | See all jobs |
| `/jobs join <job>` | Join a job |
| `/jobs stats` | See your progress |
| `/jobs info <job>` | Job details |
| `/jobs rewards open <job>` | Rewards interface |
| `/jobs top <job>` | Rankings |
| `/jobs help` | Complete help |

## 🚀 Next Steps

Now that you master the basics:

1. **Explore** other guides:
   - [Joining and Leaving Jobs](joining-leaving-jobs.md)
   - [Levels and XP System](levels-and-xp.md)  
   - [Rewards System](rewards-system.md)

2. **Discover** advanced features:
   - Integrations with other plugins
   - Complex conditions system
   - Bonuses and special events

3. **Join** the community to share your experiences!

---

**Good luck and have fun progressing in your jobs! 🎉**