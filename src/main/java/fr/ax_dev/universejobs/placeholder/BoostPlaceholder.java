package fr.ax_dev.universejobs.placeholder;

import fr.ax_dev.universejobs.UniverseJobs;
import fr.ax_dev.universejobs.bonus.BaseBonus;
import fr.ax_dev.universejobs.bonus.MoneyBonus;
import fr.ax_dev.universejobs.bonus.XpBonus;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.stream.Collectors;

public class BoostPlaceholder extends PlaceholderExpansion {

    private final UniverseJobs plugin;

    public BoostPlaceholder(UniverseJobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "universejobs";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) return null;

        String[] args = params.split("_");
        if (args.length < 2) return null;

        if (!args[0].equalsIgnoreCase("boost")) {
            return null;
        }

        String type = args[1];
        
        switch (type.toLowerCase()) {
            case "count":
                return handleCountPlaceholder(args);
            case "list":
                return handleListPlaceholder(args);
            case "player":
                return handlePlayerBoostPlaceholder(player, args);
            case "global":
                return handleGlobalBoostPlaceholder(args);
            default:
                return null;
        }
    }

    private String handleCountPlaceholder(String[] args) {
        if (args.length < 3) return null;

        String countType = args[2];
        
        switch (countType.toLowerCase()) {
            case "xp":
                return String.valueOf(getActiveXpBoostsCount());
            case "money":
                return String.valueOf(getActiveMoneyBoostsCount());
            case "total":
                return String.valueOf(getActiveXpBoostsCount() + getActiveMoneyBoostsCount());
            default:
                return null;
        }
    }

    private String handleListPlaceholder(String[] args) {
        if (args.length < 4) return null;

        String listType = args[2];
        
        try {
            int position = Integer.parseInt(args[3]);
            String info = args.length > 4 ? args[4] : "id";

            switch (listType.toLowerCase()) {
                case "xp":
                    return getXpBoostInfo(position, info);
                case "money":
                    return getMoneyBoostInfo(position, info);
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return "Invalid Position";
        }
    }

    private String handlePlayerBoostPlaceholder(OfflinePlayer player, String[] args) {
        if (player == null || args.length < 3) return null;

        String playerType = args[2];
        
        switch (playerType.toLowerCase()) {
            case "hasxp":
                return String.valueOf(playerHasActiveXpBoost(player.getUniqueId()));
            case "hasmoney":
                return String.valueOf(playerHasActiveMoneyBoost(player.getUniqueId()));
            case "hasany":
                return String.valueOf(playerHasActiveXpBoost(player.getUniqueId()) || 
                                    playerHasActiveMoneyBoost(player.getUniqueId()));
            case "xpcount":
                return String.valueOf(getPlayerActiveXpBoostsCount(player.getUniqueId()));
            case "moneycount":
                return String.valueOf(getPlayerActiveMoneyBoostsCount(player.getUniqueId()));
            case "totalcount":
                return String.valueOf(getPlayerActiveXpBoostsCount(player.getUniqueId()) + 
                                    getPlayerActiveMoneyBoostsCount(player.getUniqueId()));
            default:
                return null;
        }
    }

    private String handleGlobalBoostPlaceholder(String[] args) {
        if (args.length < 3) return null;

        String globalType = args[2];
        
        switch (globalType.toLowerCase()) {
            case "hasxp":
                return String.valueOf(hasGlobalXpBoost());
            case "hasmoney":
                return String.valueOf(hasGlobalMoneyBoost());
            case "hasany":
                return String.valueOf(hasGlobalXpBoost() || hasGlobalMoneyBoost());
            case "xpcount":
                return String.valueOf(getGlobalXpBoostsCount());
            case "moneycount":
                return String.valueOf(getGlobalMoneyBoostsCount());
            case "totalcount":
                return String.valueOf(getGlobalXpBoostsCount() + getGlobalMoneyBoostsCount());
            default:
                return null;
        }
    }

    private int getActiveXpBoostsCount() {
        return plugin.getBonusManager().getAllActiveBoostIds().size();
    }

    private int getActiveMoneyBoostsCount() {
        return plugin.getMoneyBonusManager().getAllActiveBoostIds().size();
    }

    private String getXpBoostInfo(int position, String info) {
        List<XpBonus> boosts = plugin.getBonusManager().getAllActiveBoostIds().stream()
            .map(id -> plugin.getBonusManager().getBoostById(id))
            .filter(boost -> boost != null && boost.isActive())
            .collect(Collectors.toList());

        if (position < 1 || position > boosts.size()) {
            return getEmptyBoostValue(info);
        }

        XpBonus boost = boosts.get(position - 1);
        return formatBoostInfo(boost, info);
    }

    private String getMoneyBoostInfo(int position, String info) {
        List<MoneyBonus> boosts = plugin.getMoneyBonusManager().getAllActiveBoostIds().stream()
            .map(id -> plugin.getMoneyBonusManager().getBoostById(id))
            .filter(boost -> boost != null && boost.isActive())
            .collect(Collectors.toList());

        if (position < 1 || position > boosts.size()) {
            return getEmptyBoostValue(info);
        }

        MoneyBonus boost = boosts.get(position - 1);
        return formatBoostInfo(boost, info);
    }

    private String formatBoostInfo(BaseBonus boost, String info) {
        switch (info.toLowerCase()) {
            case "id":
                return boost.getBoostId();
            case "multiplier":
                return String.valueOf(boost.getMultiplier());
            case "remainingtime":
                return boost.getRemainingTimeFormatted();
            case "playername":
                if (boost.isGlobal()) {
                    return "All Players";
                }
                var player = Bukkit.getPlayer(boost.getPlayerId());
                return player != null ? player.getName() : "Unknown";
            case "job":
                return boost.getJobId() != null ? boost.getJobId() : "All Jobs";
            case "action":
                return formatActionInfo(boost.getActionType(), boost.getActionId());
            case "isglobal":
                return String.valueOf(boost.isGlobal());
            case "isactive":
                return String.valueOf(boost.isActive());
            default:
                return "Invalid Info";
        }
    }

    private String formatActionInfo(String actionType, String actionId) {
        if (actionType == null) {
            return "All Actions";
        }
        if (actionId == null) {
            return actionType;
        }
        return actionType + ":" + actionId;
    }

    private String getEmptyBoostValue(String info) {
        switch (info.toLowerCase()) {
            case "id":
                return "No Boost";
            case "multiplier":
                return "0.0";
            case "remainingtime":
                return "0s";
            case "playername":
                return "No Player";
            case "job":
                return "No Job";
            case "action":
                return "No Action";
            case "isglobal":
            case "isactive":
                return "false";
            default:
                return "N/A";
        }
    }

    private boolean playerHasActiveXpBoost(java.util.UUID playerId) {
        return plugin.getBonusManager().getAllActiveBoostIds().stream()
            .map(id -> plugin.getBonusManager().getBoostById(id))
            .anyMatch(boost -> boost != null && boost.isActive() && 
                     (boost.isGlobal() || boost.getPlayerId().equals(playerId)));
    }

    private boolean playerHasActiveMoneyBoost(java.util.UUID playerId) {
        return plugin.getMoneyBonusManager().getAllActiveBoostIds().stream()
            .map(id -> plugin.getMoneyBonusManager().getBoostById(id))
            .anyMatch(boost -> boost != null && boost.isActive() && 
                     (boost.isGlobal() || boost.getPlayerId().equals(playerId)));
    }

    private int getPlayerActiveXpBoostsCount(java.util.UUID playerId) {
        return (int) plugin.getBonusManager().getAllActiveBoostIds().stream()
            .map(id -> plugin.getBonusManager().getBoostById(id))
            .filter(boost -> boost != null && boost.isActive() && 
                    (boost.isGlobal() || boost.getPlayerId().equals(playerId)))
            .count();
    }

    private int getPlayerActiveMoneyBoostsCount(java.util.UUID playerId) {
        return (int) plugin.getMoneyBonusManager().getAllActiveBoostIds().stream()
            .map(id -> plugin.getMoneyBonusManager().getBoostById(id))
            .filter(boost -> boost != null && boost.isActive() && 
                    (boost.isGlobal() || boost.getPlayerId().equals(playerId)))
            .count();
    }

    private boolean hasGlobalXpBoost() {
        return plugin.getBonusManager().getAllActiveBoostIds().stream()
            .map(id -> plugin.getBonusManager().getBoostById(id))
            .anyMatch(boost -> boost != null && boost.isActive() && boost.isGlobal());
    }

    private boolean hasGlobalMoneyBoost() {
        return plugin.getMoneyBonusManager().getAllActiveBoostIds().stream()
            .map(id -> plugin.getMoneyBonusManager().getBoostById(id))
            .anyMatch(boost -> boost != null && boost.isActive() && boost.isGlobal());
    }

    private int getGlobalXpBoostsCount() {
        return (int) plugin.getBonusManager().getAllActiveBoostIds().stream()
            .map(id -> plugin.getBonusManager().getBoostById(id))
            .filter(boost -> boost != null && boost.isActive() && boost.isGlobal())
            .count();
    }

    private int getGlobalMoneyBoostsCount() {
        return (int) plugin.getMoneyBonusManager().getAllActiveBoostIds().stream()
            .map(id -> plugin.getMoneyBonusManager().getBoostById(id))
            .filter(boost -> boost != null && boost.isActive() && boost.isGlobal())
            .count();
    }
}