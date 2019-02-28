package com.colebianchi.apps.Eon.util;

import java.io.OutputStream;

public class FileUtils {


    public static boolean writeFile(OutputStream o, String content) throws Exception {
        o.write(content.getBytes());
        o.flush();
        o.close();
        return true;

    }

}