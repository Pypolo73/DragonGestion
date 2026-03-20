package fr.dragon.admincore.inventory;

import fr.dragon.admincore.core.AdminCorePlugin;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

final class AxInventoryRestoreBridge {

    private static final String AX_MAIN_CLASS = "com.artillexstudios.axinventoryrestore.AxInventoryRestore";

    private final AdminCorePlugin plugin;
    private final boolean available;

    AxInventoryRestoreBridge(final AdminCorePlugin plugin) {
        this.plugin = plugin;
        final Plugin dependency = locatePlugin();
        if (dependency == null || !dependency.isEnabled()) {
            this.plugin.getLogger().warning("AxInventoryRestore absent: le GUI de backups d'inventaire sera desactive.");
            this.available = false;
            return;
        }
        if (!hasCompatibleApi()) {
            this.plugin.getLogger().warning("AxInventoryRestore detecte, mais son API publique est incompatible avec ce module.");
            this.available = false;
            return;
        }
        this.available = true;
    }

    boolean isAvailable() {
        return this.available;
    }

    CompletableFuture<List<InventoryBackupSnapshot>> loadSnapshots(final OfflinePlayer target, final InventoryTarget targetType) {
        if (!this.available) {
            return CompletableFuture.completedFuture(List.of());
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                final Class<?> axClass = Class.forName("com.artillexstudios.axinventoryrestore.AxInventoryRestore");
                final Object database = axClass.getMethod("getDatabase").invoke(null);
                final Object backup = database.getClass().getMethod("getBackupsOfPlayer", UUID.class).invoke(database, target.getUniqueId());
                if (backup == null) {
                    return List.<InventoryBackupSnapshot>of();
                }
                final Method getBackupDataList = backup.getClass().getMethod("getBackupDataList");
                final List<?> rawSnapshots = (List<?>) getBackupDataList.invoke(backup);
                return rawSnapshots.stream()
                    .filter(snapshot -> accept(targetType, invokeString(snapshot, "getReason")))
                    .map(snapshot -> new InventoryBackupSnapshot(
                        invokeInt(snapshot, "getId"),
                        Instant.ofEpochMilli(invokeLong(snapshot, "getDate")),
                        invokeString(snapshot, "getReason"),
                        snapshot
                    ))
                    .sorted(Comparator.comparing(InventoryBackupSnapshot::createdAt).reversed())
                    .toList();
            } catch (final Exception exception) {
                throw new IllegalStateException("Impossible de lire les backups AxInventoryRestore", exception);
            }
        });
    }

    CompletableFuture<ItemStack[]> loadItems(final InventoryBackupSnapshot snapshot) {
        if (!this.available || snapshot == null || snapshot.handle() == null) {
            return CompletableFuture.completedFuture(new ItemStack[0]);
        }
        try {
            final Method getItems = snapshot.handle().getClass().getMethod("getItems");
            @SuppressWarnings("unchecked")
            final CompletableFuture<ItemStack[]> future = (CompletableFuture<ItemStack[]>) getItems.invoke(snapshot.handle());
            return future.thenApply(items -> InventoryDataHandle.cloneContents(items));
        } catch (final Exception exception) {
            return CompletableFuture.failedFuture(new IllegalStateException("Impossible de charger le contenu du snapshot " + snapshot.id(), exception));
        }
    }

    CompletableFuture<Void> createBackup(final OfflinePlayer target, final InventoryTarget targetType, final String actorName) {
        if (!this.available) {
            return CompletableFuture.failedFuture(new IllegalStateException("AxInventoryRestore indisponible"));
        }
        final org.bukkit.entity.Player online = target.getPlayer();
        if (online == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Le joueur doit etre en ligne pour creer un backup manuel"));
        }
        return CompletableFuture.runAsync(() -> {
            try {
                final Class<?> axClass = Class.forName(AX_MAIN_CLASS);
                final Object database = axClass.getMethod("getDatabase").invoke(null);
                final String cause = "Cree par " + actorName;
                if (targetType.isEnderChest()) {
                    database.getClass().getMethod("saveInventory", ItemStack[].class, org.bukkit.entity.Player.class, String.class, String.class)
                        .invoke(database, online.getEnderChest().getStorageContents(), online, "ENDER_CHEST", cause);
                    return;
                }
                database.getClass().getMethod("saveInventory", org.bukkit.entity.Player.class, String.class, String.class)
                    .invoke(database, online, "MANUAL", cause);
            } catch (final Exception exception) {
                throw new IllegalStateException("Impossible de creer un backup manuel AxInventoryRestore", exception);
            }
        });
    }

    void openNativeGui(final Player viewer, final OfflinePlayer target) {
        if (!this.available) {
            throw new IllegalStateException("AxInventoryRestore indisponible");
        }
        try {
            final Class<?> mainGuiClass = Class.forName("com.artillexstudios.axinventoryrestore.guis.MainGui");
            final Object mainGui = mainGuiClass
                .getConstructor(UUID.class, Player.class, String.class)
                .newInstance(target.getUniqueId(), viewer, target.getName() == null ? target.getUniqueId().toString() : target.getName());
            mainGuiClass.getMethod("open").invoke(mainGui);
        } catch (final Exception exception) {
            throw new IllegalStateException("Impossible d'ouvrir le GUI AxInventoryRestore", exception);
        }
    }

    private static boolean accept(final InventoryTarget targetType, final String reason) {
        return targetType.isEnderChest() ? "ENDER_CHEST".equalsIgnoreCase(reason) : !"ENDER_CHEST".equalsIgnoreCase(reason);
    }

    private static String invokeString(final Object target, final String method) {
        try {
            return String.valueOf(target.getClass().getMethod(method).invoke(target));
        } catch (final Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static int invokeInt(final Object target, final String method) {
        try {
            return ((Number) target.getClass().getMethod(method).invoke(target)).intValue();
        } catch (final Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static long invokeLong(final Object target, final String method) {
        try {
            return ((Number) target.getClass().getMethod(method).invoke(target)).longValue();
        } catch (final Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Plugin locatePlugin() {
        final Plugin exact = Bukkit.getPluginManager().getPlugin("AxInventoryRestore");
        if (exact != null) {
            return exact;
        }
        final Plugin alias = Bukkit.getPluginManager().getPlugin("AxInventoryBackup");
        if (alias != null) {
            return alias;
        }
        for (final Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (AX_MAIN_CLASS.equals(plugin.getDescription().getMain())) {
                return plugin;
            }
        }
        return null;
    }

    private boolean hasCompatibleApi() {
        try {
            final Class<?> axClass = Class.forName(AX_MAIN_CLASS);
            final Method getDatabase = axClass.getMethod("getDatabase");
            final Object database = getDatabase.invoke(null);
            if (database == null) {
                return false;
            }
            database.getClass().getMethod("getBackupsOfPlayer", UUID.class);
            final Class<?> backupDataClass = Class.forName("com.artillexstudios.axinventoryrestore.backups.BackupData");
            backupDataClass.getMethod("getItems");
            backupDataClass.getMethod("getReason");
            return true;
        } catch (final Exception exception) {
            return false;
        }
    }
}
