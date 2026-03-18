package fr.dragon.admincore.sanctions;

import fr.dragon.admincore.util.MessageFormatter;
import fr.dragon.admincore.util.TimeParser;
import net.kyori.adventure.text.Component;

public final class SanctionVisuals {

    private SanctionVisuals() {
    }

    public static Component banScreen(final MessageFormatter formatter, final SanctionRecord record, final String discord) {
        return formatter.block(
            "sanctions.login-ban-screen",
            formatter.text("reason", record.reason()),
            formatter.text("actor", record.actorName()),
            formatter.text("duration", record.expiresAt() == null ? "Permanent" : TimeParser.formatRemaining(record.expiresAt())),
            formatter.text("expires", record.expiresAt() == null ? "Jamais" : record.expiresAt().toString()),
            formatter.text("discord", discord)
        );
    }

    public static Component muteMessage(final MessageFormatter formatter, final SanctionRecord record, final String discord) {
        return formatter.message(
            "sanctions.mute-chat",
            formatter.text("reason", record.reason()),
            formatter.text("duration", record.expiresAt() == null ? "Permanent" : TimeParser.formatRemaining(record.expiresAt())),
            formatter.text("discord", discord)
        );
    }
}
