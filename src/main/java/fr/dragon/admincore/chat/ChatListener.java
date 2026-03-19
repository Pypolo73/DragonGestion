package fr.dragon.admincore.chat;

import fr.dragon.admincore.core.AdminCorePlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ChatListener implements Listener {

    private final AdminCorePlugin plugin;

    public ChatListener(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(final AsyncChatEvent event) {
        final String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
        if (this.plugin.getChatPromptService().handleAsync(event.getPlayer(), plainMessage)) {
            event.setCancelled(true);
            return;
        }
        final ChatGuardResult result = this.plugin.getChatService().canTalk(event.getPlayer(), plainMessage);
        if (!result.allowed()) {
            event.setCancelled(true);
            this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
                event.getPlayer().sendMessage(this.plugin.getMessageFormatter().message(
                    result.messageKey(),
                    this.plugin.getMessageFormatter().text("time", result.detail()),
                    this.plugin.getMessageFormatter().text("word", result.detail())
                ))
            );
            return;
        }
        this.plugin.getChatService().markMessage(event.getPlayer(), plainMessage);
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent event) {
        this.plugin.getChatPromptService().clear(event.getPlayer().getUniqueId());
    }
}
