package com.iota.iri.hash;

import com.iota.iri.hash.keys.TupleArray;
import com.iota.iri.hash.keys.TupleKeyArray;
import com.iota.iri.hash.keys.MerkleNode;
import com.iota.iri.hash.keys.Tuple;
import com.iota.iri.utils.Converter;
import com.iota.iri.utils.TritMath;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * (c) 2016 Come-from-Beyond and Paul Handy
 */
public class ISS {

    public static final int NUMBER_OF_FRAGMENT_CHUNKS = 27;
    private static final int FRAGMENT_LENGTH = Curl.HASH_LENGTH * NUMBER_OF_FRAGMENT_CHUNKS;
    private static final int NUMBER_OF_SECURITY_LEVELS = 3;

    public static final int NUMBER_OF_KEYS_PER_TUPLE = 27;
    public static final int NUMBER_OF_COMBINATIONS_PER_KEY = (NUMBER_OF_KEYS_PER_TUPLE + 1)/2;
    protected static final int[] INCREMENT_MASK = IntStream.iterate(1, i -> 1 + (i << 1)).limit(ISS.NUMBER_OF_KEYS_PER_TUPLE).toArray();

    protected static final int[] OFFSET = new int[] {
            0b011011011011011011011011011,
            0b110110110110110110110110110,
            0b000111111000111111000111111,
            0b111111000111111000111111000,
            0b000000000111111111111111111,
            0b111111111111111111000000000,
    };

    private static final int SUBSEED_MASK = 0b111111111111111111111111111;

    private static final int TRYTE_WIDTH = 3;
    private static final int MIN_TRYTE_VALUE = -13, MAX_TRYTE_VALUE = 13;

    private static int[] subseedPreiamge(final int[] seed, int index) {
        if (index < 0) {
            throw new RuntimeException("Invalid subseed index: " + index);
        }

        final int[] subseedPreimage = Arrays.copyOf(seed, seed.length);

        while (index-- > 0) {
            Converter.increment(subseedPreimage, subseedPreimage.length);
        }

        return subseedPreimage;
    }
    public static int[] subseed(final int[] seed, int index) {

        final int[] subseedPreimage = subseedPreiamge(seed, index);

        final int[] subseed = new int[Curl.HASH_LENGTH];

        final Curl hash = new Curl();
        hash.absorb(subseedPreimage, 0, subseedPreimage.length);
        hash.squeeze(subseed, 0, subseed.length);
        return subseed;
    }

    private static int[] privateKey(final int[] subseed, final int keySize) {
        final int[] key = new int[keySize];

        final Curl hash = new Curl();
        hash.absorb(subseed, 0, subseed.length);
        hash.squeeze(key, 0, key.length);
        return key;
    }

    public static int[] key(final int[] subseed, final int numberOfFragments) {

        if (subseed.length != Curl.HASH_LENGTH) {
            throw new RuntimeException("Invalid subseed length: " + subseed.length);
        }
        if (numberOfFragments <= 0) {
            throw new RuntimeException("Invalid number of key fragments: " + numberOfFragments);
        }
        return privateKey(subseed, FRAGMENT_LENGTH * numberOfFragments);
    }

    public static int[] digests(final int[] key) {

        if (key.length == 0 || key.length % FRAGMENT_LENGTH != 0) {

            throw new RuntimeException("Invalid key length: " + key.length);
        }

        final int[] digests = new int[key.length / FRAGMENT_LENGTH * Curl.HASH_LENGTH];

        for (int i = 0; i < key.length / FRAGMENT_LENGTH; i++) {

            final int[] buffer = Arrays.copyOfRange(key, i * FRAGMENT_LENGTH, (i + 1) * FRAGMENT_LENGTH);
            for (int j = 0; j < NUMBER_OF_FRAGMENT_CHUNKS; j++) {

                for (int k = MAX_TRYTE_VALUE - MIN_TRYTE_VALUE; k-- > 0; ) {
                    final Curl hash = new Curl();
                    hash.absorb(buffer, j * Curl.HASH_LENGTH, Curl.HASH_LENGTH);
                    hash.squeeze(buffer, j * Curl.HASH_LENGTH, Curl.HASH_LENGTH);
                }
            }
            final Curl hash = new Curl();
            hash.absorb(buffer, 0, buffer.length);
            hash.squeeze(digests, i * Curl.HASH_LENGTH, Curl.HASH_LENGTH);
        }

        return digests;
    }

    public static int[] publicKey(final int[] digests, final int keySize) {
        if (digests.length == 0 || digests.length % Curl.HASH_LENGTH != 0) {
            throw new RuntimeException("Invalid digests length: " + digests.length);
        }

        final int[] publicKey = new int[keySize];

        final Curl hash = new Curl();
        hash.absorb(digests, 0, digests.length);
        hash.squeeze(publicKey, 0, publicKey.length);

        return publicKey;
    }
    public static int[] address(final int[] digests) {
        return publicKey(digests, Curl.HASH_LENGTH);
    }

    public static int[] normalizedBundle(final int[] bundle) {

        if (bundle.length != Curl.HASH_LENGTH) {
            throw new RuntimeException("Invalid bundle length: " + bundle.length);
        }

        final int[] normalizedBundle = new int[Curl.HASH_LENGTH / TRYTE_WIDTH];

        for (int i = 0; i < NUMBER_OF_SECURITY_LEVELS; i++) {

            int sum = 0;
            for (int j = i * (Curl.HASH_LENGTH / TRYTE_WIDTH / NUMBER_OF_SECURITY_LEVELS); j < (i + 1) * (Curl.HASH_LENGTH / TRYTE_WIDTH / NUMBER_OF_SECURITY_LEVELS); j++) {

                normalizedBundle[j] = bundle[j * TRYTE_WIDTH] + bundle[j * TRYTE_WIDTH + 1] * 3 + bundle[j * TRYTE_WIDTH + 2] * 9;
                sum += normalizedBundle[j];
            }
            if (sum > 0) {

                while (sum-- > 0) {

                    for (int j = i * (Curl.HASH_LENGTH / TRYTE_WIDTH / NUMBER_OF_SECURITY_LEVELS); j < (i + 1) * (Curl.HASH_LENGTH / TRYTE_WIDTH / NUMBER_OF_SECURITY_LEVELS); j++) {

                        if (normalizedBundle[j] > MIN_TRYTE_VALUE) {
                            normalizedBundle[j]--;
                            break;
                        }
                    }
                }

            } else {

                while (sum++ < 0) {

                    for (int j = i * (Curl.HASH_LENGTH / TRYTE_WIDTH / NUMBER_OF_SECURITY_LEVELS); j < (i + 1) * (Curl.HASH_LENGTH / TRYTE_WIDTH / NUMBER_OF_SECURITY_LEVELS); j++) {

                        if (normalizedBundle[j] < MAX_TRYTE_VALUE) {
                            normalizedBundle[j]++;
                            break;
                        }
                    }
                }
            }
        }

        return normalizedBundle;
    }

    public static int[] signatureFragment(final int[] normalizedBundleFragment, final int[] keyFragment) {

        if (normalizedBundleFragment.length != Curl.HASH_LENGTH / TRYTE_WIDTH / NUMBER_OF_SECURITY_LEVELS) {
            throw new RuntimeException("Invalid normalized bundle fragment length: " + normalizedBundleFragment.length);
        }
        if (keyFragment.length != FRAGMENT_LENGTH) {
            throw new RuntimeException("Invalid key fragment length: " + keyFragment.length);
        }

        final int[] signatureFragment = Arrays.copyOf(keyFragment, keyFragment.length);

        for (int j = 0; j < NUMBER_OF_FRAGMENT_CHUNKS; j++) {

            for (int k = MAX_TRYTE_VALUE - normalizedBundleFragment[j]; k-- > 0; ) {

                final Curl hash = new Curl();
                hash.absorb(signatureFragment, j * Curl.HASH_LENGTH, Curl.HASH_LENGTH);
                hash.squeeze(signatureFragment, j * Curl.HASH_LENGTH, Curl.HASH_LENGTH);
            }
        }

        return signatureFragment;
    }

    public static int[] digest(final int[] normalizedBundleFragment, final int[] signatureFragment) {

        if (normalizedBundleFragment.length != Curl.HASH_LENGTH / TRYTE_WIDTH / NUMBER_OF_SECURITY_LEVELS) {
            throw new RuntimeException("Invalid normalized bundle fragment length: " + normalizedBundleFragment.length);
        }
        if (signatureFragment.length != FRAGMENT_LENGTH) {
            throw new RuntimeException("Invalid signature fragment length: " + signatureFragment.length);
        }

        final int[] digest = new int[Curl.HASH_LENGTH];

        final int[] buffer = Arrays.copyOf(signatureFragment, FRAGMENT_LENGTH);
        for (int j = 0; j < NUMBER_OF_FRAGMENT_CHUNKS; j++) {

            for (int k = normalizedBundleFragment[j] - MIN_TRYTE_VALUE; k-- > 0; ) {

                final Curl hash = new Curl();
                hash.absorb(buffer, j * Curl.HASH_LENGTH, Curl.HASH_LENGTH);
                hash.squeeze(buffer, j * Curl.HASH_LENGTH, Curl.HASH_LENGTH);
            }
        }
        final Curl hash = new Curl();
        hash.absorb(buffer, 0, buffer.length);
        hash.squeeze(digest, 0, digest.length);

        return digest;
    }


    /*
        Generate 27 seeds at once
     */

    private static TupleArray offsetSubseed(int[] seed) {
        int initialValue;
        initialValue = (int) Converter.longValue(seed, 0, 3);

        final int split = initialValue - MIN_TRYTE_VALUE;
        final int[] offset = initialValue > MIN_TRYTE_VALUE ?
                Arrays.stream(OFFSET)
                        .map(i -> (((i & ~INCREMENT_MASK[split]) << (MAX_TRYTE_VALUE - split))) |
                                (((i >> (split)) & INCREMENT_MASK[split]) & SUBSEED_MASK))
                        .toArray():
                OFFSET;

        final TupleArray subseedPreimage = initialValue > MIN_TRYTE_VALUE ?
                TupleArray.create(Converter.tuple(seed))
                        .offset(initialValue - MIN_TRYTE_VALUE)
                        .apply(Converter.tuple(Converter.incremented(seed, seed.length))):
                TupleArray.create(Converter.tuple(seed));

        for(int i = 0; i < 3; i++) {
            subseedPreimage.keys[i].low = offset[i*2];
            subseedPreimage.keys[i].hi = offset[i*2+1];
        }
        return subseedPreimage;
    }

    public static TupleArray subseeds(final int[] seed, int index) {
        final int[] subseedPreimage = subseedPreiamge(seed, index);

        final TupleArray subseedTupleImage = offsetSubseed(subseedPreimage);

        return TupleArray.create(Curl.squeeze(Curl.absorb(Curl.state(),
                subseedTupleImage.keys, 0, subseedTupleImage.keys.length),
                new Tuple[Curl.HASH_LENGTH], 0, Curl.HASH_LENGTH));
    }

    private static TupleArray privateKeys(final TupleArray subseed, final int keySize) {
        return TupleArray.create(Curl.squeeze(Curl.absorb(Curl.state(), subseed.keys, 0, subseed.keys.length),
                new Tuple[keySize], 0, keySize));
    }

    public static Tuple[] digests(final TupleArray tupleArray) {

        if (tupleArray.keys.length == 0 || tupleArray.keys.length % FRAGMENT_LENGTH != 0) {

            throw new RuntimeException("Invalid key length: " + tupleArray.keys.length);
        }

        final Tuple[] digests = new Tuple[tupleArray.keys.length / FRAGMENT_LENGTH * Curl.HASH_LENGTH];
        int i, j, k;
        for(i = 0; i < tupleArray.keys.length / FRAGMENT_LENGTH; i++) {
            final Tuple[] buffer = Arrays.copyOfRange(tupleArray.keys, i * FRAGMENT_LENGTH, (i + 1) * FRAGMENT_LENGTH);
            for(j = 0; j < NUMBER_OF_FRAGMENT_CHUNKS; j++) {
                for(k = 0; k < MAX_TRYTE_VALUE - MIN_TRYTE_VALUE; k++) {
                    Curl.squeeze(
                            Curl.absorb(Curl.state(), buffer, j * Curl.HASH_LENGTH, Curl.HASH_LENGTH),
                            buffer, j * Curl.HASH_LENGTH, Curl.HASH_LENGTH);
                }

            }
            System.arraycopy(
                    Curl.squeeze(Curl.absorb(Curl.state(), buffer, 0, buffer.length),
                            digests, i * Curl.HASH_LENGTH, Curl.HASH_LENGTH),
                    0, digests, 0, digests.length);
        }
        return digests;
    }
    public static Tuple[] publicKeys(final Tuple[] digests, final int keySize) {
        if (digests.length == 0 || digests.length % Curl.HASH_LENGTH != 0) {
            throw new RuntimeException("Invalid digests length: " + digests.length);
        }

        return Curl.squeeze(
                Curl.absorb(Curl.state(), digests, 0, digests.length),
                new Tuple[keySize], 0, keySize);
    }

    /*
        grabs every other bit and shifts it

    private static Tuple[] compressThirds(Tuple[] tuple, int index) {
        return Arrays.stream(tuple).map(t -> compressTupleRight(t, NUMBER_OF_COMBINATIONS_PER_KEY, index)).toArray(Tuple[]::new);
    }
     */

    private static void combineArrays(Tuple[] first, Tuple[] second) {
        for(int i = 0; i < first.length; i++ ) {
            first[i].low |= second[i].low;
            first[i].hi |= second[i].hi;
        }
    }


    private static TupleKeyArray getNextLevel(TupleKeyArray level, int count, int size) {
        int numberOfTupleArrays = count / NUMBER_OF_KEYS_PER_TUPLE + (count % NUMBER_OF_KEYS_PER_TUPLE == 0? 0:1);
        int j, k, start, end;
        TupleKeyArray tupleKeyArray = TupleKeyArray.create(new TupleArray[numberOfTupleArrays]);
        TupleArray[] midLevel = new TupleArray[2];

        for(j = 0; j < numberOfTupleArrays; j++) {
            start = j*2;
            end = (start > level.keys.length- 2 ? level.keys.length: start + 2);
            k = start;
            while(k < end) {

                midLevel[k-start] = level.keys[k].doubleShift(0).addAll(level.keys[k].doubleShift(1));//TupleArray.create(ArrayUtils.addAll(doubleShift(level.keys[k].keys, 0), doubleShift(level.keys[k].keys, 1)));
                k++;
                if(k < level.keys.length) {
                    level.keys[k] = level.keys[k-1].unshift(NUMBER_OF_KEYS_PER_TUPLE-1).apply(level.keys[k]);
                    //unshiftTupleArray(level.keys[k - 1], level.keys[k], NUMBER_OF_KEYS_PER_TUPLE-1);
                }
            }

            Tuple[] keys = midLevel[0].meld(midLevel[1]).keys;
            //Tuple[] keys = meld(midLevel).keys;
            tupleKeyArray.keys[j] = TupleArray.create(Curl.squeeze(
                    Curl.absorb(Curl.state(), keys, 0, keys.length),
                    new Tuple[size], 0, size));
        }
        return tupleKeyArray;
    }

    public static MerkleNode[] nodesFromTuples(TupleKeyArray keys, final int keyCount) {
        MerkleNode[] individualKeys = new MerkleNode[keyCount];
        int i = 0, j = 0, k = 0;
        while(k < keyCount) {
            individualKeys[k++] = MerkleNode.create(Converter.trits(keys.keys[i].keys, j));
            j++;
            if(j == NUMBER_OF_KEYS_PER_TUPLE + 1) {
                j = 0;
                i++;
            }
        }
        return individualKeys;
    }

    public static TupleKeyArray[] createNKeys(int[] seed, int startingIndex, int numberOfKeysToCreate, final int securityLevel) {
        int i, numberOfTupleArrays;
        numberOfTupleArrays = numberOfKeysToCreate / NUMBER_OF_KEYS_PER_TUPLE + (numberOfKeysToCreate % NUMBER_OF_KEYS_PER_TUPLE == 0? 0:1);
        TupleKeyArray privateKeys = TupleKeyArray.create(numberOfTupleArrays);
        TupleKeyArray publicKeys = TupleKeyArray.create(numberOfTupleArrays);
        for(i = 0; i < numberOfTupleArrays; i ++) {
            privateKeys.keys[i] = privateKeys(subseeds(seed,i*NUMBER_OF_KEYS_PER_TUPLE + startingIndex),securityLevel*NUMBER_OF_FRAGMENT_CHUNKS*NUMBER_OF_SECURITY_LEVELS);
            publicKeys.keys[i] = TupleArray.create(publicKeys(digests(privateKeys.keys[i]), securityLevel * NUMBER_OF_SECURITY_LEVELS));
        }
        return new TupleKeyArray[]{privateKeys, publicKeys};
    }

    private static MerkleNode[] getPublicKeyNodes(TupleKeyArray[] privPubkeys, int numberOfKeysToCreate) {
        int i;
        MerkleNode[] merkleKeys = nodesFromTuples(privPubkeys[0], numberOfKeysToCreate);
        MerkleNode[] merklePubKeys = nodesFromTuples(privPubkeys[1], numberOfKeysToCreate);
        for(i = 0; i < merklePubKeys.length; i++) {
            merklePubKeys[i].children = new MerkleNode[]{merkleKeys[i]};
        }
        return merklePubKeys;
    }

    public static MerkleNode merkleTree(int[] seed, int startingIndex, int numberOfKeysToCreate, final int securityLevel) {
        if(securityLevel % 81 != 0)
            throw new RuntimeException("Invalid key Size: " + securityLevel);
        int hashSize, i, j, k, start, sz, numberOfLevels;
        hashSize = numberOfKeysToCreate;
        numberOfLevels = 32 - Integer.numberOfLeadingZeros(numberOfKeysToCreate);// + 1;//(int) Math.ceil(Math.log(numberOfKeysToCreate)/Math.log(2)) + 1;
        TupleKeyArray[] keys = createNKeys(seed, startingIndex, numberOfKeysToCreate, securityLevel);
        TupleKeyArray currentLevel = keys[1];
        MerkleNode[] currentLevelKeys, prevLevelKeys;
        prevLevelKeys = getPublicKeyNodes(keys, numberOfKeysToCreate);

        for(i = 1; i < numberOfLevels; i++) {
            hashSize = hashSize / 2 + (hashSize % 2 == 0 ? 0 : 1);
            currentLevel = getNextLevel(currentLevel, hashSize, securityLevel*NUMBER_OF_SECURITY_LEVELS);
            currentLevelKeys = nodesFromTuples(currentLevel, hashSize);
            for(j = 0; j < currentLevelKeys.length; j++) {
                start = j*2;
                sz = start+2 >= prevLevelKeys.length? prevLevelKeys.length-start:2;
                currentLevelKeys[j].children = new MerkleNode[sz];
                for(k = 0; k < sz; k++) {
                    if(j*2+k >= prevLevelKeys.length) break;
                    currentLevelKeys[j].children[k] = prevLevelKeys[j*2 + k];
                }
            }
            prevLevelKeys = currentLevelKeys;
        }
        return prevLevelKeys[0];
    }
}
