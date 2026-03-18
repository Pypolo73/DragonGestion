package fr.dragon.admincore.core.compat;

import fr.dragon.admincore.chat.ChatService;
import net.shortninja.staffplusplus.staffmode.chat.StaffChatService;

public final class StaffPlusPlusStaffChatAdapter implements StaffChatService {

    private final ChatService chatService;

    public StaffPlusPlusStaffChatAdapter(final ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public void sendMessage(final String message) {
        this.chatService.broadcastStaffChat("Staff", message);
    }

    @Override
    public void sendMessage(final String sender, final String message) {
        this.chatService.broadcastStaffChat(sender, message);
    }
}
