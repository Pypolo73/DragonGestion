package fr.dragon.admincore.geo;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.util.ConfigLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class GeoIPService {

    private final AdminCorePlugin plugin;
    private final ConfigLoader configLoader;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    
    private final Map<String, GeoInfo> geoCache = new ConcurrentHashMap<>();
    
    public GeoIPService(AdminCorePlugin plugin, ConfigLoader configLoader) {
        this.plugin = plugin;
        this.configLoader = configLoader;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        plugin.getLogger().info("GeoIP Service initialise");
    }
    
    private String getApiUrl() {
        return configLoader.config().getString("geoip.api-url", "https://ip-api.com/json/{ip}?fields=status,country,region,city,isp,as,query");
    }
    
    private int getTimeout() {
        return configLoader.config().getInt("geoip.timeout-seconds", 5);
    }
    
    private boolean isEnabled() {
        return configLoader.config().getBoolean("geoip.enabled", true);
    }
    
    public void getPlayerGeoInfo(Player player, CommandSender sender) {
        if (!isEnabled()) {
            sender.sendMessage(Component.text("Service GeoIP desactive.").color(NamedTextColor.RED));
            return;
        }
        
        if (player.getAddress() == null || player.getAddress().getAddress() == null) {
            sender.sendMessage(Component.text("Impossible de recuperer l'IP du joueur.").color(NamedTextColor.RED));
            return;
        }
        
        String ip = player.getAddress().getAddress().getHostAddress();
        
        if (geoCache.containsKey(ip)) {
            GeoInfo cached = geoCache.get(ip);
            sendGeoInfo(sender, player.getName(), cached);
            return;
        }
        
        sender.sendMessage(Component.text("Consultation des informations geographiques...").color(NamedTextColor.YELLOW));
        
        lookupGeo(ip).thenAccept(result -> {
            if (result != null) {
                geoCache.put(ip, result);
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (result != null) {
                    sendGeoInfo(sender, player.getName(), result);
                } else {
                    sender.sendMessage(Component.text("Echec de la consultation geographique.").color(NamedTextColor.RED));
                }
            });
        });
    }
    
    private void sendGeoInfo(CommandSender sender, String playerName, GeoInfo info) {
        sender.sendMessage(Component.text("═══════════════════════════════════").color(NamedTextColor.GRAY));
        sender.sendMessage(Component.text("        🌍 Info Geographique        ").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("═══════════════════════════════════").color(NamedTextColor.GRAY));
        
        sender.sendMessage(Component.text("Joueur: ").color(NamedTextColor.AQUA).append(Component.text(playerName).color(NamedTextColor.WHITE)));
        sender.sendMessage(Component.text("IP: ").color(NamedTextColor.AQUA).append(Component.text(info.ip()).color(NamedTextColor.WHITE)));
        
        if (info.status().equals("success")) {
            sender.sendMessage(Component.text("Pays: ").color(NamedTextColor.GREEN).append(Component.text(info.country()).color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Region: ").color(NamedTextColor.GREEN).append(Component.text(info.region()).color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("Ville: ").color(NamedTextColor.GREEN).append(Component.text(info.city()).color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("ISP: ").color(NamedTextColor.YELLOW).append(Component.text(info.isp()).color(NamedTextColor.WHITE)));
            sender.sendMessage(Component.text("AS: ").color(NamedTextColor.YELLOW).append(Component.text(info.as()).color(NamedTextColor.WHITE)));
        } else {
            sender.sendMessage(Component.text("Erreur: ").color(NamedTextColor.RED).append(Component.text(info.message() != null ? info.message() : "Inconnu").color(NamedTextColor.WHITE)));
        }
        
        sender.sendMessage(Component.text("═══════════════════════════════════").color(NamedTextColor.GRAY));
    }
    
    public void checkPlayerGeoFromName(String playerName, CommandSender sender) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player != null) {
            getPlayerGeoInfo(player, sender);
        } else {
            sender.sendMessage(Component.text("Joueur non connecte. Utilisation de /network info <joueur> pour les infos reseau.").color(NamedTextColor.YELLOW));
        }
    }
    
    private CompletableFuture<GeoInfo> lookupGeo(String ip) {
        String apiUrl = getApiUrl();
        
        if (apiUrl.isBlank()) {
            return CompletableFuture.completedFuture(new GeoInfo(ip, "fail", "API non configuree", "", "", "", "", ""));
        }
        
        String url = apiUrl.replace("{ip}", ip);
        
        try {
            URI uri = URI.create(url);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(getTimeout()))
                .GET()
                .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    String body = response.body();
                    return parseGeoResponse(body, ip);
                })
                .exceptionally(ex -> {
                    plugin.getLogger().warning("GeoIP lookup failed for " + ip + ": " + ex.getMessage());
                    return new GeoInfo(ip, "fail", ex.getMessage(), "", "", "", "", "");
                });
        } catch (Exception e) {
            return CompletableFuture.completedFuture(new GeoInfo(ip, "fail", "URL invalide", "", "", "", "", ""));
        }
    }
    
    private GeoInfo parseGeoResponse(String body, String ip) {
        try {
            String status = extractJsonValue(body, "status");
            String country = extractJsonValue(body, "country");
            String region = extractJsonValue(body, "regionName");
            String city = extractJsonValue(body, "city");
            String isp = extractJsonValue(body, "isp");
            String as = extractJsonValue(body, "as");
            
            return new GeoInfo(ip, status, "", country, region, city, isp, as);
        } catch (Exception e) {
            return new GeoInfo(ip, "fail", "Parse error", "", "", "", "", "");
        }
    }
    
    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return "";
        
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return "";
        
        int start = colonIndex + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\"')) {
            start++;
        }
        
        int end = start;
        boolean inQuotes = json.charAt(start) == '\"';
        if (inQuotes) {
            end = json.indexOf("\"", start + 1);
            if (end == -1) end = json.length();
            return json.substring(start + 1, end);
        } else {
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
                end++;
            }
            return json.substring(start, end).trim();
        }
    }
    
    public void clearCache() {
        geoCache.clear();
        plugin.getLogger().info("GeoIP cache cleared");
    }
    
    public Map<String, GeoInfo> getGeoCache() {
        return Map.copyOf(geoCache);
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
    
    public record GeoInfo(
        String ip,
        String status,
        String message,
        String country,
        String region,
        String city,
        String isp,
        String as
    ) {}
}