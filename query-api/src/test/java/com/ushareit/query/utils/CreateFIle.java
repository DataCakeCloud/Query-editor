package com.ushareit.query.utils;

/**
 * @author tianxu
 * @date 2023/10/12 20:13
 **/

import java.io.*;
import java.nio.charset.StandardCharsets;

public class CreateFIle {
    public static void main(String[] args) {
        File f = null;

        try {
            String url = "/Users/shareit/work/code/bdp/DataCake/ushareit-query-editor/data/logs";
            String uuid = "1122";
            File file = new File(url + File.separator + uuid + ".csv");
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8), 1024);
            writer.write(new String(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF}, StandardCharsets.UTF_8));

            writer.write("aa" + "\n");
//            writer.newLine();
            writer.write("b" + "\n");
//            writer.newLine();
            writer.write("cc" + "\n");
            writer.flush();

        } catch(Exception e) {
            // 如果有错误输出内容
            e.printStackTrace();
        }
    }
}