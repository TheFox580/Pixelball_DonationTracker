package io.github.cats1337.pixelball;

import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.Locale;

import static io.github.cats1337.pixelball.Colors.colorize;

public final class DonationBar {
    private BossBar bar;
    private BukkitTask bukkitTask;
    private final GoalEvents goalEvents;
    static FileConfiguration config = Pixelball.getInstance().getConfig();

    public DonationBar() {
        this.goalEvents = new GoalEvents(Pixelball.getInstance());
    }

    public void createBar(String id, String mainTitleColor, String mainBarColor, String goalMsg, String goalTitleColor, String goalBarColor) {
        this.bar = Bukkit.createBossBar(colorize("&cWelcome to Donation Bossbar"), BarColor.PINK, BarStyle.SOLID);
        this.bar.setVisible(true);

        this.bukkitTask = Bukkit.getScheduler().runTaskTimerAsynchronously(Pixelball.getInstance(), () -> {
            try {
                final JsonObject jsonObject = requestJson(id);
                Bukkit.getScheduler().runTask(Pixelball.getInstance(), () -> {
                    JsonObject data = jsonObject.get("data").getAsJsonObject();
                    double totalAmountRaised = data.get("amount_raised").getAsJsonObject().get("value").getAsDouble();
                    double fundraiserGoalAmount = data.get("goal").getAsJsonObject().get("value").getAsDouble();
                    double progress = totalAmountRaised / fundraiserGoalAmount;
                    if (progress >= 1) {
                        // the goal has been reached
                        this.bar.setProgress(1);
                        this.bar.setColor(BarColor.valueOf(goalBarColor));
                        this.bar.setTitle(colorize(goalTitleColor + goalMsg));
                    } else {
                        this.bar.setProgress(progress);
                        this.bar.setColor(BarColor.valueOf(mainBarColor));
                        this.bar.setTitle(colorize(mainTitleColor + "Raised $" + this.formatNumber(totalAmountRaised) + " of $" + this.formatNumber(fundraiserGoalAmount)));
                    }

                    // Check goals based on the donations that were done
                    if (config.getDouble("total_amount_raised") < totalAmountRaised){
                        double diff = totalAmountRaised - config.getDouble("total_amount_raised");
                        config.set("total_amount_raised", totalAmountRaised);
                        goalEvents.checkGoals(diff);
                        goalEvents.checkDonations(diff);
                    }
                    /*try {
                        DonationBoard.setTopTen(id);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }*/
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0L, 20 * 30L);
    }

    public static JsonElement requestToken() throws IOException {
        URL obj = new URL("https://v5api.tiltify.com/oauth/token");
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");

        // For POST only - START
        con.setDoOutput(true);
        OutputStream os = con.getOutputStream();

        String client_id = config.getString("client-id");
        String client_secret = config.getString("client-secret");
        String jsonInputString = "{\"grant_type\":\"client_credentials\",\"client_id\":\"" + client_id + "\",\"client_secret\":\"" + client_secret + "\",\"scope\":\"public\"}";

        byte[] out = jsonInputString.getBytes(StandardCharsets.UTF_8);

        os.write(out);
        os.flush();
        os.close();
        // For POST only - END

        JsonElement data = null;
        int responseCode = con.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) { //success
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                data = JsonParser.parseReader(reader);
            }
            if (data == null) throw new RuntimeException("Could not get the response, the data is null.");
            if (data.isJsonObject()) return data.getAsJsonObject();
            throw new RuntimeException("Invalid response!");


        }
        return data;
    }

    public static JsonObject requestJson(String id) throws IOException {
        URL url = new URL("https://v5api.tiltify.com/api/public/team_campaigns/" + id);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        String token = requestToken().getAsJsonObject().get("access_token").getAsString();
        conn.setRequestProperty(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestMethod("GET");

        JsonElement data;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            data = JsonParser.parseReader(reader);
        }
        if (data == null) throw new RuntimeException("Could not get the response, the data is null.");
        if (data.isJsonObject()) return data.getAsJsonObject();
        throw new RuntimeException("Invalid response!");
    }

    private String formatNumber(double number) {
        return NumberFormat.getInstance(Locale.US).format(number);
    }

    public void addPlayer(Player player) {
        this.bar.addPlayer(player);
    }

    public @Nullable BossBar getBossBar() {
        return this.bar;
    }

    public void attemptToCancel() {
        if (this.bukkitTask == null) return;
        if (this.bukkitTask.isCancelled()) return;
        this.bukkitTask.cancel();
    }

}
