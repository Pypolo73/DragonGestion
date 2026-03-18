package fr.dragon.admincore.core;

import fr.dragon.admincore.chat.ChatService;
import fr.dragon.admincore.sanctions.SanctionService;
import fr.dragon.admincore.staffmode.StaffModeService;
import fr.dragon.admincore.vanish.VanishService;
import java.util.Objects;

public final class AdminCoreAPI {

    private static volatile AdminCorePlugin plugin;
    private static volatile SanctionService sanctions;
    private static volatile VanishService vanish;
    private static volatile StaffModeService staffMode;
    private static volatile ChatService chat;
    private static volatile PlayerSessionManager sessions;

    private AdminCoreAPI() {
    }

    public static void bind(
        final AdminCorePlugin adminCorePlugin,
        final SanctionService sanctionService,
        final VanishService vanishService,
        final StaffModeService staffModeService,
        final ChatService chatService,
        final PlayerSessionManager sessionManager
    ) {
        plugin = Objects.requireNonNull(adminCorePlugin, "adminCorePlugin");
        sanctions = Objects.requireNonNull(sanctionService, "sanctionService");
        vanish = Objects.requireNonNull(vanishService, "vanishService");
        staffMode = Objects.requireNonNull(staffModeService, "staffModeService");
        chat = Objects.requireNonNull(chatService, "chatService");
        sessions = Objects.requireNonNull(sessionManager, "sessionManager");
    }

    public static void clear() {
        plugin = null;
        sanctions = null;
        vanish = null;
        staffMode = null;
        chat = null;
        sessions = null;
    }

    public static AdminCorePlugin plugin() {
        return require(plugin, "plugin");
    }

    public static SanctionService sanctions() {
        return require(sanctions, "sanctions");
    }

    public static VanishService vanish() {
        return require(vanish, "vanish");
    }

    public static StaffModeService staffMode() {
        return require(staffMode, "staffMode");
    }

    public static ChatService chat() {
        return require(chat, "chat");
    }

    public static PlayerSessionManager sessions() {
        return require(sessions, "sessions");
    }

    private static <T> T require(final T value, final String label) {
        if (value == null) {
            throw new IllegalStateException("AdminCoreAPI non initialisee: " + label);
        }
        return value;
    }
}
