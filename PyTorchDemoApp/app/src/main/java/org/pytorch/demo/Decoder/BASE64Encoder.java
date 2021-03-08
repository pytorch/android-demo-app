//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.pytorch.demo.Decoder;

import java.io.IOException;
import java.io.OutputStream;
import org.pytorch.demo.Decoder.CharacterDecoder;
//import CharacterEncoder;

public class BASE64Encoder extends CharacterEncoder {
    private static final char[] pem_array = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'};

    public BASE64Encoder() {
    }

    protected int bytesPerAtom() {
        return 3;
    }

    protected int bytesPerLine() {
        return 57;
    }

    protected void encodeAtom(OutputStream outStream, byte[] data, int offset, int len) throws IOException {
        byte a;
        if (len == 1) {
            a = data[offset];
            byte b = 0;
//            byte c = (Byte)false;
            outStream.write(pem_array[a >>> 2 & 63]);
            outStream.write(pem_array[(a << 4 & 48) + (b >>> 4 & 15)]);
            outStream.write(61);
            outStream.write(61);
        } else {
            byte b;
            if (len == 2) {
                a = data[offset];
                b = data[offset + 1];
                byte c = 0;
                outStream.write(pem_array[a >>> 2 & 63]);
                outStream.write(pem_array[(a << 4 & 48) + (b >>> 4 & 15)]);
                outStream.write(pem_array[(b << 2 & 60) + (c >>> 6 & 3)]);
                outStream.write(61);
            } else {
                a = data[offset];
                b = data[offset + 1];
                byte c = data[offset + 2];
                outStream.write(pem_array[a >>> 2 & 63]);
                outStream.write(pem_array[(a << 4 & 48) + (b >>> 4 & 15)]);
                outStream.write(pem_array[(b << 2 & 60) + (c >>> 6 & 3)]);
                outStream.write(pem_array[c & 63]);
            }
        }

    }
}
