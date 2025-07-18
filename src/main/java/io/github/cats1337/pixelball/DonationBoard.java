package io.github.cats1337.pixelball;

import com.google.common.net.HttpHeaders;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static io.github.cats1337.pixelball.DonationBar.requestToken;

public class DonationBoard {

    private static TextDisplay display;
    private static Pixelball plugin;

    public DonationBoard(Pixelball plugin, TextDisplay display){
        DonationBoard.plugin = plugin;
        DonationBoard.display = display;
    }

    public static TextDisplay getDisplay(){
        return display;
    }

    public static void setTopTen(String id) throws IOException {
        JsonObject top10Object = getTopTen(id);
        JsonArray top10Array = top10Object.get("data").getAsJsonArray();

        List<String> donatorsList = new ArrayList<>();

        top10Array.forEach((JsonElement donatorElement) -> {
            JsonObject donator = donatorElement.getAsJsonObject();

            String donatorString = donator.get("name").getAsString();
            donatorString = donatorString + ": $" + donator.get("amount").getAsJsonObject().get("value").getAsDouble();

            donatorsList.add(donatorString);
        });
        
        while (donatorsList.size() < 10){
            donatorsList.add("No donators yet.");
        }

        MiniMessage mm = MiniMessage.miniMessage();

        Component title = mm.deserialize("<rainbow>| PIXELBALL 3 TOP 10 DONATORS |</rainbow>");

        Component mainText = Component.text("\n\n");

        int index = 1;

        for (String donators : donatorsList){
            Component placement;
            switch (index){
                case 1 -> placement = Component.text("1st. ", NamedTextColor.GOLD);
                case 2 -> placement = Component.text("2nd. ", NamedTextColor.GRAY);
                case 3 -> placement = Component.text("3rd. ", TextColor.color(205, 127, 50));
                default -> placement = Component.text(index + "th. ");
            }
            Component donorComponent = mm.deserialize("<rainbow>" + donators + "</rainbow>\n");
            mainText = mainText.append(placement).append(donorComponent);
            index++;
        }

        Component footer = Component.text("\n\n");
        footer = footer.append(mm.deserialize("<rainbow>| Total raised for Trans Lifeline: $"+ plugin.getConfig().getDouble("total_amount_raised") +" |</rainbow>"));

        Component ogDisplay = display.text();

        display.text(title.append(mainText).append(footer));
        display.setGlowing(true);
        display.setBillboard(Display.Billboard.FIXED);
        display.setAlignment(TextDisplay.TextAlignment.CENTER);
    }

    private static JsonObject getTopTen(String id) throws IOException {

        URL url = new URL("https://v5api.tiltify.com/api/public/team_campaigns/" + id + "/donor_leaderboards");
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

}
