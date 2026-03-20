package fr.dragon.admincore.staffmode;

import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;

public interface StaffModeService {

    boolean toggleStaffMode(Player player);

    boolean isInStaffMode(UUID uuid);

    void clearStaffState(UUID uuid);

    boolean toggleObservationMode(Player player);

    boolean isObservationMode(UUID uuid);

    boolean toggleFreeze(Player target);

    boolean isFrozen(UUID uuid);

    boolean toggleSpy(Player player);

    boolean isSpyEnabled(UUID uuid);

    Set<UUID> getFrozenPlayers();

    Set<UUID> getSpyPlayers();

    void refreshMonitor(Player player);

    void stopMonitor(UUID uuid);
}
