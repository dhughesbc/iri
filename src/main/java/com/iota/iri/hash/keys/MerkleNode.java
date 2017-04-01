package com.iota.iri.hash.keys;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Created by paul on 2/22/17.
 */
public class MerkleNode implements Comparable<MerkleNode> {
    public int[] value;
    private final MerkleNode[] children;

    private MerkleNode(int[] val, final MerkleNode[] children) {
        value = val;
        this.children = children;
    }

    public static MerkleNode create(int[] val) {
        return new MerkleNode(val, new MerkleNode[0]);
    }

    public static MerkleNode create(int[] val, final MerkleNode[] children) {
        return new MerkleNode(val, children);
    }

    /*
    Traverse to node at given index, and return that node
     */
    public MerkleNode get(int index) {
        return children.length > index? children[index]: null;
    }

    public Stream<MerkleNode> childStream() {
        return Arrays.stream(children);
    }

    public MerkleNode getLeaf(int index) {
        return null;
    }

    /*
    Return the number of keys
     */
    public int leafCount() {
        return childCount() == 0 ? 1 : childStream().parallel()
                .map(MerkleNode::leafCount)
                .reduce(Math::addExact)
                .orElse(0);
    }

    /*
    Return the number of children
     */
    public int childCount() {
        return children.length;
    }

    /*
    returns the size of the tree (number of all nodes)
     */
    public int size() {
        return 1 + childStream().parallel()
                .map(MerkleNode::size)
                .reduce(Math::addExact)
                .orElse(0);
    }

    @Override
    public boolean equals(final Object obj) {
        if(obj == null || !(obj instanceof MerkleNode)) return false;
        return Arrays.equals(value, ((MerkleNode) obj).value);
    }

    @Override
    public int compareTo(MerkleNode merkleNode) {
        return this.equals(merkleNode) ? 0 : 1;
    }
}
