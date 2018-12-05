package com.tonyx.androidmodbusrtudemo.utilities;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ByteArrayReader extends ByteArrayInputStream {
    public ByteArrayReader(byte[] buf) {
        super(buf);
    }

    public int readInt8() {
        return super.read();
    }

    public int readInt16() throws IOException {
        byte[] d = new byte[2];
        if (super.read(d, 0, 2) < 2) {
            throw new IOException();
        }
        return ByteUtil.toInt(d);
    }

    public int readInt32() throws IOException {
        byte[] d = new byte[4];
        if (super.read(d, 0, 4) < 4) {
            throw new IOException();
        }
        return ByteUtil.toInt(d);
    }
}
