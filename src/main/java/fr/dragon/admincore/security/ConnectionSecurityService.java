package fr.dragon.admincore.security;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.util.ConfigLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
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
import java.util.concurrent.TimeUnit;

public class ConnectionSecurityService implements Listener {

    private final AdminCorePlugin plugin;
    private final ConfigLoader configLoader;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    
    private final Map<String, VpnCheckResult> vpnCache = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, Long> playerConnections = new ConcurrentHashMap<>();
    
    public ConnectionSecurityService(AdminCorePlugin plugin, ConfigLoader configLoader) {
        this.plugin = plugin;
        this.configLoader = configLoader;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        if (isAutoCheckEnabled()) {
            plugin.getLogger().info("ConnectionSecurity: Auto VPN check enabled on join");
        }
    }
    
    private boolean isAutoCheckEnabled() {
        return configLoader.config().getBoolean("security.vpn-check.auto-on-join", false);
    }
    
    private boolean isVpnCheckEnabled() {
        return configLoader.config().getBoolean("security.vpn-check.enabled", false);
    }
    
    private String getVpnApiUrl() {
        return configLoader.config().getString("security.vpn-check.api-url", "");
    }
    
    private String getVpnMatchPattern() {
        return configLoader.config().getString("security.vpn-check.vpn-detected-pattern", "\"proxy\":true");
    }
    
    private int getVpnTimeout() {
        return configLoader.config().getInt("security.vpn-check.timeout-seconds", 5);
    }
    
    private boolean shouldKickOnVpn() {
        return configLoader.config().getBoolean("security.vpn-check.kick-on-vpn", false);
    }
    
    private boolean shouldNotifyStaff() {
        return configLoader.config().getBoolean("security.vpn-check.notify-staff", true);
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        playerConnections.put(player.getUniqueId(), System.currentTimeMillis());
        
        if (!isVpnCheckEnabled() || !isAutoCheckEnabled()) {
            return;
        }
        
        InetAddress address = player.getAddress() != null ? player.getAddress().getAddress() : null;
        if (address == null) {
            return;
        }
        
        String ip = address.getHostAddress();
        
        if (vpnCache.containsKey(ip)) {
            VpnCheckResult cached = vpnCache.get(ip);
            if (cached.isVpn() && shouldKickOnVpn()) {
                player.kick(Component.text("VPN/Proxy detecte. Veuillez desactiver votre VPN pour rejoindre.").color(NamedTextColor.RED));
            }
            if (cached.isVpn() && shouldNotifyStaff()) {
                notifyStaffVpn(player, ip);
            }
            return;
        }
        
        scheduler.submit(() -> checkVpnAndAction(player, ip));
    }
    
    private void checkVpnAndAction(Player player, String ip) {
        checkVpn(ip).thenAccept(result -> {
            vpnCache.put(ip, result);
            
            if (result.isVpn()) {
                if (shouldKickOnVpn() && player.isOnline()) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.kick(Component.text("VPN/Proxy detecte. Veuillez desactiver votre VPN pour rejoindre.").color(NamedTextColor.RED));
                    });
                }
                
                if (shouldNotifyStaff()) {
                    notifyStaffVpn(player, ip);
                }
            }
        });
    }
    
    private void notifyStaffVpn(Player player, String ip) {
        String message = configLoader.config().getString("security.vpn-check.staff-notify-message", 
            "<red>[VPN]</red> <gold>{player}</gold> <gray>a rejoint avec un VPN/Proxy (<white>{ip}</white>)");
        message = message.replace("{player}", player.getName()).replace("{ip}", ip);
        
        Component component = miniMessage.deserialize(message);
        
        Bukkit.getOnlinePlayers().stream()
            .filter(p -> p.hasPermission("admincore.checkvpn"))
            .forEach(p -> p.sendMessage(component));
    }
    
    public void checkPlayerVpn(Player player, CommandSender sender) {
        if (!isVpnCheckEnabled()) {
            sender.sendMessage(Component.text("Verification VPN desactivee.").color(NamedTextColor.RED));
            return;
        }
        
        InetAddress address = player.getAddress() != null ? player.getAddress().getAddress() : null;
        if (address == null) {
            sender.sendMessage(Component.text("Impossible de recuperer l'IP du joueur.").color(NamedTextColor.RED));
            return;
        }
        
        String ip = address.getHostAddress();
        
        if (vpnCache.containsKey(ip)) {
            VpnCheckResult cached = vpnCache.get(ip);
            sendResult(sender, player.getName(), ip, cached);
            return;
        }
        
        sender.sendMessage(Component.text("Verification en cours...").color(NamedTextColor.YELLOW));
        
        checkVpn(ip).thenAccept(result -> {
            vpnCache.put(ip, result);
            Bukkit.getScheduler().runTask(plugin, () -> sendResult(sender, player.getName(), ip, result));
        });
    }
    
    private void sendResult(CommandSender sender, String playerName, String ip, VpnCheckResult result) {
        Component message;
        if (result.isVpn()) {
            message = Component.text("⚠️ ")
                .color(NamedTextColor.RED)
                .append(Component.text(playerName + " (" + ip + ")").color(NamedTextColor.WHITE))
                .append(Component.text(" - VPN/Proxy detecte!").color(NamedTextColor.RED));
        } else {
            message = Component.text("✅ ")
                .color(NamedTextColor.GREEN)
                .append(Component.text(playerName + " (" + ip + ")").color(NamedTextColor.WHITE))
                .append(Component.text(" - IP legitime").color(NamedTextColor.GREEN));
        }
        sender.sendMessage(message);
    }
    
    private CompletableFuture<VpnCheckResult> checkVpn(String ip) {
        String apiUrl = getVpnApiUrl();
        
        if (apiUrl.isBlank()) {
            return CompletableFuture.completedFuture(new VpnCheckResult(false, "API non configuree"));
        }
        
        String url = apiUrl.replace("{ip}", ip);
        
        try {
            URI uri = URI.create(url);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(getVpnTimeout()))
                .GET()
                .build();
            
            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    String body = response.body();
                    boolean isVpn = body.contains(getVpnMatchPattern());
                    return new VpnCheckResult(isVpn, body);
                })
                .exceptionally(ex -> {
                    plugin.getLogger().warning("VPN check failed for " + ip + ": " + ex.getMessage());
                    return new VpnCheckResult(false, "Erreur: " + ex.getMessage());
                });
        } catch (Exception e) {
            return CompletableFuture.completedFuture(new VpnCheckResult(false, "URL invalide"));
        }
    }
    
    public void clearCache() {
        vpnCache.clear();
        plugin.getLogger().info("VPN cache cleared");
    }
    
    public Map<String, VpnCheckResult> getVpnCache() {
        return Map.copyOf(vpnCache);
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
    
    public record VpnCheckResult(boolean isVpn, String details) {}
}