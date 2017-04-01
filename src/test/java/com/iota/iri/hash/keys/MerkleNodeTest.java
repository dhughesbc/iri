package com.iota.iri.hash.keys;

import com.iota.iri.utils.Converter;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by paul on 4/1/17.
 */
public class MerkleNodeTest {
    static final String rootString = "SOMERANDOMTRYTESROOT9";
    static MerkleNode first, second, third, firstAndSecond, nullAndThird, root;

    @BeforeClass
    public static void setup() {
        first = MerkleNode.create(Converter.trits("SOMERANDOMTRYTESONE"));
        second = MerkleNode.create(Converter.trits("SOMERANDOMTRYTESTWO"));
        third = MerkleNode.create(Converter.trits("SOMERANDOMTRYTESTHREE"));
        firstAndSecond = MerkleNode.create(Converter.trits("SOMERANDOMTRYTESONETWO"), new MerkleNode[]{first, second});
        nullAndThird = MerkleNode.create(Converter.trits("SOMERANDOMTRYTESNULLTHREE"), new MerkleNode[]{third});
    }

    @After
    public void tearDown() throws Exception {
        root = null;
    }

    @Test
    public void create() throws Exception {
        root = MerkleNode.create(Converter.trits(rootString));
        assertArrayEquals(Converter.trits(rootString), root.value);
    }

    @Test
    public void createWithChildren() throws Exception {
        root = MerkleNode.create(Converter.trits(rootString), new MerkleNode[]{firstAndSecond, nullAndThird});

        assertEquals(firstAndSecond, root.get(0));
        assertEquals(nullAndThird, root.get(1));
        assertArrayEquals(Converter.trits(rootString), root.value);
    }

    @Test
    public void get() throws Exception {
        root = MerkleNode.create(Converter.trits(rootString), new MerkleNode[]{firstAndSecond, nullAndThird});

        assertEquals(firstAndSecond, root.get(0));
        assertEquals(nullAndThird, root.get(1));
    }

    @Test
    public void leafSizeTest() throws Exception {
        root = MerkleNode.create(Converter.trits(rootString), new MerkleNode[]{firstAndSecond, nullAndThird});
        assertEquals(3, root.leafCount());
    }

    @Test
    public void countTest() throws Exception {
        root = MerkleNode.create(Converter.trits(rootString), new MerkleNode[]{firstAndSecond, nullAndThird});
        assertEquals(2, root.childCount());
    }

    @Test
    public void sizeTest() throws Exception {
        root = MerkleNode.create(Converter.trits(rootString), new MerkleNode[]{firstAndSecond, nullAndThird});
        assertEquals(6, root.size());
    }
}