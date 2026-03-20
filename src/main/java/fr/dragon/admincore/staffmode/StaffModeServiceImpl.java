package fr.dragon.admincore.staffmode;

import fr.dragon.admincore.core.PlayerSession;
import fr.dragon.admincore.core.PlayerSessionManager;
import fr.dragon.admincore.vanish.VanishService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.GameMode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.scheduler.BukkitTask;

public final class StaffModeServiceImpl implements StaffModeService {

    public static final NamespacedKey TOOL_KEY = new NamespacedKey("admincore", "staff_tool");
    private static final TextColor TITLE = TextColor.color(0xFFE45E);
    private static final TextColor LABEL = TextColor.color(0xD83BFF);
    private static final TextColor INFO = TextColor.color(0x67B9FF);

    private final Plugin plugin;
    private final PlayerSessionManager sessionManager;
    private final VanishService vanishService;
    private final Map<UUID, StaffModeSnapshot> snapshots = new ConcurrentHashMap<>();
    private final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> spyPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> observationPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BukkitTask> monitorTasks = new ConcurrentHashMap<>();

    public StaffModeServiceImpl(final Plugin plugin, final PlayerSessionManager sessionManager, final VanishService vanishService) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.vanishService = vanishService;
    }

    @Override
    public boolean toggleStaffMode(final Player player) {
        if (this.snapshots.containsKey(player.getUniqueId())) {
            disable(player);
            return false;
        }
        enable(player);
        return true;
    }

    @Override
    public boolean isInStaffMode(final UUID uuid) {
        return this.snapshots.containsKey(uuid);
    }

    @Override
    public void clearStaffState(final UUID uuid) {
        this.snapshots.remove(uuid);
        this.spyPlayers.remove(uuid);
        this.frozenPlayers.remove(uuid);
        this.observationPlayers.remove(uuid);
        stopMonitor(uuid);
    }

    @Override
    public boolean toggleObservationMode(final Player player) {
        if (this.observationPlayers.remove(player.getUniqueId())) {
            player.setGameMode(GameMode.ADVENTURE);
            player.setAllowFlight(true);
            player.setFlying(true);
            return false;
        }
        this.observationPlayers.add(player.getUniqueId());
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
        return true;
    }

    @Override
    public boolean isObservationMode(final UUID uuid) {
        return this.observationPlayers.contains(uuid);
    }

    @Override
    public boolean toggleFreeze(final Player target) {
        final boolean frozen = this.frozenPlayers.contains(target.getUniqueId());
        if (frozen) {
            this.frozenPlayers.remove(target.getUniqueId());
        } else {
            this.frozenPlayers.add(target.getUniqueId());
        }
        ((PlayerSession) this.sessionManager.get(target)).setFrozen(!frozen);
        return !frozen;
    }

    @Override
    public boolean isFrozen(final UUID uuid) {
        return this.frozenPlayers.contains(uuid);
    }

    @Override
    public boolean toggleSpy(final Player player) {
        final boolean enabled = this.spyPlayers.add(player.getUniqueId());
        if (!enabled) {
            this.spyPlayers.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    @Override
    public boolean isSpyEnabled(final UUID uuid) {
        return this.spyPlayers.contains(uuid);
    }

    @Override
    public Set<UUID> getFrozenPlayers() {
        return Set.copyOf(this.frozenPlayers);
    }

    @Override
    public Set<UUID> getSpyPlayers() {
        return Set.copyOf(this.spyPlayers);
    }

    @Override
    public void refreshMonitor(final Player player) {
        stopMonitor(player.getUniqueId());
        if (!isInStaffMode(player.getUniqueId())) {
            return;
        }
        if (readTool(player.getInventory().getItemInMainHand()) != StaffTool.MONITOR) {
            return;
        }
        final BukkitTask task = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
            if (!player.isOnline()
                || !isInStaffMode(player.getUniqueId())
                || readTool(player.getInventory().getItemInMainHand()) != StaffTool.MONITOR) {
                stopMonitor(player.getUniqueId());
                return;
            }
            player.sendActionBar(monitorBar());
        }, 0L, 20L);
        this.monitorTasks.put(player.getUniqueId(), task);
    }

    @Override
    public void stopMonitor(final UUID uuid) {
        final BukkitTask previous = this.monitorTasks.remove(uuid);
        if (previous != null) {
            previous.cancel();
        }
    }

    private void enable(final Player player) {
        final PlayerInventory inventory = player.getInventory();
        player.closeInventory();
        this.snapshots.put(player.getUniqueId(), new StaffModeSnapshot(
            inventory.getContents().clone(),
            inventory.getArmorContents().clone(),
            inventory.getItemInOffHand() == null ? null : inventory.getItemInOffHand().clone(),
            player.getGameMode(),
            player.getAllowFlight(),
            player.isFlying(),
            player.getLocation().clone(),
            this.vanishService.isVanished(player.getUniqueId())
        ));
        inventory.clear();
        inventory.setArmorContents(null);
        inventory.setItem(0, namedItem(Material.COMPASS, "Teleportation staff", StaffTool.TELEPORT, "Clic droit: te teleporter sur un joueur.", "Shift + clic droit: attirer un joueur.", "Shift + clic gauche: teleporter au bloc vise."));
        inventory.setItem(1, namedItem(Material.PACKED_ICE, "Freeze", StaffTool.FREEZE, "Clique un joueur pour le freeze ou le liberer."));
        inventory.setItem(2, namedItem(Material.ENDER_EYE, "Vanish", StaffTool.VANISH, "Active ou desactive instantanement le vanish."));
        inventory.setItem(3, namedItem(Material.SPYGLASS, "Spectateur staff", StaffTool.SPECTATOR, "Mode special: hotbar accessible, blocs et vol."));
        inventory.setItem(4, namedItem(Material.PLAYER_HEAD, "Monter sur sa tete", StaffTool.RIDE, "Clique un joueur pour te placer au-dessus de lui."));
        inventory.setItem(5, namedItem(Material.BOOK, "Historique", StaffTool.HISTORY, "Clique un joueur pour voir son historique."));
        inventory.setItem(6, namedItem(Material.PAPER, "Messages", StaffTool.CHAT_LOGS, "Clique un joueur pour voir tous ses messages recents."));
        inventory.setItem(7, namedItem(Material.COMPARATOR, "Moniteur serveur", StaffTool.MONITOR, "Tiens cet objet en main pour voir TPS, RAM et chunks.", "Le moniteur s'arrete des que tu changes de slot."));
        inventory.setItem(8, namedItem(Material.BARRIER, "Quitter", StaffTool.EXIT, "Quitte le staffmode et restaure ton inventaire."));
        inventory.setHeldItemSlot(0);
        player.setGameMode(GameMode.ADVENTURE);
        player.setAllowFlight(true);
        player.setFlying(true);
        this.vanishService.setVanished(player, true);
        this.sessionManager.getOrCreate(player).setInStaffMode(true);
        player.updateInventory();
    }

    private void disable(final Player player) {
        final StaffModeSnapshot snapshot = this.snapshots.remove(player.getUniqueId());
        if (snapshot == null) {
            return;
        }
        stopMonitor(player.getUniqueId());
        this.observationPlayers.remove(player.getUniqueId());
        player.closeInventory();
        final PlayerInventory inventory = player.getInventory();
        inventory.setContents(snapshot.contents());
        inventory.setArmorContents(snapshot.armor());
        inventory.setItemInOffHand(snapshot.offHand());
        player.setGameMode(snapshot.gameMode());
        player.setAllowFlight(snapshot.allowFlight());
        player.setFlying(snapshot.flying());
        if (!snapshot.wasVanished()) {
            this.vanishService.setVanished(player, false);
        }
        this.sessionManager.getOrCreate(player).setInStaffMode(false);
        player.updateInventory();
    }

    private ItemStack namedItem(final Material material, final String name, final StaffTool tool, final String... lore) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, TITLE).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(lore).stream()
            .map(line -> Component.text("▸ ", LABEL)
                .append(Component.text(line, INFO))
                .decoration(TextDecoration.ITALIC, false))
            .toList());
        meta.getPersistentDataContainer().set(TOOL_KEY, PersistentDataType.STRING, tool.name());
        item.setItemMeta(meta);
        return item;
    }

    private StaffTool readTool(final ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }
        final String toolName = item.getItemMeta().getPersistentDataContainer().get(TOOL_KEY, PersistentDataType.STRING);
        if (toolName == null) {
            return null;
        }
        return StaffTool.valueOf(toolName);
    }

    private Component monitorBar() {
        final double tps = Bukkit.getTPS()[0];
        final long usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024L * 1024L);
        final long maxMemory = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
        final int loadedChunks = Bukkit.getWorlds().stream().mapToInt(world -> world.getLoadedChunks().length).sum();
        return Component.join(
            JoinConfiguration.separator(Component.text(" | ", LABEL)),
            Component.text("TPS ", LABEL).append(Component.text(String.format(java.util.Locale.US, "%.2f", tps), tpsColor(tps))),
            Component.text("RAM ", LABEL).append(Component.text(usedMemory + "/" + maxMemory + " MB", INFO)),
            Component.text("Chunks ", LABEL).append(Component.text(Integer.toString(loadedChunks), TITLE))
        ).decoration(TextDecoration.ITALIC, false);
    }

    private TextColor tpsColor(final double tps) {
        if (tps > 18.0D) {
            return TextColor.color(0x5DFF7D);
        }
        if (tps >= 15.0D) {
            return TextColor.color(0xFFAF45);
        }
        return TextColor.color(0xFF6B6B);
    }
}
