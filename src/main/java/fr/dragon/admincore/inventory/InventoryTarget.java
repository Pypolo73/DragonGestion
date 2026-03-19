package fr.dragon.admincore.inventory;

public enum InventoryTarget {
    INVENTORY("Inventaire", 41),
    ENDER_CHEST("Enderchest", 27);

    private final String label;
    private final int contentSize;

    InventoryTarget(final String label, final int contentSize) {
        this.label = label;
        this.contentSize = contentSize;
    }

    public String label() {
        return this.label;
    }

    public int contentSize() {
        return this.contentSize;
    }

    public boolean isEnderChest() {
        return this == ENDER_CHEST;
    }
}
