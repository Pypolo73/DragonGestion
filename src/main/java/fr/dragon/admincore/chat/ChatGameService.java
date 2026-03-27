package fr.dragon.admincore.chat;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.util.ConfigLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatGameService implements Listener {

    private final AdminCorePlugin plugin;
    private final ConfigLoader configLoader;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Random random = new SecureRandom();

    private boolean enabled = false;
    private boolean wordGameEnabled = true;
    private boolean mathGameEnabled = true;
    private int gameInterval = 300;
    private int answerTimeout = 30;
    private int maxAttempts = 3;

    private String currentAnswer = "";
    private String currentWord = "";
    private String currentProblem = "";
    private String gameType = "";
    private boolean gameActive = false;
    private long gameStartTime = 0;

    private final Map<UUID, Integer> playerAttempts = new ConcurrentHashMap<>();
    private final Map<String, List<String>> messages = new ConcurrentHashMap<>();

    public ChatGameService(@NotNull AdminCorePlugin plugin, @NotNull ConfigLoader configLoader) {
        this.plugin = plugin;
        this.configLoader = configLoader;
        loadConfiguration();
    }

    private void loadConfiguration() {
        var chatConfig = configLoader.chatConfig();
        if (chatConfig == null) {
            plugin.getLogger().warning("chat.yml non trouvé, les jeux sont désactivés.");
            return;
        }

        var gamesConfig = chatConfig.getConfigurationSection("chat-games");
        if (gamesConfig == null) {
            return;
        }

        enabled = gamesConfig.getBoolean("enabled", false);
        wordGameEnabled = gamesConfig.getBoolean("word-game.enabled", true);
        mathGameEnabled = gamesConfig.getBoolean("math-game.enabled", true);
        gameInterval = gamesConfig.getInt("time-between-games", 300);
        answerTimeout = gamesConfig.getInt("answer-time", 30);
        maxAttempts = gamesConfig.getInt("max-attempts", 3);

        loadMessages(gamesConfig);
    }

    private void loadMessages(ConfigurationSection config) {
        messages.put("game-title-word", List.of(
            "<gold>🎯 Jeu de mots!</gold>"
        ));
        messages.put("game-title-math", List.of(
            "<gold>🧮 Jeu de math!</gold>"
        ));
        messages.put("instruction", List.of(
            "<gray>Trouve la réponse et tape-la dans le chat!</gray>"
        ));
        messages.put("victory", List.of(
            "<green>🎉 Bravo {player}!</green> <gray>Tu as trouvé la bonne réponse: <white>{answer}</white></gray>",
            "<green>✓ {player} a gagné!</green> <gold>+{reward} XP</gold>"
        ));
        messages.put("no-winner", List.of(
            "<red>😔 Personne n'a trouvé cette fois!</red> <gray>La réponse était: <white>{answer}</white></gray>"
        ));
        messages.put("wrong-answer", List.of(
            "<red>❌ Mauvaise réponse {player}!</red> <gray>Il te reste {attempts} tentatives.</gray>"
        ));
    }

    public void startGame() {
        if (!enabled) {
            return;
        }

        if (gameActive) {
            return;
        }

        gameActive = true;
        gameStartTime = System.currentTimeMillis();
        playerAttempts.clear();

        boolean useWordGame = wordGameEnabled && mathGameEnabled ?
            random.nextBoolean() : wordGameEnabled;

        if (useWordGame) {
            startWordGame();
        } else {
            startMathGame();
        }

        scheduleGameEnd();
    }

    private void startWordGame() {
        File animFile = new File(plugin.getDataFolder(), "admincore/chat/animation.yml");
        if (!animFile.exists()) {
            gameActive = false;
            return;
        }

        YamlConfiguration animConfig = YamlConfiguration.loadConfiguration(animFile);
        ConfigurationSection wordConfig = animConfig.getConfigurationSection("word-game");

        if (wordConfig == null || !wordConfig.getBoolean("enabled", true)) {
            startMathGame();
            return;
        }

        List<String> words = wordConfig.getStringList("words");
        if (words.isEmpty()) {
            words = List.of("Charbon", "Fer", "Or", "Diamant", "Emeraude");
        }

        currentWord = words.get(random.nextInt(words.size()));
        currentAnswer = currentWord.toLowerCase();
        gameType = "WORD";

        String displayMode = wordConfig.getString("display-mode", "HYPHEN");
        String displayedWord;

        switch (displayMode) {
            case "STAR" -> {
                displayedWord = currentWord.substring(0, 1);
                for (int i = 1; i < currentWord.length() - 1; i++) {
                    displayedWord += "*";
                }
                displayedWord += currentWord.substring(currentWord.length() - 1);
            }
            case "MIX" -> {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < currentWord.length(); i++) {
                    if (i % 2 == 0) {
                        sb.append(currentWord.charAt(i));
                    } else {
                        sb.append("-");
                    }
                }
                displayedWord = sb.toString();
            }
            default -> {
                StringBuilder sb = new StringBuilder();
                for (char c : currentWord.toCharArray()) {
                    sb.append(c).append("-");
                }
                displayedWord = sb.substring(0, sb.length() - 1);
            }
        }

        var chatConfig = configLoader.chatConfig();
        String messageTemplate = chatConfig != null ?
            chatConfig.getString("chat-games.word-game.broadcast",
                "<gold>🎯 Jeu de mots!</gold> <gray>Trouve le mot caché:</gray> <white>{word}</white>") :
            "<gold>🎯 Jeu de mots!</gold> <gray>Trouve le mot caché:</gray> <white>{word}</white>";

        String message = messageTemplate.replace("{word}", displayedWord);

        if (wordConfig.getBoolean("show-hint", true)) {
            String hint = currentWord.substring(0, 1) + "***" + currentWord.substring(currentWord.length() - 1);
            message += " <gray>(" + wordConfig.getString("hint-format", "Indice: {hint}").replace("{hint}", hint) + ")</gray>";
        }

        broadcastMessage(message);
    }

    private void startMathGame() {
        File animFile = new File(plugin.getDataFolder(), "admincore/chat/animation.yml");
        if (!animFile.exists()) {
            gameActive = false;
            return;
        }

        YamlConfiguration animConfig = YamlConfiguration.loadConfiguration(animFile);
        ConfigurationSection mathConfig = animConfig.getConfigurationSection("math-game");

        if (mathConfig == null || !mathConfig.getBoolean("enabled", true)) {
            gameActive = false;
            return;
        }

        List<String> operations = mathConfig.getStringList("operations");
        if (operations.isEmpty()) {
            operations = List.of("ADD", "SUBTRACT", "MULTIPLY");
        }

        String operation = operations.get(random.nextInt(operations.size()));

        int min = mathConfig.getInt("number-range.min", 1);
        int max = mathConfig.getInt("number-range.max", 50);
        int min2 = mathConfig.getInt("number-range.min2", 1);
        int max2 = mathConfig.getInt("number-range.max2", 20);

        int num1 = random.nextInt(max - min + 1) + min;
        int num2 = random.nextInt(max2 - min2 + 1) + min2;
        int result;

        switch (operation) {
            case "ADD" -> {
                result = num1 + num2;
                currentProblem = num1 + " + " + num2 + " = ?";
            }
            case "SUBTRACT" -> {
                if (mathConfig.getBoolean("avoid-negative", true) && num1 < num2) {
                    int temp = num1;
                    num1 = num2;
                    num2 = temp;
                }
                result = num1 - num2;
                currentProblem = num1 + " - " + num2 + " = ?";
            }
            case "MULTIPLY" -> {
                num1 = random.nextInt(12) + 1;
                num2 = random.nextInt(12) + 1;
                result = num1 * num2;
                currentProblem = num1 + " × " + num2 + " = ?";
            }
            default -> {
                result = num1 + num2;
                currentProblem = num1 + " + " + num2 + " = ?";
            }
        }

        currentAnswer = String.valueOf(result);
        currentWord = currentProblem;
        gameType = "MATH";

        var chatConfig = configLoader.chatConfig();
        String messageTemplate = chatConfig != null ?
            chatConfig.getString("chat-games.math-game.broadcast",
                "<gold>🧮 Jeu mathématique!</gold> <gray>Résouds l'opération:</gray> <white>{problem}</white>") :
            "<gold>🧮 Jeu mathématique!</gold> <gray>Résouds l'opération:</gray> <white>{problem}</white>";

        String message = messageTemplate.replace("{problem}", currentProblem);
        broadcastMessage(message);
    }

    private void scheduleGameEnd() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (gameActive) {
                endGame(false);
            }
        }, answerTimeout * 20L);
    }

    private void endGame(boolean winnerFound) {
        gameActive = false;

        if (!winnerFound) {
            var chatConfig = configLoader.chatConfig();
            String template = chatConfig != null ?
                chatConfig.getString("chat-games.no-winner-message",
                    "<red>😔 Personnes n'a trouvé la réponse!</red> <gray>La réponse était:</gray> <white>{answer}</white>") :
                "<red>😔 Personnes n'a trouvé la réponse!</red> <gray>La réponse était:</gray> <white>{answer}</white>";

            String message = template
                .replace("{answer}", currentAnswer)
                .replace("{problem}", currentProblem);

            broadcastMessage(message);
        }

        playerAttempts.clear();

        Bukkit.getScheduler().runTaskLater(plugin, this::startGame, gameInterval * 20L);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!gameActive || !enabled) {
            return;
        }

        Player player = event.getPlayer();
        String message = event.getMessage().trim();

        int attempts = playerAttempts.getOrDefault(player.getUniqueId(), 0);
        if (attempts >= maxAttempts) {
            return;
        }

        if (message.equalsIgnoreCase(currentAnswer)) {
            event.setCancelled(true);
            playerAttempts.put(player.getUniqueId(), maxAttempts);

            giveReward(player);

            String victoryMessage = getRandomMessage("victory")
                .replace("{player}", player.getName())
                .replace("{answer}", currentAnswer)
                .replace("{reward}", getRewardAmount());

            broadcastMessage(victoryMessage);

            endGame(true);
            return;
        }

        attempts++;
        playerAttempts.put(player.getUniqueId(), attempts);

        if (attempts >= maxAttempts) {
            event.setCancelled(true);
            String noAttemptsMessage = getRandomMessage("wrong-answer")
                .replace("{player}", player.getName())
                .replace("{attempts}", "0");
            player.sendMessage(parseMiniMessage(noAttemptsMessage));
        } else {
            event.setCancelled(true);
            String wrongMessage = getRandomMessage("wrong-answer")
                .replace("{player}", player.getName())
                .replace("{attempts}", String.valueOf(maxAttempts - attempts));
            player.sendMessage(parseMiniMessage(wrongMessage));
        }
    }

    private void giveReward(Player player) {
        File animFile = new File(plugin.getDataFolder(), "admincore/chat/animation.yml");
        if (!animFile.exists()) {
            return;
        }

        YamlConfiguration animConfig = YamlConfiguration.loadConfiguration(animFile);
        ConfigurationSection rewardsConfig = animConfig.getConfigurationSection("rewards");

        if (rewardsConfig == null || !rewardsConfig.getBoolean("enabled", true)) {
            return;
        }

        String rewardName = getSelectedReward();
        
        ConfigurationSection rewardConfig = rewardsConfig.getConfigurationSection(rewardName);
        if (rewardConfig == null) {
            rewardConfig = rewardsConfig.getConfigurationSection("xp_default");
        }
        
        if (rewardConfig == null) {
            player.giveExp(100);
            player.sendMessage(parseMiniMessage("<green>Tu as reçu +100 XP!</green>"));
            return;
        }

        String type = rewardConfig.getString("name", "xp").toLowerCase();
        int amount = rewardConfig.getInt("amount", 100);
        String command = rewardConfig.getString("command", "");

        switch (type) {
            case "xp" -> {
                player.giveExp(amount);
                player.sendMessage(parseMiniMessage("<green>Tu as reçu +" + amount + " XP!</green>"));
            }
            case "money" -> {
                player.sendMessage(parseMiniMessage("<green>Tu as reçu +" + amount + " pièces!</green>"));
            }
            case "item" -> {
                if (!command.isEmpty()) {
                    String execCommand = command.replace("{player}", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), execCommand);
                    player.sendMessage(parseMiniMessage("<green>Tu as reçu ta récompense!</green>"));
                }
            }
            case "command" -> {
                if (!command.isEmpty()) {
                    String execCommand = command.replace("{player}", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), execCommand);
                    player.sendMessage(parseMiniMessage("<green>Tu as reçu ta récompense!</green>"));
                }
            }
            default -> {
                player.giveExp(amount);
                player.sendMessage(parseMiniMessage("<green>Tu as reçu +" + amount + " XP!</green>"));
            }
        }
    }

    private String getSelectedReward() {
        var chatConfig = configLoader.chatConfig();
        if (chatConfig != null) {
            return chatConfig.getString("chat-games.reward", "xp_default");
        }
        return "xp_default";
    }

    private String getRewardAmount() {
        File animFile = new File(plugin.getDataFolder(), "admincore/chat/animation.yml");
        if (!animFile.exists()) {
            return "100";
        }

        YamlConfiguration animConfig = YamlConfiguration.loadConfiguration(animFile);
        String rewardName = getSelectedReward();
        ConfigurationSection rewardConfig = animConfig.getConfigurationSection("rewards." + rewardName);
        
        if (rewardConfig != null) {
            return String.valueOf(rewardConfig.getInt("amount", 100));
        }
        return "100";
    }

    private void broadcastMessage(String message) {
        Component component = parseMiniMessage(message);
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(component));
    }

    private Component parseMiniMessage(String message) {
        try {
            return miniMessage.deserialize(message);
        } catch (Exception e) {
            return Component.text(message);
        }
    }

    private String getRandomMessage(String key) {
        List<String> list = messages.getOrDefault(key, List.of("<gray>Message non trouvé</gray>"));
        return list.get(random.nextInt(list.size()));
    }

    public boolean isGameActive() {
        return gameActive;
    }

    public void startGameScheduler() {
        if (!enabled) {
            return;
        }

        plugin.getLogger().info("Chat games scheduler started. First game in " + gameInterval + " seconds.");
        Bukkit.getScheduler().runTaskLater(plugin, this::startGame, gameInterval * 20L);
    }
}
