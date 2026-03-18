package fr.dragon.admincore.chat;

import fr.dragon.admincore.core.AdminCoreAPI;
import fr.dragon.admincore.core.PermissionService;
import fr.dragon.admincore.util.ConfigLoader;
import fr.dragon.admincore.util.MessageFormatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class ChatServiceImpl implements ChatService {

    private final ConfigLoader configLoader;
    private final MessageFormatter formatter;
    private final Set<UUID> spyEnabled = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastMessages = new ConcurrentHashMap<>();
    private volatile boolean chatLocked;
    private volatile boolean muteAll;
    private volatile int slowModeSeconds;

    public ChatServiceImpl(final ConfigLoader configLoader, final MessageFormatter formatter) {
        this.configLoader = configLoader;
        this.formatter = formatter;
        this.slowModeSeconds = Math.max(0, configLoader.config().getInt("chat.slowmode-default", 0));
    }

    @Override
    public ChatGuardResult canTalk(final Player player) {
        if (player.hasPermission(PermissionService.CHAT_BYPASS)) {
            return ChatGuardResult.ok();
        }
        if (AdminCoreAPI.sanctions().getCachedMute(player.getUniqueId()).isPresent()) {
            return ChatGuardResult.denied("chat.muted");
        }
        if (this.chatLocked) {
            return ChatGuardResult.denied("chat.locked");
        }
        if (this.muteAll) {
            return ChatGuardResult.denied("chat.mute-all");
        }
        if (this.slowModeSeconds > 0) {
            final long now = System.currentTimeMillis();
            final long previous = this.lastMessages.getOrDefault(player.getUniqueId(), 0L);
            if (now - previous < this.slowModeSeconds * 1_000L) {
                return ChatGuardResult.denied("chat.slowmode-active");
            }
        }
        return ChatGuardResult.ok();
    }

    @Override
    public void markMessage(final Player player) {
        this.lastMessages.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @Override
    public void clearChat(final ClearChatSelection selection) {
        final int lines = Math.max(25, this.configLoader.config().getInt("chat.clear-lines", 120));
        for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            for (int index = 0; index < lines; index++) {
                onlinePlayer.sendMessage(" ");
            }
            if (selection.visibleReason() != null && !selection.visibleReason().isBlank()) {
                onlinePlayer.sendMessage(this.formatter.message(
                    "chat.clear-announcement",
                    this.formatter.text("reason", selection.visibleReason())
                ));
            }
        }
    }

    @Override
    public boolean toggleChatLock() {
        this.chatLocked = !this.chatLocked;
        return this.chatLocked;
    }

    @Override
    public boolean toggleMuteAll() {
        this.muteAll = !this.muteAll;
        return this.muteAll;
    }

    @Override
    public void setSlowMode(final int seconds) {
        this.slowModeSeconds = Math.max(0, seconds);
    }

    @Override
    public int getSlowModeSeconds() {
        return this.slowModeSeconds;
    }

    @Override
    public boolean isChatLocked() {
        return this.chatLocked;
    }

    @Override
    public boolean isMuteAll() {
        return this.muteAll;
    }

    @Override
    public void broadcast(final String message) {
        Bukkit.broadcast(this.formatter.deserialize(message));
    }

    @Override
    public void broadcastStaffChat(final String author, final String message) {
        final TagResolver resolver = this.formatter.text("author", author == null ? "Staff" : author);
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(PermissionService.STAFF)) {
                player.sendMessage(this.formatter.message("chat.staff-chat-format", resolver, this.formatter.text("message", message)));
            }
        }
    }

    @Override
    public Set<UUID> getSpyEnabled() {
        return this.spyEnabled;
    }
}
