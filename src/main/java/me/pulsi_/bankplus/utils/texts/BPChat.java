package me.pulsi_.bankplus.utils.texts;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class BPChat {

    public static final String PREFIX = "<b><green>Lumina<blue>BankPlus</blue></green></b>";

    public static Component color(String message) {
        return MiniMessage.miniMessage().deserialize(message);
    }
}