package fr.dragon.admincore.core;

import fr.dragon.admincore.util.ConfigLoader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

final class LuckPermsStaffBridge {

    private final JavaPlugin plugin;
    private final ConfigLoader configLoader;

    LuckPermsStaffBridge(final JavaPlugin plugin, final ConfigLoader configLoader) {
        this.plugin = plugin;
        this.configLoader = configLoader;
        if (providerMode() == ProviderMode.LUCKPERMS && !isLuckPermsAvailable()) {
            this.plugin.getLogger().warning("LuckPerms est requis par la configuration staff.provider=LUCKPERMS, mais le plugin est absent.");
        }
    }

    boolean isAuthoritative() {
        return isEnabled() && isLuckPermsAvailable();
    }

    boolean isEnabled() {
        return providerMode() != ProviderMode.INTERNAL;
    }

    boolean blocksInternalFallback() {
        return providerMode() == ProviderMode.LUCKPERMS;
    }

    Optional<StaffRole> resolveRole(final Player player) {
        if (!isAuthoritative()) {
            return Optional.empty();
        }
        try {
            final Object user = loadedUser(player.getUniqueId());
            if (user == null) {
                return Optional.empty();
            }
            StaffRole resolved = roleFromGroup(primaryGroup(user));
            for (final String groupName : directGroups(user)) {
                final StaffRole candidate = roleFromGroup(groupName);
                if (candidate.atLeast(resolved)) {
                    resolved = candidate;
                }
            }
            return resolved == StaffRole.NONE ? Optional.empty() : Optional.of(resolved);
        } catch (final Exception exception) {
            this.plugin.getLogger().warning("Resolution LuckPerms impossible pour " + player.getUniqueId() + ": " + exception.getMessage());
            return Optional.empty();
        }
    }

    CompletableFuture<StaffAssignmentResult> assignRole(final UUID uuid, final String username, final StaffRole role) {
        if (!isEnabled() || !isLuckPermsAvailable()) {
            return CompletableFuture.completedFuture(StaffAssignmentResult.PROVIDER_UNAVAILABLE);
        }
        final String assignmentGroup = assignmentGroup(role);
        if (assignmentGroup == null || assignmentGroup.isBlank()) {
            return CompletableFuture.completedFuture(StaffAssignmentResult.CONFIGURATION_ERROR);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                final Object api = luckPermsApi();
                final Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
                final Object user = loadUser(userManager, uuid, username);
                if (user == null) {
                    return StaffAssignmentResult.NOT_FOUND;
                }
                final Object data = user.getClass().getMethod("data").invoke(user);
                final Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node");
                for (final Object node : new ArrayList<>(directInheritanceNodes(user))) {
                    final String groupName = inheritanceGroupName(node);
                    if (mappedGroups().contains(groupName)) {
                        data.getClass().getMethod("remove", nodeClass).invoke(data, node);
                    }
                }
                data.getClass().getMethod("add", nodeClass).invoke(data, buildInheritanceNode(assignmentGroup));
                if (this.configLoader.config().getBoolean("staff.luckperms.sync-primary-group", false)) {
                    trySetPrimaryGroup(user, assignmentGroup);
                }
                saveUser(userManager, user);
                return StaffAssignmentResult.SUCCESS;
            } catch (final Exception exception) {
                this.plugin.getLogger().warning("Assignation LuckPerms impossible pour " + uuid + ": " + exception.getMessage());
                return StaffAssignmentResult.ERROR;
            }
        });
    }

    CompletableFuture<StaffAssignmentResult> removeStaffGroups(final UUID uuid, final String username) {
        if (!isEnabled() || !isLuckPermsAvailable()) {
            return CompletableFuture.completedFuture(StaffAssignmentResult.PROVIDER_UNAVAILABLE);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                final Object api = luckPermsApi();
                final Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
                final Object user = loadUser(userManager, uuid, username);
                if (user == null) {
                    return StaffAssignmentResult.NOT_FOUND;
                }
                final Object data = user.getClass().getMethod("data").invoke(user);
                final Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node");
                boolean removed = false;
                for (final Object node : new ArrayList<>(directInheritanceNodes(user))) {
                    final String groupName = inheritanceGroupName(node);
                    if (mappedGroups().contains(groupName)) {
                        data.getClass().getMethod("remove", nodeClass).invoke(data, node);
                        removed = true;
                    }
                }
                if (!removed) {
                    return StaffAssignmentResult.NOT_FOUND;
                }
                saveUser(userManager, user);
                return StaffAssignmentResult.SUCCESS;
            } catch (final Exception exception) {
                this.plugin.getLogger().warning("Suppression LuckPerms impossible pour " + uuid + ": " + exception.getMessage());
                return StaffAssignmentResult.ERROR;
            }
        });
    }

    private ProviderMode providerMode() {
        final String raw = this.configLoader.config().getString("staff.provider", "AUTO");
        return ProviderMode.from(raw);
    }

    private boolean isLuckPermsAvailable() {
        final Plugin dependency = Bukkit.getPluginManager().getPlugin("LuckPerms");
        return dependency != null && dependency.isEnabled();
    }

    private Object luckPermsApi() throws Exception {
        final Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
        return provider.getMethod("get").invoke(null);
    }

    private Object loadedUser(final UUID uuid) throws Exception {
        final Object api = luckPermsApi();
        final Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
        return userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, uuid);
    }

    private Object loadUser(final Object userManager, final UUID uuid, final String username) throws Exception {
        final Method loadUser = findLoadUserMethod(userManager.getClass());
        if (loadUser == null) {
            throw new IllegalStateException("Methode loadUser introuvable dans LuckPerms");
        }
        final Object futureResult = loadUser.getParameterCount() == 2
            ? loadUser.invoke(userManager, uuid, username)
            : loadUser.invoke(userManager, uuid);
        @SuppressWarnings("unchecked")
        final CompletableFuture<Object> future = (CompletableFuture<Object>) futureResult;
        return future.get(10, TimeUnit.SECONDS);
    }

    private Method findLoadUserMethod(final Class<?> type) {
        for (final Method method : type.getMethods()) {
            if (!method.getName().equals("loadUser")) {
                continue;
            }
            final Class<?>[] parameters = method.getParameterTypes();
            if (parameters.length == 1 && parameters[0] == UUID.class) {
                return method;
            }
            if (parameters.length == 2 && parameters[0] == UUID.class && parameters[1] == String.class) {
                return method;
            }
        }
        return null;
    }

    private void saveUser(final Object userManager, final Object user) throws Exception {
        final Class<?> userClass = Class.forName("net.luckperms.api.model.user.User");
        userManager.getClass().getMethod("saveUser", userClass).invoke(userManager, user);
    }

    private String primaryGroup(final Object user) throws Exception {
        return normalizeGroup(String.valueOf(user.getClass().getMethod("getPrimaryGroup").invoke(user)));
    }

    private List<String> directGroups(final Object user) throws Exception {
        final List<String> groups = new ArrayList<>();
        for (final Object node : directInheritanceNodes(user)) {
            final String groupName = inheritanceGroupName(node);
            if (groupName != null && !groupName.isBlank()) {
                groups.add(groupName);
            }
        }
        return groups;
    }

    private List<Object> directInheritanceNodes(final Object user) throws Exception {
        final Collection<?> nodes = (Collection<?>) user.getClass().getMethod("getNodes").invoke(user);
        final List<Object> result = new ArrayList<>();
        for (final Object node : nodes) {
            if (hasMethod(node.getClass(), "getGroupName")) {
                result.add(node);
            }
        }
        return result;
    }

    private Object buildInheritanceNode(final String groupName) throws Exception {
        final Class<?> inheritanceNode = Class.forName("net.luckperms.api.node.types.InheritanceNode");
        final Object builder = inheritanceNode.getMethod("builder", String.class).invoke(null, groupName);
        return builder.getClass().getMethod("build").invoke(builder);
    }

    private void trySetPrimaryGroup(final Object user, final String groupName) {
        try {
            user.getClass().getMethod("setPrimaryGroup", String.class).invoke(user, groupName);
        } catch (final Exception ignored) {
        }
    }

    private boolean hasMethod(final Class<?> type, final String name) {
        for (final Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 0) {
                return true;
            }
        }
        return false;
    }

    private String inheritanceGroupName(final Object node) {
        try {
            return normalizeGroup(String.valueOf(node.getClass().getMethod("getGroupName").invoke(node)));
        } catch (final Exception exception) {
            return null;
        }
    }

    private StaffRole roleFromGroup(final String groupName) {
        if (groupName == null || groupName.isBlank()) {
            return StaffRole.NONE;
        }
        StaffRole resolved = StaffRole.NONE;
        for (final Map.Entry<StaffRole, List<String>> entry : roleGroups().entrySet()) {
            if (entry.getValue().contains(groupName) && entry.getKey().atLeast(resolved)) {
                resolved = entry.getKey();
            }
        }
        return resolved;
    }

    private String assignmentGroup(final StaffRole role) {
        final ConfigurationSection section = this.configLoader.config().getConfigurationSection("staff.luckperms.assignment-groups");
        if (section == null) {
            return null;
        }
        return normalizeGroup(switch (role) {
            case GUIDE -> section.getString("guide", "guide");
            case MODERATOR -> section.getString("moderator", "modo");
            case ADMIN -> section.getString("admin", "admin");
            case NONE -> null;
        });
    }

    private Map<StaffRole, List<String>> roleGroups() {
        final Map<StaffRole, List<String>> groups = new EnumMap<>(StaffRole.class);
        final ConfigurationSection section = this.configLoader.config().getConfigurationSection("staff.luckperms.role-groups");
        groups.put(StaffRole.GUIDE, normalize(section == null ? List.of("guide") : section.getStringList("guide")));
        groups.put(StaffRole.MODERATOR, normalize(section == null ? List.of("modo", "modo+", "moderator") : section.getStringList("moderator")));
        groups.put(StaffRole.ADMIN, normalize(section == null ? List.of("admin", "owner") : section.getStringList("admin")));
        return groups;
    }

    private Set<String> mappedGroups() {
        final java.util.Set<String> mapped = new java.util.HashSet<>();
        roleGroups().values().forEach(mapped::addAll);
        return mapped;
    }

    private List<String> normalize(final List<String> values) {
        return values.stream()
            .map(this::normalizeGroup)
            .filter(value -> value != null && !value.isBlank())
            .distinct()
            .toList();
    }

    private String normalizeGroup(final String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private enum ProviderMode {
        INTERNAL,
        AUTO,
        LUCKPERMS;

        static ProviderMode from(final String raw) {
            if (raw == null) {
                return AUTO;
            }
            return switch (raw.trim().toUpperCase(Locale.ROOT)) {
                case "INTERNAL" -> INTERNAL;
                case "LUCKPERMS" -> LUCKPERMS;
                default -> AUTO;
            };
        }
    }
}
