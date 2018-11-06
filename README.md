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

