package io.github.cats1337.pixelball;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import static io.github.cats1337.pixelball.Colors.colorize;

public class NetherLimit extends BukkitRunnable {

    static FileConfiguration config = Pixelball.getInstance().getConfig();

    @Override
    public void run() {

        int netherLimit = config.getInt("nether-limit");

        for (Player loopPlayer: Bukkit.getOnlinePlayers()){
            if (loopPlayer.getWorld().getName().equalsIgnoreCase("world/DIM-1")){
                int xLoc = loopPlayer.getLocation().getBlockX();
                int yLoc = loopPlayer.getLocation().getBlockY();
                int zLoc = loopPlayer.getLocation().getBlockZ();
                if ( xLoc > netherLimit){
                    Location border = new Location(loopPlayer.getWorld(), xLoc-0.5, yLoc, zLoc+0.5);
                    loopPlayer.teleport(border);
                    loopPlayer.sendMessage(colorize("&cYou are now allowed to go behind the border."));
                    loopPlayer.sendMessage(colorize("&cCurrent Border : " + netherLimit +" blocks."));
                } else if (xLoc < -netherLimit){
                    Location border = new Location(loopPlayer.getWorld(), xLoc+1.5, yLoc, zLoc+0.5);
                    loopPlayer.teleport(border);
                    loopPlayer.sendMessage(colorize("&cYou are now allowed to go behind the border."));
                    loopPlayer.sendMessage(colorize("&cCurrent Border : " + netherLimit +" blocks."));
                } else if (zLoc > netherLimit){
                    Location border = new Location(loopPlayer.getWorld(), xLoc+0.5, yLoc, zLoc-0.5);
                    loopPlayer.teleport(border);
                    loopPlayer.sendMessage(colorize("&cYou are now allowed to go behind the border."));
                    loopPlayer.sendMessage(colorize("&cCurrent Border : " + netherLimit +" blocks."));
                } else if (zLoc < -netherLimit){
                    Location border = new Location(loopPlayer.getWorld(), xLoc+0.5, yLoc, zLoc+1.5);
                    loopPlayer.teleport(border);
                    loopPlayer.sendMessage(colorize("&cYou are now allowed to go behind the border."));
                    loopPlayer.sendMessage(colorize("&cCurrent Border : " + netherLimit +" blocks."));
                }
            }
        }

    }
}
