package fr.dragon.admincore.core;

import fr.dragon.admincore.sanctions.SanctionType;
import fr.dragon.admincore.util.ConfigLoader;
import fr.dragon.admincore.util.TimeParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

public final class StaffAccessService {

    private final JavaPlugin plugin;
    private final ConfigLoader configLoader;
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();
    private final Map<UUID, StaffRole> runtimeRoles = new ConcurrentHashMap<>();
    private final Map<UUID, StaffMemberRecord> members = new ConcurrentHashMap<>();
    private LuckPermsStaffBridge luckPermsBridge;
    private Path storageFile;

    public StaffAccessService(final JavaPlugin plugin, final ConfigLoader configLoader) {
        this.plugin = plugin;
        this.configLoader = configLoader;
        reloadStorage();
    }

    public void reloadStorage() {
        this.luckPermsBridge = new LuckPermsStaffBridge(this.plugin, this.configLoader);
        this.storageFile = this.plugin.getDataFolder().toPath().resolve(
            this.configLoader.config().getString("staff.storage-file", "staff-members.yml")
        );
        this.members.clear();
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(this.storageFile.toFile());
        final ConfigurationSection section = configuration.getConfigurationSection("members");
        if (section == null) {
            return;
        }
        for (final String key : section.getKeys(false)) {
            try {
                final UUID uuid = UUID.fromString(key);
                final String name = section.getString(key + ".name", key);
                final StaffRole role = StaffRole.fromInput(section.getString(key + ".role", "none"));
                if (role.isStaff()) {
                    this.members.put(uuid, new StaffMemberRecord(uuid, name, role));
                }
            } catch (final IllegalArgumentException ignored) {
            }
        }
    }

    public void refresh(final Player player) {
        clearRuntime(player.getUniqueId());
        final StaffRole role = resolveAssignedRole(player);
        this.runtimeRoles.put(player.getUniqueId(), role);
        mirrorRole(player, role);
        final boolean mutedInVoice = shouldMuteVoice(player, role);
        if (!role.isStaff() && !mutedInVoice) {
            return;
        }
        final PermissionAttachment attachment = player.addAttachment(this.plugin);
        if (role.isStaff()) {
            attachment.setPermission(PermissionService.STAFF, true);
            for (final String permission : permissionsFor(role)) {
                attachment.setPermission(permission, true);
            }
        }
        applyVoiceChatPermissions(player, role, attachment, mutedInVoice);
        this.attachments.put(player.getUniqueId(), attachment);
    }

    public void clearRuntime(final UUID uuid) {
        this.runtimeRoles.remove(uuid);
        final PermissionAttachment attachment = this.attachments.remove(uuid);
        if (attachment != null) {
            attachment.remove();
        }
    }

    public CompletableFuture<StaffAssignmentResult> assignStaffMember(final OfflinePlayer target, final StaffRole role) {
        if (target.getUniqueId() == null || !role.isStaff()) {
            return CompletableFuture.completedFuture(StaffAssignmentResult.NOT_FOUND);
        }
        if (this.luckPermsBridge != null && this.luckPermsBridge.blocksInternalFallback() && !this.luckPermsBridge.isAuthoritative()) {
            return CompletableFuture.completedFuture(StaffAssignmentResult.PROVIDER_UNAVAILABLE);
        }
        if (this.luckPermsBridge != null && this.luckPermsBridge.isAuthoritative()) {
            final String name = target.getName() == null ? target.getUniqueId().toString() : target.getName();
            return this.luckPermsBridge.assignRole(target.getUniqueId(), name, role).thenApply(result -> {
                if (result == StaffAssignmentResult.SUCCESS) {
                    mirrorStaffMember(target, role);
                    final Player online = target.getPlayer();
                    if (online != null) {
                        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> refresh(online));
                    }
                }
                return result;
            });
        }
        mirrorStaffMember(target, role);
        return CompletableFuture.completedFuture(StaffAssignmentResult.SUCCESS);
    }

    public CompletableFuture<StaffAssignmentResult> removeStaffMember(final OfflinePlayer target) {
        if (target.getUniqueId() == null) {
            return CompletableFuture.completedFuture(StaffAssignmentResult.NOT_FOUND);
        }
        if (this.luckPermsBridge != null && this.luckPermsBridge.blocksInternalFallback() && !this.luckPermsBridge.isAuthoritative()) {
            return CompletableFuture.completedFuture(StaffAssignmentResult.PROVIDER_UNAVAILABLE);
        }
        if (this.luckPermsBridge != null && this.luckPermsBridge.isAuthoritative()) {
            final String name = target.getName() == null ? target.getUniqueId().toString() : target.getName();
            return this.luckPermsBridge.removeStaffGroups(target.getUniqueId(), name).thenApply(result -> {
                if (result == StaffAssignmentResult.SUCCESS) {
                    removeMirror(target);
                    final Player online = target.getPlayer();
                    if (online != null) {
                        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> refresh(online));
                    }
                }
                return result;
            });
        }
        final boolean removed = removeMirror(target);
        return CompletableFuture.completedFuture(removed ? StaffAssignmentResult.SUCCESS : StaffAssignmentResult.NOT_FOUND);
    }

    private void mirrorStaffMember(final OfflinePlayer target, final StaffRole role) {
        final String name = target.getName() == null ? target.getUniqueId().toString() : target.getName();
        this.members.put(target.getUniqueId(), new StaffMemberRecord(target.getUniqueId(), name, role));
        saveMembers();
    }

    private boolean removeMirror(final OfflinePlayer target) {
        if (target.getUniqueId() == null) {
            return false;
        }
        final StaffMemberRecord removed = this.members.remove(target.getUniqueId());
        if (removed == null) {
            return false;
        }
        saveMembers();
        return true;
    }

    public List<StaffMemberRecord> members() {
        return this.members.values().stream()
            .sorted(Comparator.comparing((StaffMemberRecord entry) -> entry.role().ordinal()).reversed()
                .thenComparing(StaffMemberRecord::lastKnownName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public Optional<StaffMemberRecord> member(final UUID uuid) {
        return Optional.ofNullable(this.members.get(uuid));
    }

    public StaffRole roleOf(final Player player) {
        return this.runtimeRoles.computeIfAbsent(player.getUniqueId(), ignored -> resolveAssignedRole(player));
    }

    public boolean isStaff(final Player player) {
        return roleOf(player).isStaff();
    }

    public StaffRole requiredApproverRole(final Player player, final SanctionType type, final Duration duration) {
        final StaffRole role = roleOf(player);
        if (role == StaffRole.NONE || role == StaffRole.ADMIN) {
            return StaffRole.NONE;
        }
        if (role == StaffRole.GUIDE) {
            if (type == SanctionType.MUTE && duration != null && duration.compareTo(durationConfig("staff.approvals.guide.max-direct-tempmute", "1h")) <= 0) {
                return StaffRole.NONE;
            }
            if (type == SanctionType.WARN || type == SanctionType.KICK) {
                return StaffRole.NONE;
            }
            return StaffRole.MODERATOR;
        }
        if (role == StaffRole.MODERATOR && type == SanctionType.BAN) {
            if (duration == null && booleanConfig(true,
                "staff.approvals.moderator.permanent-ban-requires-admin",
                "staff.approvals.moderator-permanent-ban-requires-admin"
            )) {
                return StaffRole.ADMIN;
            }
            if (duration != null && duration.compareTo(durationConfig(
                "staff.approvals.moderator.max-direct-tempban",
                legacyDuration("staff.approvals.moderator-ban-over-days", "31d")
            )) > 0) {
                return StaffRole.ADMIN;
            }
        }
        return StaffRole.NONE;
    }

    public boolean canUseStaffMode(final Player player) {
        final StaffRole role = roleOf(player);
        return role.isStaff() || player.hasPermission(PermissionService.STAFFMODE);
    }

    public boolean canManageAssignments(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return true;
        }
        return roleOf(player).isAdmin() || player.hasPermission(PermissionService.ADMIN) || player.isOp();
    }

    public boolean canApprove(final Player player, final StaffRole requiredRole) {
        return player.hasPermission(PermissionService.ADMIN) || player.isOp() || roleOf(player).atLeast(requiredRole);
    }

    public StaffRole approverRoleOf(final CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return StaffRole.ADMIN;
        }
        if (player.hasPermission(PermissionService.ADMIN) || player.isOp()) {
            return StaffRole.ADMIN;
        }
        return roleOf(player);
    }

    public boolean isAdminApprover(final Player player) {
        return canApprove(player, StaffRole.ADMIN);
    }

    private StaffRole resolveAssignedRole(final Player player) {
        if (player.hasPermission(PermissionService.ADMIN) || player.isOp()) {
            return StaffRole.ADMIN;
        }
        if (this.luckPermsBridge != null && this.luckPermsBridge.blocksInternalFallback() && !this.luckPermsBridge.isAuthoritative()) {
            return StaffRole.NONE;
        }
        if (this.luckPermsBridge != null && this.luckPermsBridge.isAuthoritative()) {
            return this.luckPermsBridge.resolveRole(player).orElse(StaffRole.NONE);
        }
        final StaffMemberRecord record = this.members.get(player.getUniqueId());
        return record == null ? StaffRole.NONE : record.role();
    }

    private void mirrorRole(final Player player, final StaffRole role) {
        if (this.luckPermsBridge == null || !this.luckPermsBridge.isAuthoritative()) {
            return;
        }
        final UUID uuid = player.getUniqueId();
        if (!role.isStaff()) {
            final StaffMemberRecord removed = this.members.remove(uuid);
            if (removed != null) {
                saveMembers();
            }
            return;
        }
        final StaffMemberRecord previous = this.members.get(uuid);
        final StaffMemberRecord next = new StaffMemberRecord(uuid, player.getName(), role);
        if (!next.equals(previous)) {
            this.members.put(uuid, next);
            saveMembers();
        }
    }

    private List<String> permissionsFor(final StaffRole role) {
        final ConfigurationSection section = this.configLoader.config().getConfigurationSection("staff.roles.permissions");
        if (section == null) {
            return List.of();
        }
        return switch (role) {
            case GUIDE -> section.getStringList("guide");
            case MODERATOR -> section.getStringList("moderator");
            case ADMIN -> section.getStringList("admin");
            case NONE -> List.of();
        };
    }

    private void applyVoiceChatPermissions(
        final Player player,
        final StaffRole role,
        final PermissionAttachment attachment,
        final boolean mutedInVoice
    ) {
        if (this.configLoader.config().getBoolean("staff.voice-chat.enabled", true) && roleAllowedForVoice(role)) {
            for (final String permission : this.configLoader.config().getStringList("staff.voice-chat.permissions")) {
                if (!permission.isBlank()) {
                    attachment.setPermission(permission, true);
                }
            }
        }
        if (mutedInVoice) {
            for (final String permission : this.configLoader.config().getStringList("staff.voice-chat.mute-permissions")) {
                if (!permission.isBlank()) {
                    attachment.setPermission(permission, false);
                }
            }
        }
    }

    private boolean shouldMuteVoice(final Player player, final StaffRole role) {
        if (!this.configLoader.config().getBoolean("staff.voice-chat.enabled", true)) {
            return false;
        }
        final var mute = AdminCoreAPI.sanctions().getCachedMute(player.getUniqueId());
        if (mute.isEmpty()) {
            return false;
        }
        final boolean temporary = mute.get().expiresAt() != null;
        if (temporary) {
            return this.configLoader.config().getBoolean("staff.voice-chat.sync-tempmute", true);
        }
        return this.configLoader.config().getBoolean("staff.voice-chat.sync-mute", true);
    }

    private boolean roleAllowedForVoice(final StaffRole role) {
        if (!role.isStaff()) {
            return false;
        }
        final List<String> configuredRoles = this.configLoader.config().getStringList("staff.voice-chat.auto-allow-roles");
        if (configuredRoles.isEmpty()) {
            return true;
        }
        return configuredRoles.stream()
            .map(value -> value.toUpperCase(Locale.ROOT))
            .anyMatch(value -> value.equals(role.name()));
    }

    private Duration durationConfig(final String path, final String fallback) {
        return TimeParser.parse(this.configLoader.config().getString(path, fallback)).orElse(Duration.ofHours(1));
    }

    private String legacyDuration(final String legacyDaysPath, final String fallback) {
        if (this.configLoader.config().contains(legacyDaysPath)) {
            return Math.max(0, this.configLoader.config().getInt(legacyDaysPath, 31)) + "d";
        }
        return fallback;
    }

    private boolean booleanConfig(final boolean fallback, final String... paths) {
        for (final String path : paths) {
            if (this.configLoader.config().contains(path)) {
                return this.configLoader.config().getBoolean(path, fallback);
            }
        }
        return fallback;
    }

    private void saveMembers() {
        final YamlConfiguration configuration = new YamlConfiguration();
        for (final StaffMemberRecord member : this.members.values()) {
            final String path = "members." + member.uuid();
            configuration.set(path + ".name", member.lastKnownName());
            configuration.set(path + ".role", member.role().name());
        }
        try {
            Files.createDirectories(this.storageFile.getParent());
            configuration.save(this.storageFile.toFile());
        } catch (final IOException exception) {
            throw new IllegalStateException("Impossible d'enregistrer les membres du staff", exception);
        }
    }
}
