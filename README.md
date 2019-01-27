# pmanvil-converter
A tool for converting pmanvil to anvil format

## Rationale
I started using [PocketMine-MP](https://pmmp.io)(PMMP), but soon got into performance and stability issues (I'm not blaming php, but hey, php...). Instead I downloaded [Nukkit](https://github.com/NukkitX/Nukkit), only to discover that the huge world my kids had created was not usable by Nukkit. :-(

In short, Nukkit and PocketMine-MP (and [PocketMine](https://www.pocketmine.net)) use *almost* the same format, but is's not 100% identical. Pocketmine stores the level data as PMAnvil files (`*.mcapm`), while Nukkit (and most other Minecreaft servers) use the Anvil (`*.mca`) format. The difference is that the binary data is transposed in mcapm files, for reasons unknown. (Possibly due to performance reasons in pocketmine map data loading/storing.)

I thought it would be easy to grab a tool to convert these maps, but unfortunately, I could find no such tool. So, I wrote one.

## Usage

You need Java to run this program. Also, you need some understanding of what you are doing, and a working brain.

**Make sure you have proper backups of all critical data before attempting any conversion!**

This program works like the Mojang [McRegion to Anvil conversion tool](https://www.mojang.com/2012/02/new-minecraft-map-format-anvil/) on which it is based. That is, you call it like this:

```java -jar pmanvil-converter.jar /path/to/worlds/basedir world```

where `world` is the name of the world you want to convert. According to a user report, this does not work if the path has spaces in it (common on Windows platform). If this is the case, rename the directory or make a copy to a directory without spaces.

After conversion, the old mcapm files are left in place, and can be removed.

A typical usage scenario can look like this. **Beware that this is an example only. You need to supply your own paths. Always make backups before deleting files.**
```
cd /opt/nukkit
wget https://github.com/magicus/pmanvil-converter/releases/download/v1.0/pmanvil-converter.jar
rm -rf worlds
cp -r /opt/pocketmine/worlds .
java -jar pmanvil-converter.jar worlds world
rm -rf worlds/world/region/r*.mcapm
```

## Installation

* [Download the prebuilt jar file](https://github.com/magicus/pmanvil-converter/releases/download/v1.0/pmanvil-converter.jar)
* Or clone this repo and build it yourself

## Credits

Most of the credits for this work goes to someone else. :-)
* [Mojang](https://www.mojang.com) wrote the original [McRegion to Anvil conversion tool](https://www.mojang.com/2012/02/new-minecraft-map-format-anvil/). This is just a slightly modified copy of that tool.
* [Awzaw](https://github.com/Awzaw) created [another copy of the Mojang tool](https://github.com/Awzaw/AnvilConverter), which made it clear to me how the PMAnvil format worked. The bytearray flipping code comes from him.
