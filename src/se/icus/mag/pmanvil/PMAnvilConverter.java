package se.icus.mag.pmanvil;

/**
 * Copyright Mojang AB.
 * 
 * Don't do evil.
 *
 * Adaptations for PMAnvil conversions by magicus 2018.
 */


import net.minecraft.world.level.storage.ProgressListener;

import java.io.File;

public class PMAnvilConverter {

    public static void main(String[] args) {

        if (args.length != 2) {
            printUsageAndExit();
        }

        File baseFolder;
        try {
            baseFolder = new File(args[0]);
            if (!baseFolder.exists()) {
                throw new RuntimeException(args[0] + " doesn't exist");
            } else if (!baseFolder.isDirectory()) {
                throw new RuntimeException(args[0] + " is not a folder");
            }
        } catch (Exception e) {
            System.err.println("Base folder problem: " + e.getMessage());
            System.out.println("");
            printUsageAndExit();
            return;
        }

        PMAnvilLevelStorageSource storage = new PMAnvilLevelStorageSource(baseFolder);
        if (!storage.isConvertible(args[1])) {
            System.err.println("World called " + args[1] + " is not convertible to the Anvil format");
            System.out.println("");
            printUsageAndExit();
            return;
        }

        System.out.println("Converting map!");
        storage.convertLevel(args[1], new ProgressListener() {
            private long timeStamp = System.currentTimeMillis();

            public void progressStartNoAbort(String string) {
            }

            public void progressStart(String string) {
            }

            public void progressStagePercentage(int i) {
                if ((System.currentTimeMillis() - timeStamp) >= 1000L) {
                    timeStamp = System.currentTimeMillis();
                    System.out.println("Converting... " + i + "%");
                }
            }

            public void progressStage(String string) {
            }
        });
        System.out.println("Done!");
        System.out.println("You can now delete all *.mcapm files in the converted world.");
    }

    private static void printUsageAndExit() {
        System.out.println("Map converter for Minecraft, from format \"PMAnvil\" to \"Anvil\". (c) Mojang AB 2012, magicus 2018");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("\tjava -jar pmanvil-converter.jar <base folder> <world name>");
        System.out.println("Where:");
        System.out.println("\t<base folder>\tThe full path to the folder containing Minecraft world folders");
        System.out.println("\t<world name>\tThe folder name of the Minecraft world to be converted");
        System.out.println("Example:");
        System.out.println("\tjava -jar pmanvil-converter.jar /home/jeb_/minecraft world");
        System.exit(1);
    }

}
