package fr.dragon.admincore.chat;

import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;

public interface ChatService {

    ChatGuardResult canTalk(Player player);

    void markMessage(Player player);

    void clearChat(ClearChatSelection selection);

    boolean toggleChatLock();

    boolean toggleMuteAll();

    void setSlowMode(int seconds);

    int getSlowModeSeconds();

    boolean isChatLocked();

    boolean isMuteAll();

    void broadcast(String message);

    void broadcastStaffChat(String author, String message);

    Set<UUID> getSpyEnabled();
}
