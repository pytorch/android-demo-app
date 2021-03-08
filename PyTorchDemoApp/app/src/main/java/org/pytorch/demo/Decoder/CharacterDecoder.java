//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.pytorch.demo.Decoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.nio.ByteBuffer;

import org.pytorch.demo.Decoder.CEStreamExhausted;

public abstract class CharacterDecoder {
    public CharacterDecoder() {
    }

    protected abstract int bytesPerAtom();

    protected abstract int bytesPerLine();

    protected void decodeBufferPrefix(PushbackInputStream aStream, OutputStream bStream) throws IOException {
    }

    protected void decodeBufferSuffix(PushbackInputStream aStream, OutputStream bStream) throws IOException {
    }

    protected int decodeLinePrefix(PushbackInputStream aStream, OutputStream bStream) throws IOException {
        return this.bytesPerLine();
    }

    protected void decodeLineSuffix(PushbackInputStream aStream, OutputStream bStream) throws IOException {
    }

    protected void decodeAtom(PushbackInputStream aStream, OutputStream bStream, int l) throws IOException {
        throw new CEStreamExhausted();
    }

    protected int readFully(InputStream in, byte[] buffer, int offset, int len) throws IOException {
        for(int i = 0; i < len; ++i) {
            int q = in.read();
            if (q == -1) {
                return i == 0 ? -1 : i;
            }

            buffer[i + offset] = (byte)q;
        }

        return len;
    }

    public void decodeBuffer(InputStream aStream, OutputStream bStream) throws IOException {
        int totalBytes = 0;
        PushbackInputStream ps = new PushbackInputStream(aStream);
        this.decodeBufferPrefix(ps, bStream);

        while(true) {
            try {
                int length = this.decodeLinePrefix(ps, bStream);

                int i;
                for(i = 0; i + this.bytesPerAtom() < length; i += this.bytesPerAtom()) {
                    this.decodeAtom(ps, bStream, this.bytesPerAtom());
                    totalBytes += this.bytesPerAtom();
                }

                if (i + this.bytesPerAtom() == length) {
                    this.decodeAtom(ps, bStream, this.bytesPerAtom());
                    totalBytes += this.bytesPerAtom();
                } else {
                    this.decodeAtom(ps, bStream, length - i);
                    totalBytes += length - i;
                }

                this.decodeLineSuffix(ps, bStream);
            } catch (CEStreamExhausted var8) {
                this.decodeBufferSuffix(ps, bStream);
                return;
            }
        }
    }

    public byte[] decodeBuffer(String inputString) throws IOException {
        byte[] inputBuffer = new byte[inputString.length()];
        inputString.getBytes(0, inputString.length(), inputBuffer, 0);
        ByteArrayInputStream inStream = new ByteArrayInputStream(inputBuffer);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        this.decodeBuffer(inStream, outStream);
        return outStream.toByteArray();
    }

    public byte[] decodeBuffer(InputStream in) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        this.decodeBuffer(in, outStream);
        return outStream.toByteArray();
    }

    public ByteBuffer decodeBufferToByteBuffer(String inputString) throws IOException {
        return ByteBuffer.wrap(this.decodeBuffer(inputString));
    }

    public ByteBuffer decodeBufferToByteBuffer(InputStream in) throws IOException {
        return ByteBuffer.wrap(this.decodeBuffer(in));
    }
}
