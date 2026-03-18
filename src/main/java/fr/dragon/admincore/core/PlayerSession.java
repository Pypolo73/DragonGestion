package fr.dragon.admincore.core;

import java.util.Optional;
import java.util.UUID;
import net.shortninja.staffplusplus.session.IPlayerSession;
import net.shortninja.staffplusplus.vanish.VanishType;

public final class PlayerSession implements IPlayerSession {

    private final UUID uuid;
    private final String name;
    private boolean frozen;
    private boolean protectedState;
    private String activeStaffChatChannel;
    private boolean underInvestigation;
    private boolean muted;
    private boolean vanished;
    private boolean inStaffMode;
    private VanishType vanishType = VanishType.NONE;
    private boolean canViewStyleIds;

    public PlayerSession(final UUID uuid, final String name) {
        this.uuid = uuid;
        this.name = name;
    }

    @Override
    public UUID getUuid() {
        return this.uuid;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean isFrozen() {
        return this.frozen;
    }

    public void setFrozen(final boolean frozen) {
        this.frozen = frozen;
    }

    @Override
    public void setProtected(final boolean protectedState) {
        this.protectedState = protectedState;
    }

    @Override
    public boolean isProtected() {
        return this.protectedState;
    }

    @Override
    public Optional<String> getActiveStaffChatChannel() {
        return Optional.ofNullable(this.activeStaffChatChannel);
    }

    @Override
    public void setActiveStaffChatChannel(final String activeStaffChatChannel) {
        this.activeStaffChatChannel = activeStaffChatChannel;
    }

    @Override
    public void setUnderInvestigation(final boolean underInvestigation) {
        this.underInvestigation = underInvestigation;
    }

    @Override
    public void setMuted(final boolean muted) {
        this.muted = muted;
    }

    @Override
    public boolean isUnderInvestigation() {
        return this.underInvestigation;
    }

    @Override
    public boolean isMuted() {
        return this.muted;
    }

    @Override
    public boolean isVanished() {
        return this.vanished;
    }

    public void setVanished(final boolean vanished) {
        this.vanished = vanished;
    }

    @Override
    public boolean isInStaffMode() {
        return this.inStaffMode;
    }

    public void setInStaffMode(final boolean inStaffMode) {
        this.inStaffMode = inStaffMode;
    }

    @Override
    public VanishType getVanishType() {
        return this.vanishType;
    }

    public void setVanishType(final VanishType vanishType) {
        this.vanishType = vanishType;
    }

    @Override
    public void setCanViewStyleIds(final boolean canViewStyleIds) {
        this.canViewStyleIds = canViewStyleIds;
    }

    public boolean canViewStyleIds() {
        return this.canViewStyleIds;
    }
}
