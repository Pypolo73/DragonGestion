package fr.dragon.admincore.reports;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.dialog.PlayerPickerDialog;
import fr.dragon.admincore.dialog.ReportCategoryDialog;
import fr.dragon.admincore.dialog.ReportDescriptionDialog;
import fr.dragon.admincore.dialog.ReportDiscordDialog;
import fr.dragon.admincore.dialog.ReportTypeDialog;
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
        openReportTypeStep(reporter);
        return true;
    }

    private void openReportTypeStep(final Player reporter) {
        reporter.showDialog(ReportTypeDialog.create(
            () -> openPlayerPickerStep(reporter),
            () -> openBugReportStep(reporter)
        ));
    }

    private void openPlayerPickerStep(final Player reporter) {
        this.plugin.getDialogSupportService().openPlayerPicker(reporter, "Signaler un joueur", target -> {
            final ReportTarget reportTarget = new ReportTarget(target.getUniqueId(), target.getName());
            openCategoryStep(reporter, reportTarget, "", false);
        });
    }

    private void openBugReportStep(final Player reporter) {
        final UUID bugUuid = UUID.randomUUID();
        final String bugName = "BUG_" + System.currentTimeMillis();
        openCategoryStep(reporter, new ReportTarget(bugUuid, bugName), "", true);
    }

    private void openCategoryStep(final Player reporter, final ReportTarget target, final String discord, final boolean isBugReport) {
        final List<String> categories = isBugReport ? bugCategories() : reportCategories();
        reporter.showDialog(ReportCategoryDialog.createWithCancel(categories, (response, audience) -> {
            final String category = response.getText("categorie");
            final String effectiveCategory = (category == null || category.isBlank()) ? categories.getFirst() : category.trim();
            openDiscordStep(reporter, target, effectiveCategory, isBugReport);
        }, () -> openReportTypeStep(reporter)));
    }

    private void openDiscordStep(final Player reporter, final ReportTarget target, final String category, final boolean isBugReport) {
        reporter.showDialog(ReportDiscordDialog.createWithCancel(target.name(), 
            (response, audience) -> {
                final String discord = response.getText("discord");
                openDescriptionStep(reporter, target, discord == null ? "" : discord.trim(), category, isBugReport);
            },
            () -> openDescriptionStep(reporter, target, "", category, isBugReport),
            () -> openCategoryStep(reporter, target, "", isBugReport)
        ));
    }

    private void openDescriptionStep(final Player reporter, final ReportTarget target, final String discord, final String category, final boolean isBugReport) {
        final String title = target.name().startsWith("BUG_") ? "Signaler un bug" : "Signaler " + target.name();
        final String placeholder = target.name().startsWith("BUG_") 
            ? "Decris le bug en detail (ce qui se passe, ce qui devrait se passer, etc.)"
            : "Decris les faits";
        reporter.showDialog(ReportDescriptionDialog.createWithCancel(title, placeholder, (response, audience) -> {
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
        }, () -> openDiscordStep(reporter, target, category, isBugReport)));
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

    private List<String> bugCategories() {
        final List<String> configured = this.plugin.getConfigLoader().config().getStringList("reports.bug-categories").stream()
            .filter(entry -> entry != null && !entry.isBlank())
            .toList();
        if (!configured.isEmpty()) {
            return configured;
        }
        return List.of(
            "Bug de gameplay",
            "Bug de lag / performance",
            "Bug d'affichage",
            "Probleme de connexion",
            "Autre bug"
        );
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
