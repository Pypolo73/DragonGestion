# AdminCore

AdminCore est un plugin staff pour Paper 1.21.8, structure par modules (`core`, `dialog`, `sanctions`, `chat`, `staffmode`, `vanish`, `database`, `gui`, `util`) et expose une compatibilite `StaffPlusPlus API` via `IStaffPlus`.

## Installation

1. Compiler avec `mvn package`.
2. Copier dans `plugins/` soit `target/admincore-0.1.0-SNAPSHOT.jar`, soit le jar versionne dans `versions/DragonGestion-1.0.X/`.
3. Ne pas utiliser `target/original-*.jar`, c'est l'artefact intermediaire non deployable.
4. Demarrer le serveur pour generer `config.yml` et `messages.yml`.
5. Configurer la base de donnees:
   - `database.type: SQLITE` pour le mode par defaut
   - `database.type: MYSQL` pour MySQL
6. Redemarrer le serveur.

## Dialogs

Les sanctions `tempban`, `tempmute`, `kick` et `warn` ouvrent un flux Dialog si aucun argument inline n'est fourni.

Exemple `tempban`:

1. Dialog raison
2. Dialog duree
3. Confirmation

Si le client est dans la liste `dialogs.blocked-client-brands`, le plugin bascule sur un prompt chat.

## Commandes

- `tempban`, `tempmute`, `ban`, `mute`, `unban`, `unmute`, `kick`, `warn`, `warnings`, `clearwarns`
- `clearchat`, `slowmode`, `chatlock`, `muteall`, `broadcast`
- `staffmode`, `vanish`, `freeze`, `spy`, `invsee`, `stafflist`
- `history`, `lookup`, `alts`, `note`, `notes`, `ipban`, `checkvpn`
- `admincore reload|debug|db export`

## Permissions

Chaque commande utilise la permission correspondante:

- `admincore.tempban`
- `admincore.tempmute`
- `admincore.ban`
- `admincore.mute`
- `admincore.unban`
- `admincore.unmute`
- `admincore.kick`
- `admincore.warn`
- `admincore.warnings`
- `admincore.clearwarns`
- `admincore.clearchat`
- `admincore.slowmode`
- `admincore.chatlock`
- `admincore.muteall`
- `admincore.broadcast`
- `admincore.staffmode`
- `admincore.vanish`
- `admincore.freeze`
- `admincore.spy`
- `admincore.invsee`
- `admincore.stafflist`
- `admincore.history`
- `admincore.lookup`
- `admincore.alts`
- `admincore.note`
- `admincore.notes`
- `admincore.ipban`
- `admincore.checkvpn`
- `admincore.admin`
- `admincore.chat.bypass`
- `admincore.freeze.bypass`
- `admincore.vanish.see`

## Compatibilite StaffPlusPlus

Le plugin enregistre `IStaffPlus` dans le `ServicesManager` Bukkit et expose:

- `BanService`
- `MuteService`
- `WarningService`
- `SessionManager`
- `StaffChatService`
- `ReportService` minimal

La compatibilite reprend le contrat API public StaffPlusPlus, tout en gardant une implementation Paper 1.21.8 native avec Dialogs Adventure.

## Versionnement auto

Chaque `mvn package`:

- incremente automatiquement la version de build `DragonGestion` (`1.0.0` -> `1.0.1` -> `1.0.2`, etc.)
- met a jour la `version` embarquee dans `plugin.yml`
- sort le jar final dans `versions/DragonGestion-1.0.X/DragonGestion-1.0.X.jar`

L'etat de version est stocke dans [.dragon-build.properties](/home/pypoo73/Documents/plugin/DragonGestion/.dragon-build.properties).
