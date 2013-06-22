/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.z.spatialjoin;

import com.geophile.z.index.Cursor;
import com.geophile.z.index.Record;
import com.geophile.z.index.SpatialObjectKey;
import com.geophile.z.space.SpaceImpl;
import com.geophile.z.space.SpatialIndexImpl;

import java.io.IOException;

class Ancestors
{
    public Record find(long z) throws IOException, InterruptedException
    {
        Record record;
        int level = SpaceImpl.length(z);
        record = records[level];
        if (record == null) {
            record = new Record();
            records[level] = record;
        }
        if (record.key() == null || record.key().z() != z) {
            SpatialObjectKey key = SpatialObjectKey.keyLowerBound(z);
            cursor.goTo(key);
            cursor.next().copyTo(record);
        } else {
            counters.countAncestorInCache();
        }
        counters.countAncestorFind();
        return !record.eof() && record.key().z() == z ? record : null;
    }

    public Ancestors(SpatialIndexImpl index) throws IOException, InterruptedException
    {
        this.cursor = SpatialJoinInput.newCursor(index);
    }

    // Object state

    private final Cursor cursor;
    private final Record[] records = new Record[SpaceImpl.MAX_Z_BITS];
    private final Counters counters = Counters.forThread();
}
