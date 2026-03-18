package fr.dragon.admincore.vanish;

import fr.dragon.admincore.core.AdminCorePlugin;
import fr.dragon.admincore.core.PermissionService;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.shortninja.staffplusplus.vanish.VanishType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public final class VanishServiceImpl implements VanishService {

    private static final String TEAM_NAME = "admincore_vanished";

    private final AdminCorePlugin plugin;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();

    public VanishServiceImpl(final AdminCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean toggle(final Player player) {
        final boolean newState = !this.vanished.contains(player.getUniqueId());
        setVanished(player, newState);
        return newState;
    }

    @Override
    public void setVanished(final Player player, final boolean vanishedState) {
        if (vanishedState) {
            this.vanished.add(player.getUniqueId());
            this.plugin.getPlayerSessionManager().getOrCreate(player).setVanished(true);
            this.plugin.getPlayerSessionManager().getOrCreate(player).setVanishType(VanishType.TOTAL);
            addToTeam(player);
        } else {
            this.vanished.remove(player.getUniqueId());
            this.plugin.getPlayerSessionManager().getOrCreate(player).setVanished(false);
            this.plugin.getPlayerSessionManager().getOrCreate(player).setVanishType(VanishType.NONE);
            removeFromTeam(player);
        }
        refreshAll();
    }

    @Override
    public boolean isVanished(final UUID uuid) {
        return this.vanished.contains(uuid);
    }

    @Override
    public Set<UUID> getVanishedPlayers() {
        return Set.copyOf(this.vanished);
    }

    @Override
    public void refreshVisibility(final Player viewer) {
        for (final Player target : Bukkit.getOnlinePlayers()) {
            if (target.equals(viewer)) {
                continue;
            }
            if (isVanished(target.getUniqueId()) && !viewer.hasPermission(PermissionService.VANISH_SEE)) {
                viewer.hidePlayer(this.plugin, target);
            } else {
                viewer.showPlayer(this.plugin, target);
            }
        }
    }

    @Override
    public void refreshAll() {
        for (final Player viewer : Bukkit.getOnlinePlayers()) {
            refreshVisibility(viewer);
        }
    }

    private void addToTeam(final Player player) {
        final Team team = getOrCreateTeam();
        team.addEntry(player.getName());
    }

    private void removeFromTeam(final Player player) {
        final Team team = getOrCreateTeam();
        team.removeEntry(player.getName());
    }

    private Team getOrCreateTeam() {
        final Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam(TEAM_NAME);
        if (team == null) {
            team = scoreboard.registerNewTeam(TEAM_NAME);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        return team;
    }
}
