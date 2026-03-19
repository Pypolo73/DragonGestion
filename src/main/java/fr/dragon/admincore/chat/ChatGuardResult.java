package fr.dragon.admincore.chat;

public record ChatGuardResult(boolean allowed, String messageKey, String detail) {

    public static ChatGuardResult ok() {
        return new ChatGuardResult(true, "", "");
    }

    public static ChatGuardResult denied(final String messageKey) {
        return new ChatGuardResult(false, messageKey, "");
    }

    public static ChatGuardResult denied(final String messageKey, final String detail) {
        return new ChatGuardResult(false, messageKey, detail);
    }
}
