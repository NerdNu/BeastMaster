BeastMaster
===========
A Bukkit plugin that handles custom mob spawning and custom drops for mobs and
mining.


Features
--------
 * BeastMaster can spawn custom mobs with the following characteristics:
   * They are based on a Minecraft mob type.
   * They can have a custom name, and it can be shown or not.
   * They can be baby mobs with a certain probability, if the mob type supports
     it.
   * They wear defined gear (armour, items in hand), with specified drop
     probabilities for each item.
   * They have a specified maximum health.
   * They have a specified maximum speed.
   * They drop a specified amount of XP on death.
   * They do not pick up items (to prevent easy ways of taking their gear).
   * They can have custom drops, in addition to or instead of their vanilla
     Minecraft drops.
   * They can impart on-hit potion effects.
   * Creepers can have custom explosion power.
 * BeastMaster can drop custom items when a block is mined.
 * Dropped items are defined by running a command while holding the item.
 * TODO Dropped items can have an associated sound and XP bonus.
 * Drop probabilities are managed with *tables*, which specify the following
   information for one or more items:
   * Probability of dropping the item.
   * Minimum and maximum number of items to drop. 


Concepts
--------
### Zones

Zones are volumes of space where plugin behaviours are enabled. They are similar
in principle to WorldGuard *regions*.

Zones are arranged in a parent-child hierarchy. Child zones inherit behaviours
(such as spawning custom mobs) from their immediate parent.


### Profiles

A profile





Features
--------
TODO: Revise

Currently this is just a small plugin that restores Minecraft 1.10 wither
skeleton spawning behaviour to 1.11 survival mode servers. In specific terms, it
replaces a configurable percentage of skeletons spawned in the PLAINS biome in
the NETHER environment with wither skeletons.

Time permitting, this plugin may be expanded to be a more general custom-mob
spawning system with facilities like OtherDrops (currently defunct) or
MythicMobs.

The design principles for expanding the capabilities of the plugin would be:

 * No reliance on NMS classes, since that presents a maintenance obstacle.
 * Loose coupling to other plugins (soft dependencies only).
 * As much as possible, in-game configuration only: mob types, including their
   equipment should be designable in-game with minimal commands and no 
   configuration editing. Their spawnable areas should be defined in-game as
   spheres or rectangular prisms from a specified location.
 * Complex configuration languages should be avoided; YAML should not be abused
   for this purpose: most Bukkit plugin configuration languages based on YAML
   have horrible, quirky, poorly documented syntax. OtherDrops and MythicMobs
   are prime examples of this. The YAML parser in Bukkit is extremely
   unforgiving of minor syntax errors. Instead of YAML, the configuration
   language, which would be used mostly for configurating mob *behaviours*,
   should use a pre-existing Java implementation of an established scripting
   language like Lua, Python (Jython) or Groovy to set properties of a
   well-documented object model.


Commands
--------

 * `/beastmaster reload` - Reload the plugin configuration.


Configuration
-------------

| Setting | Description |
| :--- | :--- |
| `debug.config` | If true, log the configuration on reload. |
| `debug.replace` | If true, log replacement of skeletons by wither skeletons. |
| `chance.wither-skeleton` | Probability, in the range [0.0,1.0] that a plains biome skeleton spawn in the nether environment will be replaced by a wither skeleton. |


Permissions
-----------

 * `beastmaster.admin` - Permission to administer the plugin (run `/beastmaster reload`).


Build Instructions
------------------

* NOTE: Maven gets confused by the presence of a pom.xml file in the current working directory. Do these commands somewhere else.

Install [BlockStore](https://www.spigotmc.org/resources/blockstore.19494/) in 
the local Maven repository:

```
mvn install:install-file -DgroupId=net.sothatsit -DartifactId=blockstore \
    -Dversion=1.4 -Dpackaging=jar -Dfile=/path/to/BlockStore.jar
```

