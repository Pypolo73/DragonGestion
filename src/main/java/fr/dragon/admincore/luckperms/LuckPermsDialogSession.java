package fr.dragon.admincore.luckperms;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

final class LuckPermsDialogSession {

    private final UUID staffUuid;
    private String groupName;
    private final Deque<PermissionAction> undo = new ArrayDeque<>();
    private final Deque<PermissionAction> redo = new ArrayDeque<>();

    LuckPermsDialogSession(final UUID staffUuid) {
        this.staffUuid = staffUuid;
    }

    UUID staffUuid() {
        return this.staffUuid;
    }

    String groupName() {
        return this.groupName;
    }

    void groupName(final String groupName) {
        this.groupName = groupName;
    }

    Deque<PermissionAction> undo() {
        return this.undo;
    }

    Deque<PermissionAction> redo() {
        return this.redo;
    }

    void pushUndo(final PermissionAction action) {
        if (this.undo.size() >= 10) {
            this.undo.removeFirst();
        }
        this.undo.addLast(action);
        this.redo.clear();
    }

    void clearAll() {
        this.undo.clear();
        this.redo.clear();
        this.groupName = null;
    }
}
