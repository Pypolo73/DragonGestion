package fr.dragon.admincore.sanctions;

import fr.dragon.admincore.util.ConfigLoader;
import fr.dragon.admincore.util.MessageFormatter;
import fr.dragon.admincore.util.TimeParser;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public final class SanctionVisuals {

    private SanctionVisuals() {
    }

    public static Component banScreen(final ConfigLoader configLoader, final MessageFormatter formatter, final SanctionRecord record, final String discord) {
        final List<String> lines = withSanctionLine(configLoader.config().getStringList("sanctions.login-ban-screen"));
        final TagResolver sanctionResolver = formatter.text("sanction", typeLabel(record.type()));
        final TagResolver discordResolver = Placeholder.component("discord", discordComponent(discord));
        if (lines.isEmpty()) {
            return formatter.block(
                "sanctions.login-ban-screen",
                formatter.text("target", record.targetName()),
                sanctionResolver,
                formatter.text("reason", record.reason()),
                formatter.text("actor", record.actorName()),
                formatter.text("duration", record.expiresAt() == null ? "Permanent" : TimeParser.formatRemaining(record.expiresAt())),
                formatter.text("expires", record.expiresAt() == null ? "Jamais" : record.expiresAt().toString()),
                discordResolver
            );
        }
        Component result = Component.empty();
        for (int index = 0; index < lines.size(); index++) {
            result = result.append(formatter.deserialize(
                lines.get(index),
                formatter.text("target", record.targetName()),
                sanctionResolver,
                formatter.text("reason", record.reason()),
                formatter.text("actor", record.actorName()),
                formatter.text("duration", record.expiresAt() == null ? "Permanent" : TimeParser.formatRemaining(record.expiresAt())),
                formatter.text("expires", record.expiresAt() == null ? "Jamais" : record.expiresAt().toString()),
                discordResolver
            ));
            if (index + 1 < lines.size()) {
                result = result.append(Component.newline());
            }
        }
        return result;
    }

    public static Component muteMessage(final MessageFormatter formatter, final SanctionRecord record, final String discord) {
        return formatter.message(
            "sanctions.mute-chat",
            formatter.text("reason", record.reason()),
            formatter.text("duration", record.expiresAt() == null ? "Permanent" : TimeParser.formatRemaining(record.expiresAt())),
            Placeholder.component("discord", discordComponent(discord))
        );
    }

    private static List<String> withSanctionLine(final List<String> lines) {
        if (lines.isEmpty() || lines.stream().anyMatch(line -> line.contains("<sanction>"))) {
            return lines;
        }
        final List<String> updated = new ArrayList<>(lines);
        int insertIndex = -1;
        for (int index = 0; index < updated.size(); index++) {
            if (updated.get(index).contains("<target>")) {
                insertIndex = index + 1;
                break;
            }
        }
        if (insertIndex < 0 || insertIndex > updated.size()) {
            insertIndex = Math.min(3, updated.size());
        }
        updated.add(insertIndex, "<#d83bff>Sanction :</#d83bff> <white><sanction></white>");
        return updated;
    }

    private static String typeLabel(final SanctionType type) {
        return switch (type) {
            case BAN -> "Ban";
            case MUTE -> "Mute";
            case KICK -> "Kick";
            case WARN -> "Warn";
        };
    }

    private static Component discordComponent(final String discord) {
        final String display = (discord == null || discord.isBlank()) ? "discord.gg/example" : discord.trim();
        return Component.text(display)
            .color(NamedTextColor.AQUA)
            .decorate(TextDecoration.UNDERLINED)
            .hoverEvent(HoverEvent.showText(Component.text("Cliquer pour ouvrir le Discord")))
            .clickEvent(ClickEvent.openUrl(normalizeDiscordUrl(display)));
    }

    private static String normalizeDiscordUrl(final String raw) {
        final String value = raw.trim();
        if (value.startsWith("https://") || value.startsWith("http://")) {
            return value;
        }
        return "https://" + value;
    }
}
