package fr.dragon.admincore.luckperms;

public record PermissionAction(String group, String node, boolean before, boolean after) {
}
