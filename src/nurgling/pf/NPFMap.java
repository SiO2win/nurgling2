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
    // 0 have path
    // 1 hitbox
    // 2 unpathable tiles
    // 4 pf been here
    // 7 approach point (marked blue)
    // 8 pf line
    // 9 pf turn

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

    public enum  CellType {
        NonWater((byte) 0, (byte) -1, -1),
        Bog((byte) 1, (byte) -1, -1),
        Shallow((byte) 2, (byte) -1, -1),
        ShallowOcean((byte) 3, (byte) -1, -1),
        Deep((byte) 4, (byte) -1, -1),
        DeepOcean((byte) 5, (byte) -1, -1),
        OpenSea((byte) 6, (byte) -1, -1),

        Placeable((byte) -1, (byte) 0, -1),
        Passable((byte) -1, (byte) 1, -1),
        Blocked((byte) -1, (byte) 2, -1),
        Forbidden((byte) -1, (byte) 3, -1),

        UnknownFloor((byte) -1, (byte) -1, 1),
        PavedFloor((byte) -1, (byte) -1, 1),
        GrassFloor((byte) -1, (byte) -1, 0.8),
        ForestFloor((byte) -1, (byte) -1, 0.6),
        SubmergedFloor((byte) -1, (byte) -1, 0.2),

        Default((byte) 0, (byte) 0, 1);

        CellType(byte land, byte obstruct, double moveSpeed) {
            landType = land;
            obstructionState = obstruct;
            speedK = moveSpeed;
        }

        void cumulate(CellType other) {
            if (other.landType != -1) {
                if (landType < other.landType)
                    landType = other.landType;
                if (other.landType > 0)
                    includeWater = true;
                if (other.landType == 0)
                    includeLand = true;
                caLayed = true;
            }
            if (other.obstructionState != -1) {
                if (obstructionState < other.obstructionState)
                    obstructionState = other.obstructionState;
                caLayed = true;
            }
            if (other.speedK != -1) {
                if (speedK > other.speedK)
                    speedK = other.speedK;
                caLayed = true;
            }
        }
        void force(CellType other) {
            if (other.landType != -1)
                landType = other.landType;
            if (other.obstructionState != -1)
                obstructionState = other.obstructionState;
            if (other.speedK != -1)
                speedK = other.speedK;
        }

        private boolean includeWater = false;
        private boolean includeLand = false;
        private boolean pfVisited = false;
        private boolean caLayed = false;
        private byte landType;
        //0 walkable
        //1 bog
        //2 shallow
        //3 shallow ocean
        //4 deep
        //5 deep ocean
        //6 open sea

        private byte obstructionState;
        //0 passable & placeable
        //1 non-placeable
        //2 blocked by hit-box
        //3 blocked by immovable object

        private double speedK;
        public short pfColour = -1;
        //0 traversable
        //1


        public boolean isPfVisited() {
            return pfVisited;
        }
        public boolean isCALayed() {
            return caLayed;
        }

        public void pfVisit() {
            this.pfVisited = true;
        }

        public boolean isPlace_able() {
            return ((landType == 0) && (obstructionState < 1) && !includeWater);
        }

        public boolean isWalk_able() {
            return ((landType <= 3) && (obstructionState < 2));
        }

        public boolean isSwim_able() {
            return ((landType >= 1) && (landType <= 6) && (obstructionState < 2));
        }

        public boolean isSail_able() {
            return ((landType >= 2) && (landType <= 6) && (obstructionState < 2) && !includeLand);
        }

        public boolean isDangerWaters() {
            return (landType == 6);
        }

        public boolean isBlockedByMajorObject() {
            return (obstructionState == 3);
        }

        public boolean isBlockedByHitbox() {
            return (obstructionState == 2);
        }

        public boolean isBlocked() {
            return (obstructionState == 3) || (obstructionState == 2);
        }

        public double getMoveSpeed() {
            return speedK;
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
        public CellType fullVal = CellType.Default;

        public ArrayList<Long> content = new ArrayList<>();
    }



    public NPFMap(Coord2d src, Coord2d tgt, double mul) throws InterruptedException {
        Coord2d a = new Coord2d(Math.min(src.x, tgt.x), Math.min(src.y, tgt.y));
        Coord2d b = new Coord2d(Math.max(src.x, tgt.x), Math.max(src.y, tgt.y));
        Coord center = Utils.toPfGrid((a.add(b)).div(2));
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
                    cells[i][j].fullVal.force(CellType.Forbidden);
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

    public void build() {
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

        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++)
                if (!cells[i][j].fullVal.isCALayed()) {


                    ArrayList<Coord> cand = new ArrayList<>();
                    cand.add((Utils.pfGridToWorld(cells[i][j].pos).add(new Coord2d(-MCache.tileqsz.x, MCache.tileqsz.y))).div(MCache.tilesz).floor());
                    cand.add((Utils.pfGridToWorld(cells[i][j].pos).add(new Coord2d(MCache.tileqsz.x, -MCache.tileqsz.y))).div(MCache.tilesz).floor());
                    cand.add((Utils.pfGridToWorld(cells[i][j].pos).add(new Coord2d(-MCache.tileqsz.x, -MCache.tileqsz.y))).div(MCache.tilesz).floor());
                    cand.add((Utils.pfGridToWorld(cells[i][j].pos).add(new Coord2d(MCache.tileqsz.x, MCache.tileqsz.y))).div(MCache.tilesz).floor());

                    for (Coord c : cand) {
                        String name = NUtils.getGameUI().ui.sess.glob.map.tilesetname(NUtils.getGameUI().ui.sess.glob.map.gettile(c));
                        if (!waterMode) {
                            if (name != null && (name.startsWith("gfx/tiles/cave") ||
                                    name.startsWith("gfx/tiles/rocks") ||
                                    name.equals("gfx/tiles/deep") ||
                                    name.equals("gfx/tiles/odeep"))) {
                                cells[i][j].val = 2;
                            }
                        } else {
                            if (name != null && !(name.startsWith("gfx/tiles/water") ||
                                    name.startsWith("gfx/tiles/owater") ||
                                    name.equals("gfx/tiles/deep") ||
                                    name.equals("gfx/tiles/odeep"))) {
                                cells[i][j].val = 2;
                            }
                        }
                    }
                }

    }

    public ArrayList<Coord> checkCA(CellsArray ca) {
        ArrayList<Coord> result = new ArrayList<>();
        if ((ca.begin.x >= begin.x && ca.begin.x <= end.x ||
                ca.end.x >= begin.x && ca.end.x <= end.x) &&
                (ca.begin.y >= begin.y && ca.begin.y <= end.y ||
                        ca.end.y >= begin.y && ca.end.y <= end.y))
        {
            for (int i = 0; i < ca.x_len; i++)
                for (int j = 0; j < ca.y_len; j++)
                {
                    int ii = i + ca.begin.x - begin.x;
                    int jj = j + ca.begin.y - begin.y;
                    if (ii > 0 && ii < size && jj > 0 && jj < size)
                    {
                        if(ca.boolMask[i][j] && cells[ii][jj].val !=0)
                        {
                            result.add(new Coord(ii,jj));
                        }
                    }
                }
        }
        return result;
    }

    public static Window wnd = null;
    public static void print(int size, Cell[][] cells)
    {
        if(NUtils.getUI().core.debug)
        {
            Coord csz = new Coord(UI.scale(10), UI.scale(10));
            if(wnd!=null)
                wnd.destroy();
            wnd = NUtils.getUI().root.add(new Window(new Coord(size * UI.scale(10), size * UI.scale(10)), "PFMAP")
            {
                @Override
                public void draw(GOut g)
                {
                    super.draw(g);
                    for (int i = 0; i < size; i++)
                    {
                        for (int j = size - 1; j >= 0; j--)
                        {
                            if (cells[i][j].val == 1) {
                                g.chcolor(Color.RED);
                                g.frect(new Coord(i * UI.scale(10), j * UI.scale(10)).add(deco.contarea().ul), csz);
                                continue;
                            }
                            else if (cells[i][j].val == 0)
                                g.chcolor(Color.GREEN);
                            else if (cells[i][j].val == 4)
                            {
                                g.chcolor(Color.YELLOW);
                                g.frect(new Coord(i * UI.scale(10), j * UI.scale(10)).add(deco.contarea().ul), csz);
                                continue;
                            }
                            else if (cells[i][j].val == 7)
                                g.chcolor(Color.BLUE);
                            else if (cells[i][j].val == 8)
                            {
                                g.chcolor(Color.MAGENTA);
                                g.frect(new Coord(i * UI.scale(10), j * UI.scale(10)).add(deco.contarea().ul), csz);
                                continue;
                            }
                            else if (cells[i][j].val == 9)
                            {
                                g.chcolor(Color.CYAN);
                                g.frect(new Coord(i * UI.scale(10), j * UI.scale(10)).add(deco.contarea().ul), csz);
                                continue;
                            }
                            else
                                g.chcolor(Color.BLACK);
                            g.rect(new Coord(i * UI.scale(10), j * UI.scale(10)).add(deco.contarea().ul), csz);
                        }
                    }
                }

                public void wdgmsg(Widget sender, String msg, Object... args)
                {
                    if ((sender == this) && (msg == "close"))
                    {
                        destroy();
                    }
                    else
                    {
                        super.wdgmsg(sender, msg, args);
                    }
                }

            }, new Coord(UI.scale(100), UI.scale(100)));
            NUtils.getUI().bind(wnd, 7002);
        }
    }
}
