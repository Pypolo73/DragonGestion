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
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

final class AxInventoryRestoreBridge {

    private final AdminCorePlugin plugin;
    private final boolean available;

    AxInventoryRestoreBridge(final AdminCorePlugin plugin) {
        this.plugin = plugin;
        final Plugin dependency = Bukkit.getPluginManager().getPlugin("AxInventoryRestore");
        if (dependency == null || !dependency.isEnabled()) {
            this.plugin.getLogger().warning("AxInventoryRestore absent: le GUI de backups d'inventaire sera desactive.");
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
}
