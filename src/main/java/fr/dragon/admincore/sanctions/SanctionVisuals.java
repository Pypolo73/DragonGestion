package fr.dragon.admincore.sanctions;

import fr.dragon.admincore.util.ConfigLoader;
import fr.dragon.admincore.util.MessageFormatter;
import fr.dragon.admincore.util.TimeParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public final class SanctionVisuals {

    private SanctionVisuals() {
    }

    public static Component banScreen(final ConfigLoader configLoader, final MessageFormatter formatter, final SanctionRecord record, final String discord) {
        return banScreen(configLoader, formatter, record, discord, record.targetName());
    }

    public static Component banScreen(
        final ConfigLoader configLoader,
        final MessageFormatter formatter,
        final SanctionRecord record,
        final String discord,
        final String displayTarget
    ) {
        final TextColor gold = TextColor.fromHexString("#ffe45e");
        final TextColor magenta = TextColor.fromHexString("#d83bff");
        final TextColor blue = TextColor.fromHexString("#67b9ff");
        return Component.join(JoinConfiguration.newlines(), java.util.List.of(
            Component.text("DragonGestion", gold).decorate(TextDecoration.BOLD),
            Component.empty(),
            label("Joueur", displayTarget == null || displayTarget.isBlank() ? record.targetName() : displayTarget, magenta),
            label("Sanction", typeLabel(record.type()), magenta),
            label("Raison", record.reason(), magenta),
            label("Par", record.actorName(), magenta),
            label("Duree", record.expiresAt() == null ? "Permanent" : TimeParser.formatRemaining(record.expiresAt()), magenta),
            label("Expire", record.expiresAt() == null ? "Jamais" : record.expiresAt().toString(), magenta),
            Component.empty(),
            Component.text("Discord: ", blue).append(Component.text(normalizeDiscordText(discord), NamedTextColor.WHITE))
        ));
    }

    public static Component muteMessage(final MessageFormatter formatter, final SanctionRecord record, final String discord) {
        return formatter.message(
            "sanctions.mute-chat",
            formatter.text("reason", record.reason()),
            formatter.text("duration", record.expiresAt() == null ? "Permanent" : TimeParser.formatRemaining(record.expiresAt())),
            Placeholder.component("discord", discordComponent(discord))
        );
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
        return Component.text(normalizeDiscordText(discord), NamedTextColor.AQUA);
    }

    private static String normalizeDiscordText(final String raw) {
        if (raw == null || raw.isBlank()) {
            return "discord.gg/example";
        }
        return raw.trim();
    }

    private static Component label(final String label, final String value, final TextColor color) {
        return Component.text(label + ": ", color)
            .append(Component.text(value, NamedTextColor.WHITE));
    }
}
