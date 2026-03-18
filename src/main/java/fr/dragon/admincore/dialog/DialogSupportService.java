package fr.dragon.admincore.dialog;

import fr.dragon.admincore.chat.ClearChatSelection;
import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.util.ConfigLoader;
import fr.dragon.admincore.util.MessageFormatter;
import fr.dragon.admincore.util.TimeParser;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class DialogSupportService {

    private final AdminCorePlugin plugin;
    private final ConfigLoader configLoader;
    private final MessageFormatter formatter;
    private final ChatPromptService chatPromptService;

    public DialogSupportService(
        final AdminCorePlugin plugin,
        final ConfigLoader configLoader,
        final MessageFormatter formatter,
        final ChatPromptService chatPromptService
    ) {
        this.plugin = plugin;
        this.configLoader = configLoader;
        this.formatter = formatter;
        this.chatPromptService = chatPromptService;
    }

    public boolean supportsDialogs(final Player player) {
        if (!this.configLoader.config().getBoolean("dialogs.enabled", true)) {
            return false;
        }
        final Set<String> blocked = Set.copyOf(this.configLoader.config().getStringList("dialogs.blocked-client-brands"));
        final String brand = player.getClientBrandName();
        return brand == null || blocked.stream().noneMatch(entry -> brand.toLowerCase(Locale.ROOT).contains(entry.toLowerCase(Locale.ROOT)));
    }

    public void openReasonFlow(final Player player, final String title, final Consumer<String> onConfirm) {
        if (supportsDialogs(player)) {
            player.showDialog(ReasonDialog.create(title, "Suivant ->", "", (response, audience) -> {
                final String reason = sanitizeReason(response.getText("reason"));
                player.showDialog(ConfirmDialog.create(
                    title,
                    List.of(Component.text("Raison : " + reason)),
                    "Confirmer",
                    "Retour",
                    (confirmResponse, confirmAudience) -> onConfirm.accept(reason),
                    (backResponse, backAudience) -> openReasonFlow(player, title, onConfirm)
                ));
            }));
            return;
        }
        chatPromptService.start(player, new ChatPromptService.Session() {
            private String reason;

            @Override
            public void open(final Player actor) {
                actor.sendMessage(formatter.message("dialogs.chat-prompt-reason"));
            }

            @Override
            public boolean handle(final Player actor, final String input) {
                if (this.reason == null) {
                    this.reason = sanitizeReason(input);
                    actor.sendMessage(formatter.message("dialogs.chat-prompt-confirm", formatter.text("value", this.reason)));
                    return true;
                }
                if ("confirm".equalsIgnoreCase(input)) {
                    onConfirm.accept(this.reason);
                }
                return false;
            }
        });
    }

    public void openTimedReasonFlow(final Player player, final String actionTitle, final BiConsumer<String, Duration> onConfirm) {
        if (supportsDialogs(player)) {
            player.showDialog(ReasonDialog.create(actionTitle, "Suivant ->", "", (reasonResponse, audience) -> {
                final String reason = sanitizeReason(reasonResponse.getText("reason"));
                openDurationDialog(player, actionTitle, reason, 1, "minutes", onConfirm);
            }));
            return;
        }
        chatPromptService.start(player, new ChatPromptService.Session() {
            private String reason;

            @Override
            public void open(final Player actor) {
                actor.sendMessage(formatter.message("dialogs.chat-prompt-reason"));
            }

            @Override
            public boolean handle(final Player actor, final String input) {
                if (this.reason == null) {
                    this.reason = sanitizeReason(input);
                    actor.sendMessage(formatter.message("dialogs.chat-prompt-duration"));
                    return true;
                }
                final Duration duration = TimeParser.parse(input).orElse(Duration.ofHours(1));
                actor.sendMessage(formatter.message("dialogs.chat-prompt-confirm",
                    formatter.text("value", this.reason + " / " + TimeParser.format(duration))));
                onConfirm.accept(this.reason, duration);
                return false;
            }
        });
    }

    public void openClearChatFlow(final Player player, final Consumer<ClearChatSelection> onConfirm) {
        if (supportsDialogs(player)) {
            player.showDialog(ClearChatDialog.create((response, audience) -> onConfirm.accept(new ClearChatSelection(
                Boolean.TRUE.equals(response.getBoolean("public")),
                Boolean.TRUE.equals(response.getBoolean("players")),
                Boolean.TRUE.equals(response.getBoolean("logs")),
                Boolean.TRUE.equals(response.getBoolean("system")),
                response.getText("reason")
            )), (response, audience) -> audience.sendMessage(Component.text("Action annulee."))));
            return;
        }
        chatPromptService.start(player, new ChatPromptService.Session() {
            @Override
            public void open(final Player actor) {
                actor.sendMessage(formatter.message("dialogs.chat-prompt-clearchat"));
            }

            @Override
            public boolean handle(final Player actor, final String input) {
                final List<String> parts = Arrays.asList(input.toLowerCase(Locale.ROOT).split(" "));
                onConfirm.accept(new ClearChatSelection(
                    parts.contains("all") || parts.contains("public"),
                    parts.contains("all") || parts.contains("players"),
                    parts.contains("logs"),
                    parts.contains("all") || parts.contains("system"),
                    ""
                ));
                return false;
            }
        });
    }

    public void openPlayerPicker(final Player player, final String title, final Consumer<Player> consumer) {
        if (!supportsDialogs(player)) {
            player.sendMessage(this.formatter.message("dialogs.player-picker-unavailable"));
            return;
        }
        final List<Player> choices = new java.util.ArrayList<>(Bukkit.getOnlinePlayers().stream().filter(target -> !target.equals(player)).toList());
        if (choices.isEmpty()) {
            player.sendMessage(this.formatter.message("errors.no-player-found"));
            return;
        }
        player.showDialog(PlayerPickerDialog.create(title, choices, consumer));
    }

    private void openDurationDialog(
        final Player player,
        final String title,
        final String reason,
        final int initialAmount,
        final String initialUnit,
        final BiConsumer<String, Duration> onConfirm
    ) {
        player.showDialog(DurationDialog.create(
            "Duree de la sanction",
            initialAmount,
            initialUnit,
            (response, audience) -> {
                final int amount = Math.max(1, Math.round(response.getFloat("amount") == null ? 1.0F : response.getFloat("amount")));
                final String unit = response.getText("unit") == null ? "minutes" : response.getText("unit");
                final Duration duration = switch (unit.toLowerCase(Locale.ROOT)) {
                    case "hours" -> Duration.ofHours(amount);
                    case "days" -> Duration.ofDays(amount);
                    case "weeks" -> Duration.ofDays(amount * 7L);
                    case "months" -> Duration.ofDays(amount * 30L);
                    default -> Duration.ofMinutes(amount);
                };
                player.showDialog(ConfirmDialog.create(
                    title,
                    List.of(
                        Component.text("Raison : " + reason),
                        Component.text("Duree : " + TimeParser.format(duration))
                    ),
                    "Confirmer",
                    "Retour",
                    (confirmResponse, confirmAudience) -> onConfirm.accept(reason, duration),
                    (backResponse, backAudience) -> openDurationDialog(player, title, reason, amount, unit, onConfirm)
                ));
            },
            (response, audience) -> audience.sendMessage(Component.text("Action annulee."))
        ));
    }

    private String sanitizeReason(final String raw) {
        final String value = raw == null || raw.isBlank()
            ? this.configLoader.config().getString("sanctions.default-reason", "Aucune raison fournie")
            : raw.trim();
        return value.length() > 120 ? value.substring(0, 120) : value;
    }
}
