package io.github.cats1337.pixelball;

import com.google.common.net.HttpHeaders;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.github.cats1337.pixelball.Colors.colorize;
import static io.github.cats1337.pixelball.DonationBar.requestToken;

public class GoalEvents {
    private final Pixelball plugin;
    private final Map<Double, String> donationActions = new HashMap<>();
    private final Map<Double, GoalAction> goalActions = new HashMap<>();
    static FileConfiguration config = Pixelball.getInstance().getConfig();

    public GoalEvents(@NotNull Pixelball plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    // Load donation and goal actions from the config
    private void loadConfig() {
        // Load donation actions
        ConfigurationSection donationsSection = plugin.getConfig().getConfigurationSection("donations");
        if (donationsSection != null) {
            for (String key : donationsSection.getKeys(false)) {
                double amount = Double.parseDouble(key);
                String action = donationsSection.getString(key);
                if (action != null) {
                    donationActions.put(amount, action);
                }
            }
        }

        // Load goal actions
        ConfigurationSection goalsSection = plugin.getConfig().getConfigurationSection("goals");
        if (goalsSection != null) {
            for (String key : goalsSection.getKeys(false)) {
                double amount = Double.parseDouble(key);
                String action = goalsSection.getString(key + ".action");
                boolean reached = goalsSection.getBoolean(key + ".reached", false);
                if (action != null) {
                    goalActions.put(amount, new GoalAction(action, reached));
                }
            }
        }
    }

    // Check and process donations based on amount
    public void checkDonations(final double amount) {

        Objects.requireNonNull(config.getConfigurationSection("donations")).getKeys(false).forEach(key -> {
            try {
                final JsonObject jsonObject = requestDonorsJson(config.getString("campaign-id"));
                JsonArray data = jsonObject.get("data").getAsJsonArray();
                double dono = amount;
                for (int i = 0; i < 3; i++){
                    if (dono <= 0){
                        break;
                    } else if (data.get(i).isJsonNull()){
                        break;
                    } else {
                        JsonObject donorData = data.get(i).getAsJsonObject();
                        double donorAmount = donorData.get("amount").getAsJsonObject().get("value").getAsDouble();
                        dono -= donorAmount;
                        if (donorAmount >= Double.parseDouble(key)) {
                            String action = config.getString("donations." + key + ".action");
                            String title = config.getString("donations." + key + ".title");
                            String donator = donorData.get("donor_name").getAsString();
                            String comment = "";
                            if (donorData.get("donor_comment").isJsonNull()){
                                Bukkit.broadcastMessage(colorize("&aDonation from " + donator + " of &2$" + donorAmount + "&a! &eExecuting action: " + title));

                            } else {
                                comment = donorData.get("donor_comment").getAsString();
                                Bukkit.broadcastMessage(colorize("&aDonation from " + donator + " of &2$" + donorAmount + "&a! Their comment : \"" + comment + "\" &eExecuting action: " + title));
                            }
                            // check the type of action
                            // 'give {player} pokeball 1'
                            // 'summon skeleton ~ ~ ~'
                            // 'random_effect'

                            // get the first word of the action
                            String[] actionParts = action.split(" ");
                            String actionType = actionParts[0];
                            // if actionType is 'give'
                            switch (actionType) {
                                case "spawnpokemon":
                                    // spawnpokemon shiny random
                                    String rarity = actionParts[1];
                                    String pokemon = actionParts[2];
                                    for (Player p : Bukkit.getOnlinePlayers()) {
                                        int xLoc = p.getLocation().getBlockX();
                                        int yLoc = p.getLocation().getBlockY()+1;
                                        int zLoc = p.getLocation().getBlockZ();
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawnpokemonat " + xLoc + " " + yLoc + " " + zLoc + " " + pokemon + " " + rarity);
                                    }
                                    break;
                                case "give":
                                    // give the player the item

                                    for (Player p : Bukkit.getOnlinePlayers()) {
                                        String formattedAction = action.replace("player", p.getName());
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedAction);
                                    }
                                    break;
                                case "summon":
                                    // summon the entity
                                    for (Player p : Bukkit.getOnlinePlayers()) {
                                        String entityName = actionParts[1];
                                        EntityType entityType = EntityType.valueOf(entityName.toUpperCase());

                                        // summon the entity at a random location near player's location
                                        Location spawnLocation = p.getLocation().add(Math.random() * 6 - 3, 0, Math.random() * 6 - 3);
                                        Entity entity = p.getWorld().spawnEntity(spawnLocation, entityType);
                                        if (entity instanceof LivingEntity) {
                                            ((LivingEntity) entity).setAI(true);
                                        }
                                    }
                                    break;

                                case "random_stone":
                                    // list of stones
                                    String[] stones = {"fire_stone", "water_stone", "thunder_stone", "leaf_stone", "moon_stone", "sun_stone", "shiny_stone", "dusk_stone", "dawn_stone", "ice_stone", "oval_stone"};

                                    for (Player p : Bukkit.getOnlinePlayers()) {
                                        String stone = stones[(int) (Math.random() * stones.length)];
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give " + p.getName() + " cobblemon:" + stone + " 1");
                                    }
                                    break;

                                case "random_held_item":
                                    // list of held items
                                    String[] heldItems = {"ability_shield", "absorb_bulb", "air_balloon", "assault_vest",
                                            "big_root", "binding_band", "black_sludge", "blunder_policy", "bright_powder", "black_belt", "black_glasses",
                                            "cell_battery", "choice_band", "choice_scarf", "choice_specs", "cleanse_tag", "covert_cloak", "charcoal_stick",
                                            "damp_rock", "deep_sea_scale", "deep_sea_tooth", "destiny_knot", "dragon_fang",
                                            "eject_button", "eject_pack", "everstone", "eviolite", "expert_belt", "exp_share",
                                            "flame_orb", "float_stone", "focus_band", "focus_sash", "fairy_feather",
                                            "heat_rock", "hard_stone",
                                            "icy_rock", "iron_ball",
                                            "kings_rock",
                                            "leftovers", "life_orb", "light_ball", "light_clay", "loaded_dice", "lucky_egg",
                                            "medicinal_leek", "mental_herb", "metal_powder", "metronome", "mirror_herb", "muscle_band", "magnet", "metal_coat", "miracle_seed", "mystic_water",
                                            "never_melt_ice",
                                            "power_herb", "punching_glove", "protective_pads", "poison_barb", "power_anklet", "power_band", "power_belt", "power_bracer", "power_lens", "power_weight",
                                            "quick_claw", "quick_powder",
                                            "razor_claw", "razor_fang", "red_card", "ring_target", "rocky_helmet", "room_service",
                                            "safety_goggles", "scope_lens", "shed_shell", "shell_bell", "smoke_ball", "smooth_rock", "soothe_bell", "sticky_barb", "sharp_beak", "silk_scarf", "silver_powder", "soft_sand", "spell_tag",
                                            "terrain_extender", "throat_spray", "toxic_orb", "twisted_spoon",
                                            "utility_umbrella",
                                            "weakness_policy", "white_herb", "wide_lens", "wise_glasses",
                                            "zoom_lens"};

                                    for (Player p : Bukkit.getOnlinePlayers()) {
                                        String heldItem = heldItems[(int) (Math.random() * heldItems.length)];
                                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "give " + p.getName() + " cobblemon:" + heldItem + " 1");
                                    }
                                    break;

                                case "random_effect":
                                    int duration = Integer.parseInt(actionParts[1]);
                                    // random effect
                                    for (Player p : Bukkit.getOnlinePlayers()) {
                                        PotionEffect effect = new PotionEffect(PotionEffectType.values()[(int) (Math.random() * PotionEffectType.values().length)], duration, 0); // random effect, lvl 0 aka lvl 1
                                        // give the player a random effect
                                        p.addPotionEffect(effect);
                                    }
                                    break;
                                default:
                                    Bukkit.broadcastMessage(colorize("&cInvalid action type: " + actionType));
                                    break;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // Check and process goals based on total raised amount
    public void checkGoals(double totalRaised) {
        for (Map.Entry<Double, GoalAction> entry : goalActions.entrySet()) {
            if (totalRaised >= entry.getKey() && !entry.getValue().isReached()) {
                String action = entry.getValue().getAction();
                Bukkit.broadcastMessage(colorize("&aGoal of $" + entry.getKey() + " reached! Executing action: " + action));
                entry.getValue().setReached(true);

                // Update the config to mark the goal as reached
                updateGoalInConfig(entry.getKey(), true);

                // execute the action
//                action: "spawnpokemon shiny random"
//                action: "spawnpokemon shiny random"
//                action: "enablenether"
//                action: "spawnpokemon shiny random"
//                action: "legendaryspawn 3"

                String[] actionParts = action.split(" ");
                String actionType = actionParts[0];
                switch (actionType) {
                    case "spawnpokemon":
                        // spawnpokemon shiny random
                        String rarity = actionParts[1];
                        String pokemon = actionParts[2];
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            int xLoc = p.getLocation().getBlockX();
                            int yLoc = p.getLocation().getBlockY()+1;
                            int zLoc = p.getLocation().getBlockZ();
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawnpokemonat " + xLoc + " " + yLoc + " " + zLoc + " " + pokemon + " " + rarity);
                        }
                        break;
                    case "enablenether":
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "say Nether Enabled!");
                        break;
                    case "legendaryspawn":
                        int amount = Integer.parseInt(actionParts[1]);

                        String[] legendaries = {"articuno lvl=55", "moltres lvl=55", "zapdos lvl=55", "mewtwo lvl=70", "xerneas lvl=70", "ironleaves lvl=100"};

                        // spawn amount of legendaries
                        for (int i = 0; i < amount; i++) {
//                            random location
                            int randomX = (int) (Math.random() * 1000 - 500);
                            int randomZ = (int) (Math.random() * 1000 - 500);
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spawnpokemonat " + randomX + " 100 " + randomZ + " " + legendaries[i]);
                            Bukkit.broadcastMessage(colorize("&6Legendary  " + legendaries[i] + " spawned! "));
                        }

                        break;
                    default:
                        Bukkit.broadcastMessage(colorize("&cInvalid action type: " + actionType));
                        break;
                }

            }
        }
    }

    // Update the goal's "reached" status in the config file
    private void updateGoalInConfig(double amount, boolean reached) {
        ConfigurationSection goalsSection = plugin.getConfig().getConfigurationSection("goals");
        if (goalsSection != null) {
            for (String key : goalsSection.getKeys(false)) {
                if (Double.parseDouble(key) == amount) {
                    goalsSection.set(key + ".reached", reached);
                    plugin.saveConfig();
                    break;
                }
            }
        }
    }

    // Represents a goal action with its reached status
    private static class GoalAction {
        private final String action;
        private boolean reached;

        public GoalAction(String action, boolean reached) {
            this.action = action;
            this.reached = reached;
        }

        public String getAction() {
            return action;
        }

        public boolean isReached() {
            return reached;
        }

        public void setReached(boolean reached) {
            this.reached = reached;
        }
    }

    private JsonObject requestDonorsJson(String id) throws IOException {
        URL url = new URL("https://v5api.tiltify.com/api/public/team_campaigns/" + id + "/donations?limit=3");
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
