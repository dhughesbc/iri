package com.iota.iri.utils;

import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Created by paul on 4/1/17.
 */
public class Crypto {
    /*
        Vernam Cipher for trinary logic
        plain (+) key = cipher
     */
    public static Function<int[], int[]> encrypt(int[] key) {
        return (plainTrits) -> {
            assert key.length >= plainTrits.length;
            return IntStream.range(0, plainTrits.length).parallel()
                    .map(i -> TritMath.sum(plainTrits[i]).apply(key[i]))
                    .toArray();
        };
    }
    /*
        cipher (+) !key = plain
     */
    public static Function<int[], int[]> decrypt(int[] key) {
        return (cipherTrits) -> {
            assert key.length >= cipherTrits.length;
            return IntStream.range(0, cipherTrits.length).parallel()
                    .map(i -> TritMath.sum(cipherTrits[i]).apply( - key[i]))
                    .toArray();
        };
    }
}
