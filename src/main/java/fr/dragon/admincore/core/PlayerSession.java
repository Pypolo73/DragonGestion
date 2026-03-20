package fr.dragon.admincore.core;

import java.util.Optional;
import java.util.UUID;

public final class PlayerSession {

    private final UUID uuid;
    private final String name;
    private boolean frozen;
    private boolean protectedState;
    private String activeStaffChatChannel;
    private boolean underInvestigation;
    private boolean muted;
    private boolean vanished;
    private boolean inStaffMode;
    private PlayerVanishState vanishType = PlayerVanishState.NONE;
    private boolean canViewStyleIds;

    public PlayerSession(final UUID uuid, final String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID getUuid() {
        return this.uuid;
    }

    public String getName() {
        return this.name;
    }

    public boolean isFrozen() {
        return this.frozen;
    }

    public void setFrozen(final boolean frozen) {
        this.frozen = frozen;
    }

    public void setProtected(final boolean protectedState) {
        this.protectedState = protectedState;
    }

    public boolean isProtected() {
        return this.protectedState;
    }

    public Optional<String> getActiveStaffChatChannel() {
        return Optional.ofNullable(this.activeStaffChatChannel);
    }

    public void setActiveStaffChatChannel(final String activeStaffChatChannel) {
        this.activeStaffChatChannel = activeStaffChatChannel;
    }

    public void setUnderInvestigation(final boolean underInvestigation) {
        this.underInvestigation = underInvestigation;
    }

    public void setMuted(final boolean muted) {
        this.muted = muted;
    }

    public boolean isUnderInvestigation() {
        return this.underInvestigation;
    }

    public boolean isMuted() {
        return this.muted;
    }

    public boolean isVanished() {
        return this.vanished;
    }

    public void setVanished(final boolean vanished) {
        this.vanished = vanished;
    }

    public boolean isInStaffMode() {
        return this.inStaffMode;
    }

    public void setInStaffMode(final boolean inStaffMode) {
        this.inStaffMode = inStaffMode;
    }

    public PlayerVanishState getVanishType() {
        return this.vanishType;
    }

    public void setVanishType(final PlayerVanishState vanishType) {
        this.vanishType = vanishType;
    }

    public void setCanViewStyleIds(final boolean canViewStyleIds) {
        this.canViewStyleIds = canViewStyleIds;
    }

    public boolean canViewStyleIds() {
        return this.canViewStyleIds;
    }
}
