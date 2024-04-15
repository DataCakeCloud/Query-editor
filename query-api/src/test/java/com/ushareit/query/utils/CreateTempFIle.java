package com.ushareit.query.utils;

/**
 * @author tianxu
 * @date 2023/10/12 20:13
 **/

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class CreateTempFIle {
    public static void main(String[] args) {
        File f = null;

        try {
            new Thread(new Runnable() {
                @Override
                public void run() {

                }
            });
            // 创建临时文件
            f = File.createTempFile("tmp", ".txt", new File("/Users/shareit/work/code/bdp/DataCake/ushareit-query-editor/data/logs"));
            // 输出绝对路径
            System.out.println("File path: "+f.getAbsolutePath());
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            writer.write("aa" + "\n");
//            writer.newLine();
            writer.write("b" + "\n");
//            writer.newLine();
            writer.write("cc" + "\n");
            writer.flush();

            // 终止后删除临时文件
            f.deleteOnExit();

        } catch(Exception e) {
            // 如果有错误输出内容
            e.printStackTrace();
        }
    }
}