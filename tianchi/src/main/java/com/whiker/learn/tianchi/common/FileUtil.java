package com.whiker.learn.tianchi.common;

import com.google.common.io.Resources;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author whiker@163.com create on 16-10-19.
 */
public class FileUtil {

    private static String ROOT_PATH;

    static {
        String path = Resources.getResource(".").getFile();
        ROOT_PATH = path.substring(0, path.indexOf("/target")) + "/src/main/resources";
    }

    public static String getRootPath() {
        return ROOT_PATH;
    }

    public static BufferedWriter getWriter(String filepath) throws IOException {
        return new BufferedWriter(new FileWriter(new File(filepath)));
    }

    public static void main(String[] args) {
        System.out.println(Resources.getResource(".").getFile());
    }

}
