package me.pulsi_.bankplus.managers;

import me.pulsi_.bankplus.utils.BPScheduler;
import java.util.HashMap;

public class BPTaskManager {

    public static final String INTEREST_TASK = "interest", MONEY_SAVING_TASK = "money_saving", BANKTOP_BROADCAST_TASK = "banktop_broadcast";

    private static final HashMap<String, BPScheduler.TaskWrapper> tasks = new HashMap<>();

    public static void setTask(String name, BPScheduler.TaskWrapper task) {
        String identifier = name.toLowerCase();
        if (tasks.containsKey(identifier)) tasks.get(identifier).cancel();
        tasks.put(identifier, task);
    }

    public static BPScheduler.TaskWrapper getTask(String name) {
        return tasks.get(name.toLowerCase());
    }

    public static BPScheduler.TaskWrapper removeTask(String name) {
        return tasks.remove(name.toLowerCase());
    }

    public static boolean contains(String name) {
        return tasks.containsKey(name.toLowerCase());
    }
}