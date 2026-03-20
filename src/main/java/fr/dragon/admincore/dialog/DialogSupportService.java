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
import java.util.function.Consumer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public final class DialogSupportService {

    public record ReasonFlowResult(String reason, String actorLabel) {
    }

    public record TimedReasonFlowResult(String reason, Duration duration, String actorLabel) {
    }

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

    public void openReasonFlow(final Player player, final String title, final Consumer<ReasonFlowResult> onConfirm) {
        if (supportsDialogs(player)) {
            openReasonSelection(player, title, onConfirm);
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
                    onConfirm.accept(new ReasonFlowResult(this.reason, actor.getName()));
                }
                return false;
            }
        });
    }

    public void openTimedReasonFlow(final Player player, final String actionTitle, final Consumer<TimedReasonFlowResult> onConfirm) {
        if (supportsDialogs(player)) {
            openTimedReasonSelection(player, actionTitle, onConfirm);
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
                onConfirm.accept(new TimedReasonFlowResult(this.reason, duration, actor.getName()));
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
        final String initialValue,
        final Consumer<TimedReasonFlowResult> onConfirm
    ) {
        player.showDialog(DurationDialog.create(
            "Duree de la sanction",
            initialValue,
            (response, audience) -> {
                final String rawDuration = response.getText("duration") == null ? initialValue : response.getText("duration").trim();
                final Duration duration = TimeParser.parse(rawDuration).orElse(null);
                if (duration == null) {
                    player.sendMessage(this.formatter.message("errors.invalid-duration"));
                    openDurationDialog(player, title, reason, initialValue, onConfirm);
                    return;
                }
                openTimedConfirm(player, title, reason, duration, player.getName(), rawDuration, onConfirm);
            },
            (response, audience) -> openTimedDurationSelection(player, title, reason, onConfirm)
        ));
    }

    private void openReasonConfirm(
        final Player player,
        final String title,
        final String reason,
        final String actorLabel,
        final Consumer<ReasonFlowResult> onConfirm
    ) {
        player.showDialog(SanctionConfirmDialog.create(
            title,
            List.of(
                Component.text("Raison : " + reason),
                Component.text("Par : " + actorLabel)
            ),
            (confirmResponse, confirmAudience) -> onConfirm.accept(new ReasonFlowResult(reason, actorLabel)),
            (actorResponse, actorAudience) -> openReasonActorChoice(player, title, reason, actorLabel, onConfirm),
            (backResponse, backAudience) -> openReasonSelection(player, title, onConfirm)
        ));
    }

    private void openReasonActorChoice(
        final Player player,
        final String title,
        final String reason,
        final String actorLabel,
        final Consumer<ReasonFlowResult> onConfirm
    ) {
        player.showDialog(SanctionActorChoiceDialog.create(
            "Affichage du staff",
            player.getName(),
            (response, audience) -> openReasonConfirm(player, title, reason, "Decision staff", onConfirm),
            (response, audience) -> openReasonConfirm(player, title, reason, "Non precise", onConfirm),
            (response, audience) -> openReasonConfirm(player, title, reason, player.getName(), onConfirm),
            (response, audience) -> openReasonConfirm(player, title, reason, actorLabel, onConfirm)
        ));
    }

    private void openTimedConfirm(
        final Player player,
        final String title,
        final String reason,
        final Duration duration,
        final String actorLabel,
        final String durationValue,
        final Consumer<TimedReasonFlowResult> onConfirm
    ) {
        player.showDialog(SanctionConfirmDialog.create(
            title,
            List.of(
                Component.text("Raison : " + reason),
                Component.text("Duree : " + TimeParser.format(duration)),
                Component.text("Par : " + actorLabel)
            ),
            (confirmResponse, confirmAudience) -> onConfirm.accept(new TimedReasonFlowResult(reason, duration, actorLabel)),
            (actorResponse, actorAudience) -> openTimedActorChoice(player, title, reason, duration, actorLabel, durationValue, onConfirm),
            (backResponse, backAudience) -> openTimedDurationSelection(player, title, reason, onConfirm)
        ));
    }

    private void openTimedActorChoice(
        final Player player,
        final String title,
        final String reason,
        final Duration duration,
        final String actorLabel,
        final String durationValue,
        final Consumer<TimedReasonFlowResult> onConfirm
    ) {
        player.showDialog(SanctionActorChoiceDialog.create(
            "Affichage du staff",
            player.getName(),
            (response, audience) -> openTimedConfirm(player, title, reason, duration, "Decision staff", durationValue, onConfirm),
            (response, audience) -> openTimedConfirm(player, title, reason, duration, "Non precise", durationValue, onConfirm),
            (response, audience) -> openTimedConfirm(player, title, reason, duration, player.getName(), durationValue, onConfirm),
            (response, audience) -> openTimedConfirm(player, title, reason, duration, actorLabel, durationValue, onConfirm)
        ));
    }

    private void openReasonSelection(final Player player, final String title, final Consumer<ReasonFlowResult> onConfirm) {
        final List<String> presets = presetValues("sanctions.presets.reasons");
        if (presets.isEmpty()) {
            openReasonTextEntry(player, title, onConfirm);
            return;
        }
        player.showDialog(PresetChoiceDialog.create(
            "Raison de la sanction",
            "Choisis une raison predefinie ou saisis une raison personnalisee.",
            presets,
            preset -> openReasonConfirm(player, title, sanitizeReason(preset), player.getName(), onConfirm),
            "Raison personnalisee",
            () -> openReasonTextEntry(player, title, onConfirm),
            "Annuler",
            () -> player.sendMessage(this.formatter.message("dialogs.action-cancelled")),
            2
        ));
    }

    private void openReasonTextEntry(final Player player, final String title, final Consumer<ReasonFlowResult> onConfirm) {
        player.showDialog(ReasonDialog.create(title, "Suivant ->", "", (response, audience) -> {
            final String reason = sanitizeReason(response.getText("reason"));
            openReasonConfirm(player, title, reason, player.getName(), onConfirm);
        }));
    }

    private void openTimedReasonSelection(final Player player, final String title, final Consumer<TimedReasonFlowResult> onConfirm) {
        final List<String> presets = presetValues("sanctions.presets.reasons");
        if (presets.isEmpty()) {
            openTimedReasonTextEntry(player, title, onConfirm);
            return;
        }
        player.showDialog(PresetChoiceDialog.create(
            "Raison de la sanction",
            "Choisis une raison predefinie ou saisis une raison personnalisee.",
            presets,
            preset -> openTimedDurationSelection(player, title, sanitizeReason(preset), onConfirm),
            "Raison personnalisee",
            () -> openTimedReasonTextEntry(player, title, onConfirm),
            "Annuler",
            () -> player.sendMessage(this.formatter.message("dialogs.action-cancelled")),
            2
        ));
    }

    private void openTimedReasonTextEntry(final Player player, final String title, final Consumer<TimedReasonFlowResult> onConfirm) {
        player.showDialog(ReasonDialog.create(title, "Suivant ->", "", (reasonResponse, audience) -> {
            final String reason = sanitizeReason(reasonResponse.getText("reason"));
            openTimedDurationSelection(player, title, reason, onConfirm);
        }));
    }

    private void openTimedDurationSelection(final Player player, final String title, final String reason, final Consumer<TimedReasonFlowResult> onConfirm) {
        final List<String> presets = presetValues("sanctions.presets.durations");
        if (presets.isEmpty()) {
            openDurationDialog(player, title, reason, "1h", onConfirm);
            return;
        }
        player.showDialog(PresetChoiceDialog.create(
            "Duree de la sanction",
            "Choisis une duree predefinie ou saisis une duree en mois, jour, heure ou minute.",
            presets,
            preset -> {
                final Duration duration = TimeParser.parse(preset).orElse(null);
                if (duration == null) {
                    player.sendMessage(this.formatter.message("errors.invalid-duration"));
                    openTimedDurationSelection(player, title, reason, onConfirm);
                    return;
                }
                openTimedConfirm(player, title, reason, duration, player.getName(), preset, onConfirm);
            },
            "Duree personnalisee",
            () -> openDurationDialog(player, title, reason, firstDurationPreset(), onConfirm),
            "Retour",
            () -> openTimedReasonSelection(player, title, onConfirm),
            2
        ));
    }

    private List<String> presetValues(final String path) {
        return this.configLoader.config().getStringList(path).stream()
            .filter(value -> value != null && !value.isBlank())
            .toList();
    }

    private String firstDurationPreset() {
        final List<String> presets = presetValues("sanctions.presets.durations");
        return presets.isEmpty() ? "1h" : presets.getFirst();
    }

    private String sanitizeReason(final String raw) {
        final String value = raw == null || raw.isBlank()
            ? this.configLoader.config().getString("sanctions.default-reason", "Aucune raison fournie")
            : raw.trim();
        return value.length() > 120 ? value.substring(0, 120) : value;
    }
}
