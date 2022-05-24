package com.kmodel99;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        Ripemd160 r = new Ripemd160();

        byte[] calc2HashBuff = new byte[]{0x11, (byte) 0xaa, (byte) 0xbb};
        r.printHash(r.calcHash(calc2HashBuff));

        byte[] calcHashBuff = fromHexString("908d8fbbaa");
        r.printHash(r.calcHash(calcHashBuff));

        byte[] stringHashBuff = "abcdefghijklabcdefghijklabcdefghijklabcdefghijklabcdefghijklabcdefghijkl".getBytes(StandardCharsets.UTF_8);
        r.printHash(r.calcHash(stringHashBuff));
    }

    private static byte[] fromHexString(String src) {
        byte[] biBytes = new BigInteger("10" + src.replaceAll("\\s", ""), 16).toByteArray();
        return Arrays.copyOfRange(biBytes, 1, biBytes.length);
    }
}
