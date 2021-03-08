package org.pytorch.demo.util;

import java.io.IOException;
import org.pytorch.demo.Decoder.BASE64Decoder;
import org.pytorch.demo.Decoder.BASE64Encoder;
//import Decoder.BASE64Decoder;
//import Decoder.BASE64Encoder;

/**
 * 使用Base64来保存和获取密码数据
 */
public class Base64Utils {


    /**
     * BASE64解密
     *
     * @param key
     * @return
     * @throws Exception
     */
    public static String decryptBASE64(String key) {
        int decodetime = 5;//压缩和解压的次数，防止被简单破解
        byte[] bt;
        key = key.trim().replace(" ", "");//去掉空格
        try {
            while (decodetime > 0) {
                bt = (new BASE64Decoder()).decodeBuffer(key);
                key = new String(bt);
                decodetime--;
            }

            return key;//如果出现乱码可以改成： String(bt, "utf-8")或 gbk
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * BASE64加密
     *
     * @param key
     * @return
     * @throws Exception
     */
    public static String encryptBASE64(String key) {
        int decodetime = 5;//压缩和解压的次数，防止被简单破解
        byte[] bt = null;
        key = key.trim().replace(" ", "");//去掉空格
        while (decodetime > 0) {
            bt = key.getBytes();
            key = (new BASE64Encoder()).encodeBuffer(bt);
            decodetime--;
        }

        return key;
    }
}
