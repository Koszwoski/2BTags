# 2BTags

2BTags is a client-side Fabric mod that shows a player's group memberships above their Minecraft nameplate.

It is made for communities that want shared group tags without requiring a server-side mod. Tags, colours and optional logos are loaded from the 2BTags API.

An example: 

<img width="521" height="907" alt="screenshot" src="https://github.com/user-attachments/assets/211e942b-7597-42e7-907c-ddec779777af" />

## Groups added - 09/22/2026

  - 2B2T Party Committee
  - Astral Brotherhood
  - Divinity
  - Donfuer
  - Emperium
  - Enclave
  - Fifth Column
  - Highway Workers Union
  - Hunters Union
  - Journeymen
  - Loot Lords
  - Mercenaries Corp
  - New Spawn Order
  - Spawn Builders Association
  - SpawnMasons
  - Stasis Co
  - The Devs
  - The Imperials
  - The Trading Post
  - Vapepens Elite Alliance

    Can you help me add more groups or IGN's let me know.


## What it does

- Shows a compact stack of group names above a player's IGN.
- Supports multiple groups per player.
- Keeps the primary group at the top of the stack.
- Uses each group's configured colour and optional logo.
- Refreshes nearby-player tags automatically.
- Works alongside Meteor's player nametag setting.
- Lets each player hide or show tags with a configurable keybind.

The default keybind is **G**. Change it in **Options → Controls → 2BTags**.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for the matching Minecraft version.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api).
3. Download the 2BTags JAR from [Releases](https://github.com/Koszwoski/2BTags/releases).
4. Place the JAR in your Minecraft instance's `mods` folder.
5. Launch Minecraft.

No server-side mod is required.

## Compatibility

This branch (`mc-1.21.4`) targets **Minecraft 1.21.4**. 2BTags is also available for other Minecraft versions, each on its own branch:

| Minecraft | Branch |
| --- | --- |
| 1.21.11 | [`main`](this branch) |
| 1.21.4 | [`mc-1.21.4`](https://github.com/Koszwoski/2BTags/tree/mc-1.21.4) |
| 1.21.1 | [`mc-1.21.1`](https://github.com/Koszwoski/2BTags/tree/mc-1.21.1) |

Prebuilt JARs for all versions are published on the [Releases](https://github.com/Koszwoski/2BTags/releases) page.

| Requirement | Version |
| --- | --- |
| Java | 21 or newer |
| Fabric Loader | 0.19.5 or newer |
| Fabric API | Required |

## In game

When a tagged player is nearby, their groups are rendered as separate lines above their name:

```text
[logo] The Devs
[logo] Divinity
[logo] Journeymen
PlayerName
```

Groups are supplied by the API in display order. The client does not let players edit tags locally.

## Network behaviour

2BTags sends the UUIDs of nearby players to its lookup endpoint and receives their public tag data. It checks periodically while you are in a world and clears cached tags when leaving it.

## Public data export

For transparency, a public export of player group assignments is available in the [2BTags Daily Export](https://github.com/Koszwoski/2BTags-Daily-Export/tree/data/exports) repository.

The export is updated automatically when player or group assignments change.

## Contact

Feel free to contact me if you have any corrections or changes to suggest.

**Discord / Minecraft IGN:** Koszwoski

**Discord Server:** https://discord.gg/EPE7VFDUVy

## Special thanks to

https://2b2t.miraheze.org | for the first data, some might be outdated but its a great start.

## Development

```bash
./gradlew build
```

The built JAR is written to:

```text
build/libs/
```

## License

[MIT](LICENSE)
