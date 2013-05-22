/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.z.spatialjoin;

import com.geophile.z.Pair;
import com.geophile.z.SpatialIndex;
import com.geophile.z.spatialobject.SpatialObject;

import java.util.Iterator;

public class SpatialJoin<LEFT extends SpatialObject, RIGHT extends SpatialObject>
{
    public SpatialJoin(SpatialJoinFilter<LEFT, RIGHT> filter, Duplicates duplicates)
    {
        this.filter = filter;
        this.duplicates = duplicates;
    }

    public Iterator<Pair<LEFT, RIGHT>> iterator(SpatialIndex<LEFT> leftSpatialIndex,
                                                SpatialIndex<RIGHT> rightSpatialIndex)
    {
        Iterator<Pair<LEFT, RIGHT>> iterator =
            new SpatialJoinIterator<>(leftSpatialIndex, rightSpatialIndex, filter);
        if (duplicates == Duplicates.EXCLUDE) {
            iterator = new DuplicateEliminatingIterator<>(iterator);
        }
        return iterator;
    }

    public enum Duplicates { INCLUDE, EXCLUDE }

    private final SpatialJoinFilter<LEFT, RIGHT> filter;
    private final Duplicates duplicates;
}
