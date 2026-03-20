package fr.dragon.admincore.reports;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.dialog.ReportCategoryDialog;
import fr.dragon.admincore.dialog.ReportDescriptionDialog;
import fr.dragon.admincore.dialog.ReportDiscordDialog;
import fr.dragon.admincore.database.PlayerProfile;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class ReportCommand implements CommandExecutor, TabCompleter {

    private final AdminCorePlugin plugin;

    public ReportCommand(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        return report(sender, args);
    }

    private boolean report(final CommandSender sender, final String[] args) {
        if (!(sender instanceof Player reporter)) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.player-only"));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(this.plugin.getMessageFormatter().deserialize("<prefix><red>Usage: /report <joueur></red>"));
            return true;
        }
        resolveTarget(args[0]).thenAccept(target ->
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
                if (target == null) {
                    reporter.sendMessage(this.plugin.getMessageFormatter().message("errors.no-player-found"));
                    return;
                }
                openDiscordStep(reporter, target);
            })
        ).exceptionally(throwable -> {
            this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
                reporter.sendMessage(this.plugin.getMessageFormatter().message("errors.database"))
            );
            return null;
        });
        return true;
    }

    private void openDiscordStep(final Player reporter, final ReportTarget target) {
        reporter.showDialog(ReportDiscordDialog.create(target.name(), (response, audience) -> {
            final String discord = response.getText("discord");
            openCategoryStep(reporter, target, discord == null ? "" : discord.trim());
        }));
    }

    private void openCategoryStep(final Player reporter, final ReportTarget target, final String discord) {
        final List<String> categories = reportCategories();
        reporter.showDialog(ReportCategoryDialog.create(categories, (response, audience) -> {
            final String category = response.getText("categorie");
            final String effectiveCategory = (category == null || category.isBlank()) ? categories.getFirst() : category.trim();
            openDescriptionStep(reporter, target, discord, effectiveCategory);
        }));
    }

    private void openDescriptionStep(final Player reporter, final ReportTarget target, final String discord, final String category) {
        reporter.showDialog(ReportDescriptionDialog.create((response, audience) -> {
            final String description = response.getText("description");
            this.plugin.getReportService().create(reporter, target.uuid(), target.name(), discord, category, description).thenAccept(ticket ->
                this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
                    reporter.sendMessage(this.plugin.getMessageFormatter().message(
                        "reports.created",
                        this.plugin.getMessageFormatter().text("target", target.name()),
                        this.plugin.getMessageFormatter().text("id", Long.toString(ticket.id()))
                    ))
                )
            ).exceptionally(throwable -> {
                this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
                    reporter.sendMessage(this.plugin.getMessageFormatter().message("errors.database"))
                );
                return null;
            });
        }));
    }

    private List<String> reportCategories() {
        final List<String> configured = this.plugin.getConfigLoader().config().getStringList("reports.categories").stream()
            .filter(entry -> entry != null && !entry.isBlank())
            .toList();
        if (!configured.isEmpty()) {
            return configured;
        }
        return List.of(
            "Triche / Hack",
            "Comportement toxique",
            "Spam / Pub / Arnaque",
            "Bug exploite",
            "Autre"
        );
    }

    private CompletableFuture<ReportTarget> resolveTarget(final String rawName) {
        final Player online = Bukkit.getPlayerExact(rawName);
        if (online != null) {
            return CompletableFuture.completedFuture(new ReportTarget(online.getUniqueId(), online.getName()));
        }
        return this.plugin.getSanctionService().playerProfile(null, rawName).thenApply(profile -> toReportTarget(rawName, profile));
    }

    private ReportTarget toReportTarget(final String rawName, final PlayerProfile profile) {
        if (profile == null || profile.uuid() == null || profile.name() == null || !profile.name().equalsIgnoreCase(rawName)) {
            return null;
        }
        return new ReportTarget(profile.uuid(), profile.name());
    }

    private record ReportTarget(UUID uuid, String name) {
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if (args.length == 1) {
            final List<String> onlineMatches = Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                .toList();
            if (!onlineMatches.isEmpty()) {
                return onlineMatches;
            }
        }
        return List.of();
    }
}
