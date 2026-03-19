package fr.dragon.admincore.database;

import java.util.List;
import java.util.UUID;

public record PlayerProfile(
    UUID uuid,
    String name,
    String ip,
    String clientBrand,
    int level,
    int sanctionCount,
    List<String> sanctionTypes
) {
}
