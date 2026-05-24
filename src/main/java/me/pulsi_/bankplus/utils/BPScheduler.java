package me.pulsi_.bankplus.utils;

import me.pulsi_.bankplus.BankPlus;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

public class BPScheduler {

    private static boolean isFolia = false;

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.RegionScheduler");
            isFolia = true;
        } catch (ClassNotFoundException ignored) {}
    }

    public static boolean isFolia() {
        return isFolia;
    }

    public interface TaskWrapper {
        void cancel();
    }

    public static TaskWrapper runTask(Runnable runnable) {
        Plugin plugin = BankPlus.INSTANCE();
        if (isFolia) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getGlobalRegionScheduler()
                    .run(plugin, t -> runnable.run());
            return task::cancel;
        } else {
            BukkitTask task = Bukkit.getScheduler().runTask(plugin, runnable);
            return task::cancel;
        }
    }

    public static TaskWrapper runTask(Player player, Runnable runnable) {
        Plugin plugin = BankPlus.INSTANCE();
        if (isFolia && player != null) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask task = player.getScheduler()
                    .run(plugin, t -> runnable.run(), null);
            return () -> { if (task != null) task.cancel(); };
        } else {
            BukkitTask task = Bukkit.getScheduler().runTask(plugin, runnable);
            return task::cancel;
        }
    }

    public static TaskWrapper runTaskLater(Runnable runnable, long ticksDelay) {
        Plugin plugin = BankPlus.INSTANCE();
        if (isFolia) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getGlobalRegionScheduler()
                    .runDelayed(plugin, t -> runnable.run(), ticksDelay);
            return task::cancel;
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, ticksDelay);
            return task::cancel;
        }
    }

    public static TaskWrapper runTaskLater(Player player, Runnable runnable, long ticksDelay) {
        Plugin plugin = BankPlus.INSTANCE();
        if (isFolia && player != null) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask task = player.getScheduler()
                    .runDelayed(plugin, t -> runnable.run(), null, ticksDelay);
            return () -> { if (task != null) task.cancel(); };
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, runnable, ticksDelay);
            return task::cancel;
        }
    }

    public static TaskWrapper runTaskTimer(Runnable runnable, long ticksDelay, long ticksPeriod) {
        Plugin plugin = BankPlus.INSTANCE();
        if (isFolia) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getGlobalRegionScheduler()
                    .runAtFixedRate(plugin, t -> runnable.run(), Math.max(1, ticksDelay), ticksPeriod);
            return task::cancel;
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, ticksDelay, ticksPeriod);
            return task::cancel;
        }
    }

    public static TaskWrapper runTaskTimer(Player player, Runnable runnable, long ticksDelay, long ticksPeriod) {
        Plugin plugin = BankPlus.INSTANCE();
        if (isFolia && player != null) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask task = player.getScheduler()
                    .runAtFixedRate(plugin, t -> runnable.run(), null, Math.max(1, ticksDelay), ticksPeriod);
            return () -> { if (task != null) task.cancel(); };
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, runnable, ticksDelay, ticksPeriod);
            return task::cancel;
        }
    }

    public static TaskWrapper runTaskAsynchronously(Runnable runnable) {
        Plugin plugin = BankPlus.INSTANCE();
        if (isFolia) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getAsyncScheduler()
                    .runNow(plugin, t -> runnable.run());
            return task::cancel;
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            return task::cancel;
        }
    }

    public static TaskWrapper runTaskLaterAsynchronously(Runnable runnable, long ticksDelay) {
        Plugin plugin = BankPlus.INSTANCE();
        if (isFolia) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getAsyncScheduler()
                    .runDelayed(plugin, t -> runnable.run(), ticksDelay * 50, TimeUnit.MILLISECONDS);
            return task::cancel;
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, ticksDelay);
            return task::cancel;
        }
    }

    public static TaskWrapper runTaskTimerAsynchronously(Runnable runnable, long ticksDelay, long ticksPeriod) {
        Plugin plugin = BankPlus.INSTANCE();
        if (isFolia) {
            io.papermc.paper.threadedregions.scheduler.ScheduledTask task = Bukkit.getAsyncScheduler()
                    .runAtFixedRate(plugin, t -> runnable.run(), ticksDelay * 50, ticksPeriod * 50, TimeUnit.MILLISECONDS);
            return task::cancel;
        } else {
            BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, ticksDelay, ticksPeriod);
            return task::cancel;
        }
    }
}
