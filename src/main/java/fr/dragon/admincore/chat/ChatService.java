package fr.dragon.admincore.chat;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;

public interface ChatService {

    ChatGuardResult canTalk(Player player, String message);

    void markMessage(Player player, String message);

    void clearChat(ClearChatSelection selection);

    void clearChatFor(Player target, String reason);

    boolean toggleChatLock();

    boolean toggleMuteAll();

    void setSlowMode(int seconds);

    int getSlowModeSeconds();

    boolean isChatLocked();

    boolean isMuteAll();

    void broadcast(String message);

    void broadcastStaffChat(String author, String message);

    Set<UUID> getSpyEnabled();

    List<ChatHistoryEntry> recentMessages(int limit);

    List<ChatHistoryEntry> recentMessagesByAuthor(String author, int limit);

    boolean removeRecentMessage(ChatHistoryEntry entry);

    void clearChatHistory();
}
