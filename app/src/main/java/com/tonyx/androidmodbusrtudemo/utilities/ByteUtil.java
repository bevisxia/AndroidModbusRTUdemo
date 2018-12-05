package com.tonyx.androidmodbusrtudemo.utilities;

public class ByteUtil {
    public static byte[] toBytes(String str){
        return str!=null ? str.getBytes() : null;
    }

    public static byte[] subBytes(byte[] input, int start, int length) {
        if ((length > 0) && (input.length >= start + length)) {
            byte[] result = new byte[length];
            for (int i = 0; i < length; i++) {
                result[i] = input[i + start];
            }
            return result;
        }
        return new byte[0];
    }

    public static void fillBytes(byte[] dest, int pos, byte[] source, int start, int length) {
        for (int i = 0; i < length; i++) {
            dest[i + pos] = source[i + start];
        }
    }

    public static String toHexString(byte[] input, String separator) {
        if (input==null) return null;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length; i++) {
            if (separator != null && sb.length() > 0) {
                sb.append(separator);
            }
            String str = Integer.toHexString(input[i] & 0xff);
            if (str.length() == 1) str = "0" + str;
            sb.append(str);
        }
        return sb.toString();
    }

    public static String toHexString(byte[] input) {
        return toHexString(input, " ");
    }

    public static int toInt(byte[] input) {
        String hex = toHexString(input, null);
        try {
            return Integer.parseInt(hex, 16);
        } catch (Exception ex) {
            return 0;
        }
    }

    public static byte[] fromInt32(int input){
        byte[] result=new byte[4];
        result[3]=(byte)(input >> 24 & 0xFF);
        result[2]=(byte)(input >> 16 & 0xFF);
        result[1]=(byte)(input >> 8 & 0xFF);
        result[0]=(byte)(input & 0xFF);
        return result;
    }

    public static byte[] fromInt16(int input){
        byte[] result=new byte[2];
        result[0]=(byte)(input >> 8 & 0xFF);
        result[1]=(byte)(input & 0xFF);
        return result;
    }

    public static byte[] fromInt16Reversal(int input){
        byte[] result=new byte[2];
        result[1]=(byte)(input>>8&0xFF);
        result[0]=(byte)(input&0xFF);
        return result;
    }

    public static int getInt32(byte[] input, int pos){
        return toInt(subBytes(input, pos, 4));
    }

    public static short getInt16(byte[] input, int pos){
        return (short)toInt(subBytes(input, pos,2));
    }

    public static boolean getBit(byte input, int pos){
        return ((input >> (7-pos)) & 0x1)>1;
    }
}
