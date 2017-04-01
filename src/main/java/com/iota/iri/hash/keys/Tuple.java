package com.iota.iri.hash.keys;

import com.iota.iri.hash.ISS;

import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Created by paul on 2/18/17.
 */
public class Tuple {
    public static final int RADIX = 3;
    public static final int MAX_TRIT_VALUE = (RADIX - 1) / 2, MIN_TRIT_VALUE = -MAX_TRIT_VALUE;
    protected static final int[] INCREMENT_MASK = IntStream.iterate(1, i -> 1 + (i << 1)).limit(ISS.NUMBER_OF_KEYS_PER_TUPLE).toArray();

    private static final int HIGH_BITS = 0b11111111111111111111111111111111;
    private static final int LOW_BITS = 0b00000000000000000000000000000000;
    private static final int SHIFT_MASK = 0b0101010101010101010101010101;

    public int low;
    public int hi;

    public Tuple() {
        low = HIGH_BITS;
        hi = HIGH_BITS;
    }
    public Tuple(int low, int hi) {
        this.low = low;
        this.hi = hi;
    }
    public Tuple(int trit) {
        low = trit == 0 ? HIGH_BITS: (trit == 1 ? LOW_BITS: HIGH_BITS);
        hi = trit == 0 ? HIGH_BITS: (trit == 1 ? HIGH_BITS: LOW_BITS);
    }
    public int value(int bitIndex) {
        return (low & (1 << bitIndex)) == 0 ? MAX_TRIT_VALUE : (hi & (1 << bitIndex)) == 0 ? MIN_TRIT_VALUE : 0;
    }
    public int value() {
        return value(0);
        //return low == LOW_BITS ? MAX_TRIT_VALUE : hi == LOW_BITS ? MIN_TRIT_VALUE : 0;
    }
    public void unshift(Tuple tuple) {
        low <<= 1;
        hi <<= 1;
        low &= (tuple.low & 1);
        hi &= (tuple.hi & 1);
    }

    public Tuple alternatingShift(int index) {
        return new Tuple(alternatingShiftMask(low, index), alternatingShiftMask(hi, index));
    }
    // was doubleShiftMask
    private static int alternatingShiftMask(int val, int index) {
        return (val & (SHIFT_MASK << index)) >> index;
    }

    public Tuple compressRight(int size, int index) {
        int i;
        Tuple tuple = new Tuple(low, hi);
        for(i = 0; i < size; i++) {
            tuple.low = compressValueRight(tuple.low, i);
            tuple.hi = compressValueRight(tuple.hi, i);
        }
        tuple.low <<= index*size;
        tuple.hi <<= index*size;
        return tuple;
    }

    private static int compressValueRight(int val, int index) {
        return (val & INCREMENT_MASK[index]) | ((val & ~INCREMENT_MASK[index])>>1);
    }

    public Tuple combine(Tuple tuple) {
        return new Tuple(low | tuple.low, hi | tuple.hi);
    }

    public Function<Tuple, Tuple> incresplit(int split) {
        return (tuple) ->
                new Tuple((low & INCREMENT_MASK[split])| (tuple.low & (HIGH_BITS & ~INCREMENT_MASK[split])),
                        (low & INCREMENT_MASK[split])| (tuple.hi & (HIGH_BITS & ~INCREMENT_MASK[split])));
    }
}
