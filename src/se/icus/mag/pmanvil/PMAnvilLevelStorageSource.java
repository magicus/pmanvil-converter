package se.icus.mag.pmanvil;

/**
 * Copyright Mojang AB.
 * 
 * Don't do evil.
 *
 * Adaptations for PMAnvil conversions by magicus 2018.
 */

import java.io.*;
import java.util.ArrayList;

import net.minecraft.world.level.chunk.storage.*;

import com.mojang.nbt.*;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.ProgressListener;

public class PMAnvilLevelStorageSource {

    public static final String PM_ANVIL_EXTENSION = ".mcapm";

    private File baseDir;

    public PMAnvilLevelStorageSource(File dir) {
        baseDir = dir;
    }

    public boolean isConvertible(String levelId) {

        // Pocketmine does not set level data version properly, so ignore it
        // Instead, check if there is mcapm files present
        File baseFolder = new File(baseDir, levelId);
        File regionFolder = new File(baseFolder, "region");
        File[] list = regionFolder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(PM_ANVIL_EXTENSION);
            }
        });

        if (list == null) {
            return false;
        }

        return true;
    }

    private CompoundTag getDataTagFor(String levelId) {
        File dir = new File(baseDir, levelId);
        if (!dir.exists()) return null;

        File dataFile = new File(dir, "level.dat");
        if (dataFile.exists()) {
            try {
                CompoundTag root = NbtIo.readCompressed(new FileInputStream(dataFile));
                CompoundTag tag = root.getCompound("Data");
                return tag;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        dataFile = new File(dir, "level.dat_old");
        if (dataFile.exists()) {
            try {
                CompoundTag root = NbtIo.readCompressed(new FileInputStream(dataFile));
                CompoundTag tag = root.getCompound("Data");
                return tag;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public boolean convertLevel(String levelId, ProgressListener progress) {

        progress.progressStagePercentage(0);

        ArrayList<File> normalRegions = new ArrayList<File>();
        ArrayList<File> netherRegions = new ArrayList<File>();
        ArrayList<File> enderRegions = new ArrayList<File>();
//
        File baseFolder = new File(baseDir, levelId);
        File netherFolder = new File(baseFolder, LevelStorage.NETHER_FOLDER);
        File enderFolder = new File(baseFolder, LevelStorage.ENDER_FOLDER);

        System.out.println("Scanning folders...");

        // find normal world
        addRegionFiles(baseFolder, normalRegions);

        // find hell world
        if (netherFolder.exists()) {
            addRegionFiles(netherFolder, netherRegions);
        }
        if (enderFolder.exists()) {
            addRegionFiles(enderFolder, enderRegions);
        }

        int totalCount = normalRegions.size() + netherRegions.size() + enderRegions.size();
        System.out.println("Total conversion count is " + totalCount);

        // convert normal world
        convertRegions(new File(baseFolder, "region"), normalRegions, 0, totalCount, progress);
        // convert hell world
        convertRegions(new File(netherFolder, "region"), netherRegions, normalRegions.size(), totalCount, progress);
        // convert end world
        convertRegions(new File(enderFolder, "region"), enderRegions, normalRegions.size() + netherRegions.size(), totalCount, progress);


        return true;
    }

    private void convertRegions(File baseFolder, ArrayList<File> regionFiles, int currentCount, int totalCount, ProgressListener progress) {

        for (File regionFile : regionFiles) {
            convertRegion(baseFolder, regionFile, currentCount, totalCount, progress);

            currentCount++;
            int percent = (int) Math.round(100.0d * (double) currentCount / (double) totalCount);
            progress.progressStagePercentage(percent);
        }

    }

    private void convertRegion(File baseFolder, File regionFile, int currentCount, int totalCount, ProgressListener progress) {

        try {
            String name = regionFile.getName();

            RegionFile regionSource = new RegionFile(regionFile);
            RegionFile regionDest = new RegionFile(new File(baseFolder, name.substring(0, name.length() - PM_ANVIL_EXTENSION.length()) + RegionFile.ANVIL_EXTENSION));

            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    if (regionSource.hasChunk(x, z) && !regionDest.hasChunk(x, z)) {
                        DataInputStream regionChunkInputStream = regionSource.getChunkDataInputStream(x, z);
                        if (regionChunkInputStream == null) {
                            System.out.println("Failed to fetch input stream");
                            continue;
                        }
                        CompoundTag chunkData = NbtIo.read(regionChunkInputStream);
                        regionChunkInputStream.close();

                        CompoundTag compound = chunkData.getCompound("Level");
                        {
                            CompoundTag tag = new CompoundTag();
                            CompoundTag levelData = new CompoundTag();
                            tag.put("Level", levelData);
                            convertFromPmAnvilFormat(compound, levelData);

                            DataOutputStream chunkDataOutputStream = regionDest.getChunkDataOutputStream(x, z);
                            NbtIo.write(tag, chunkDataOutputStream);
                            chunkDataOutputStream.close();
                        }
                    }
                }
                int basePercent = (int) Math.round(100.0d * (double) (currentCount * 1024) / (double) (totalCount * 1024));
                int newPercent = (int) Math.round(100.0d * (double) ((x + 1) * 32 + currentCount * 1024) / (double) (totalCount * 1024));
                if (newPercent > basePercent) {
                    progress.progressStagePercentage(newPercent);
                }
            }

            regionSource.close();
            regionDest.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addRegionFiles(File baseFolder, ArrayList<File> regionFiles) {

        File regionFolder = new File(baseFolder, "region");
        File[] list = regionFolder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(PM_ANVIL_EXTENSION);
            }
        });

        if (list != null) {
            for (File file : list) {
                regionFiles.add(file);
            }
        }
    }

    private void convertFromPmAnvilFormat(CompoundTag inTag, CompoundTag outTag) {

        outTag.putInt("xPos", inTag.getInt("xPos"));
        outTag.putInt("zPos", inTag.getInt("zPos"));
        outTag.putLong("LastUpdate", inTag.getLong("LastUpdate"));
        outTag.putIntArray("HeightMap", inTag.getIntArray("HeightMap"));
        outTag.putByte("TerrainPopulated", inTag.getByte("TerrainPopulated"));

        ListTag<CompoundTag> outSectionTags = new ListTag<CompoundTag>("Sections");
        ListTag inSectionTags = inTag.getList("Sections");

        for (int i = 0; i < inSectionTags.size(); i++) {
            CompoundTag inSectionTag = (CompoundTag) inSectionTags.get(i);
            byte[] blocks = inSectionTag.getByteArray("Blocks");
            byte[] blockLight = inSectionTag.getByteArray("BlockLight");
            byte[] skyLight = inSectionTag.getByteArray("SkyLight");
            byte[] data = inSectionTag.getByteArray("Data");

            CompoundTag outSectionTag = new CompoundTag();
            outSectionTag.putByte("Y", inSectionTag.getByte("Y"));
            outSectionTag.putByteArray("Blocks", reorderByteArray(blocks));
            outSectionTag.putByteArray("Data", reorderNibbleArray(data));
            outSectionTag.putByteArray("SkyLight", reorderNibbleArray(skyLight));
            outSectionTag.putByteArray("BlockLight", reorderNibbleArray(blockLight));

            outSectionTags.add(outSectionTag);
        }
        outTag.put("Sections", outSectionTags);

        if (inTag.contains("Biomes")) {
            outTag.putByteArray("Biomes", inTag.getByteArray("Biomes"));
        }

        outTag.put("Entities", inTag.getList("Entities"));

        outTag.put("TileEntities", inTag.getList("TileEntities"));

        if (inTag.contains("TileTicks")) {
            outTag.put("TileTicks", inTag.getList("TileTicks"));
        }
    }

    // Code for reorderByteArray and reorderNibbleArray, and processing of the
    // PMAnvil format is based on https://github.com/Awzaw/AnvilConverter
    private byte[] reorderByteArray(byte[] myarray) {
        byte[] result = new byte[16 * 16 * 16];
        int i = 0;

        for (int x = 0; x < 16; x++) {
            int zMax = x + 256;
            for (int z = x; z < zMax; z += 16) {
                int yMax = z + 4096;
                for (int y = z; y < yMax; y += 256) {
                    result[i] = myarray[y];
                    i++;
                }
            }
        }
        return result;
    }

    private byte[] reorderNibbleArray(byte[] myarray) {
        byte commonValue = 0x00;
        byte[] result = new byte[16 * 16 * 8];
        int i = 0;

        for (int x = 0; x < 8; x++) {
            for (int z = 0; z < 16; z++) {
                int zx = ((z << 3) | x);
                for (int y = 0; y < 8; y++) {
                    int j = ((y << 8) | zx);
                    int j80 = (j | 0x80);
                    if (myarray[j] == commonValue && myarray[j80] == commonValue) {
                        // values are already filled
                    } else {
                        byte i1 = myarray[j];
                        byte i2 = myarray[j80];
                        result[i] = (byte) ((i2 << 4) | (i1 & 0x0f));
                        result[i | 0x80] = (byte) (((i1 & 0xff) >>> 4) | (i2 & 0xf0));
                    }
                    i++;
                }
            }
            i += 128;
        }

        return result;
    }
}
