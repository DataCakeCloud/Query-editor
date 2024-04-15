package com.ushareit.query.web.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author tianxu
 * @date 2023/12/20
 **/
@Slf4j
public class PathUtil {
    public ArrayList<String> pathSort(ArrayList<String> urls) {
        // 使用自定义比较器进行排序
        Collections.sort(urls, new CustomUrlComparator());
        return urls;
    }

    static class CustomUrlComparator implements Comparator<String> {
        @Override
        public int compare(String url1, String url2) {
            // 从URL中提取数字部分进行比较
            int num1 = extractNumber(url1);
            int num2 = extractNumber(url2);

            // 如果数字相同，按照完整的URL进行排序
            if (num1 == num2) {
                return url1.compareTo(url2);
            }

            // 返回比较结果
            return Integer.compare(num1, num2);
        }

        private int extractNumber(String url) {
            // 从URL中提取数字部分
            String[] parts = url.split("_|\\.csv|\\?");
            for (String part : parts) {
                try {
                    return Integer.parseInt(part);
                } catch (NumberFormatException ignored) {
                }
            }
            return 0; // 如果未找到数字，默认为0
        }
    }

    public static ArrayList<String> fileNameSort(ArrayList<String> urls) {
        // 使用自定义比较器进行排序
        Collections.sort(urls, new FileNameComparator());
        return urls;
    }

    static class FileNameComparator implements Comparator<String> {
        @Override
        public int compare(String fileName1, String fileName2) {
            return extractNumber(fileName1) - extractNumber(fileName2);
        }

        private int extractNumber(String fileName) {
            // 从文件名中提取数字部分
            String[] parts = fileName.split("_");
            if (parts.length > 1) {
                String number = parts[1].split("\\.")[0];
                try {
                    return Integer.parseInt(number);
                } catch (NumberFormatException e) {
                    // 如果无法解析为数字，返回0
                    return 0;
                }
            }
            // 如果没有数字部分，返回0
            return 0;
        }
    }

    public static String fileName(String url, String uuid) throws Exception {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();

            String[] pathSegments = path.split("/");
            return pathSegments[pathSegments.length - 1];
        } catch (URISyntaxException e) {
            log.error(String.format("[uuid=%s, url=%s]%s", uuid, url, CommonUtil.printStackTraceToString(e)));
            throw new Exception(e.getMessage());
        }
    }

    public static boolean deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!deleteDirectory(file)) {
                    return false;
                }
            }
        }
        return dir.delete();
    }
}
