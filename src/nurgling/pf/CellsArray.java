package nurgling.pf;

import haven.*;
import nurgling.*;

public class CellsArray {
    public Coord begin;
    public Coord end;
    public boolean[][] boolMask;

    public int x_len;
    public int y_len;

    public CellsArray copy(){
        //TODO implement full copy
        CellsArray theCopy = new CellsArray(x_len, y_len);
        return theCopy;
    }

    public CellsArray copy(Coord clipUL,Coord clipBR){
        //TODO implement clipped copy, null included
        CellsArray theCopy = new CellsArray(x_len, y_len);
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
