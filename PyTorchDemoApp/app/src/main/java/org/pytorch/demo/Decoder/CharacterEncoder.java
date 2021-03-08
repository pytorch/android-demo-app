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
import java.io.PrintStream;
import java.nio.ByteBuffer;

public abstract class CharacterEncoder {
    protected PrintStream pStream;

    public CharacterEncoder() {
    }

    protected abstract int bytesPerAtom();

    protected abstract int bytesPerLine();

    protected void encodeBufferPrefix(OutputStream aStream) throws IOException {
        this.pStream = new PrintStream(aStream);
    }

    protected void encodeBufferSuffix(OutputStream aStream) throws IOException {
    }

    protected void encodeLinePrefix(OutputStream aStream, int aLength) throws IOException {
    }

    protected void encodeLineSuffix(OutputStream aStream) throws IOException {
        this.pStream.println();
    }

    protected abstract void encodeAtom(OutputStream var1, byte[] var2, int var3, int var4) throws IOException;

    protected int readFully(InputStream in, byte[] buffer) throws IOException {
        for(int i = 0; i < buffer.length; ++i) {
            int q = in.read();
            if (q == -1) {
                return i;
            }

            buffer[i] = (byte)q;
        }

        return buffer.length;
    }

    public void encode(InputStream inStream, OutputStream outStream) throws IOException {
        byte[] tmpbuffer = new byte[this.bytesPerLine()];
        this.encodeBufferPrefix(outStream);

        while(true) {
            int numBytes = this.readFully(inStream, tmpbuffer);
            if (numBytes == 0) {
                break;
            }

            this.encodeLinePrefix(outStream, numBytes);

            for(int j = 0; j < numBytes; j += this.bytesPerAtom()) {
                if (j + this.bytesPerAtom() <= numBytes) {
                    this.encodeAtom(outStream, tmpbuffer, j, this.bytesPerAtom());
                } else {
                    this.encodeAtom(outStream, tmpbuffer, j, numBytes - j);
                }
            }

            if (numBytes < this.bytesPerLine()) {
                break;
            }

            this.encodeLineSuffix(outStream);
        }

        this.encodeBufferSuffix(outStream);
    }

    public void encode(byte[] aBuffer, OutputStream aStream) throws IOException {
        ByteArrayInputStream inStream = new ByteArrayInputStream(aBuffer);
        this.encode((InputStream)inStream, aStream);
    }

    public String encode(byte[] aBuffer) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ByteArrayInputStream inStream = new ByteArrayInputStream(aBuffer);
        String retVal = null;

        try {
            this.encode((InputStream)inStream, outStream);
            retVal = outStream.toString("8859_1");
            return retVal;
        } catch (Exception var6) {
            throw new Error("CharacterEncoder.encode internal error");
        }
    }

    private byte[] getBytes(ByteBuffer bb) {
        byte[] buf = (byte[])null;
        if (bb.hasArray()) {
            byte[] tmp = bb.array();
            if (tmp.length == bb.capacity() && tmp.length == bb.remaining()) {
                buf = tmp;
                bb.position(bb.limit());
            }
        }

        if (buf == null) {
            buf = new byte[bb.remaining()];
            bb.get(buf);
        }

        return buf;
    }

    public void encode(ByteBuffer aBuffer, OutputStream aStream) throws IOException {
        byte[] buf = this.getBytes(aBuffer);
        this.encode(buf, aStream);
    }

    public String encode(ByteBuffer aBuffer) {
        byte[] buf = this.getBytes(aBuffer);
        return this.encode(buf);
    }

    public void encodeBuffer(InputStream inStream, OutputStream outStream) throws IOException {
        byte[] tmpbuffer = new byte[this.bytesPerLine()];
        this.encodeBufferPrefix(outStream);

        int numBytes;
        do {
            numBytes = this.readFully(inStream, tmpbuffer);
            if (numBytes == 0) {
                break;
            }

            this.encodeLinePrefix(outStream, numBytes);

            for(int j = 0; j < numBytes; j += this.bytesPerAtom()) {
                if (j + this.bytesPerAtom() <= numBytes) {
                    this.encodeAtom(outStream, tmpbuffer, j, this.bytesPerAtom());
                } else {
                    this.encodeAtom(outStream, tmpbuffer, j, numBytes - j);
                }
            }

            this.encodeLineSuffix(outStream);
        } while(numBytes >= this.bytesPerLine());

        this.encodeBufferSuffix(outStream);
    }

    public void encodeBuffer(byte[] aBuffer, OutputStream aStream) throws IOException {
        ByteArrayInputStream inStream = new ByteArrayInputStream(aBuffer);
        this.encodeBuffer((InputStream)inStream, aStream);
    }

    public String encodeBuffer(byte[] aBuffer) {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        ByteArrayInputStream inStream = new ByteArrayInputStream(aBuffer);

        try {
            this.encodeBuffer((InputStream)inStream, outStream);
        } catch (Exception var5) {
            throw new Error("CharacterEncoder.encodeBuffer internal error");
        }

        return outStream.toString();
    }

    public void encodeBuffer(ByteBuffer aBuffer, OutputStream aStream) throws IOException {
        byte[] buf = this.getBytes(aBuffer);
        this.encodeBuffer(buf, aStream);
    }

    public String encodeBuffer(ByteBuffer aBuffer) {
        byte[] buf = this.getBytes(aBuffer);
        return this.encodeBuffer(buf);
    }
}
