package io.github.cats1337.pixelball;

import org.bukkit.ChatColor;

public final class Colors {
    private Colors() {

    }

    public static String colorize(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}