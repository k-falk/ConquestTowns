package com.kfalk.conquesttowns.misc;

import java.io.File;


public class FileUtils {

    public static void deleteRecursive(File path){
        File[] files = path.listFiles();

        for (File file : files){
            if (file.isDirectory()){
                deleteRecursive(file);
                file.delete();
            } else {
                file.delete();
            }
        }

        path.delete();
    }

}
