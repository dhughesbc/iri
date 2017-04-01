package com.iota.iri.hash.keys;

import com.iota.iri.hash.ISS;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.function.Function;

/**
 * Created by paul on 2/22/17.
 */
public class TupleArray {


    public final Tuple[] keys;

    private TupleArray(int size) {
        this.keys = new Tuple[size];
    }

    private TupleArray(Tuple[] tuples) {
        keys = tuples.clone();
    }

    public static TupleArray create(Tuple[] tupleTrits) {
        TupleArray tupleArray = new TupleArray(tupleTrits.length);
        System.arraycopy(tupleTrits, 0, tupleArray.keys, 0, tupleTrits.length);
        return tupleArray;
    }

    public Function<TupleArray, TupleArray> unshift(int count) {
        return (tupleArray) -> unshift(tupleArray, count);
    }


    private static int unshiftValue(int source, int dest, int index) {
        return (dest << 1) | ((source & (1 << index)) >> index);
    }

    private TupleArray unshift(TupleArray source, int index) {
        assert(keys.length == source.keys.length);
        TupleArray tupleArray = new TupleArray(keys);
        int i;
        for(i = 0; i < source.keys.length; i++) {
            source.keys[i] = new Tuple(
                    unshiftValue(tupleArray.keys[i].low ,source.keys[i].low, index),
                    unshiftValue(tupleArray.keys[i].hi ,source.keys[i].hi, index)
            );
        }
        return tupleArray;
    }

    public TupleArray doubleShift(int index) {
        return create(Arrays.stream(keys).parallel()
                .map(t -> t.alternatingShift(index))
                .toArray(Tuple[]::new));
    }

    public TupleArray addAll(TupleArray tupleArray) {
        return TupleArray.create(ArrayUtils.addAll(keys, tupleArray.keys));
    }

    public TupleArray meld(TupleArray tupleArray) {
        int i;
        TupleArray result = new TupleArray(this.keys);
        if(tupleArray != null) {
            for(i = 0; i < result.keys.length; i++) {
                result.keys[i] = result.keys[i]
                        .compressRight(ISS.NUMBER_OF_COMBINATIONS_PER_KEY, 0)
                        .combine(tupleArray.keys[i].compressRight(ISS.NUMBER_OF_COMBINATIONS_PER_KEY, 1));
            }
        }
        return result;
    }

    public Function<Tuple[], TupleArray> offset(int split) {
        return tupleArray -> {
            TupleArray result = TupleArray.create(keys);
            for(int i = 0; i < keys.length; i++) {
                result.keys[i] = keys[i].incresplit(split).apply(tupleArray[i]);
            }
            return result;
        };
    }
}
