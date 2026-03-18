package fr.dragon.admincore.vanish;

import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;

public interface VanishService {

    boolean toggle(Player player);

    void setVanished(Player player, boolean vanished);

    boolean isVanished(UUID uuid);

    Set<UUID> getVanishedPlayers();

    void refreshVisibility(Player player);

    void refreshAll();
}
