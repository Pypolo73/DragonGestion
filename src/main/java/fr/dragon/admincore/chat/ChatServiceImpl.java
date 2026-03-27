package fr.dragon.admincore.chat;

import fr.dragon.admincore.core.AdminCoreAPI;
import fr.dragon.admincore.core.PermissionService;
import fr.dragon.admincore.util.ConfigLoader;
import fr.dragon.admincore.util.MessageFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.Deque;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class ChatServiceImpl implements ChatService {

    private final ConfigLoader configLoader;
    private final MessageFormatter formatter;
    private final Set<UUID> spyEnabled = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastMessages = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastRepeatedMessages = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastMessageContent = new ConcurrentHashMap<>();
    private final Deque<ChatHistoryEntry> recentMessages = new ConcurrentLinkedDeque<>();
    private volatile boolean chatLocked;
    private volatile boolean muteAll;
    private volatile int slowModeSeconds;

    public ChatServiceImpl(final ConfigLoader configLoader, final MessageFormatter formatter) {
        this.configLoader = configLoader;
        this.formatter = formatter;
        this.slowModeSeconds = Math.max(0, configLoader.config().getInt("chat.slowmode-default", 0));
    }

    @Override
    public ChatGuardResult canTalk(final Player player, final String message) {
        if (player.hasPermission(PermissionService.CHAT_BYPASS)) {
            return ChatGuardResult.ok();
        }
        final String normalized = normalize(message);
        for (final String blockedWord : this.configLoader.config().getStringList("chat.blocked-words")) {
            if (!blockedWord.isBlank() && normalized.contains(blockedWord.toLowerCase(Locale.ROOT))) {
                return ChatGuardResult.denied("chat.blocked-word", blockedWord);
            }
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
        final long now = System.currentTimeMillis();
        final long previous = this.lastMessages.getOrDefault(player.getUniqueId(), 0L);
        final int minimumDelay = Math.max(this.slowModeSeconds, configuredInt(180,
            "chat.minimum-delay-seconds",
            "chat.message-delay-seconds"
        ));
        if (minimumDelay > 0 && now - previous < minimumDelay * 1_000L) {
            final long remaining = Math.max(1L, ((minimumDelay * 1_000L) - (now - previous) + 999L) / 1_000L);
            return ChatGuardResult.denied("chat.wait-before-talk", Long.toString(remaining));
        }
        final String previousMessage = this.lastMessageContent.get(player.getUniqueId());
        final long previousRepeat = this.lastRepeatedMessages.getOrDefault(player.getUniqueId(), 0L);
        final int repeatDelay = configuredInt(30,
            "chat.same-message-delay-seconds",
            "chat.repeat-delay-seconds"
        );
        if (repeatDelay > 0 && normalized.equals(previousMessage) && now - previousRepeat < repeatDelay * 1_000L) {
            return ChatGuardResult.denied("chat.repeat-message");
        }
        return ChatGuardResult.ok();
    }

    @Override
    public void markMessage(final Player player, final String message) {
        final long now = System.currentTimeMillis();
        this.lastMessages.put(player.getUniqueId(), now);
        this.lastRepeatedMessages.put(player.getUniqueId(), now);
        this.lastMessageContent.put(player.getUniqueId(), normalize(message));
        this.recentMessages.addFirst(new ChatHistoryEntry(java.time.Instant.now(), player.getName(), message));
        while (this.recentMessages.size() > Math.max(25, this.configLoader.config().getInt("chat.history-limit", 100))) {
            this.recentMessages.pollLast();
        }
    }

    @Override
    public void clearChat(final ClearChatSelection selection) {
        final int lines = Math.max(25, this.configLoader.config().getInt("chat.clear-lines", 120));
        for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            clearView(onlinePlayer, lines, selection.visibleReason());
        }
        if (selection.publicMessages()) {
            clearChatHistory();
        }
    }

    @Override
    public void clearChatFor(final Player target, final String reason) {
        clearView(target, Math.max(25, this.configLoader.config().getInt("chat.clear-lines", 120)), reason);
        this.recentMessages.removeIf(entry -> entry.author().equalsIgnoreCase(target.getName()));
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
        final boolean allowMiniMessage = this.configLoader.config().getBoolean("chat.broadcast-allow-minimessage", false);
        Bukkit.broadcast(allowMiniMessage ? this.formatter.deserialize(message) : Component.text(message == null ? "" : message));
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

    @Override
    public List<ChatHistoryEntry> recentMessages(final int limit) {
        return this.recentMessages.stream().limit(Math.max(1, limit)).toList();
    }

    @Override
    public List<ChatHistoryEntry> recentMessagesByAuthor(final String author, final int limit) {
        return this.recentMessages.stream()
            .filter(entry -> entry.author().equalsIgnoreCase(author))
            .limit(Math.max(1, limit))
            .toList();
    }

    @Override
    public boolean removeRecentMessage(final ChatHistoryEntry entry) {
        return this.recentMessages.remove(entry);
    }

    @Override
    public void clearChatHistory() {
        this.recentMessages.clear();
    }

    private void clearView(final Player target, final int lines, final String reason) {
        for (int index = 0; index < lines; index++) {
            target.sendMessage(" ");
        }
        if (reason != null && !reason.isBlank()) {
            target.sendMessage(this.formatter.message(
                "chat.clear-announcement",
                this.formatter.text("reason", reason)
            ));
        }
    }

    private String normalize(final String message) {
        return message == null ? "" : message.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private int configuredInt(final int fallback, final String... paths) {
        for (final String path : paths) {
            if (this.configLoader.config().contains(path)) {
                return Math.max(0, this.configLoader.config().getInt(path, fallback));
            }
        }
        return Math.max(0, fallback);
    }
}
