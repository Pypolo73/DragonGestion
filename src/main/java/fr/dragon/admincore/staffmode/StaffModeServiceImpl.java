package fr.dragon.admincore.staffmode;

import fr.dragon.admincore.core.PlayerSession;
import fr.dragon.admincore.core.PlayerSessionManager;
import fr.dragon.admincore.vanish.VanishService;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public final class StaffModeServiceImpl implements StaffModeService {

    private final PlayerSessionManager sessionManager;
    private final VanishService vanishService;
    private final Map<UUID, StaffModeSnapshot> snapshots = new ConcurrentHashMap<>();
    private final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> spyPlayers = ConcurrentHashMap.newKeySet();

    public StaffModeServiceImpl(final PlayerSessionManager sessionManager, final VanishService vanishService) {
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

    private void enable(final Player player) {
        final PlayerInventory inventory = player.getInventory();
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
        inventory.setItem(0, namedItem(Material.COMPASS, "Selection"));
        inventory.setItem(1, namedItem(Material.PACKED_ICE, "Freeze"));
        inventory.setItem(2, namedItem(Material.ENDER_EYE, "Vanish"));
        inventory.setItem(3, namedItem(Material.BOOK, "History"));
        inventory.setItem(8, namedItem(Material.BARRIER, "Quitter"));
        player.setGameMode(GameMode.CREATIVE);
        player.setAllowFlight(true);
        player.setFlying(true);
        this.vanishService.setVanished(player, true);
        this.sessionManager.getOrCreate(player).setInStaffMode(true);
    }

    private void disable(final Player player) {
        final StaffModeSnapshot snapshot = this.snapshots.remove(player.getUniqueId());
        if (snapshot == null) {
            return;
        }
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
    }

    private ItemStack namedItem(final Material material, final String name) {
        final ItemStack item = new ItemStack(material);
        final ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text(name));
        item.setItemMeta(meta);
        return item;
    }
}
