BeastMaster
===========
A Bukkit plugin that handles custom mob spawning.


Features
--------
Currently this is just a small plugin that restores Minecraft 1.10 wither
skeleton spawning behaviour to 1.11 survival mode servers. In specific terms, it
replaces a configurable percentage of skeletons spawned in the PLAINS biome in
the NETHER environement with wither skeletons.

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
