package io.github.cats1337.pixelball;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Text;

import javax.naming.Name;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;

import static io.github.cats1337.pixelball.Colors.colorize;
import static io.github.cats1337.pixelball.DonationBar.requestJson;

public final class Pixelball extends JavaPlugin {

    private DonationBar donationBar;
    private DonationBoard donationBoard;
    private final Map<Player, Player> tpa = new HashMap<>();

    public static Pixelball getInstance() {
        return Pixelball.getPlugin(Pixelball.class);
    }

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        TextDisplay display = null;

        for (Entity entity : Objects.requireNonNull(Bukkit.getWorld("world")).getEntities()){
            if (entity instanceof TextDisplay textDisplay){
                display = textDisplay;
                break;
            }
        }

        donationBoard = new DonationBoard(this, display);

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
        else if (label.equalsIgnoreCase("tpa")){
            if (sender instanceof Player playerSender){
                if (args.length != 1){
                    sender.sendMessage(Component.text("Please only enter a username.", NamedTextColor.RED));
                } else {
                    Player player = Bukkit.getPlayer(args[0]);
                    if (player == null){
                        sender.sendMessage(Component.text("Please enter a valid username.", NamedTextColor.RED));
                    } else {
                        Component startText = Component.text(sender.getName() + " wants to teleport to you. Click ");

                        Component teleportYes = Component.text("here or type /tpaccept to accept", NamedTextColor.GREEN, TextDecoration.BOLD);
                        teleportYes = teleportYes.clickEvent(ClickEvent.runCommand("/tpaccept"));

                        Component or = Component.text(" or ", NamedTextColor.WHITE);

                        Component teleportNo = Component.text("here or type /tprefuse to refuse.", NamedTextColor.RED, TextDecoration.BOLD);
                        teleportNo = teleportNo.clickEvent(ClickEvent.runCommand("/tprefuse"));

                        tpa.put(player, playerSender);

                        player.sendMessage(startText.append(teleportYes).append(or).append(teleportNo));
                        sender.sendMessage(Component.text("Sent request to " + args[0] + "."));
                    }
                }
            } else {
                sender.sendMessage(Component.text("You need to be a player to execute this command.", NamedTextColor.RED));
            }
        }
        else if (label.equalsIgnoreCase("tpaccept")){
            if (sender instanceof Player playerSender){
                if (!Objects.equals(tpa.get(playerSender), null)){
                    Player player = tpa.get(playerSender);

                    playerSender.teleport(player);

                    player.sendMessage(Component.text(sender.getName() + " teleported to you.", NamedTextColor.GREEN));
                    sender.sendMessage(Component.text("You got teleported to " + player.getName() + ".", NamedTextColor.GREEN));

                    tpa.put(playerSender, null);
                } else {
                    sender.sendMessage(Component.text("You have no ongoing tpa requests.", NamedTextColor.RED));
                }
            } else {
                sender.sendMessage(Component.text("You need to be a player to execute this command.", NamedTextColor.RED));
            }
        }
        else if (label.equalsIgnoreCase("tprefuse")){
            if (sender instanceof Player playerSender){
                if (!Objects.equals(tpa.get(playerSender), null)){
                    Player player = tpa.get(playerSender);

                    player.sendMessage(Component.text("You refused " + sender.getName() + "'s teleportation request.", NamedTextColor.RED));
                    sender.sendMessage(Component.text(player.getName() + " refused your teleportation request.", NamedTextColor.RED));

                    tpa.put(playerSender, null);
                } else {
                    sender.sendMessage(Component.text("You have no ongoing tpa requests.", NamedTextColor.RED));
                }
            } else {
                sender.sendMessage(Component.text("You need to be a player to execute this command.", NamedTextColor.RED));
            }
        }
        return false;
    }

    public DonationBoard getDonationBoard(){
        return donationBoard;
    }

}
