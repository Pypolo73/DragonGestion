package fr.dragon.admincore.chat;

public record ClearChatSelection(
    boolean publicMessages,
    boolean playerBubbles,
    boolean connectionLogs,
    boolean systemMessages,
    String visibleReason
) {
}
