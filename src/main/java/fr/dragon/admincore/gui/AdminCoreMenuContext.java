package fr.dragon.admincore.gui;

import fr.dragon.admincore.sanctions.SanctionRecord;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class AdminCoreMenuContext {

    public record SearchResultEntry(String name, int sanctionCount) {
    }

    private AdminCoreMenuContext() {
    }

    public record MainHolder() implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public record ActiveHolder(List<SanctionRecord> sanctions) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public record SearchModeHolder() implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public record ProfileListHolder(int page, List<String> names) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public record SearchResultsHolder(String query, List<SearchResultEntry> results) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public record RecentHolder(List<SanctionRecord> sanctions) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public record OnlineHolder(List<Player> players) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
