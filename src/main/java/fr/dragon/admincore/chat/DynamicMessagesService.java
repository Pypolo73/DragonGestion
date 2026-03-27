package fr.dragon.admincore.chat;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.util.ConfigLoader;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DynamicMessagesService {

    private final AdminCorePlugin plugin;
    private final ConfigLoader configLoader;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<java.util.UUID, Boolean> firstJoinCache = new ConcurrentHashMap<>();
    private final Map<String, String> gradeColors;

    public DynamicMessagesService(final AdminCorePlugin plugin, final ConfigLoader configLoader) {
        this.plugin = plugin;
        this.configLoader = configLoader;
        this.gradeColors = loadGradeColors();
    }

    private Map<String, String> loadGradeColors() {
        final Map<String, String> colors = new ConcurrentHashMap<>();
        final var config = configLoader.chatConfig();
        if (config == null) {
            return Map.of(
                "default", "<gray>[Joueur]</gray>",
                "guide", "<green>[Guide]</green>",
                "moderator", "<blue>[Modo]</blue>",
                "admin", "<red>[Admin]</red>"
            );
        }
        
        final var gradesSection = config.getConfigurationSection("grades");
        if (gradesSection != null) {
            for (final String key : gradesSection.getKeys(false)) {
                colors.put(key.toLowerCase(), gradesSection.getString(key, "<gray>[Joueur]</gray>"));
            }
        }
        
        return colors;
    }

    public void onPlayerJoin(final PlayerJoinEvent event) {
        if (!isJoinEnabled()) {
            return;
        }

        final Player player = event.getPlayer();
        final boolean isFirstJoin = !firstJoinCache.containsKey(player.getUniqueId());
        
        if (isFirstJoin) {
            firstJoinCache.put(player.getUniqueId(), true);
            if (isFirstJoinEnabled()) {
                sendBroadcast(parseMessage(getFirstJoinMessage(), player, null, null, null));
            }
        } else if (isJoinEnabled()) {
            sendBroadcast(parseMessage(getJoinMessage(), player, null, null, null));
        }
    }

    public void onPlayerQuit(final PlayerQuitEvent event) {
        if (!isQuitEnabled()) {
            return;
        }

        final Player player = event.getPlayer();
        sendBroadcast(parseMessage(getQuitMessage(), player, null, null, null));
    }

    public void onPlayerDeath(final PlayerDeathEvent event) {
        if (!isDeathEnabled()) {
            return;
        }

        final Player player = event.getEntity();
        final Player killer = event.getEntity().getKiller();
        
        String deathCause = event.getDeathMessage() != null 
            ? extractDeathCause(event.getDeathMessage()) 
            : player.getLastDamageCause() != null 
                ? player.getLastDamageCause().getCause().name() 
                : "Inconnu";
        
        sendBroadcast(parseMessage(getDeathMessage(), player, killer, deathCause, null));
    }

    private String extractDeathCause(String deathMessage) {
        if (deathMessage == null) {
            return "Inconnu";
        }
        
        final String[] causes = {"VOID", "LAVA", "FIRE", "WATER", "FALL", "PROJECTILE", "ENTITY", "EXPLOSION", "MAGIC", "SUFFOCATION", "BLOCK"};
        for (final String cause : causes) {
            if (deathMessage.toUpperCase().contains(cause)) {
                return cause;
            }
        }
        
        return deathMessage.replaceAll(".*?(est mort|a été tué|a explosé|s'est noyé|est tombé|est brûlé).*", "$1");
    }

    private Component parseMessage(final String template, final Player player, final Player killer, final String deathCause, final String killerWeapon) {
        if (template == null || template.isEmpty()) {
            return Component.empty();
        }

        final String grade = getLuckPermsGroup(player);
        final String coloredGrade = getColoredGrade(grade);

        final TagResolver.Builder resolvers = TagResolver.builder()
            .resolver(Placeholder.parsed("player", player.getName()))
            .resolver(Placeholder.parsed("grade", coloredGrade != null ? coloredGrade : getDefaultGrade()));

        if (deathCause != null) {
            resolvers.resolver(Placeholder.parsed("death_cause", deathCause));
        }
        if (killer != null) {
            resolvers.resolver(Placeholder.parsed("player_killer", killer.getName()));
        } else {
            resolvers.resolver(Placeholder.parsed("player_killer", "Inconnu"));
        }
        if (killerWeapon != null) {
            resolvers.resolver(Placeholder.parsed("killer_weapon", killerWeapon));
        }
        if (player.getWorld() != null) {
            resolvers.resolver(Placeholder.parsed("world", player.getWorld().getName()));
        }

        try {
            return miniMessage.deserialize(template, resolvers.build());
        } catch (final Exception e) {
            plugin.getLogger().warning("Erreur lors du parsing du message: " + template);
            return Component.text(template);
        }
    }

    private String getLuckPermsGroup(final Player player) {
        try {
            final Class<?> clazz = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            final Method method = clazz.getMethod("setPlaceholders", Player.class, String.class);
            final Object result = method.invoke(null, player, "%luckperms_primary_group%");
            if (result != null) {
                return result.toString().toLowerCase();
            }
        } catch (final Exception ignored) {
        }
        return "default";
    }

    private String getColoredGrade(final String grade) {
        return gradeColors.getOrDefault(grade.toLowerCase(), gradeColors.get("default"));
    }

    private String getDefaultGrade() {
        return gradeColors.getOrDefault("default", "<gray>[Joueur]</gray>");
    }

    private void sendBroadcast(final Component message) {
        if (message == null || message.equals(Component.empty())) {
            return;
        }
        plugin.getServer().getOnlinePlayers().forEach(player -> player.sendMessage(message));
    }

    private boolean isJoinEnabled() {
        final var config = configLoader.chatConfig();
        return config != null && config.getBoolean("join.enabled", true);
    }

    private boolean isFirstJoinEnabled() {
        final var config = configLoader.chatConfig();
        return config != null && config.getBoolean("first_join.enabled", true);
    }

    private boolean isQuitEnabled() {
        final var config = configLoader.chatConfig();
        return config != null && config.getBoolean("quit.enabled", true);
    }

    private boolean isDeathEnabled() {
        final var config = configLoader.chatConfig();
        return config != null && config.getBoolean("death.enabled", true);
    }

    private String getJoinMessage() {
        final var config = configLoader.chatConfig();
        return config != null ? config.getString("join.broadcast", "") : "";
    }

    private String getFirstJoinMessage() {
        final var config = configLoader.chatConfig();
        return config != null ? config.getString("first_join.broadcast", "") : "";
    }

    private String getQuitMessage() {
        final var config = configLoader.chatConfig();
        return config != null ? config.getString("quit.broadcast", "") : "";
    }

    private String getDeathMessage() {
        final var config = configLoader.chatConfig();
        return config != null ? config.getString("death.broadcast", "") : "";
    }
}