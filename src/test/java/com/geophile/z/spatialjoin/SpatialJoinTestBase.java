/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.z.spatialjoin;

import com.geophile.z.*;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

public abstract class SpatialJoinTestBase
{
    protected final void testJoin(SpatialJoin spatialJoin, TestInput leftInput, TestInput rightInput)
        throws IOException, InterruptedException
    {
        testManyManyJoin(spatialJoin, leftInput, rightInput);
        if (leftInput.spatialObjects().size() == 1) {
            testOneManyJoin(spatialJoin, leftInput.only(), rightInput);
        } else if (rightInput.spatialObjects().size() == 1) {
            testOneManyJoin(spatialJoin, rightInput.only(), leftInput);
        }
    }

    protected final void testOneManyJoin(SpatialJoin spatialJoin, SpatialObject query, TestInput data)
        throws IOException, InterruptedException
    {
        // The query object is the left join argument, data is the right join argument.
        this.spatialJoin = spatialJoin;
        this.query = query;
        this.rightInput = data;
        this.testStats = new TestStats();
        try {
            Map<SpatialObject, Integer> actual = computeOneManySpatialJoin();
            Set<SpatialObject> expected = null;
            if (verify()) {
                if (spatialJoin.duplicates() == SpatialJoin.Duplicates.EXCLUDE) {
                    checkNoDuplicates(actual);
                }
                expected = computeExpectedOneManySpatialJoin();
                checkEquals(expected, actual.keySet());
            }
            if (trace()) {
                if (expected != null) {
                    print("expected");
                    for (SpatialObject spatialObject : expected) {
                        print("    %s", spatialObject);
                    }
                }
                print("actual");
                for (Map.Entry<SpatialObject, Integer> entry : actual.entrySet()) {
                    print("    %s: %s", entry.getKey(), entry.getValue());
                }
            }
            if (printSummary() && expected != null) {
                double speedup = (double) testStats.slowJoinTimeNsec / testStats.joinTimeNsec;
                if (expected.size() == 0) {
                    print("%s\taccuracy = EMPTY RESULT\tspeedup = %s",
                          describeTest(), speedup);
                } else {
                    double accuracy = (double) testStats.overlapCount / testStats.filterCount;
                    print("%s\tcount = %s\taccuracy = %s\tspeedup = %s",
                          describeTest(), actual.size(), accuracy, speedup);
                }
            }
        } catch (AssertionError e) {
            print("Assertion error on %s", describeTest());
            throw e;
        }
    }

    protected final void testManyManyJoin(SpatialJoin spatialJoin, TestInput leftInput, TestInput rightInput)
        throws IOException, InterruptedException
    {
        this.spatialJoin = spatialJoin;
        this.leftInput = leftInput;
        this.rightInput = rightInput;
        this.testStats = new TestStats();
        try {
            Map<Pair, Integer> actual = computeManyManySpatialJoin();
            Set<Pair> expected = null;
            if (verify()) {
                if (spatialJoin.duplicates() == SpatialJoin.Duplicates.EXCLUDE) {
                    checkNoDuplicates(actual);
                }
                expected = computeExpectedManyManySpatialJoin();
                checkEquals(expected, actual.keySet());
            }
            if (trace()) {
                if (expected != null) {
                    print("expected");
                    for (Pair pair : expected) {
                        print("    %s", pair);
                    }
                }
                print("actual");
                for (Map.Entry<Pair, Integer> entry : actual.entrySet()) {
                    print("    %s: %s", entry.getKey(), entry.getValue());
                }
            }
            if (printSummary() && expected != null) {
                double speedup = (double) testStats.slowJoinTimeNsec / testStats.joinTimeNsec;
                if (expected.size() == 0) {
                    print("%s\taccuracy = EMPTY RESULT\tspeedup = %s",
                          describeTest(), speedup);
                } else {
                    double accuracy = (double) testStats.overlapCount / testStats.filterCount;
                    print("%s\tcount = %s\taccuracy = %s\tspeedup = %s",
                          describeTest(), actual.size(), accuracy, speedup);
                }
            }
        } catch (AssertionError e) {
            print("Assertion error on %s", describeTest());
            throw e;
        }
    }

    protected final TestInput newTestInput(int n, SpatialObjectGenerator spatialObjectGenerator)
        throws IOException, InterruptedException
    {
        SpatialIndex spatialIndex = SpatialIndex.newSpatialIndex(space(), newIndex());
        TestInput testInput = new TestInput(spatialIndex, spatialObjectGenerator.description());
        load(n, spatialObjectGenerator, testInput);
        commit();
        return testInput;
    }

    protected abstract Space space();

    protected abstract Index newIndex();

    protected void commit()
    {
    }

    protected abstract boolean overlap(SpatialObject s, SpatialObject t);

    protected abstract boolean verify();

    protected abstract boolean printSummary();

    protected abstract boolean trace();

    protected void checkEquals(Object expected, Object actual)
    {
        assertEquals(expected, actual);
    }

    protected void load(int n, SpatialObjectGenerator spatialObjectGenerator, TestInput testInput)
        throws IOException, InterruptedException
    {
        for (int i = 0; i < n; i++) {
            testInput.add(spatialObjectGenerator.newSpatialObject());
        }
    }

    protected final void print(String template, Object... args)
    {
        System.out.println(String.format(template, args));
    }

    private void checkNoDuplicates(Map<?, Integer> actual)
    {
        for (Integer count : actual.values()) {
            assertEquals(1, count.intValue());
        }
    }

    private Map<SpatialObject, Integer> computeOneManySpatialJoin() throws IOException, InterruptedException
    {
        Map<SpatialObject, Integer> actual = new HashMap<>(); // SpatialObject -> occurrence count
        long start = System.nanoTime();
        Iterator<SpatialObject> joinScan = spatialJoin.iterator(query, rightInput.spatialIndex());
        while (joinScan.hasNext()) {
            SpatialObject spatialObject = joinScan.next();
            if (verify()) {
                Integer count = actual.get(spatialObject);
                if (count == null) {
                    count = 0;
                }
                actual.put(spatialObject, count + 1);
            }
            testStats.outputRowCount++;
        }
        long stop = System.nanoTime();
        testStats.joinTimeNsec += stop - start;
        return actual;
    }

    private Map<Pair, Integer> computeManyManySpatialJoin() throws IOException, InterruptedException
    {
        Map<Pair, Integer> actual = new HashMap<>(); // Pair -> occurrence count
        long start = System.nanoTime();
        Iterator<Pair> joinScan = spatialJoin.iterator(leftInput.spatialIndex(), rightInput.spatialIndex());
        while (joinScan.hasNext()) {
            Pair pair = joinScan.next();
            if (verify()) {
                Integer count = actual.get(pair);
                if (count == null) {
                    count = 0;
                }
                actual.put(pair, count + 1);
            }
            testStats.outputRowCount++;
        }
        long stop = System.nanoTime();
        testStats.joinTimeNsec += stop - start;
        return actual;
    }

    private Set<SpatialObject> computeExpectedOneManySpatialJoin()
    {
        Set<SpatialObject> expected = new HashSet<>();
        long start = System.nanoTime();
        for (SpatialObject s : rightInput.spatialObjects()) {
            if (overlap(query, s)) {
                expected.add(s);
            }
        }
        long stop = System.nanoTime();
        testStats.slowJoinTimeNsec += stop - start;
        return expected;
    }

    private Set<Pair> computeExpectedManyManySpatialJoin()
    {
        Set<Pair> expected = new HashSet<>();
        long start = System.nanoTime();
        for (SpatialObject s : leftInput.spatialObjects()) {
            for (SpatialObject t : rightInput.spatialObjects()) {
                if (overlap(s, t)) {
                    expected.add(new Pair(s, t));
                }
            }
        }
        long stop = System.nanoTime();
        testStats.slowJoinTimeNsec += stop - start;
        return expected;
    }

    private String describeTest()
    {
        return String.format("duplicates = %s\tLEFT: %s\tRIGHT: %s\t", spatialJoin.duplicates(), leftInput, rightInput);
    }

    private SpatialJoin spatialJoin;
    private TestInput leftInput;
    private TestInput rightInput;
    private SpatialObject query;
    protected TestStats testStats;
}