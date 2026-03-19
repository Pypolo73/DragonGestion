package fr.dragon.admincore.sanctions;

import fr.dragon.admincore.util.ConfigLoader;
import fr.dragon.admincore.util.MessageFormatter;
import fr.dragon.admincore.util.TimeParser;
import java.util.List;
import net.kyori.adventure.text.Component;

public final class SanctionVisuals {

    private SanctionVisuals() {
    }

    public static Component banScreen(final ConfigLoader configLoader, final MessageFormatter formatter, final SanctionRecord record, final String discord) {
        final List<String> lines = configLoader.config().getStringList("sanctions.login-ban-screen");
        if (lines.isEmpty()) {
            return formatter.block(
                "sanctions.login-ban-screen",
                formatter.text("target", record.targetName()),
                formatter.text("reason", record.reason()),
                formatter.text("actor", record.actorName()),
                formatter.text("duration", record.expiresAt() == null ? "Permanent" : TimeParser.formatRemaining(record.expiresAt())),
                formatter.text("expires", record.expiresAt() == null ? "Jamais" : record.expiresAt().toString()),
                formatter.text("discord", discord)
            );
        }
        Component result = Component.empty();
        for (int index = 0; index < lines.size(); index++) {
            result = result.append(formatter.deserialize(
                lines.get(index),
                formatter.text("target", record.targetName()),
                formatter.text("reason", record.reason()),
                formatter.text("actor", record.actorName()),
                formatter.text("duration", record.expiresAt() == null ? "Permanent" : TimeParser.formatRemaining(record.expiresAt())),
                formatter.text("expires", record.expiresAt() == null ? "Jamais" : record.expiresAt().toString()),
                formatter.text("discord", discord)
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
            formatter.text("discord", discord)
        );
    }
}
