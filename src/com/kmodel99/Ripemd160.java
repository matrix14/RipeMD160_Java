package com.kmodel99;

import java.lang.reflect.Array;
import java.util.Arrays;

class BlockStruct {
    public final int blocks;
    public final byte[] input;

    BlockStruct(int blocks, byte[] input) {
        this.blocks = blocks;
        this.input = input;
    }
}

public class Ripemd160 {
    int[] CV;
    int[] CV_copy;
    int[] rho;
    int[] pi;
    int[][] rhoL = new int[5][16];
    int[][] rhoR = new int[5][16];
    int[][] leftShiftAmount = new int[][]{
        {11, 14, 15, 12, 5, 8, 7, 9, 11, 13, 14, 15, 6, 7, 9, 8},
        {12, 13, 11, 15, 6, 9, 9, 7, 12, 15, 11, 13, 7, 8, 7, 7},
        {13, 15, 14, 11, 7, 7, 6, 8, 13, 14, 13, 12, 5, 5, 6, 9},
        {14, 11, 12, 14, 8, 6, 5, 5, 15, 12, 15, 14, 9, 9, 8, 6},
        {15, 12, 13, 13, 9, 5, 8, 6, 14, 11, 12, 11, 8, 6, 5, 5}
    };
    int[] Kl = { 0x0, 0x5a827999, 0x6ed9eba1, 0x8f1bbcdc, 0xa953fd4e };
    int[] Kr = { 0x50a28be6, 0x5c4dd124, 0x6d703ef3, 0x7a6d76e9, 0x0 };

    private void precalculation_rho() {
        int a, b;
        for (a = 0; a < 16; a++) {
            rhoL[0][a] = a;
            rhoR[0][a] = pi[a];
            for (b = 1; b < 5; b++) {
                rhoL[b][a] = rho[rhoL[b - 1][a]];
                rhoR[b][a] = rho[rhoR[b - 1][a]];
            }
        }
    }

    private int func(int round, int A, int B, int C) {
        return switch (round) {
            case 1 -> A ^ B ^ C;
            case 2 -> (A & B) | (~A & C);
            case 3 -> (A | ~B) ^ C;
            case 4 -> (A & C) | (B & ~C);
            case 5 -> A ^ (B | ~C);
            default -> 0;
        };
    }

    private byte[] addAll(final byte[] array1, byte[] array2) {
        byte[] joinedArray = Arrays.copyOf(array1, array1.length + array2.length);
        System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
        return joinedArray;
    }

    private int roll(int input, int shift) {
        int a = input << shift;
        int b = input >>> (32-shift);
        return (a | b);
    }

    private BlockStruct addPadding(byte[] input) {
        int bitLength = input.length * 8;

        input = addAll(input, new byte[]{(byte) 0x80});
        int howManyZeroAdd;
        if((input.length%64)<56) {
            howManyZeroAdd = 56-(input.length%64);
        } else if ((input.length%64)==56) {
            howManyZeroAdd = 0;
        } else {
            howManyZeroAdd = 64-(input.length%64) + 56;
        }

        howManyZeroAdd += 8;

        byte[] inputAdd = new byte[howManyZeroAdd];
        Arrays.fill(inputAdd, (byte) 0x0);

        input = addAll(input, inputAdd);

        int startLength = input.length - 8;

        for(int i=0; i<8; i++) {
            input[startLength+i] = (byte) (bitLength & 0xFF);
            bitLength = bitLength >> 8;
        }
        return new BlockStruct(input.length/64, input);
    }

    public Ripemd160() {
        CV = new int[]{0x67452301, 0xefcdab89, 0x98badcfe, 0x10325476, 0xc3d2e1f0};
        CV_copy = CV.clone();
    }

    public Ripemd160(int[] CV2) {
        CV = CV2.clone();
        CV_copy = CV.clone();
    }

    private void reInitRipemd160() {
        CV = CV_copy.clone();
        rho = new int[]{ 7, 4, 13, 1, 10, 6, 15, 3, 12, 0, 9, 5, 2, 14, 11, 8 };
        pi = new int[]{ 5, 14, 7, 0, 9, 2, 11, 4, 13, 6, 15, 8, 1, 10, 3, 12 };
        Arrays.stream(rhoL).forEach(arr -> Arrays.fill(arr, 0));
        Arrays.stream(rhoR).forEach(arr -> Arrays.fill(arr, 0));

        precalculation_rho();
    }

    public byte[] calcHash(byte[] input) {
        reInitRipemd160();
        BlockStruct bs = addPadding(input);

        for(int i=0; i<bs.blocks; i++) {
            int V;
            int[] message = new int[16];
            for(int k=0; k<16; k++) {
                int start = i*64 + k*4;
                message[k] = java.nio.ByteBuffer.wrap(Arrays.copyOfRange(bs.input, start, start+4+1)).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
            }

            int[] aL = Arrays.copyOf(CV, 5);
            int[] aR = Arrays.copyOf(CV, 5);

            for(int a=0; a<5; a++) {
                for (int b = 0; b < 16; b++) {
                    V = aL[0];
                    V += func(a+1, aL[1], aL[2], aL[3]);

                    roundCalc(V, message, aL, a, b, rhoL, Kl);

                    V = aR[0];
                    V += func(5-a, aR[1], aR[2], aR[3]);

                    roundCalc(V, message, aR, a, b, rhoR, Kr);
                }
            }

            V = CV[1] + aL[2] + aR[3];
            for(int a=1; a<5; a++) {
                CV[a] = CV[(a+1)%5] + aL[(a+2)%5] + aR[(a+3)%5];
            }
            CV[0] = V;
        }

        byte[] result = new byte[20];

        for (int a = 0; a < 5; a++) {
            for (int b = 0; b < 4; b++) {
                result[a * 4 + b] = (byte)(CV[a] & 0xFF);
                CV[a] = CV[a] >> 8;
            }
        }

        return result;
    }

    private void roundCalc(int v, int[] message, int[] aR, int a, int b, int[][] rhoR, int[] kr) {
        v += message[rhoR[a][b]];
        v += kr[a];
        v = roll(v, leftShiftAmount[a][rhoR[a][b]]);
        v += aR[4];

        aR[0] = aR[4];
        aR[4] = aR[3];
        aR[3] = roll(aR[2], 10);
        aR[2] = aR[1];
        aR[1] = v;
    }

    public void printHash(byte[] hash) {
        for (int i = 0; i < 20; i++) {
            System.out.printf("%02X", hash[i]);
        }
        System.out.print("\n");
    }
}
