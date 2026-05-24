package me.pulsi_.bankplus.commands.list;

import me.pulsi_.bankplus.BankPlus;
import me.pulsi_.bankplus.commands.BPCmdExecution;
import me.pulsi_.bankplus.commands.BPCommand;
import me.pulsi_.bankplus.economy.BPEconomy;
import me.pulsi_.bankplus.sql.BPSQL;
import me.pulsi_.bankplus.utils.BPLogger;
import me.pulsi_.bankplus.utils.texts.BPArgs;
import me.pulsi_.bankplus.utils.texts.BPFormatter;
import me.pulsi_.bankplus.utils.texts.BPMessages;
import me.pulsi_.bankplus.values.ConfigValues;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class TransferCmd extends BPCommand {

    public TransferCmd(FileConfiguration commandsConfig, String commandID) {
        super(commandsConfig, commandID);
    }

    public TransferCmd(FileConfiguration commandsConfig, String commandID, String... aliases) {
        super(commandsConfig, commandID, aliases);
    }

    @Override
    public List<String> defaultUsage() {
        return Arrays.asList(
                "%prefix% Usage: /bank migrate [from] [to]",
                "Available types: <aqua>YML</aqua>, <aqua>SQLite</aqua>, <aqua>MySQL</aqua>.",
                "Example: <aqua>/bank migrate YML SQLite</aqua>"
        );
    }

    @Override
    public int defaultConfirmCooldown() {
        return 5;
    }

    @Override
    public List<String> defaultConfirmMessage() {
        return Collections.singletonList("%prefix% <red>This command will overwrite the data from a place to another, type the command again within 5 seconds to confirm.");
    }

    @Override
    public int defaultCooldown() {
        return 0;
    }

    @Override
    public List<String> defaultCooldownMessage() {
        return Collections.emptyList();
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public boolean skipUsage() {
        return false;
    }

    @Override
    public BPCmdExecution onExecution(CommandSender s, String[] args) {
        if (args.length < 3) {
            for (String usage : defaultUsage()) BPMessages.sendMessage(s, usage);
            return BPCmdExecution.invalidExecution();
        }

        String fromType = args[1].toUpperCase();
        String toType = args[2].toUpperCase();

        if (fromType.equals(toType)) {
            BPMessages.sendMessage(s, "%prefix% <red>Source and destination types cannot be the same!");
            return BPCmdExecution.invalidExecution();
        }

        List<String> validTypes = Arrays.asList("YML", "SQLITE", "MYSQL");
        if (!validTypes.contains(fromType) || !validTypes.contains(toType)) {
            BPMessages.sendMessage(s, "%prefix% <red>Invalid types! Use YML, SQLite, or MySQL.");
            return BPCmdExecution.invalidExecution();
        }

        return new BPCmdExecution() {
            @Override
            public void execute() {
                BPMessages.sendMessage(s, "%prefix% Task initialized, wait a few moments...");

                Bukkit.getScheduler().runTaskAsynchronously(BankPlus.INSTANCE(), () -> {
                    try {
                        BPMessages.sendMessage(s, "%prefix% <yellow>Starting migration from " + fromType + " to " + toType + "... Please wait.");

                        // 1. Disconnect current connection so we can manage connections cleanly
                        BPSQL.disconnect();

                        // 2. Connect to the source and load data into memory
                        Map<OfflinePlayer, Map<String, MigrateData>> loadedData;
                        if (fromType.equals("YML")) {
                            loadedData = loadFromYML();
                        } else {
                            if (fromType.equals("MYSQL")) {
                                BPSQL.MySQL.connect();
                            } else {
                                BPSQL.SQLite.connect();
                            }
                            loadedData = loadFromDatabase();
                            BPSQL.disconnect();
                        }

                        // 3. Connect to the destination and save data from memory
                        if (toType.equals("YML")) {
                            saveToYML(loadedData);
                        } else {
                            if (toType.equals("MYSQL")) {
                                BPSQL.MySQL.connect();
                            } else {
                                BPSQL.SQLite.connect();
                            }
                            saveToDatabase(loadedData);
                            BPSQL.disconnect();
                        }

                        // 4. Reconnect to the server's configured storage
                        if (!ConfigValues.isUseFiles()) {
                            if (ConfigValues.isMySqlEnabled()) {
                                BPSQL.MySQL.connect();
                            } else {
                                BPSQL.SQLite.connect();
                            }
                        }

                        BPMessages.sendMessage(s, "%prefix% <green>Migration successfully completed! Migrated " + loadedData.size() + " player records.");
                    } catch (Exception e) {
                        BPLogger.Console.error(e, "Error occurred during migration.");
                        BPMessages.sendMessage(s, "%prefix% <red>An error occurred during migration: " + e.getMessage());

                        // Try to restore connection
                        try {
                            BPSQL.disconnect();
                            if (!ConfigValues.isUseFiles()) {
                                if (ConfigValues.isMySqlEnabled()) BPSQL.MySQL.connect();
                                else BPSQL.SQLite.connect();
                            }
                        } catch (Exception ignored) {}
                    }
                });
            }
        };
    }

    @Override
    public List<String> tabCompletion(CommandSender s, String[] args) {
        if (args.length == 2)
            return BPArgs.getArgs(args, "YML", "SQLite", "MySQL");
        if (args.length == 3)
            return BPArgs.getArgs(args, "YML", "SQLite", "MySQL");
        return null;
    }

    private static class MigrateData {
        int level;
        BigDecimal debt;
        BigDecimal money;
        BigDecimal interest;
    }

    private Map<OfflinePlayer, Map<String, MigrateData>> loadFromYML() {
        Map<OfflinePlayer, Map<String, MigrateData>> allData = new HashMap<>();
        List<BPEconomy> economies = BPEconomy.list();
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            String identifier = (ConfigValues.isStoringUUIDs() ? p.getUniqueId().toString() : p.getName());
            if (identifier == null) continue;
            File file = new File(BankPlus.INSTANCE().getDataFolder(), "playerdata" + File.separator + identifier + ".yml");
            if (!file.exists()) continue;

            FileConfiguration pConfig = YamlConfiguration.loadConfiguration(file);
            Map<String, MigrateData> playerBanks = new HashMap<>();
            for (BPEconomy economy : economies) {
                String bankName = economy.getOriginBank().getIdentifier();
                MigrateData data = new MigrateData();
                data.level = Math.max(pConfig.getInt("banks." + bankName + ".level", 1), 1);
                data.debt = BPFormatter.getStyledBigDecimal(pConfig.getString("banks." + bankName + ".debt", "0"));
                data.money = BPFormatter.getStyledBigDecimal(pConfig.getString("banks." + bankName + ".money", "0"));
                data.interest = BPFormatter.getStyledBigDecimal(pConfig.getString("banks." + bankName + ".interest", "0"));
                playerBanks.put(bankName, data);
            }
            allData.put(p, playerBanks);
        }
        return allData;
    }

    private Map<OfflinePlayer, Map<String, MigrateData>> loadFromDatabase() {
        Map<OfflinePlayer, Map<String, MigrateData>> allData = new HashMap<>();
        List<BPEconomy> economies = BPEconomy.list();
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            Map<String, MigrateData> playerBanks = new HashMap<>();
            boolean hasData = false;
            for (BPEconomy economy : economies) {
                String bankName = economy.getOriginBank().getIdentifier();
                if (BPSQL.isRegistered(p, bankName)) {
                    MigrateData data = new MigrateData();
                    data.level = BPSQL.getBankLevel(p, bankName);
                    data.debt = BPSQL.getDebt(p, bankName);
                    data.money = BPSQL.getMoney(p, bankName);
                    data.interest = BPSQL.getInterest(p, bankName);
                    playerBanks.put(bankName, data);
                    hasData = true;
                }
            }
            if (hasData) {
                allData.put(p, playerBanks);
            }
        }
        return allData;
    }

    private void saveToYML(Map<OfflinePlayer, Map<String, MigrateData>> allData) {
        for (Map.Entry<OfflinePlayer, Map<String, MigrateData>> entry : allData.entrySet()) {
            OfflinePlayer p = entry.getKey();
            String identifier = (ConfigValues.isStoringUUIDs() ? p.getUniqueId().toString() : p.getName());
            if (identifier == null) continue;
            File file = new File(BankPlus.INSTANCE().getDataFolder(), "playerdata" + File.separator + identifier + ".yml");
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException ignored) {}
            }
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set("name", p.getName());
            for (Map.Entry<String, MigrateData> bankEntry : entry.getValue().entrySet()) {
                String bankName = bankEntry.getKey();
                MigrateData data = bankEntry.getValue();
                config.set("banks." + bankName + ".level", data.level);
                config.set("banks." + bankName + ".debt", data.debt.toPlainString());
                config.set("banks." + bankName + ".money", data.money.toPlainString());
                config.set("banks." + bankName + ".interest", data.interest.toPlainString());
            }
            try {
                config.save(file);
            } catch (IOException ignored) {}
        }
    }

    private void saveToDatabase(Map<OfflinePlayer, Map<String, MigrateData>> allData) {
        for (Map.Entry<OfflinePlayer, Map<String, MigrateData>> entry : allData.entrySet()) {
            OfflinePlayer p = entry.getKey();
            BPSQL.fillRecords(p);
            for (Map.Entry<String, MigrateData> bankEntry : entry.getValue().entrySet()) {
                String bankName = bankEntry.getKey();
                MigrateData data = bankEntry.getValue();
                BPSQL.setBankLevel(p, bankName, data.level);
                BPSQL.setDebt(p, bankName, data.debt);
                BPSQL.setMoney(p, bankName, data.money);
                BPSQL.setInterest(p, bankName, data.interest);
            }
        }
    }
}