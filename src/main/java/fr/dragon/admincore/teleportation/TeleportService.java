package fr.dragon.admincore.teleportation;

import fr.dragon.admincore.core.AdminCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportService {
    private final AdminCorePlugin plugin;
    private final TeleportDatabase database;
    private final TeleportConfig config;
    private final TeleportLogger logger;
    private final Map<UUID, TeleportTask> activeTeleports = new ConcurrentHashMap<>();
    private final Map<UUID, Long> rtpCooldowns = new ConcurrentHashMap<>();
    private final Set<UUID> tpaIgnore = ConcurrentHashMap.newKeySet();

    public TeleportService(AdminCorePlugin plugin, TeleportDatabase database, TeleportConfig config) {
        this.plugin = plugin;
        this.database = database;
        this.config = config;
        this.logger = new TeleportLogger(plugin);
        
        Bukkit.getScheduler().runTaskTimer(plugin, () -> database.deleteExpiredRequests(), 20 * 60, 20 * 60);
    }

    public TeleportLogger getLogger() {
        return logger;
    }

    // Home methods
    public boolean setHome(Player player, String name) {
        
        if (name == null || name.isEmpty()) {
            name = config.home().defaultName();
        }
        
        int currentHomes = database.getHomeCount(player.getUniqueId());
        int maxHomes = getMaxHomes(player);
        
        if (currentHomes >= maxHomes && database.getHome(player.getUniqueId(), name) == null) {
            sendMessage(player, Component.text("Vous avez atteint la limite de " + maxHomes + " homes.", NamedTextColor.RED));
            return false;
        }
        
        TeleportData home = TeleportData.home(player.getUniqueId(), name, player.getLocation());
        database.saveHome(home);
        sendMessage(player, Component.text("Home ", NamedTextColor.GREEN).append(Component.text(name, NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)).append(Component.text(" cree avec succes !", NamedTextColor.GREEN)));
        return true;
    }

    public boolean teleportToHome(Player player, String name) {
        TeleportData home = database.getHome(player.getUniqueId(), name);
        if (home == null) {
            sendMessage(player, Component.text("Home ", NamedTextColor.RED).append(Component.text(name, NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)).append(Component.text(" introuvable.", NamedTextColor.RED)));
            return false;
        }
        return teleportPlayer(player, home.toLocation(plugin.getServer()), config.home().warmup());
    }

    public boolean deleteHome(Player player, String name) {
        if (database.getHome(player.getUniqueId(), name) == null) {
            sendMessage(player, Component.text("Home introuvable.", NamedTextColor.RED));
            return false;
        }
        database.deleteHome(player.getUniqueId(), name);
        sendMessage(player, Component.text("Home ", NamedTextColor.GREEN).append(Component.text(name, NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)).append(Component.text(" supprime.", NamedTextColor.GREEN)));
        return true;
    }

    public List<TeleportData> getHomes(Player player) {
        return database.getHomes(player.getUniqueId());
    }

    public int getMaxHomes(Player player) {
        int max = config.home().maxHomes();
        if (player.hasPermission("admincore.teleport.homes.bonus")) {
            max += 5;
        }
        if (player.hasPermission("admincore.teleport.homes.unlimited")) {
            max = Integer.MAX_VALUE;
        }
        return max;
    }

    // Warp methods
    public boolean setWarp(Player player, String name, String description) {
        TeleportData warp = TeleportData.warp(name, player.getLocation(), description != null ? description : "");
        database.saveWarp(warp);
        sendMessage(player, Component.text("Warp ", NamedTextColor.GOLD).append(Component.text(name, NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)).append(Component.text(" cree avec succes !", NamedTextColor.GREEN)));
        return true;
    }

    public boolean teleportToWarp(Player player, String name) {
        TeleportData warp = database.getWarp(name);
        if (warp == null) {
            sendMessage(player, Component.text("Warp ", NamedTextColor.RED).append(Component.text(name, NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)).append(Component.text(" introuvable.", NamedTextColor.RED)));
            return false;
        }
        
        if (!player.hasPermission("admincore.teleport.warp.*") && 
            !player.hasPermission("admincore.teleport.warp." + name.toLowerCase())) {
            sendMessage(player, Component.text("Vous n'avez pas la permission d'utiliser ce warp.", NamedTextColor.RED));
            return false;
        }
        
        return teleportPlayer(player, warp.toLocation(plugin.getServer()), config.warp().warmup());
    }

    public boolean deleteWarp(Player player, String name) {
        if (database.getWarp(name) == null) {
            sendMessage(player, Component.text("Warp introuvable.", NamedTextColor.RED));
            return false;
        }
        database.deleteWarp(name);
        sendMessage(player, Component.text("Warp ", NamedTextColor.GREEN).append(Component.text(name, NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)).append(Component.text(" supprime.", NamedTextColor.GREEN)));
        return true;
    }

    public List<TeleportData> getWarps() {
        return database.getWarps();
    }

    // Spawn methods
    public boolean setSpawn(Player player) {
        database.saveSpawn(player.getLocation());
        sendMessage(player, Component.text("Position du spawn deplacee.", NamedTextColor.GREEN));
        return true;
    }

    public boolean teleportToSpawn(Player player) {
        Location spawn = database.getSpawn();
        if (spawn == null) {
            sendMessage(player, Component.text("Le spawn n'a pas ete defini.", NamedTextColor.RED));
            return false;
        }
        return teleportPlayer(player, spawn, config.spawn().warmup());
    }

    public Location getSpawn() {
        return database.getSpawn();
    }

    // RTP methods
    public boolean startRtp(Player player) {
        if (!config.rtp().enabled()) {
            sendMessage(player, Component.text("Le RTP est desactive.", NamedTextColor.RED));
            return false;
        }
        
        String worldName = player.getWorld().getName();
        
        if (!config.rtp().allowedWorlds().isEmpty() && !config.rtp().allowedWorlds().contains(worldName)) {
            sendMessage(player, Component.text("Vous ne pouvez pas faire de RTP dans ce monde.", NamedTextColor.RED));
            return false;
        }
        
        if (!config.rtp().restrictedWorlds().isEmpty() && config.rtp().restrictedWorlds().contains(worldName)) {
            sendMessage(player, Component.text("Vous ne pouvez pas faire de RTP dans ce monde.", NamedTextColor.RED));
            return false;
        }
        
        if (!player.hasPermission("admincore.teleport.rtp")) {
            sendMessage(player, Component.text("Vous n'avez pas la permission de faire /rtp.", NamedTextColor.RED));
            return false;
        }
        
        // Check cooldown
        if (config.rtp().cooldown() > 0) {
            long lastRtp = rtpCooldowns.getOrDefault(player.getUniqueId(), 0L);
            long remaining = (lastRtp + config.rtp().cooldown() * 1000L) - System.currentTimeMillis();
            if (remaining > 0) {
                sendMessage(player, Component.text("Cooldown: attendez " + formatTime(remaining), NamedTextColor.RED));
                return false;
            }
        }
        
        sendMessage(player, Component.text("Recherche d'une position aleatoire...", NamedTextColor.YELLOW));
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Location randomLoc = findRandomLocation(player.getWorld(), player.getLocation());
            
            if (randomLoc == null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    sendMessage(player, Component.text("Aucune position securisee trouvee.", NamedTextColor.RED));
                });
                return;
            }
            
            // Save back position
            saveBackPosition(player, "rtp");
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                rtpCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                teleportPlayer(player, randomLoc, config.rtp().warmup(), () -> {
                    sendMessage(player, Component.text("Teleportation aleatoire reussie !", NamedTextColor.GREEN));
                });
            });
        });
        
        return true;
    }

    private Location findRandomLocation(World world, Location center) {
        int attempts = config.rtp().maxAttempts();
        Random random = new Random();
        
        for (int i = 0; i < attempts; i++) {
            int x = center.getBlockX() + random.nextInt(config.rtp().maxDistance() * 2) - config.rtp().maxDistance();
            int z = center.getBlockZ() + random.nextInt(config.rtp().maxDistance() * 2) - config.rtp().maxDistance();
            int y = world.getHighestBlockYAt(x, z);
            
            if (y < config.rtp().minHeight()) {
                y = config.rtp().minHeight();
            }
            
            Location loc = new Location(world, x + 0.5, y, z + 0.5);
            
            if (isSafeLocation(loc)) {
                return loc;
            }
        }
        return null;
    }

    private boolean isSafeLocation(Location loc) {
        Block feet = loc.getBlock();
        Block head = loc.clone().add(0, 1, 0).getBlock();
        Block ground = loc.clone().subtract(0, 1, 0).getBlock();
        
        return !feet.getType().isSolid() && 
               !head.getType().isSolid() && 
               ground.getType().isSolid() &&
               !feet.getType().equals(Material.LAVA) &&
               !feet.getType().equals(Material.FIRE);
    }

    // TPA methods
    public boolean sendTpaRequest(Player sender, Player target, boolean isTpaHere) {
        if (sender.equals(target)) {
            sendMessage(sender, Component.text("Vous ne pouvez pas vous teleporter sur vous-meme.", NamedTextColor.RED));
            return false;
        }
        
        if (tpaIgnore.contains(target.getUniqueId()) && !sender.hasPermission("admincore.teleport.tpa.bypass")) {
            sendMessage(sender, Component.text("Ce joueur ignore les requetes de teleportation.", NamedTextColor.RED));
            return false;
        }
        
        String type = isTpaHere ? "tpahere" : "tpa";
        database.saveTpaRequest(sender.getUniqueId(), target.getUniqueId(), type, config.tpa().requestTimeout());
        
        if (isTpaHere) {
            sendMessage(sender, Component.text("Requete envoyee a ", NamedTextColor.GREEN).append(Component.text(target.getName(), NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)).append(Component.text(" (tpahere).", NamedTextColor.GREEN)));
            sendMessage(target, Component.text(sender.getName(), NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true).append(Component.text(" demande que vous vous teleportiez sur lui.", NamedTextColor.YELLOW)));
        } else {
            sendMessage(sender, Component.text("Requete envoyee a ", NamedTextColor.GREEN).append(Component.text(target.getName(), NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)).append(Component.text(" (tpa).", NamedTextColor.GREEN)));
            sendMessage(target, Component.text(sender.getName(), NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true).append(Component.text(" demande a se teleporter sur vous.", NamedTextColor.YELLOW)));
        }
        
        sendTpaButtons(target, sender.getName(), isTpaHere);
        return true;
    }

    private void sendTpaButtons(Player target, String senderName, boolean isTpaHere) {
        Component acceptBtn = Component.text("[Accepter]", NamedTextColor.GREEN)
            .hoverEvent(HoverEvent.showText(Component.text("Cliquer pour accepter", NamedTextColor.GREEN)))
            .clickEvent(ClickEvent.runCommand("/tpaccept " + senderName));
        
        Component declineBtn = Component.text("[Refuser]", NamedTextColor.RED)
            .hoverEvent(HoverEvent.showText(Component.text("Cliquer pour refuser", NamedTextColor.RED)))
            .clickEvent(ClickEvent.runCommand("/tpdeny " + senderName));
        
        target.sendMessage(Component.text("  ").append(acceptBtn).append(Component.text("  ")).append(declineBtn));
    }

    public boolean acceptTpa(Player player) {
        TeleportDatabase.TeleportRequest request = database.getTpaRequest(player.getUniqueId());
        if (request == null) {
            sendMessage(player, Component.text("Vous n'avez aucune requete de teleportation.", NamedTextColor.RED));
            return false;
        }
        
        Player sender = Bukkit.getPlayer(request.sender());
        if (sender == null || !sender.isOnline()) {
            sendMessage(player, Component.text("Le joueur qui a envoye la requete n'est plus en ligne.", NamedTextColor.RED));
            database.deleteTpaRequest(player.getUniqueId());
            return false;
        }
        
        database.deleteTpaRequest(player.getUniqueId());
        
        if (request.type().equals("tpahere")) {
            sendMessage(player, Component.text("Requete acceptee.", NamedTextColor.GREEN));
            sendMessage(sender, Component.text(player.getName(), NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true).append(Component.text(" a accepte votre requete !", NamedTextColor.GREEN)));
            return teleportPlayer(sender, player.getLocation(), config.tpa().warmup());
        } else {
            sendMessage(player, Component.text("Requete acceptee.", NamedTextColor.GREEN));
            sendMessage(sender, Component.text(player.getName(), NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true).append(Component.text(" a accepte votre requete !", NamedTextColor.GREEN)));
            return teleportPlayer(player, sender.getLocation(), config.tpa().warmup());
        }
    }

    public boolean declineTpa(Player player, String senderName) {
        TeleportDatabase.TeleportRequest request = database.getTpaRequest(player.getUniqueId());
        if (request == null) {
            sendMessage(player, Component.text("Vous n'avez aucune requete de teleportation.", NamedTextColor.RED));
            return false;
        }
        
        Player sender = Bukkit.getPlayer(request.sender());
        database.deleteTpaRequest(player.getUniqueId());
        
        if (sender != null && sender.isOnline()) {
            sendMessage(sender, Component.text(player.getName(), NamedTextColor.RED).decoration(TextDecoration.BOLD, true).append(Component.text(" a refuse votre requete de teleportation.", NamedTextColor.RED)));
        }
        
        sendMessage(player, Component.text("Requete refusee.", NamedTextColor.GREEN));
        return true;
    }

    public boolean toggleTpaIgnore(Player player) {
        if (tpaIgnore.contains(player.getUniqueId())) {
            tpaIgnore.remove(player.getUniqueId());
            sendMessage(player, Component.text("Vous recevez desormais les requetes de teleportation.", NamedTextColor.GREEN));
        } else {
            tpaIgnore.add(player.getUniqueId());
            sendMessage(player, Component.text("Vous ignorez desormais les requetes de teleportation.", NamedTextColor.YELLOW));
        }
        return true;
    }

    public boolean isTpaIgnore(Player player) {
        return tpaIgnore.contains(player.getUniqueId());
    }

    // Back methods
    public boolean saveBackPosition(Player player, Location from, String reason) {
        if (!config.back().enabled()) {
            return false;
        }
        
        if (!config.back().restrictedWorlds().isEmpty() && 
            config.back().restrictedWorlds().contains(from.getWorld().getName())) {
            return false;
        }
        
        database.saveBackPosition(player, from, reason);
        return true;
    }

    public boolean teleportBack(Player player) {
        
        Location backLoc = database.getBackPosition(player.getUniqueId());
        if (backLoc == null) {
            sendMessage(player, Component.text("Vous n'avez aucune position precedente.", NamedTextColor.RED));
            return false;
        }
        return teleportPlayer(player, backLoc, config.back().enabled() ? 3 : 0);
    }

    // Core teleport method
    public boolean teleportPlayer(Player player, Location destination, int warmupSeconds) {
        return teleportPlayer(player, destination, warmupSeconds, null);
    }

    public boolean teleportPlayer(Player player, Location destination, int warmupSeconds, Runnable onComplete) {
        if (activeTeleports.containsKey(player.getUniqueId())) {
            sendMessage(player, Component.text("Vous etes deja en cours de teleportation.", NamedTextColor.RED));
            return false;
        }
        
        if (warmupSeconds <= 0) {
            performTeleport(player, destination, onComplete);
            return true;
        }
        
        activeTeleports.put(player.getUniqueId(), new TeleportTask(player.getUniqueId(), destination, onComplete));
        
        Component msg = Component.text("Teleportation dans ", NamedTextColor.GREEN)
            .append(Component.text(warmupSeconds, NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true))
            .append(Component.text(" secondes, ne bougez plus...", NamedTextColor.GREEN));
        sendMessage(player, msg);
        
        player.sendActionBar(Component.text("Teleportation dans " + warmupSeconds + "...", NamedTextColor.GREEN));
        
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            TeleportTask teleport = activeTeleports.get(player.getUniqueId());
            if (teleport == null) {
                return;
            }
            
            Location current = player.getLocation();
            if (current.distance(teleport.startLocation()) > 0.5) {
                activeTeleports.remove(player.getUniqueId());
                player.sendActionBar(Component.text("Teleportation annulee !", NamedTextColor.RED));
                sendMessage(player, Component.text("Teleportation annulee — vous avez bouge !", NamedTextColor.RED));
                return;
            }
            
            teleport.countdown(teleport.countdown() - 1);
            
            if (teleport.countdown() <= 0) {
                activeTeleports.remove(player.getUniqueId());
                performTeleport(player, destination, onComplete);
            } else {
                player.sendActionBar(Component.text("Teleportation dans " + teleport.countdown() + "...", NamedTextColor.GREEN));
            }
        }, 20L, 20L);
        
        activeTeleports.get(player.getUniqueId()).taskId(task.getTaskId());
        
        return true;
    }

    private void performTeleport(Player player, Location destination, Runnable onComplete) {
        player.teleportAsync(destination).thenAccept(success -> {
            if (success) {
                sendMessage(player, Component.text("Teleportation terminee !", NamedTextColor.GREEN));
                if (onComplete != null) {
                    onComplete.run();
                }
            } else {
                sendMessage(player, Component.text("Echec de la teleportation.", NamedTextColor.RED));
            }
        });
    }

    public void cancelTeleport(Player player) {
        activeTeleports.remove(player.getUniqueId());
    }

    public boolean isTeleporting(Player player) {
        return activeTeleports.containsKey(player.getUniqueId());
    }

    private void saveBackPosition(Player player, String reason) {
        saveBackPosition(player, player.getLocation(), reason);
    }

    private void sendMessage(Player player, Component message) {
        player.sendMessage(Component.text("[Teleport] ", NamedTextColor.DARK_GRAY).append(message));
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "s";
    }

    public void handlePlayerDeath(Player player) {
        if (config.back().saveOnDeath()) {
            saveBackPosition(player, player.getLocation(), "death");
        }
    }

    public record TeleportTask(
        UUID player,
        Location destination,
        Runnable onComplete,
        Location startLocation,
        int countdown,
        int taskId
    ) {
        public TeleportTask(UUID player, Location destination, Runnable onComplete) {
            this(player, destination, onComplete, null, 0, -1);
        }
        
        public TeleportTask withStart(Location start) {
            return new TeleportTask(player, destination, onComplete, start, countdown, taskId);
        }
        
        public TeleportTask countdown(int count) {
            return new TeleportTask(player, destination, onComplete, startLocation, count, taskId);
        }
        
        public TeleportTask taskId(int id) {
            return new TeleportTask(player, destination, onComplete, startLocation, countdown, id);
        }
    }
}
