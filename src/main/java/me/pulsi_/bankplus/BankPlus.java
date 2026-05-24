package me.pulsi_.bankplus;

import me.pulsi_.bankplus.interest.BPInterest;
import me.pulsi_.bankplus.managers.BPAFK;
import me.pulsi_.bankplus.managers.BPConfigs;
import me.pulsi_.bankplus.managers.BPData;
import me.pulsi_.bankplus.placeholders.BPPlaceholders;
import me.pulsi_.bankplus.utils.BPLogger;
import me.pulsi_.bankplus.utils.BPScheduler;
import me.pulsi_.bankplus.utils.BPVersions;
import me.pulsi_.bankplus.utils.texts.BPChat;
import me.pulsi_.bankplus.values.ConfigValues;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

public final class BankPlus extends JavaPlugin {

    public static String actualVersion;

    private static String serverVersion;
    private static BankPlus INSTANCE;

    private Economy vaultEconomy = null;
    private Permission perms = null;

    private BPPlaceholders bpPlaceholders;
    private BPConfigs bpConfigs;
    private BPData bpData;
    private BPAFK BPAfk;
    private BPInterest interest;

    private boolean isPlaceholderApiHooked = false, isEssentialsXHooked = false, isCmiHooked = false, isUpdated;

    private int tries = 1;

    @Override
    public void onEnable() {
        INSTANCE = this;
        actualVersion = getDescription().getVersion();

        PluginManager plManager = Bukkit.getPluginManager();
        if (plManager.getPlugin("Vault") == null) {
            BPLogger.Console.log("");
            BPLogger.Console.log("<red>Cannot load " + BPChat.PREFIX + ", Vault is not installed.");
            BPLogger.Console.log("<red>Please download it in order to use this plugin.");
            BPLogger.Console.log("");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (!setupEconomy()) {
            if (tries < 4) {
                BPLogger.Console.warn("BankPlus didn't find any economy plugin on this server, the plugin will re-search in 2 seconds. (" + tries + " try)");
                BPScheduler.runTaskLater(this::onEnable, 40);
                tries++;
                return;
            }
            BPLogger.Console.log("");
            BPLogger.Console.log("<red>Cannot load " + BPChat.PREFIX + ", No economy plugin found.");
            BPLogger.Console.log("<red>Please download an economy plugin to use this plugin.");
            BPLogger.Console.log("");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        String v = getServer().getVersion();
        serverVersion = v.substring(v.lastIndexOf("MC:"), v.length() - 1).replace("MC: ", "");

        this.bpConfigs = new BPConfigs(this);
        this.bpData = new BPData(this);
        this.BPAfk = new BPAFK(this);
        this.interest = new BPInterest();

        if (!BPConfigs.isUpdated()) {
            BPVersions.renameInterestMoneyGiveToRate();
            BPVersions.convertPlayerFilesToNewStyle();
            BPVersions.changeBankUpgradesSection();
        }

        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        if (rsp != null) perms = rsp.getProvider();

        bpData.setupPlugin();

        if (plManager.getPlugin("PlaceholderAPI") != null) {
            BPLogger.Console.info("Hooked into PlaceholderAPI!");
            bpPlaceholders = new BPPlaceholders();
            bpPlaceholders.registerPlaceholders();
            bpPlaceholders.register();
            isPlaceholderApiHooked = true;
        }
        if (plManager.getPlugin("Essentials") != null) {
            BPLogger.Console.info("Hooked into Essentials!");
            isEssentialsXHooked = true;
        }
        if (plManager.getPlugin("CMI") != null) {
            BPLogger.Console.info("Hooked into CMI!");
            isCmiHooked = true;
        }

        if (ConfigValues.isUpdateCheckerEnabled())
            BPScheduler.runTaskTimerAsynchronously(() -> isUpdated = isPluginUpdated(), 0, (8 * 1200) * 60 /*8 hours*/);
    }

    @Override
    public void onDisable() {
        bpData.shutdownPlugin();
    }

    public static BankPlus INSTANCE() {
        return INSTANCE;
    }

    public static String getServerVersion() {
        return serverVersion;
    }

    public static boolean isAlphaVersion() {
        return INSTANCE.getDescription().getVersion().toLowerCase().contains("-alpha");
    }

    public Economy getVaultEconomy() {
        return vaultEconomy;
    }

    public Permission getPermissions() {
        return perms;
    }

    public boolean isPlaceholderApiHooked() {
        return isPlaceholderApiHooked;
    }

    public boolean isEssentialsXHooked() {
        return isEssentialsXHooked;
    }

    public boolean isCmiHooked() {
        return isCmiHooked;
    }

    public boolean isUpdated() {
        return isUpdated;
    }

    public BPConfigs getConfigs() {
        return bpConfigs;
    }

    public BPData getDataManager() {
        return bpData;
    }

    public BPAFK getAfkManager() {
        return BPAfk;
    }

    public BPInterest getInterest() {
        return interest;
    }

    public BPPlaceholders getBpPlaceholders() {
        return bpPlaceholders;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        vaultEconomy = rsp.getProvider();
        return true;
    }

    private boolean isPluginUpdated() {
        String currentVersion = getDescription().getVersion();
        String newVersion = currentVersion;
        boolean updated = true;
        try {
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) URI.create("https://api.github.com/repos/luminaworld/BankPlus/releases/latest").toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("\"tag_name\":")) {
                        newVersion = line.split(":")[1].replace("\"", "").replace(",", "").trim();
                        if (newVersion.startsWith("v")) newVersion = newVersion.substring(1);
                        break;
                    }
                }
            }

            updated = currentVersion.equalsIgnoreCase(newVersion);
        } catch (Exception e) {
            BPLogger.Console.warn("Could not check for updates on GitHub. (No internet connection or API limit reached)");
        }

        if (isAlphaVersion() && !ConfigValues.isSilentInfoMessages())
            BPLogger.Console.info("You are using an alpha version of the plugin, please report any bug or problem found in my discord!");

        if (updated) {
            if (!ConfigValues.isSilentInfoMessages()) BPLogger.Console.info("The plugin is updated!");
        } else {
            // Even if the info is disabled, notify when there is a new update
            // because it is important to keep users at the latest version.
            BPLogger.Console.info("New version of the plugin available! (v" + newVersion + ").");
            BPLogger.Console.info("Please download the latest version here: https://github.com/luminaworld/BankPlus/releases/latest");
        }
        return updated;
    }
}