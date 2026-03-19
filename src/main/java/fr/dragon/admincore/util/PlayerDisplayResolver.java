package fr.dragon.admincore.util;

import fr.dragon.admincore.database.PlayerProfile;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class PlayerDisplayResolver {

    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer SECTION = LegacyComponentSerializer.legacySection();

    private PlayerDisplayResolver() {
    }

    public static Component resolveGradeComponent(final OfflinePlayer offlinePlayer) {
        final String prefix = placeholderValue(offlinePlayer, "%luckperms_prefix%");
        if (prefix != null && !prefix.isBlank() && !prefix.contains("%")) {
            return parseLegacy(prefix);
        }
        final String group = placeholderValue(offlinePlayer, "%luckperms_primary_group%");
        if (group != null && !group.isBlank() && !group.contains("%")) {
            return Component.text(group).decoration(TextDecoration.ITALIC, false);
        }
        final String directGroup = directLuckPermsGroup(offlinePlayer.getUniqueId());
        if (directGroup != null && !directGroup.isBlank()) {
            return Component.text(directGroup).decoration(TextDecoration.ITALIC, false);
        }
        return Component.text("Inconnu", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    public static Component levelComponent(final int level) {
        final NamedTextColor color = level >= 75 ? NamedTextColor.RED
            : level >= 40 ? NamedTextColor.GOLD
            : level >= 15 ? NamedTextColor.YELLOW
            : NamedTextColor.GREEN;
        return Component.text(Integer.toString(level), color).decoration(TextDecoration.ITALIC, false);
    }

    public static String punishmentTypes(final List<String> types) {
        if (types == null || types.isEmpty()) {
            return "Aucune";
        }
        return String.join(", ", types.stream().map(type -> type.toLowerCase(Locale.ROOT)).toList());
    }

    public static String client(final PlayerProfile profile) {
        return profile.clientBrand() == null || profile.clientBrand().isBlank() ? "Inconnu" : profile.clientBrand();
    }

    private static String placeholderValue(final OfflinePlayer player, final String placeholder) {
        try {
            final Class<?> clazz = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            try {
                final Method offlineMethod = clazz.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
                return (String) offlineMethod.invoke(null, player, placeholder);
            } catch (final NoSuchMethodException ignored) {
                final Player online = player.getPlayer();
                if (online == null) {
                    return null;
                }
                final Method playerMethod = clazz.getMethod("setPlaceholders", Player.class, String.class);
                return (String) playerMethod.invoke(null, online, placeholder);
            }
        } catch (final Exception exception) {
            return null;
        }
    }

    private static Component parseLegacy(final String value) {
        final Component component = value.indexOf('§') >= 0 ? SECTION.deserialize(value) : AMPERSAND.deserialize(value);
        return component.decoration(TextDecoration.ITALIC, false);
    }

    private static String directLuckPermsGroup(final UUID uuid) {
        if (uuid == null) {
            return null;
        }
        try {
            final Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            final Object api = provider.getMethod("get").invoke(null);
            final Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
            final Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, uuid);
            if (user == null) {
                return null;
            }
            return (String) user.getClass().getMethod("getPrimaryGroup").invoke(user);
        } catch (final Exception exception) {
            return null;
        }
    }
}
