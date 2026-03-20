package fr.dragon.admincore.core;

import fr.dragon.admincore.util.MessageFormatter;
import org.bukkit.command.CommandSender;

public final class PermissionService {

    public static final String STAFF = "admincore.staff";
    public static final String TEMPBAN = "admincore.tempban";
    public static final String TEMPMUTE = "admincore.tempmute";
    public static final String BAN = "admincore.ban";
    public static final String MUTE = "admincore.mute";
    public static final String UNBAN = "admincore.unban";
    public static final String UNMUTE = "admincore.unmute";
    public static final String KICK = "admincore.kick";
    public static final String WARN = "admincore.warn";
    public static final String WARNINGS = "admincore.warnings";
    public static final String CLEARWARNS = "admincore.clearwarns";
    public static final String CLEARCHAT = "admincore.clearchat";
    public static final String SLOWMODE = "admincore.slowmode";
    public static final String CHATLOCK = "admincore.chatlock";
    public static final String MUTEALL = "admincore.muteall";
    public static final String BROADCAST = "admincore.broadcast";
    public static final String STAFFMODE = "admincore.staffmode";
    public static final String VANISH = "admincore.vanish";
    public static final String FREEZE = "admincore.freeze";
    public static final String SPY = "admincore.spy";
    public static final String INVSEE = "admincore.invsee";
    public static final String STAFFLIST = "admincore.stafflist";
    public static final String HISTORY = "admincore.history";
    public static final String LOOKUP = "admincore.lookup";
    public static final String ALTS = "admincore.alts";
    public static final String NOTE = "admincore.note";
    public static final String NOTES = "admincore.notes";
    public static final String IPBAN = "admincore.ipban";
    public static final String CHECKVPN = "admincore.checkvpn";
    public static final String ALERTS = "admincore.alerts";
    public static final String REPORTS = "admincore.reports";
    public static final String STAFFTICKET = "admincore.staffticket";
    public static final String STAFFLUCKPERM = "admincore.staffluckperm";
    public static final String INVENTORY_VIEW = "admincore.inventory.view";
    public static final String INVENTORY_EDIT = "admincore.inventory.edit";
    public static final String INVENTORY_BACKUP = "admincore.inventory.backup";
    public static final String STAFFLOGS = "admincore.stafflogs";
    public static final String ADMIN = "admincore.admin";
    public static final String CHAT_BYPASS = "admincore.chat.bypass";
    public static final String VANISH_SEE = "admincore.vanish.see";
    public static final String FREEZE_BYPASS = "admincore.freeze.bypass";

    private final MessageFormatter formatter;

    public PermissionService(final MessageFormatter formatter) {
        this.formatter = formatter;
    }

    public boolean check(final CommandSender sender, final String permission) {
        if (sender.hasPermission(permission)) {
            return true;
        }
        sender.sendMessage(this.formatter.message("errors.no-permission"));
        return false;
    }
}
