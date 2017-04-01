package com.iota.iri.utils;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by paul on 4/1/17.
 */
public class CryptoTest {

    @Test
    public void encrypt() throws Exception {
        final String key, plain, cipher;
        key = "ABCDEFGHIJKLM9NOPQRSTUVWXYZ";
        plain = "9NOPQRSTUVWXYZABCDEFGHIJKLM";
        cipher = "ASRQYXWDCBJIHZOQSUWY9BDFHJL";
        assert(Converter.trytes(Crypto.encrypt(Converter.trits(key)).apply(Converter.trits(plain))).equals(cipher));
    }

    @Test
    public void decrypt() throws Exception {
        final String key, plain, cipher;
        key = "ABCDEFGHIJKLM9NOPQRSTUVWXYZ";
        plain = "9NOPQRSTUVWXYZABCDEFGHIJKLM";
        cipher = "ASRQYXWDCBJIHZOQSUWY9BDFHJL";
        assert(Converter.trytes(Crypto.decrypt(Converter.trits(key)).apply(Converter.trits(cipher))).equals(plain));
    }
}