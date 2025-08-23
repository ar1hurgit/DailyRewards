package Reward;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

public class Utils {

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    public static String getPlayerName(UUID uuid, JavaPlugin plugin) {
        // 1) plugin data (si tu l’enregistres)
        try {
            File dataFile = new File(plugin.getDataFolder(), "player_data.yml");
            if (dataFile.exists()) {
                YamlConfiguration cfg = YamlConfiguration.loadConfiguration(dataFile);
                String name = cfg.getString(uuid.toString() + ".name");
                if (name != null && !name.isEmpty()) return name;
            }
        } catch (Exception ignored) {}

        // 2) usercache.json du serveur (pas de Bukkit.getOfflinePlayer)
        try {
            File usercache = new File(Bukkit.getWorldContainer(), "usercache.json");
            if (!usercache.exists()) usercache = new File("usercache.json");
            if (usercache.exists()) {
                String json = Files.readString(usercache.toPath(), StandardCharsets.UTF_8);
                JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
                for (JsonElement el : arr) {
                    JsonObject o = el.getAsJsonObject();
                    String id = o.get("uuid").getAsString();
                    if (UUID.fromString(id).equals(uuid)) {
                        return o.get("name").getAsString();
                    }
                }
            }
        } catch (Exception ignored) {}

        return null; // inconnu => pas de fallback UUID pour éviter l’auto-complétion moche
    }

    public static ItemStack getPlayerHead(UUID uuid, JavaPlugin plugin) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        PlayerProfile profile = Bukkit.createProfile(uuid);
        try {
            // Paper 1.20+ : remplit nom + textures
            profile.complete(true);
        } catch (Throwable t) {
            try { profile.complete(); } catch (Throwable ignored) {}
        }
        meta.setPlayerProfile(profile);

        head.setItemMeta(meta);
        return head;
    }

    public static ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(name));
        item.setItemMeta(meta);
        return item;
    }

    public static String serializeItemStack(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    public static ItemStack deserializeItemStack(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception e) {
            return null;
        }
    }
    public static String getOrdinal(int num) {

        if (num >= 11 && num <= 13) {
            return num + "th";
        }
        switch (num % 10) {

            case 1: return num + "st";
            case 2: return num + "nd";
            case 3: return num + "rd";
            default: return num + "th";

        }

    }
}
