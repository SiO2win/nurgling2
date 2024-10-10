package nurgling.pf;

import haven.*;
import nurgling.*;
import java.util.Arrays;

public class CellsArray {
    public Coord begin;
    public Coord end;
    public boolean[][] boolMask;

    public int x_len;
    public int y_len;

    public CellsArray cloneClippedCA(Coord clipUL, Coord clipBR) {
        Coord ul = Coord.of(Math.max(Math.min(clipUL.x, clipBR.x), begin.x), Math.max(Math.min(clipUL.y, clipBR.y), begin.y));
        Coord br = Coord.of(Math.min(Math.max(clipUL.x, clipBR.x), end.x), Math.min(Math.max(clipUL.y, clipBR.y), end.y));
        if (br.compareTo(ul) < 0)
            return null;
        CellsArray theCopy = new CellsArray(br.x - ul.x + 1, br.y - ul.y + 1);
        for (int ix = Math.max(ul.x, begin.x) - begin.x; ix < Math.min(br.x, end.x) - end.x + x_len; ix++)
            System.arraycopy(boolMask[ix], Math.max(ul.y, begin.y) - begin.y, theCopy.boolMask[ix - (Math.max(ul.x, begin.x) - begin.x)], 0, theCopy.y_len);
        theCopy.begin = ul;
        theCopy.end = br;
        return theCopy;
    }

    public CellsArray(Gob gob) {
        this(gob.ngob.hitBox, gob.a, gob.rc);
    }

    public CellsArray(int x_len, int y_len) {
        this.boolMask = new boolean[x_len][y_len];
        this.x_len = x_len;
        this.y_len = y_len;
    }

    public CellsArray(NHitBox hb, double angl, Coord2d rc) {
        NHitBoxD objToApproach = new NHitBoxD(hb.begin, hb.end, rc, angl);
        begin = Utils.toPfGrid(objToApproach.getCircumscribedUL());
        end = Utils.toPfGrid(objToApproach.getCircumscribedBR());
        NHitBoxD tile = new NHitBoxD(begin);
        x_len = end.x - begin.x + 1;
        y_len = end.y - begin.y + 1;
        boolMask = new boolean[x_len][y_len];
        for (int i = 0; i < x_len; i++) {
            for (int j = 0; j < y_len; j++) {
                tile.setUnitSquare(begin.add(i, j));
                boolMask[i][j] = tile.intersects(objToApproach,false);
            }
        }
    }
}
