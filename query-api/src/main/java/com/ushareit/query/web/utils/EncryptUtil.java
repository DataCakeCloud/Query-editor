package com.ushareit.query.web.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Base64;

/**
 * @author: licg
 * @create: 2021-05-12 15:24
 */
public class EncryptUtil {

    public static void main(String[] args) throws Exception {
        String data = "Abcd@1234";
        String key = "DataStudio-20210628";
        String hex = encrypt(data, key);
        System.out.println("加密之后的数据：" + hex);
        String origin = decrypt(hex, key);
        System.out.println("解密之后的数据:" + origin);
    }

    public static String encrypt(String data, String key) throws Exception {
        //创建秘钥
        DESKeySpec desKeySpec = new DESKeySpec(key.getBytes());
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey secretKey = secretKeyFactory.generateSecret(desKeySpec);
        //加密
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] result =  cipher.doFinal(data.getBytes());
        //使用base64进行编码
        return Base64.getEncoder().encodeToString(result);
    }

    public static String decrypt(String data, String key) throws Exception {
        //使用base64进行解码
        byte[] bs = Base64.getDecoder().decode(data);
        //创建秘钥
        DESKeySpec desKeySpec = new DESKeySpec(key.getBytes());
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("DES");
        SecretKey secretKey = secretKeyFactory.generateSecret(desKeySpec);
        //解密
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] result =  cipher.doFinal(bs);
        return new String(result);
    }

    public static String base64_encode(String data) {
        if (null == data || data.isEmpty()) {
            return "";
        }
        String content = data;
        try {
            content = URLEncoder.encode(data, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return Base64.getEncoder().encodeToString(content.getBytes());
    }

    public static String base64_decode(String base64Data) {
        if (base64Data == null || base64Data.isEmpty()) {
            return "";
        }
        String content = new String(Base64.getDecoder().decode(base64Data));
        try {
            return  URLDecoder.decode(content, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

}
