/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.geophile.z.spatialjoin;

import com.geophile.z.ApplicationSpace;
import com.geophile.z.Space;
import com.geophile.z.SpatialJoin;
import com.geophile.z.SpatialObject;
import com.geophile.z.spatialobject.jts.JTSLineString;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.junit.Test;

import java.io.IOException;
import java.util.logging.Level;

import static org.junit.Assert.assertEquals;

public class SpatialJoinIteratorLineStringTest extends SpatialJoinIteratorJTSTestBase
{
    @Override
    protected JTSLineString newLeftObject(int maxXSize, int maxYSize)
    {
        int nx = (int) (appSpace.hi(0) - appSpace.lo(0));
        int ny = (int) (appSpace.hi(1) - appSpace.lo(1));
        Coordinate[] coords = new Coordinate[3];
        int c = 0;
        long x = random.nextInt(nx);
        long y = random.nextInt(ny);
        coords[c++] = new Coordinate(x, y);
        for (int i = 0; i < 2; i++) {
            long xNew;
            long yNew;
            do {
                xNew = x - maxXSize + random.nextInt(2 * maxXSize);
                yNew = y - maxYSize + random.nextInt(2 * maxYSize);
            } while (!(xNew >= 0 && xNew < nx && yNew >= 0 && yNew < ny));
            coords[c++] = new Coordinate(xNew, yNew);
            x = xNew;
            y = yNew;
        }
        return new JTSLineString(space, factory.createLineString(coords));
    }
}
