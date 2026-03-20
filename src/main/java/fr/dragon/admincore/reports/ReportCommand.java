package fr.dragon.admincore.reports;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.PermissionService;
import fr.dragon.admincore.dialog.ReportCategoryDialog;
import fr.dragon.admincore.dialog.ReportDescriptionDialog;
import fr.dragon.admincore.dialog.ReportDiscordDialog;
import fr.dragon.admincore.gui.TicketMenu;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
        final String name = command.getName().toLowerCase(Locale.ROOT);
        if ("report".equals(name)) {
            return report(sender, args);
        }
        return tickets(sender, args);
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
        final Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage(this.plugin.getMessageFormatter().message("errors.no-player-found"));
            return true;
        }
        openDiscordStep(reporter, target);
        return true;
    }

    private void openDiscordStep(final Player reporter, final Player target) {
        reporter.showDialog(ReportDiscordDialog.create(target.getName(), (response, audience) -> {
            final String discord = response.getText("discord");
            openCategoryStep(reporter, target, discord == null ? "" : discord.trim());
        }));
    }

    private void openCategoryStep(final Player reporter, final Player target, final String discord) {
        final List<String> categories = reportCategories();
        reporter.showDialog(ReportCategoryDialog.create(categories, (response, audience) -> {
            final String category = response.getText("categorie");
            if (category == null || category.isBlank()) {
                reporter.sendMessage(this.plugin.getMessageFormatter().message("dialogs.action-cancelled"));
                return;
            }
            openDescriptionStep(reporter, target, discord, category);
        }));
    }

    private void openDescriptionStep(final Player reporter, final Player target, final String discord, final String category) {
        reporter.showDialog(ReportDescriptionDialog.create((response, audience) -> {
            final String description = response.getText("description");
            this.plugin.getReportService().create(reporter, target, discord, category, description).thenAccept(ticket ->
                this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
                    reporter.sendMessage(this.plugin.getMessageFormatter().message(
                        "reports.created",
                        this.plugin.getMessageFormatter().text("target", target.getName()),
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

    private boolean tickets(final CommandSender sender, final String[] args) {
        if (!this.plugin.getPermissionService().check(sender, PermissionService.REPORTS)) {
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(this.plugin.getMessageFormatter().deserialize("<prefix><red>Cette commande ouvre un GUI et doit etre lancee en jeu.</red>"));
            return true;
        }
        if (args.length >= 2 && "history".equalsIgnoreCase(args[0])) {
            final org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            this.plugin.getReportService().history(target.getUniqueId(), args[1], 0).thenAccept(page ->
                this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
                    TicketMenu.openHistory(player, args[1], page)
                )
            ).exceptionally(throwable -> {
                this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
                    player.sendMessage(this.plugin.getMessageFormatter().message("errors.database"))
                );
                return null;
            });
            return true;
        }
        this.plugin.getReportService().openPage(0).thenAccept(page ->
            this.plugin.getServer().getScheduler().runTask(this.plugin, () -> TicketMenu.openOpen(player, page))
        ).exceptionally(throwable -> {
            this.plugin.getServer().getScheduler().runTask(this.plugin, () ->
                player.sendMessage(this.plugin.getMessageFormatter().message("errors.database"))
            );
            return null;
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args) {
        if ("report".equalsIgnoreCase(command.getName())) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
            }
            return List.of();
        }
        if (args.length == 1) {
            return List.of("history");
        }
        if (args.length == 2 && "history".equalsIgnoreCase(args[0])) {
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                .toList();
        }
        return List.of();
    }
}
