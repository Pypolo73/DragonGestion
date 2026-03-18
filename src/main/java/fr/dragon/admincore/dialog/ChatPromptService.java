package fr.dragon.admincore.dialog;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.util.MessageFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public final class ChatPromptService {

    public interface Session {
        void open(Player player);

        boolean handle(Player player, String input);
    }

    private final AdminCorePlugin plugin;
    private final MessageFormatter formatter;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public ChatPromptService(final AdminCorePlugin plugin, final MessageFormatter formatter) {
        this.plugin = plugin;
        this.formatter = formatter;
    }

    public void start(final Player player, final Session session) {
        this.sessions.put(player.getUniqueId(), session);
        session.open(player);
    }

    public boolean handleAsync(final Player player, final String input) {
        final Session session = this.sessions.get(player.getUniqueId());
        if (session == null) {
            return false;
        }
        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            final boolean keep = session.handle(player, input);
            if (!keep) {
                this.sessions.remove(player.getUniqueId());
                player.sendMessage(this.formatter.message("dialogs.chat-prompt-finished"));
            }
        });
        return true;
    }

    public void clear(final UUID uuid) {
        this.sessions.remove(uuid);
    }
}
