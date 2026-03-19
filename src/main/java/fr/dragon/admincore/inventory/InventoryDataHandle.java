package fr.dragon.admincore.inventory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

final class InventoryDataHandle {

    private final OfflinePlayer target;
    private Player syntheticPlayer;
    private boolean syntheticDirty;

    InventoryDataHandle(final OfflinePlayer target) {
        this.target = target;
    }

    ItemStack[] read(final InventoryTarget targetType) {
        return normalize(extract(holder(), targetType), targetType.contentSize());
    }

    void write(final InventoryTarget targetType, final ItemStack[] contents) {
        final Player holder = holder();
        final ItemStack[] normalized = normalize(contents, targetType.contentSize());
        if (targetType.isEnderChest()) {
            holder.getEnderChest().setContents(normalized);
        } else {
            holder.getInventory().setContents(normalized);
            holder.updateInventory();
        }
        if (holder == this.syntheticPlayer) {
            holder.saveData();
            this.syntheticDirty = false;
        }
    }

    void close() {
        if (this.syntheticPlayer != null && this.syntheticDirty) {
            this.syntheticPlayer.saveData();
            this.syntheticDirty = false;
        }
    }

    private Player holder() {
        final Player online = this.target.getPlayer();
        if (online != null) {
            return online;
        }
        if (this.syntheticPlayer == null) {
            this.syntheticPlayer = createSyntheticPlayer();
            this.syntheticPlayer.loadData();
            this.syntheticDirty = true;
        }
        return this.syntheticPlayer;
    }

    private ItemStack[] extract(final Player holder, final InventoryTarget targetType) {
        if (holder == null) {
            return new ItemStack[0];
        }
        return targetType.isEnderChest()
            ? holder.getEnderChest().getContents()
            : holder.getInventory().getContents();
    }

    private Player createSyntheticPlayer() {
        try {
            final Object craftServer = Bukkit.getServer();
            final Method getServer = craftServer.getClass().getMethod("getServer");
            final Object minecraftServer = getServer.invoke(craftServer);
            final Method overworld = minecraftServer.getClass().getMethod("overworld");
            final Object level = overworld.invoke(minecraftServer);

            final Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            final Constructor<?> gameProfileConstructor = gameProfileClass.getConstructor(UUID.class, String.class);
            final Object profile = gameProfileConstructor.newInstance(this.target.getUniqueId(), fallbackName(this.target));

            final Class<?> clientInformationClass = Class.forName("net.minecraft.server.level.ClientInformation");
            final Method createDefault = clientInformationClass.getMethod("createDefault");
            final Object clientInformation = createDefault.invoke(null);

            final Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
            Constructor<?> constructor = null;
            for (final Constructor<?> candidate : serverPlayerClass.getConstructors()) {
                if (candidate.getParameterCount() == 4) {
                    constructor = candidate;
                    break;
                }
            }
            if (constructor == null) {
                throw new IllegalStateException("Constructeur ServerPlayer introuvable");
            }
            final Object serverPlayer = constructor.newInstance(minecraftServer, level, profile, clientInformation);
            final Method getBukkitEntity = serverPlayerClass.getMethod("getBukkitEntity");
            return (Player) getBukkitEntity.invoke(serverPlayer);
        } catch (final Exception exception) {
            throw new IllegalStateException("Impossible de charger les donnees offline du joueur " + this.target.getUniqueId(), exception);
        }
    }

    private static String fallbackName(final OfflinePlayer target) {
        if (target.getName() != null && !target.getName().isBlank()) {
            return target.getName();
        }
        final String raw = target.getUniqueId().toString().replace("-", "");
        return raw.substring(0, Math.min(16, raw.length()));
    }

    static ItemStack[] normalize(final ItemStack[] source, final int size) {
        final ItemStack[] normalized = new ItemStack[size];
        for (int index = 0; index < Math.min(size, source == null ? 0 : source.length); index++) {
            normalized[index] = clone(source[index]);
        }
        return normalized;
    }

    static ItemStack[] cloneContents(final ItemStack[] source) {
        return source == null ? new ItemStack[0] : Arrays.stream(source).map(InventoryDataHandle::clone).toArray(ItemStack[]::new);
    }

    static ItemStack clone(final ItemStack item) {
        return item == null ? null : item.clone();
    }
}
