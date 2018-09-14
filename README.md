# Perm
This is a simple permissions plugin intended for a multi-server network. It provides player permissions and group permissions. It requires the [SQL plugin](https://github.com/StarTux/SQL) and supports [Vault](https://dev.bukkit.org/projects/vault). Permissions are best changed with a simple but rich set of admin commands.

## Commands
The admin command comes with sufficient online help. Just type any command and the full syntax will be listed. Functions include creating and deleting groups, adding players to groups or removing them again, and assigning permissions to players or groups.
- `/perm player <name> ...` - Player settings
- `/perm group <group> ...` - Group settings
- `/perm reload` - Reload configuration
- `/perm refresh` - Refresh permission cache
- `/perm list <what> ...` - List things

## Permissions
There is only on permission to use the admin command:
- `perm.perm` - Use the `/perm` command.

## Groups
Players can be members of any number of groups. If a player has not been assigned a group, the configured default group is assumed. Each group has a parent group which it inherits all permissions from. Every group has a priority which determines in which order group permissions are inherited. Player permissions always have the highest priority.

## Permissions
Permissions can be assigned either to a group or a player. They can be positive or negative.
