package fr.dragon.admincore.luckperms;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.StaffActionType;
import fr.dragon.admincore.dialog.DialogHelper;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;

public final class LuckPermsUiService {

    private static final int PLUGINS_PER_PAGE = 10;
    private static final int PERMISSIONS_PER_PAGE = 12;
    private static final int PRIMARY_BUTTON_WIDTH = 210;
    private static final int STATUS_BUTTON_WIDTH = 70;
    private static final int ACTION_BUTTON_WIDTH = 140;
    private static final int DIALOG_COLUMNS = 4;

    private final AdminCorePlugin plugin;
    private final Map<UUID, LuckPermsDialogSession> sessions = new java.util.concurrent.ConcurrentHashMap<>();

    public LuckPermsUiService(final AdminCorePlugin plugin) {
        this.plugin = plugin;
        if (!isAvailable()) {
            this.plugin.getLogger().warning("LuckPerms absent: /staffluckperm restera indisponible.");
        }
    }

    public boolean isAvailable() {
        final Plugin dependency = Bukkit.getPluginManager().getPlugin("LuckPerms");
        return dependency != null && dependency.isEnabled();
    }

    public void openGroupSelection(final Player player) {
        if (!isAvailable()) {
            player.sendMessage(this.plugin.getMessageFormatter().message("luckperms.unavailable"));
            return;
        }
        final List<String> groups = api().getGroupManager().getLoadedGroups().stream()
            .map(Group::getName)
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
        if (groups.isEmpty()) {
            player.sendMessage(this.plugin.getMessageFormatter().deserialize("<prefix><red>Aucun groupe LuckPerms charge.</red>"));
            return;
        }
        final List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();
        for (int index = 0; index < groups.size(); index++) {
            final String group = groups.get(index);
            entries.add(SingleOptionDialogInput.OptionEntry.create(group, Component.text(group), index == 0));
        }
        final Dialog dialog = DialogHelper.create(
            Component.text("Choisir un groupe"),
            List.of(DialogBody.plainMessage(Component.text("Selectionne le groupe LuckPerms a modifier."), 320)),
            List.of(DialogInput.singleOption("group", 180, entries, Component.text("Groupe"), true)),
            DialogType.notice(DialogHelper.button(
                Component.text("Ouvrir"),
                150,
                DialogAction.customClick((response, audience) -> {
                    final String groupName = response.getText("group");
                    if (groupName == null || groupName.isBlank()) {
                        player.sendMessage(this.plugin.getMessageFormatter().message("luckperms.unavailable"));
                        return;
                    }
                    session(player).clearAll();
                    session(player).groupName(groupName);
                    openPlugins(player, groupName, 0);
                }, DialogHelper.singleUseOptions())
            ))
        );
        player.showDialog(dialog);
    }

    public void openPlugins(final Player player, final String groupName, final int page) {
        final Group group = api().getGroupManager().getGroup(groupName);
        if (group == null) {
            player.sendMessage(this.plugin.getMessageFormatter().message("luckperms.group-missing"));
            return;
        }
        session(player).groupName(groupName);
        final List<PluginPermissions> plugins = pluginPermissions().stream().sorted(Comparator.comparing(PluginPermissions::name, String.CASE_INSENSITIVE_ORDER)).toList();
        final int from = Math.max(0, page) * PLUGINS_PER_PAGE;
        if (from >= plugins.size() && !plugins.isEmpty()) {
            openPlugins(player, groupName, Math.max(0, page - 1));
            return;
        }
        final int to = Math.min(plugins.size(), from + PLUGINS_PER_PAGE);
        final List<ActionButton> actions = new ArrayList<>();
        actions.add(navButton("Revenir", () -> openGroupSelection(player)));
        for (final PluginPermissions pluginPermissions : plugins.subList(from, to)) {
            actions.add(primaryRowButton(pluginPermissions.name(), () -> openPermissionLevel(player, groupName, pluginPermissions.name(), "", 0)));
            actions.add(statusRowButton(statusBadge(statusLabel(groupStatus(group, pluginPermissions.permissions()))), () -> openPlugins(player, groupName, page)));
        }
        if (page > 0) {
            actions.add(navButton("Page precedente", () -> openPlugins(player, groupName, page - 1)));
        }
        if (to < plugins.size()) {
            actions.add(navButton("Page suivante", () -> openPlugins(player, groupName, page + 1)));
        }
        addSpacerRow(actions);
        addSessionButtons(player, groupName, actions, () -> openPlugins(player, groupName, page));
        player.showDialog(DialogHelper.create(
            Component.text("UiLuckPerm — " + groupName),
            List.of(
                DialogBody.plainMessage(Component.text("Choisis un plugin puis navigue dans ses permissions."), 360),
                DialogBody.plainMessage(Component.empty(), 360)
            ),
            List.of(),
            DialogType.multiAction(actions, null, DIALOG_COLUMNS)
        ));
    }

    public void openPermissionLevel(final Player player, final String groupName, final String pluginName, final String path, final int page) {
        final Group group = api().getGroupManager().getGroup(groupName);
        if (group == null) {
            player.sendMessage(this.plugin.getMessageFormatter().message("luckperms.group-missing"));
            return;
        }
        final PluginPermissions pluginPermissions = pluginPermissions().stream()
            .filter(entry -> entry.name().equals(pluginName))
            .findFirst()
            .orElse(null);
        if (pluginPermissions == null) {
            player.sendMessage(this.plugin.getMessageFormatter().message("luckperms.plugin-missing"));
            return;
        }
        final List<NodeEntry> entries = nodeEntries(pluginPermissions.permissions(), path);
        final int from = Math.max(0, page) * PERMISSIONS_PER_PAGE;
        if (from >= entries.size() && !entries.isEmpty()) {
            openPermissionLevel(player, groupName, pluginName, path, Math.max(0, page - 1));
            return;
        }
        final int to = Math.min(entries.size(), from + PERMISSIONS_PER_PAGE);
        final List<ActionButton> actions = new ArrayList<>();
        actions.add(navButton("Revenir", () -> openPlugins(player, groupName, 0)));
        for (final NodeEntry entry : entries.subList(from, to)) {
            actions.add(primaryRowButton(entry.displayNode(), () -> {
                if (entry.hasChildren()) {
                    openPermissionLevel(player, groupName, pluginName, entry.displayNode(), 0);
                }
            }));
            actions.add(statusRowButton(statusBadge(permissionLabel(group, entry)), () -> {
                if (entry.exactNode() == null) {
                    return;
                }
                togglePermission(player, groupName, entry.exactNode(), pluginName, path, page);
            }));
        }
        if (page > 0) {
            actions.add(navButton("Page precedente", () -> openPermissionLevel(player, groupName, pluginName, path, page - 1)));
        }
        if (to < entries.size()) {
            actions.add(navButton("Page suivante", () -> openPermissionLevel(player, groupName, pluginName, path, page + 1)));
        }
        addSpacerRow(actions);
        addSessionButtons(player, groupName, actions, () -> openPermissionLevel(player, groupName, pluginName, path, page));
        player.showDialog(DialogHelper.create(
            Component.text("UiLuckPerm — " + groupName),
            List.of(
                DialogBody.plainMessage(Component.text(pluginName + (path.isBlank() ? "" : " / " + path)), 360),
                DialogBody.plainMessage(Component.empty(), 360)
            ),
            List.of(),
            DialogType.multiAction(actions, null, DIALOG_COLUMNS)
        ));
    }

    private void togglePermission(final Player player, final String groupName, final String node, final String pluginName, final String path, final int page) {
        final Group group = api().getGroupManager().getGroup(groupName);
        if (group == null) {
            player.sendMessage(this.plugin.getMessageFormatter().message("luckperms.group-missing"));
            return;
        }
        final boolean before = currentValue(group, node);
        final boolean after = !before;
        applyValue(group, node, after).thenRun(() -> {
            final PermissionAction action = new PermissionAction(group.getName(), node, before, after);
            session(player).pushUndo(action);
            logChange(player, action, "toggle");
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> openPermissionLevel(player, group.getName(), pluginName, path, page));
        }).exceptionally(throwable -> handleLuckPermsFailure(player, throwable));
    }

    private void undo(final Player player, final Runnable reopen) {
        final Deque<PermissionAction> undo = session(player).undo();
        final PermissionAction action = undo.pollLast();
        if (action == null) {
            reopen.run();
            return;
        }
        final Group group = api().getGroupManager().getGroup(action.group());
        if (group == null) {
            player.sendMessage(this.plugin.getMessageFormatter().message("luckperms.group-missing"));
            return;
        }
        applyValue(group, action.node(), action.before()).thenRun(() -> {
            session(player).redo().addLast(action);
            logChange(player, action, "undo");
            this.plugin.getServer().getScheduler().runTask(this.plugin, reopen);
        }).exceptionally(throwable -> handleLuckPermsFailure(player, throwable));
    }

    private void redo(final Player player, final Runnable reopen) {
        final Deque<PermissionAction> redo = session(player).redo();
        final PermissionAction action = redo.pollLast();
        if (action == null) {
            reopen.run();
            return;
        }
        final Group group = api().getGroupManager().getGroup(action.group());
        if (group == null) {
            player.sendMessage(this.plugin.getMessageFormatter().message("luckperms.group-missing"));
            return;
        }
        applyValue(group, action.node(), action.after()).thenRun(() -> {
            session(player).undo().addLast(action);
            logChange(player, action, "redo");
            this.plugin.getServer().getScheduler().runTask(this.plugin, reopen);
        }).exceptionally(throwable -> handleLuckPermsFailure(player, throwable));
    }

    private void save(final Player player, final String groupName, final Runnable reopen) {
        final Group group = api().getGroupManager().getGroup(groupName);
        if (group == null) {
            player.sendMessage(this.plugin.getMessageFormatter().message("luckperms.group-missing"));
            return;
        }
        api().getGroupManager().saveGroup(group).thenRun(() -> {
            session(player).undo().clear();
            session(player).redo().clear();
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                player.sendMessage(this.plugin.getMessageFormatter().message("luckperms.saved"));
                reopen.run();
            });
        }).exceptionally(throwable -> {
            this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
                player.sendMessage(this.plugin.getMessageFormatter().message("errors.database"))
            );
            return null;
        });
    }

    private void cancel(final Player player) {
        final LuckPermsDialogSession session = session(player);
        final String groupName = session.groupName();
        final Group group = groupName == null ? null : api().getGroupManager().getGroup(groupName);
        if (group == null) {
            session.clearAll();
            return;
        }
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        while (!session.undo().isEmpty()) {
            final PermissionAction action = session.undo().pollLast();
            future = future.thenCompose(ignored -> applyValue(group, action.node(), action.before()).thenRun(() ->
                logChange(player, action, "cancel")
            ));
        }
        future.thenRun(() -> this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            session.redo().clear();
            session.groupName(null);
            player.closeInventory();
        })).exceptionally(throwable -> handleLuckPermsFailure(player, throwable));
    }

    private void addSessionButtons(final Player player, final String groupName, final List<ActionButton> actions, final Runnable reopen) {
        actions.add(globalActionButton(Component.text("Annuler derniere action", NamedTextColor.GRAY), () -> undo(player, reopen)));
        actions.add(globalActionButton(Component.text("Retablir", NamedTextColor.GRAY), () -> redo(player, reopen)));
        actions.add(globalActionButton(Component.text("Enregistrer", NamedTextColor.BLUE), () -> save(player, groupName, reopen)));
        actions.add(globalActionButton(Component.text("Annuler", NamedTextColor.RED), () -> cancel(player)));
    }

    private CompletableFuture<Void> applyValue(final Group group, final String node, final boolean value) {
        return CompletableFuture.runAsync(() -> {
            final List<PermissionNode> existing = new ArrayList<>(group.getNodes(NodeType.PERMISSION).stream()
                .filter(permissionNode -> permissionNode.getPermission().equalsIgnoreCase(node))
                .toList());
            for (final PermissionNode permissionNode : existing) {
                group.data().remove(permissionNode);
            }
            group.data().add(PermissionNode.builder(node).value(value).build());
            group.getCachedData().invalidate();
        });
    }

    private boolean currentValue(final Group group, final String node) {
        final PermissionNode direct = group.getNodes(NodeType.PERMISSION).stream()
            .filter(permissionNode -> permissionNode.getPermission().equalsIgnoreCase(node))
            .findFirst()
            .orElse(null);
        if (direct != null) {
            return direct.getValue();
        }
        return group.getCachedData().getPermissionData().checkPermission(node).asBoolean();
    }

    private String groupStatus(final Group group, final Collection<String> permissions) {
        boolean anyTrue = false;
        boolean anyFalse = false;
        for (final String permission : permissions) {
            if (currentValue(group, permission)) {
                anyTrue = true;
            } else {
                anyFalse = true;
            }
            if (anyTrue && anyFalse) {
                return "Partiel";
            }
        }
        if (anyTrue) {
            return "Accorde";
        }
        return "Refuse";
    }

    private String statusLabel(final String value) {
        return switch (value) {
            case "Accorde" -> "Accorde";
            case "Partiel" -> "Partiel";
            default -> "Refuse";
        };
    }

    private String permissionLabel(final Group group, final NodeEntry entry) {
        if (entry.exactNode() == null) {
            return entry.hasChildren() ? "..." : "FALSE";
        }
        return currentValue(group, entry.exactNode()) ? "TRUE" : "FALSE";
    }

    private List<PluginPermissions> pluginPermissions() {
        final List<PluginPermissions> result = new ArrayList<>();
        for (final Plugin installed : Bukkit.getPluginManager().getPlugins()) {
            final Set<String> permissions = new LinkedHashSet<>();
            for (final Permission permission : installed.getDescription().getPermissions()) {
                collectPermissions(permission, permissions);
            }
            result.add(new PluginPermissions(installed.getName(), permissions));
        }
        return result;
    }

    private void collectPermissions(final Permission permission, final Set<String> permissions) {
        permissions.add(permission.getName());
        permissions.addAll(permission.getChildren().keySet());
    }

    private List<NodeEntry> nodeEntries(final Collection<String> permissions, final String path) {
        final Map<String, String> exactNodes = new LinkedHashMap<>();
        final Map<String, Boolean> children = new LinkedHashMap<>();
        final Set<String> displayNodes = new LinkedHashSet<>();
        final String prefix = path == null || path.isBlank() ? "" : path + ".";
        for (final String permission : permissions.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList()) {
            if (!prefix.isEmpty() && !permission.startsWith(prefix) && !permission.equalsIgnoreCase(path)) {
                continue;
            }
            final String remaining = prefix.isEmpty() ? permission : permission.substring(Math.min(prefix.length(), permission.length()));
            final String segment = remaining.contains(".") ? remaining.substring(0, remaining.indexOf('.')) : remaining;
            final String display = prefix.isEmpty() ? segment : prefix + segment;
            if (display.isBlank()) {
                continue;
            }
            displayNodes.add(display);
            if (permission.equalsIgnoreCase(display)) {
                exactNodes.put(display, permission);
            } else if (permission.startsWith(display + ".")) {
                children.put(display, true);
            }
        }
        return displayNodes.stream()
            .map(key -> new NodeEntry(key, exactNodes.get(key), children.getOrDefault(key, false)))
            .toList();
    }

    private void logChange(final Player player, final PermissionAction action, final String source) {
        this.plugin.getStaffActionLogger().log(
            player,
            StaffActionType.LUCKPERMS_EDIT,
            null,
            action.group(),
            source + " | " + action.group() + " | " + action.node() + " | " + action.before() + " -> " + action.after()
        );
    }

    private ActionButton navButton(final String label, final Runnable runnable) {
        return actionButton(label, PRIMARY_BUTTON_WIDTH, runnable);
    }

    private ActionButton primaryRowButton(final String label, final Runnable runnable) {
        return actionButton(label, PRIMARY_BUTTON_WIDTH, runnable);
    }

    private ActionButton statusRowButton(final String label, final Runnable runnable) {
        return actionButton(Component.text(label, NamedTextColor.GRAY), STATUS_BUTTON_WIDTH, runnable);
    }

    private ActionButton globalActionButton(final Component label, final Runnable runnable) {
        return actionButton(label, ACTION_BUTTON_WIDTH, runnable);
    }

    private ActionButton actionButton(final String label, final int width, final Runnable runnable) {
        return actionButton(Component.text(label), width, runnable);
    }

    private ActionButton actionButton(final Component label, final int width, final Runnable runnable) {
        return DialogHelper.button(
            label,
            width,
            DialogAction.customClick((response, audience) -> runnable.run(), DialogHelper.singleUseOptions())
        );
    }

    private void addSpacerRow(final List<ActionButton> actions) {
        for (int index = 0; index < DIALOG_COLUMNS; index++) {
            actions.add(actionButton(Component.text(" "), 1, () -> {
            }));
        }
    }

    private String statusBadge(final String value) {
        return switch (value) {
            case "TRUE" -> "[TRUE]";
            case "FALSE" -> "[FALSE]";
            case "Accorde" -> "[Accorde]";
            case "Refuse" -> "[Refuse]";
            case "Partiel" -> "[Partiel]";
            default -> "[" + value + "]";
        };
    }

    private Void handleLuckPermsFailure(final Player player, final Throwable throwable) {
        this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
            player.sendMessage(this.plugin.getMessageFormatter().message("errors.database"))
        );
        return null;
    }

    private LuckPermsDialogSession session(final Player player) {
        return this.sessions.computeIfAbsent(player.getUniqueId(), LuckPermsDialogSession::new);
    }

    private LuckPerms api() {
        return LuckPermsProvider.get();
    }

    private record PluginPermissions(String name, Collection<String> permissions) {
    }

    private record NodeEntry(String displayNode, String exactNode, boolean hasChildren) {
    }
}
