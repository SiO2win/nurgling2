package nurgling.pf;

import haven.*;
import haven.Window;
import nurgling.*;

import java.awt.*;
import java.util.*;


public class NPFMap
{
    public boolean waterMode = false;
    public Cell[][] cells;

    private final Coord begin;
    private final Coord end;
    public int dsize;
    public int size;
    long currentTransport = -1;

    private Cell[][] stash_cells;
    private Coord stash_begin;
    private Coord stash_size;

    public void addGob(Gob gob) {
        CellsArray ca;
        if (gob.ngob != null && gob.ngob.hitBox != null && (ca = gob.ngob.getCA()) != null && NUtils.player() != null && gob.id != NUtils.player().id && gob.getattr(Following.class) == null) {
            int ulx = Math.max(Math.min(ca.begin.x, ca.end.x), begin.x) - begin.x;
            int uly = Math.max(Math.min(ca.begin.y, ca.end.y), begin.y) - begin.y;
            int szx = Math.min(Math.max(ca.begin.x, ca.end.x), end.x) - begin.x - ulx + 1;
            int szy = Math.min(Math.max(ca.begin.y, ca.end.y), end.y) - begin.y - uly + 1;
            for (int i = 0; i < szx; i++)
                for (int j = 0; j < szy; j++)
                    if (ca.boolMask[i][j]) {
                        cells[i + ulx][j + uly].fullVal.cumulate(CellType.Blocked);
                        cells[i + ulx][j + uly].content.add(gob.id);
                    }
        }
    }

    public void push_segment(Coord ul, Coord br) {
        stash_begin = Coord.of(Math.max(Math.min(ul.x, br.x), begin.x) - begin.x, Math.max(Math.min(ul.y, br.y), begin.y) - begin.y);
        stash_size = Coord.of(Math.min(Math.max(ul.x, br.x), end.x) - begin.x + 1, Math.min(Math.max(ul.y, br.y), end.y) - begin.y + 1).sub(stash_begin);
        if ((stash_size.x < 0) || (stash_size.y < 0))
            stash_begin = null;
        else {
            stash_cells = new Cell[stash_size.x][stash_size.y];
            for (int ix = stash_begin.x; ix < stash_size.x; ix++)
                System.arraycopy(cells[ix], stash_begin.y, stash_cells[ix - stash_begin.x], 0, stash_size.y);
        }
    }

    public void pull_segment() {
        if (stash_begin != null) {
            for (int ix = stash_begin.x; ix < stash_size.x; ix++)
                System.arraycopy(stash_cells[ix - stash_begin.x], 0, cells[ix], stash_begin.y, stash_size.y);
            stash_begin = null;
        }
    }



    public static class Cell
    {
        public Cell(Coord pos)
        {
            this.pos = pos;
        }

        public Coord pos;

        public short val;
        // 0 have path
        // 1 hitbox
        // 2 unpathable tiles
        // 4 pf been here
        // 7 approach point (marked blue)
        // 8 pf line
        // 9 pf turn
        public CellType fullVal = new CellType();

        public ArrayList<Long> content = new ArrayList<>();
    }



    public NPFMap(Coord2d src, Coord2d tgt, double mul) throws InterruptedException {
        Coord2d a = new Coord2d(Math.min(src.x, tgt.x), Math.min(src.y, tgt.y));
        Coord2d b = new Coord2d(Math.max(src.x, tgt.x), Math.max(src.y, tgt.y));
        Coord center = Utils.worldToPf((a.add(b)).div(2));
        dsize = Math.max(8, (int) Math.ceil(b.dist(a) * mul / MCache.tilehsz.x));
        size = 2 * dsize + 1;
        if (dsize > 160) {
            NUtils.getGameUI().error("Unable to build grid of required size");
            throw new InterruptedException();
        }

        cells = new Cell[size][size];
        begin = center.sub(dsize, dsize);
        end = center.add(dsize, dsize);
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++) {
                cells[i][j] = new Cell(begin.add(i, j));
                if (i == 0 || j == 0 || i == (size - 1) || j == (size - 1))
                    cells[i][j].fullVal.cumulate(CellType.Forbidden);
            }
    }

    public final Coord getBegin()
    {
        return begin;
    }

    public final Coord getEnd()
    {
        return end;
    }

    public final Cell[][] getCells()
    {
        return cells;
    }

    public final int getSize()
    {
        return size;
    }

    public void build(PFRegime rg) {
        //TODO implement rg usage
        if (NUtils.playerID() != -1) {
            Following fl = NUtils.player().getattr(Following.class);
            if (fl != null)
                currentTransport = fl.tgt;
        }
        synchronized (NUtils.getGameUI().ui.sess.glob.oc) {
            for (Gob gob : NUtils.getGameUI().ui.sess.glob.oc)
                if (gob.id != currentTransport)
                    addGob(gob);
        }

        Coord start_tile = Utils.pfToTile(cells[0][0].pos);
        Coord end_tile = Utils.pfToTile(cells[size - 1][size - 1].pos);
        Coord tile_cursor = new Coord();

        for ( tile_cursor.x = start_tile.x; tile_cursor.x <= end_tile.x; tile_cursor.x++)
            for ( tile_cursor.y = start_tile.y; tile_cursor.y <= end_tile.y; tile_cursor.y++) {
                String name = NUtils.getGameUI().ui.sess.glob.map.tilesetname(NUtils.getGameUI().ui.sess.glob.map.gettile(tile_cursor));
                CellType type;

                if (name != null) {
                    if (name.endsWith("/cave"))
                        type = CellType.Forbidden;
                    else if (name.endsWith("/rocks"))
                        type = CellType.Forbidden;
                    else if (name.equals("gfx/tiles/lava"))
                        type = CellType.Forbidden;
                    else if (name.endsWith("/deep"))
                        type = CellType.Deep;
                    else if (name.endsWith("/odeep"))
                        type = CellType.DeepOcean;
                    else if (name.endsWith("/water"))
                        type = CellType.Shallow;
                    else if (name.endsWith("/owater"))
                        type = CellType.ShallowOcean;
                    else
                        type = null;



                    if (type != null) {
                        Coord tileCenter = Utils.tileToPf(tile_cursor);
                        Coord fpTilePart;
                        for (int di = -1; di <= 1; di++)
                            for (int dj = -1; dj <= 1; dj++) {
                                fpTilePart = tileCenter.add(di, dj);
                                if (begin.lessEqThen( fpTilePart)  &&  fpTilePart.lessEqThen(end)) {
                                    cells[fpTilePart.x - begin.x][fpTilePart.y - begin.y].fullVal.cumulate(type);
                                }
                            }
                    }
                }
            }
    }

    public ArrayList<Coord> checkCA(CellsArray ca) {
        ArrayList<Coord> result = new ArrayList<>();
        //TODO update later for water PF system
        int ulx = Math.max(ca.begin.x, begin.x) - begin.x;
        int uly = Math.max(ca.begin.y, begin.y) - begin.y;
        int szx = Math.min(ca.begin.x, end.x) - begin.x - ulx + 1;
        int szy = Math.min(ca.begin.y, end.y) - begin.y - uly + 1;
        for (int i = 0; i < szx; i++)
            for (int j = 0; j < szy; j++)
                if (ca.boolMask[i][j] && !(cells[i + ulx][j + uly].fullVal.isBlocked()))
                    result.add(new Coord(i + ulx, j + uly));
        return result;
    }

    public static Window wnd = null;
    public static void print(int size, Cell[][] cells) {
        if (NUtils.getUI().core.debug) {
            Coord csz = new Coord(UI.scale(10), UI.scale(10));
            if (wnd != null)
                wnd.destroy();
            wnd = NUtils.getUI().root.add(new Window(new Coord(size * UI.scale(10), size * UI.scale(10)), "PFMAP") {
                @Override
                public void draw(GOut g) {
                    super.draw(g);
                    for (int i = 0; i < size; i++)
                        for (int j = size - 1; j >= 0; j--) {
                            Color clr = cells[i][j].fullVal.getClr();
                            g.chcolor(clr);
                            if (clr.equals(Color.RED) ||
                                    clr.equals(Color.YELLOW) ||
                                    clr.equals(Color.MAGENTA) ||
                                    clr.equals(Color.CYAN) ||
                                    clr.equals(Color.BLACK))
                                g.frect(new Coord(i * UI.scale(10), j * UI.scale(10)).add(deco.contarea().ul), csz);
                            else
                                g.rect(new Coord(i * UI.scale(10), j * UI.scale(10)).add(deco.contarea().ul), csz);
                        }

                }

                public void wdgmsg(Widget sender, String msg, Object... args) {
                    if ((sender == this) && (msg == "close"))
                        destroy();
                    else
                        super.wdgmsg(sender, msg, args);
                }
            }, new Coord(UI.scale(100), UI.scale(100)));
            NUtils.getUI().bind(wnd, 7002);
        }
    }
}
