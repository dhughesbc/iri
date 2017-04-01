package com.iota.iri.hash;

import com.iota.iri.hash.keys.MerkleNode;
import com.iota.iri.utils.Converter;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Stack;

/**
 * Created by paul on 2/13/17.
 */
public class ISSTest {
    static final String seedStr = "MYSEEDSARETHEBEST9SEEDSWHODONTUSEMYSEEDARESADALLSEEDSSHOULDBEZEROLENGTHORGREATER9";

    @Test
    public void stateTritConversion() throws Exception {
        Assert.assertEquals(seedStr, Converter.trytes(Converter.trits(Converter.tuple(Converter.trits(seedStr)))));
    }

    @Test
    public void merkleHash() throws Exception {
        final String seedStr;
        final int[] seed;
        int startIndex, numberOfKeys, keySize;
        long startTime, diff;
        seedStr = "MYSEEDSARETHEBEST9SEEDSWHODONTUSEMYSEEDARESADALLSEEDSSHOULDBEZEROLENGTHORGREATER9";
        seed = Converter.trits(seedStr);
        startIndex = 1;
        numberOfKeys = 1029;
        keySize = 81;
        MerkleNode merkleRoot = null;
        //for(numberOfKeys = 9; numberOfKeys < 200; numberOfKeys *= 9) {
            startTime = System.nanoTime();
            merkleRoot = ISS.createMerkleTree(seed, startIndex, numberOfKeys, keySize);
            diff = System.nanoTime() - startTime;
            System.out.println("Tree creation rate for "+numberOfKeys+" keys: " + (int)(numberOfKeys*1e9/diff) + " Keys/s. Time elapsed: " + diff/1e9 + "s");
        //}
        Stack<MerkleNode> parents = new Stack<MerkleNode>();
        parents.push(merkleRoot);
        MerkleNode child = parents.peek().get(0);
        while(child.get(0).childCount() != 0) {
            //System.out.println(Converter.trytes(child.value));
            parents.push(child);
            child = parents.peek().get(0);
        }
        final String fragment = "MYFRAGMEN";//"TZRNICE9BEFOREQURY";
        final int[] bundleFragment = Converter.trits(fragment);
        int[] signatureFragment = ISS.signatureFragment(bundleFragment, child.get(0).value);
        MerkleNode parent;
        int[] hash = ISS.address(ISS.digest(bundleFragment, signatureFragment));//new int[parents.peek().value.length];
        Assert.assertArrayEquals(hash, child.value);
        Curl curl = new Curl();
        int[] siblings;
        int i;
        while(!parents.empty()) {
            parent = parents.pop();

            siblings = parent.childStream().parallel()
                    .map(a -> a == null? MerkleNode.create(new int[Curl.HASH_LENGTH]).value: a.value)
                    .reduce((a,b) -> ArrayUtils.addAll(a, b)).orElse(new int[Curl.HASH_LENGTH*3]);
            for(i = 0; i < hash.length; i++) {
                siblings[i] = hash[i];
            }
            if(siblings.length < Curl.HASH_LENGTH*2) {
                siblings = ArrayUtils.addAll(siblings, new int[Curl.HASH_LENGTH*3 - siblings.length]);
            }
            curl.absorb(siblings, 0, hash.length*2);
            curl.squeeze(hash, 0, hash.length);
            curl.reset();
            Assert.assertArrayEquals(hash, parent.value);
        }
    }

    @Test
    public void checKMerkleSignatureTest() {
        String seedTrytes;
        seedTrytes = "MYSEEDSARETHEBEST9SEEDSWHODONTUSEMYSEEDARESADALLSEEDSSHOULDBEZEROLENGTHORGREATER9";
        MerkleNode merkleNode;
        int startIndex = 1;
        int numberOfKeys = 1029;
        int keySize = 81;
        merkleNode = ISS.createMerkleTree(Converter.trits(seedTrytes), startIndex, numberOfKeys, keySize);

    }
}