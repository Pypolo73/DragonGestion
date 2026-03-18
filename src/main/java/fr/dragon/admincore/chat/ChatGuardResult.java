package fr.dragon.admincore.chat;

public record ChatGuardResult(boolean allowed, String messageKey) {

    public static ChatGuardResult ok() {
        return new ChatGuardResult(true, "");
    }

    public static ChatGuardResult denied(final String messageKey) {
        return new ChatGuardResult(false, messageKey);
    }
}
