package fr.dragon.admincore.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

public final class MessageFormatter {

    private final ConfigLoader configLoader;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MessageFormatter(final ConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    public Component message(final String path, final TagResolver... resolvers) {
        final String raw = this.configLoader.messages().getString(path, "<red>Message manquant: " + path + "</red>");
        return deserialize(raw, resolvers);
    }

    public Component block(final String path, final TagResolver... resolvers) {
        final List<String> lines = this.configLoader.messages().getStringList(path);
        if (lines.isEmpty()) {
            return message(path, resolvers);
        }
        Component result = Component.empty();
        for (int index = 0; index < lines.size(); index++) {
            result = result.append(deserialize(lines.get(index), resolvers));
            if (index + 1 < lines.size()) {
                result = result.append(Component.newline());
            }
        }
        return result;
    }

    public List<Component> lines(final String path, final TagResolver... resolvers) {
        final List<Component> components = new ArrayList<>();
        for (final String line : this.configLoader.messages().getStringList(path)) {
            components.add(deserialize(line, resolvers));
        }
        return components;
    }

    public Component deserialize(final String raw, final TagResolver... resolvers) {
        final List<TagResolver> merged = new ArrayList<>();
        merged.add(Placeholder.parsed("prefix", this.configLoader.messages().getString("prefix", "")));
        for (final TagResolver resolver : resolvers) {
            if (resolver != null) {
                merged.add(resolver);
            }
        }
        return this.miniMessage.deserialize(raw, TagResolver.resolver(merged));
    }

    public TagResolver text(final String key, final String value) {
        return Placeholder.unparsed(key, Objects.requireNonNullElse(value, ""));
    }

    public TagResolver parsed(final String key, final String value) {
        return Placeholder.parsed(key, Objects.requireNonNullElse(value, ""));
    }

    public void send(final CommandSender sender, final String path, final TagResolver... resolvers) {
        sender.sendMessage(message(path, resolvers));
    }

    public String raw(final String path, final String fallback) {
        return this.configLoader.messages().getString(path, fallback);
    }
}
