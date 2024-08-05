package io.github.cats1337.pixelball;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

import static io.github.cats1337.pixelball.Colors.colorize;
import static io.github.cats1337.pixelball.DonationBar.requestJson;

public final class Pixelball extends JavaPlugin {

    private DonationBar donationBar;

    public static Pixelball getInstance() {
        return Pixelball.getPlugin(Pixelball.class);
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.createBossBar(); // create the boss bar before registering the event listener

        this.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                if (!Pixelball.this.donationBar.getBossBar().getPlayers().contains(event.getPlayer())) {
                    Pixelball.this.donationBar.addPlayer(event.getPlayer());
                }
            }
        }, this);

        BukkitTask netherLimit = new NetherLimit().runTaskTimer(this, 0L, 1L);

    }

    @Override
    public void onDisable() {
        if (this.donationBar == null) {
            return; // if donation bar is null, then it hasn't been created yet
        }
        this.donationBar.getBossBar().removeAll();
    }

    public void createBossBar() {
        this.reloadConfig();

        if (this.donationBar == null) { // if null then create a new instance
            this.donationBar = new DonationBar();
        }

        // remove all players from the boss bar and cancel the task
        BossBar bossBar = this.donationBar.getBossBar();
        if (bossBar != null) bossBar.removeAll(); // if the boss bar is null, then it hasn't been created yet
        this.donationBar.attemptToCancel();

        // get the config values
        String id = this.getConfig().getString("campaign-id");
        String mainTitleColor = this.getConfig().getString("main-title-color");
        String mainBarColor = this.getConfig().getString("main-bar-color");
        String goalMsg = this.getConfig().getString("goal-message");
        String goalTitleColor = this.getConfig().getString("goal-title-color");
        String goalBarColor = this.getConfig().getString("goal-bar-color");

        // create the boss bar
        this.donationBar = new DonationBar();
        this.donationBar.createBar(id, mainTitleColor, mainBarColor, goalMsg, goalTitleColor, goalBarColor);

        // add all online players to the boss bar
        Bukkit.getOnlinePlayers().forEach(this.donationBar::addPlayer);
    }

    /**
     * The donation boss bar command.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, String label, String[] args) {
        if (label.equalsIgnoreCase("donationbb")) {
            if (!sender.hasPermission("donationbb")) {
                sender.sendMessage(ChatColor.RED + "I'm sorry, but you do not have permission to perform this command" +
                        ". Please contact the server administrators if you believe that this is in error.");
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(colorize("&2[&aDonation Bossbar&2]\n&aVersion:&7 1.0\n&aDeveloper:&7 awesomepandapig\n&aCommands:&7 /donationbb reload"));
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                sender.sendMessage(colorize("&aReloaded Donation Bossbar&r"));
                this.createBossBar();
            }

            if (args[0].equalsIgnoreCase("raised")) {
                if (args[1].equalsIgnoreCase("set")) {
                    this.getConfig().set("total_amount_raised", Double.parseDouble(args[2]));
                    String mainTitleColor = this.getConfig().getString("main-title-color");
                    String id = this.getConfig().getString("campaign-id");
                    try {
                        final JsonObject jsonObject = requestJson(id);
                        JsonObject data = jsonObject.get("data").getAsJsonObject();
                        double fundraiserGoalAmount = data.get("goal").getAsJsonObject().get("value").getAsDouble();
                        this.donationBar.getBossBar().setTitle(colorize(mainTitleColor + "Raised $" + NumberFormat.getInstance(Locale.US).format(Double.parseDouble(args[2])) + " of $" + NumberFormat.getInstance(Locale.US).format(fundraiserGoalAmount)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return false;
    }

}
