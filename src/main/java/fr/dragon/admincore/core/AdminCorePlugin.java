package fr.dragon.admincore.core;

import fr.dragon.admincore.chat.ChatService;
import fr.dragon.admincore.chat.ChatServiceImpl;
import fr.dragon.admincore.chat.ChatCommand;
import fr.dragon.admincore.chat.ChatListener;
import fr.dragon.admincore.database.DatabaseManager;
import fr.dragon.admincore.database.NoteRepository;
import fr.dragon.admincore.database.PlayerRepository;
import fr.dragon.admincore.database.SanctionRepository;
import fr.dragon.admincore.dialog.ChatPromptService;
import fr.dragon.admincore.dialog.DialogSupportService;
import fr.dragon.admincore.gui.GuiListener;
import fr.dragon.admincore.inventory.InventoryCommand;
import fr.dragon.admincore.inventory.InventoryListener;
import fr.dragon.admincore.inventory.InventoryManagerService;
import fr.dragon.admincore.sanctions.ConnectionSanctionListener;
import fr.dragon.admincore.sanctions.SanctionApprovalService;
import fr.dragon.admincore.sanctions.SanctionCommand;
import fr.dragon.admincore.sanctions.SanctionService;
import fr.dragon.admincore.sanctions.SanctionServiceImpl;
import fr.dragon.admincore.sanctions.SanctionsAdminCommand;
import fr.dragon.admincore.staffmode.StaffCommand;
import fr.dragon.admincore.staffmode.StaffModeListener;
import fr.dragon.admincore.staffmode.StaffModeService;
import fr.dragon.admincore.staffmode.StaffModeServiceImpl;
import fr.dragon.admincore.util.ConfigLoader;
import fr.dragon.admincore.util.MessageFormatter;
import fr.dragon.admincore.vanish.VanishCommand;
import fr.dragon.admincore.vanish.VanishService;
import fr.dragon.admincore.vanish.VanishServiceImpl;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class AdminCorePlugin extends JavaPlugin {

    private ConfigLoader configLoader;
    private MessageFormatter messageFormatter;
    private PermissionService permissionService;
    private DatabaseManager databaseManager;
    private PlayerSessionManager playerSessionManager;
    private StaffAccessService staffAccessService;
    private SanctionService sanctionService;
    private SanctionApprovalService sanctionApprovalService;
    private ChatService chatService;
    private VanishService vanishService;
    private StaffModeService staffModeService;
    private ChatPromptService chatPromptService;
    private DialogSupportService dialogSupportService;
    private InventoryManagerService inventoryManagerService;
    private BukkitTask runtimeRefreshTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.configLoader = new ConfigLoader(this);
        this.configLoader.reload();
        this.messageFormatter = new MessageFormatter(this.configLoader);
        this.permissionService = new PermissionService(this.messageFormatter);
        this.playerSessionManager = new PlayerSessionManager();
        this.staffAccessService = new StaffAccessService(this, this.configLoader);

        this.databaseManager = new DatabaseManager(this, this.configLoader);
        this.databaseManager.start();

        final SanctionRepository sanctionRepository = new SanctionRepository(this.databaseManager);
        final PlayerRepository playerRepository = new PlayerRepository(this.databaseManager);
        final NoteRepository noteRepository = new NoteRepository(this.databaseManager);
        this.sanctionService = new SanctionServiceImpl(this.databaseManager, sanctionRepository, playerRepository, noteRepository, this.configLoader);
        this.sanctionApprovalService = new SanctionApprovalService(this);
        this.chatService = new ChatServiceImpl(this.configLoader, this.messageFormatter);
        this.vanishService = new VanishServiceImpl(this);
        this.staffModeService = new StaffModeServiceImpl(this.playerSessionManager, this.vanishService);
        this.chatPromptService = new ChatPromptService(this, this.messageFormatter);
        this.dialogSupportService = new DialogSupportService(this, this.configLoader, this.messageFormatter, this.chatPromptService);
        this.inventoryManagerService = new InventoryManagerService(this);

        AdminCoreAPI.bind(this, this.sanctionService, this.vanishService, this.staffModeService, this.chatService, this.playerSessionManager);

        registerCommands();
        registerListeners();
        scheduleRuntimeRefreshTask();
        for (final var online : Bukkit.getOnlinePlayers()) {
            this.staffAccessService.refresh(online);
        }
        getLogger().info("AdminCore active.");
    }

    @Override
    public void onDisable() {
        AdminCoreAPI.clear();
        if (this.runtimeRefreshTask != null) {
            this.runtimeRefreshTask.cancel();
            this.runtimeRefreshTask = null;
        }
        if (this.databaseManager != null) {
            this.databaseManager.close();
        }
    }

    public void reloadPlugin() {
        this.configLoader.reload();
        this.messageFormatter = new MessageFormatter(this.configLoader);
        this.permissionService = new PermissionService(this.messageFormatter);
        this.staffAccessService.reloadStorage();
        this.inventoryManagerService.reload();
        scheduleRuntimeRefreshTask();
    }

    public ConfigLoader getConfigLoader() {
        return this.configLoader;
    }

    public MessageFormatter getMessageFormatter() {
        return this.messageFormatter;
    }

    public PermissionService getPermissionService() {
        return this.permissionService;
    }

    public PlayerSessionManager getPlayerSessionManager() {
        return this.playerSessionManager;
    }

    public StaffAccessService getStaffAccessService() {
        return this.staffAccessService;
    }

    public SanctionService getSanctionService() {
        return this.sanctionService;
    }

    public SanctionApprovalService getSanctionApprovalService() {
        return this.sanctionApprovalService;
    }

    public ChatService getChatService() {
        return this.chatService;
    }

    public VanishService getVanishService() {
        return this.vanishService;
    }

    public StaffModeService getStaffModeService() {
        return this.staffModeService;
    }

    public DialogSupportService getDialogSupportService() {
        return this.dialogSupportService;
    }

    public ChatPromptService getChatPromptService() {
        return this.chatPromptService;
    }

    public InventoryManagerService getInventoryManagerService() {
        return this.inventoryManagerService;
    }

    private void registerCommands() {
        final SanctionCommand sanctionCommand = new SanctionCommand(this);
        final SanctionsAdminCommand sanctionsAdminCommand = new SanctionsAdminCommand(this);
        final ChatCommand chatCommand = new ChatCommand(this);
        final StaffCommand staffCommand = new StaffCommand(this);
        final VanishCommand vanishCommand = new VanishCommand(this);
        final AdminCoreCommand adminCoreCommand = new AdminCoreCommand(this);
        final InventoryCommand inventoryCommand = new InventoryCommand(this);

        bind("tempban", sanctionCommand);
        bind("tempmute", sanctionCommand);
        bind("ban", sanctionCommand);
        bind("mute", sanctionCommand);
        bind("kick", sanctionCommand);
        bind("warn", sanctionCommand);

        bind("unban", sanctionsAdminCommand);
        bind("unmute", sanctionsAdminCommand);
        bind("warnings", sanctionsAdminCommand);
        bind("clearwarns", sanctionsAdminCommand);
        bind("history", sanctionsAdminCommand);
        bind("lookup", sanctionsAdminCommand);
        bind("alts", sanctionsAdminCommand);
        bind("note", sanctionsAdminCommand);
        bind("notes", sanctionsAdminCommand);
        bind("ipban", sanctionsAdminCommand);
        bind("checkvpn", sanctionsAdminCommand);

        bind("clearchat", chatCommand);
        bind("chat", chatCommand);
        bind("slowmode", chatCommand);
        bind("chatlock", chatCommand);
        bind("muteall", chatCommand);
        bind("broadcast", chatCommand);

        bind("staffmode", staffCommand);
        bind("staff", staffCommand);
        bind("freeze", staffCommand);
        bind("spy", staffCommand);
        bind("invsee", staffCommand);
        bind("stafflist", staffCommand);

        bind("vanish", vanishCommand);
        bind("admincore", adminCoreCommand);
        bind("inventory", inventoryCommand);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new ConnectionSanctionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new StaffModeListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new InventoryListener(this), this);
    }

    private void bind(final String name, final CommandExecutor executor) {
        final PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Commande absente du plugin.yml: " + name);
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof org.bukkit.command.TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }

    private void scheduleRuntimeRefreshTask() {
        if (this.runtimeRefreshTask != null) {
            this.runtimeRefreshTask.cancel();
        }
        final long seconds = Math.max(10L, this.configLoader.config().getLong("staff.runtime-refresh-seconds", 30L));
        this.runtimeRefreshTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (final var online : Bukkit.getOnlinePlayers()) {
                this.staffAccessService.refresh(online);
            }
        }, seconds * 20L, seconds * 20L);
    }
}
