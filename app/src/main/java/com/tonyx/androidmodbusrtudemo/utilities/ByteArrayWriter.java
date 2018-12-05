package com.tonyx.androidmodbusrtudemo.utilities;

import java.io.ByteArrayOutputStream;

public class ByteArrayWriter extends ByteArrayOutputStream {
    public ByteArrayWriter() {
        super();
    }

    public void writeInt8(byte b)
    {
        this.write(b);
    }

    public void writeInt8(int b)
    {
        this.write((byte)b);
    }

    public void writeInt16(int n) {
        byte[] bytes = ByteUtil.fromInt16(n);
        this.write(bytes, 0, bytes.length);
    }

    public void writeInt16Reversal(int n){
        byte[] bytes=ByteUtil.fromInt16Reversal(n);
        this.write(bytes,0,bytes.length);
    }

    public void writeInt32(int n) {
        byte[] bytes = ByteUtil.fromInt32(n);
        this.write(bytes, 0, bytes.length);
    }

    public void writeBytes(byte[] bs,int len){
        this.write(bs,0,len);
    }

}
