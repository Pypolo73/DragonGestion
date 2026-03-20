package fr.dragon.admincore.sanctions;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.PermissionService;
import fr.dragon.admincore.core.StaffActionType;
import fr.dragon.admincore.database.NoteRepository;
import fr.dragon.admincore.gui.ActiveSanctionsMenu;
import fr.dragon.admincore.gui.HistoryMenu;
import fr.dragon.admincore.lookup.LookupMenus;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class SanctionsAdminCommand implements CommandExecutor, TabCompleter {

    private final AdminCorePlugin plugin;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    public SanctionsAdminCommand(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        final String name = command.getName().toLowerCase(Locale.ROOT);
        if (!this.plugin.getPermissionService().check(sender, permission(name))) {
            return true;
        }
        if (args.length == 0 && !"stafflist".equals(name)) {
            sender.sendMessage("/" + name + " <joueur>");
            return true;
        }
        return switch (name) {
            case "unban" -> handleRevoke(sender, args[0], SanctionType.BAN);
            case "unmute" -> handleRevoke(sender, args[0], SanctionType.MUTE);
            case "warnings" -> handleWarnings(sender, args[0]);
            case "clearwarns" -> handleClearWarns(sender, args[0]);
            case "history" -> handleHistory(sender, args[0]);
            case "lookup" -> handleLookup(sender, args[0]);
            case "alts" -> handleAlts(sender, args[0]);
            case "note" -> handleNote(sender, args);
            case "notes" -> handleNotes(sender, args[0]);
            case "ipban" -> handleIpBan(sender, args[0]);
            case "checkvpn" -> handleCheckVpn(sender, args[0]);
            default -> true;
        };
    }

    private boolean handleRevoke(final CommandSender sender, final String targetName, final SanctionType type) {
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        this.plugin.getSanctionService().revoke(type, target.getUniqueId(), targetName).thenAccept(updated ->
            sync(() -> {
                if (updated <= 0) {
                    sender.sendMessage(this.plugin.getMessageFormatter().message(
                        "sanctions.revoke-none",
                        this.plugin.getMessageFormatter().text("type", type.name()),
                        this.plugin.getMessageFormatter().text("target", targetName)
                    ));
                    return;
                }
                sender.sendMessage(this.plugin.getMessageFormatter().message(
                    "sanctions.revoked",
                    this.plugin.getMessageFormatter().text("type", type.name()),
                    this.plugin.getMessageFormatter().text("target", targetName)
                ));
                this.plugin.getStaffActionLogger().log(
                    sender,
                    StaffActionType.SANCTION_REVOKE,
                    target.getUniqueId(),
                    targetName,
                    "Levee " + type.name()
                );
                final Player online = target.getPlayer();
                if (online != null) {
                    this.plugin.getStaffAccessService().refresh(online);
                }
            })
        );
        return true;
    }

    private boolean handleWarnings(final CommandSender sender, final String targetName) {
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        this.plugin.getSanctionService().activeWarnings(target.getUniqueId(), targetName).thenAccept(warnings ->
            sync(() -> {
                if (sender instanceof Player player) {
                    ActiveSanctionsMenu.openWarnings(player, targetName, warnings);
                } else {
                    warnings.forEach(warning -> sender.sendMessage("- " + warning.reason() + " (" + warning.createdAt() + ")"));
                }
            })
        );
        return true;
    }

    private boolean handleClearWarns(final CommandSender sender, final String targetName) {
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        this.plugin.getSanctionService().clearWarnings(target.getUniqueId(), targetName).thenAccept(updated ->
            sync(() -> sender.sendMessage(this.plugin.getMessageFormatter().message(
                "sanctions.clearwarns",
                this.plugin.getMessageFormatter().text("target", targetName)
            )))
        );
        return true;
    }

    private boolean handleHistory(final CommandSender sender, final String targetName) {
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        this.plugin.getSanctionService().history(target.getUniqueId(), targetName, 54).thenAccept(history ->
            sync(() -> {
                if (sender instanceof Player player) {
                    HistoryMenu.open(player, targetName, history);
                } else {
                    history.forEach(entry -> sender.sendMessage("- " + entry.type() + " / " + entry.reason()));
                }
            })
        );
        return true;
    }

    private boolean handleLookup(final CommandSender sender, final String target) {
        if (target.contains(".") || target.contains(":")) {
            this.plugin.getSanctionService().findAccountsByIp(target).thenAccept(accounts ->
                sync(() -> sender.sendMessage(this.plugin.getMessageFormatter().message(
                    "lookup.ip-result",
                    this.plugin.getMessageFormatter().text("ip", target),
                    this.plugin.getMessageFormatter().text("accounts", String.join(", ", accounts))
                )))
            );
            return true;
        }
        final Player online = Bukkit.getPlayer(target);
        if (online != null) {
            openLookup(sender, online.getUniqueId(), online.getName());
            return true;
        }
        this.plugin.getSanctionService().searchPlayerNames(target, 10).thenAccept(matches -> {
            final String resolvedName = matches.stream()
                .filter(name -> name.equalsIgnoreCase(target))
                .findFirst()
                .orElse(null);
            if (resolvedName == null) {
                sync(() -> sender.sendMessage(this.plugin.getMessageFormatter().message("errors.no-player-found")));
                return;
            }
            openLookup(sender, null, resolvedName);
        }).exceptionally(throwable -> {
            sync(() -> sender.sendMessage(this.plugin.getMessageFormatter().message("errors.database")));
            return null;
        });
        return true;
    }

    private void openLookup(final CommandSender sender, final java.util.UUID initialUuid, final String resolvedName) {
        final CompletableFuture<fr.dragon.admincore.database.PlayerProfile> profileFuture =
            this.plugin.getSanctionService().playerProfile(initialUuid, resolvedName);
        final CompletableFuture<Optional<String>> ipFuture =
            this.plugin.getSanctionService().findLatestIp(resolvedName);

        profileFuture.thenCompose(profile -> {
            final java.util.UUID effectiveUuid = profile.uuid() == null ? initialUuid : profile.uuid();
            final CompletableFuture<List<SanctionRecord>> historyFuture =
                this.plugin.getSanctionService().history(effectiveUuid, resolvedName, 10);
            final CompletableFuture<fr.dragon.admincore.lookup.SessionSummary> sessionsFuture =
                this.plugin.getLookupService().summary(effectiveUuid, resolvedName);
            return ipFuture.thenCombine(historyFuture, (ip, history) -> java.util.Map.entry(ip, history))
                .thenCombine(sessionsFuture, (entry, sessions) -> new LookupResult(
                    effectiveUuid,
                    resolvedName,
                    profile,
                    entry.getKey(),
                    entry.getValue(),
                    sessions
                ));
        }).thenAccept(result ->
            sync(() -> {
                final String displayName = result.profile().name() == null || result.profile().name().isBlank()
                    ? result.resolvedName()
                    : result.profile().name();
                sender.sendMessage(this.plugin.getMessageFormatter().message(
                    "lookup.player-result",
                    this.plugin.getMessageFormatter().text("target", displayName),
                    this.plugin.getMessageFormatter().text("ip", result.ip().orElse("inconnue")),
                    this.plugin.getMessageFormatter().text("historyCount", Integer.toString(result.history().size())),
                    this.plugin.getMessageFormatter().text("client", result.profile().clientBrand() == null ? "inconnu" : result.profile().clientBrand())
                ));
                if (sender instanceof Player player) {
                    LookupMenus.openOverview(player, result.targetUuid(), displayName, result.profile(), result.sessions());
                }
            })
        ).exceptionally(throwable -> {
            sync(() -> sender.sendMessage(this.plugin.getMessageFormatter().message("errors.database")));
            return null;
        });
    }

    private boolean handleAlts(final CommandSender sender, final String targetName) {
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        this.plugin.getSanctionService().findAlts(target.getUniqueId(), targetName).thenAccept(alts ->
            sync(() -> sender.sendMessage(this.plugin.getMessageFormatter().message(
                "lookup.alts-result",
                this.plugin.getMessageFormatter().text("target", targetName),
                this.plugin.getMessageFormatter().text("alts", String.join(", ", alts))
            )))
        );
        return true;
    }

    private boolean handleNote(final CommandSender sender, final String[] args) {
        if (args.length < 2) {
            sender.sendMessage("/note <joueur> <texte>");
            return true;
        }
        final String targetName = args[0];
        final String note = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        this.plugin.getSanctionService().addNote(
            target.getUniqueId(),
            targetName,
            sender instanceof Player player ? player.getUniqueId() : null,
            sender.getName(),
            note
        ).thenRun(() -> sync(() -> sender.sendMessage(this.plugin.getMessageFormatter().message(
            "notes.created",
            this.plugin.getMessageFormatter().text("target", targetName)
        ))));
        return true;
    }

    private boolean handleNotes(final CommandSender sender, final String targetName) {
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        this.plugin.getSanctionService().notes(target.getUniqueId(), targetName).thenAccept(notes ->
            sync(() -> {
                sender.sendMessage(this.plugin.getMessageFormatter().message(
                    "notes.header",
                    this.plugin.getMessageFormatter().text("target", targetName)
                ));
                for (final NoteRepository.NoteEntry note : notes) {
                    sender.sendMessage("- [" + note.actorName() + "] " + note.note());
                }
            })
        );
        return true;
    }

    private boolean handleIpBan(final CommandSender sender, final String targetOrIp) {
        final CompletableFuture<String> ipFuture;
        if (targetOrIp.contains(".") || targetOrIp.contains(":")) {
            ipFuture = CompletableFuture.completedFuture(targetOrIp);
        } else {
            ipFuture = this.plugin.getSanctionService().findLatestIp(targetOrIp).thenApply(optional -> optional.orElse(null));
        }
        ipFuture.thenAccept(ip -> sync(() -> {
            if (ip == null || ip.isBlank()) {
                sender.sendMessage(this.plugin.getMessageFormatter().message("errors.no-player-found"));
                return;
            }
            this.plugin.getSanctionService().create(new CreateSanctionRequest(
                null,
                targetOrIp,
                sender instanceof Player player ? player.getUniqueId() : null,
                sender.getName(),
                SanctionType.BAN,
                "IP Ban",
                null,
                SanctionScope.IP,
                ip
            )).whenComplete((ignored, throwable) -> sync(() -> {
                if (throwable != null) {
                    sender.sendMessage(this.plugin.getMessageFormatter().message("errors.database"));
                    return;
                }
                Bukkit.getOnlinePlayers().stream()
                    .filter(player -> player.getAddress() != null && player.getAddress().getAddress().getHostAddress().equals(ip))
                    .forEach(player -> player.kick(this.plugin.getMessageFormatter().message("sanctions.ipban-kick")));
                sender.sendMessage(this.plugin.getMessageFormatter().message("sanctions.ipban-applied", this.plugin.getMessageFormatter().text("ip", ip)));
            }));
        }));
        return true;
    }

    private boolean handleCheckVpn(final CommandSender sender, final String targetName) {
        if (!this.plugin.getConfigLoader().config().getBoolean("vpn-check.enabled", false)) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("vpn.disabled"));
            return true;
        }
        final CompletableFuture<String> ipFuture = targetName.contains(".")
            ? CompletableFuture.completedFuture(targetName)
            : this.plugin.getSanctionService().findLatestIp(targetName).thenApply(optional -> optional.orElse(null));
        ipFuture.thenAccept(ip -> {
            if (ip == null) {
                sync(() -> sender.sendMessage(this.plugin.getMessageFormatter().message("errors.no-player-found")));
                return;
            }
            final String urlTemplate = this.plugin.getConfigLoader().config().getString("vpn-check.url-template", "");
            if (urlTemplate.isBlank()) {
                sync(() -> sender.sendMessage(this.plugin.getMessageFormatter().message("vpn.disabled")));
                return;
            }
            final String url = urlTemplate.replace("{ip}", URLEncoder.encode(ip, StandardCharsets.UTF_8));
            final URI uri;
            try {
                uri = URI.create(url);
            } catch (final IllegalArgumentException exception) {
                sync(() -> sender.sendMessage(this.plugin.getMessageFormatter().message("vpn.disabled")));
                return;
            }
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                sync(() -> sender.sendMessage(this.plugin.getMessageFormatter().message("vpn.disabled")));
                return;
            }
            final HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(Math.max(1, this.plugin.getConfigLoader().config().getInt("vpn-check.timeout-seconds", 5))))
                .GET()
                .build();
            this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> sync(() -> sender.sendMessage(this.plugin.getMessageFormatter().message(
                    "vpn.result",
                    this.plugin.getMessageFormatter().text("ip", ip),
                    this.plugin.getMessageFormatter().text(
                        "result",
                        response.body().contains(this.plugin.getConfigLoader().config().getString("vpn-check.true-match", "\"proxy\":true")) ? "VPN detecte" : "Aucun VPN detecte"
                    )
                ))))
                .exceptionally(throwable -> {
                    sync(() -> sender.sendMessage(this.plugin.getMessageFormatter().message("errors.database")));
                    return null;
                });
        });
        return true;
    }

    private String permission(final String name) {
        return switch (name) {
            case "unban" -> PermissionService.UNBAN;
            case "unmute" -> PermissionService.UNMUTE;
            case "warnings" -> PermissionService.WARNINGS;
            case "clearwarns" -> PermissionService.CLEARWARNS;
            case "history" -> PermissionService.HISTORY;
            case "lookup" -> PermissionService.LOOKUP;
            case "alts" -> PermissionService.ALTS;
            case "note" -> PermissionService.NOTE;
            case "notes" -> PermissionService.NOTES;
            case "ipban" -> PermissionService.IPBAN;
            case "checkvpn" -> PermissionService.CHECKVPN;
            default -> PermissionService.ADMIN;
        };
    }

    private void sync(final Runnable runnable) {
        this.plugin.getServer().getScheduler().runTask(this.plugin, runnable);
    }

    private record LookupResult(
        java.util.UUID targetUuid,
        String resolvedName,
        fr.dragon.admincore.database.PlayerProfile profile,
        Optional<String> ip,
        List<SanctionRecord> history,
        fr.dragon.admincore.lookup.SessionSummary sessions
    ) {
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))).toList();
        }
        return List.of();
    }
}
