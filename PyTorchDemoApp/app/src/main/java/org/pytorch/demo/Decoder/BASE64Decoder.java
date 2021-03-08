//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.pytorch.demo.Decoder;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;

import org.pytorch.demo.Decoder.CEFormatException;
import org.pytorch.demo.Decoder.CEStreamExhausted;
import org.pytorch.demo.Decoder.CharacterDecoder;

public class BASE64Decoder extends CharacterDecoder {
    private static final char[] pem_array = new char[]{'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/'};
    private static final byte[] pem_convert_array = new byte[256];
    byte[] decode_buffer = new byte[4];

    static {
        int i;
        for(i = 0; i < 255; ++i) {
            pem_convert_array[i] = -1;
        }

        for(i = 0; i < pem_array.length; ++i) {
            pem_convert_array[pem_array[i]] = (byte)i;
        }

    }

    public BASE64Decoder() {
    }

    protected int bytesPerAtom() {
        return 4;
    }

    protected int bytesPerLine() {
        return 72;
    }

    protected void decodeAtom(PushbackInputStream inStream, OutputStream outStream, int rem) throws IOException {
        byte a = -1;
        byte b = -1;
        byte c = -1;
        byte d = -1;
        if (rem < 2) {
            throw new CEFormatException("BASE64Decoder: Not enough bytes for an atom.");
        } else {
            int i;
            do {
                i = inStream.read();
                if (i == -1) {
                    throw new CEStreamExhausted();
                }
            } while(i == 10 || i == 13);

            this.decode_buffer[0] = (byte)i;
            i = this.readFully(inStream, this.decode_buffer, 1, rem - 1);
            if (i == -1) {
                throw new CEStreamExhausted();
            } else {
                if (rem > 3 && this.decode_buffer[3] == 61) {
                    rem = 3;
                }

                if (rem > 2 && this.decode_buffer[2] == 61) {
                    rem = 2;
                }

                switch(rem) {
                    case 4:
                        d = pem_convert_array[this.decode_buffer[3] & 255];
                    case 3:
                        c = pem_convert_array[this.decode_buffer[2] & 255];
                    case 2:
                        b = pem_convert_array[this.decode_buffer[1] & 255];
                        a = pem_convert_array[this.decode_buffer[0] & 255];
                    default:
                        switch(rem) {
                            case 2:
                                outStream.write((byte)(a << 2 & 252 | b >>> 4 & 3));
                                break;
                            case 3:
                                outStream.write((byte)(a << 2 & 252 | b >>> 4 & 3));
                                outStream.write((byte)(b << 4 & 240 | c >>> 2 & 15));
                                break;
                            case 4:
                                outStream.write((byte)(a << 2 & 252 | b >>> 4 & 3));
                                outStream.write((byte)(b << 4 & 240 | c >>> 2 & 15));
                                outStream.write((byte)(c << 6 & 192 | d & 63));
                        }

                }
            }
        }
    }
}
