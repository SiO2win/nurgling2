package nurgling.actions;

import haven.*;
import nurgling.*;
import nurgling.pf.*;
import nurgling.pf.Utils;
import nurgling.tools.Finder;

import java.util.*;
import java.util.concurrent.atomic.*;

import static nurgling.pf.Graph.getPath;

public class PathFinder implements Action
{
    public static double pfmdelta = 0.1;
    NPFMap pfmap = null;
    Coord start_pos = null;
    Coord end_pos = null;
    ArrayList<Coord> end_poses = null;
    public boolean isHardMode = false;
    public boolean waterMode = false;
    Coord2d begin;
    Coord2d end;

    public PathFinder(Coord2d begin, Coord2d end)
    {
        this.begin = begin;
        this.end = end;
    }
    public PathFinder(Coord2d end)
    {
        this(NUtils.getGameUI().map.player().rc, end);
    }

    public PathFinder(Gob target)
    {
        this(target.rc);
        target_id = target.id;
    }


    long target_id = -2;
    private boolean fixStartEnd(boolean test)
    {
        NPFMap.Cell[][] cells = pfmap.getCells();
        if (cells[start_pos.x][start_pos.y].val != 0) {
            if (target_id >=0 && cells[start_pos.x][start_pos.y].content.contains(target_id) && !test)
                return false;
            ArrayList<Coord> st_poses = findFreeNear(start_pos,true);
            if(st_poses.isEmpty())
                return false;
            start_pos = st_poses.get(0);
        }
        if(start_pos.equals(end_pos)) {
            dn = true;
            return false;
        }
//        cells[start_pos.x][start_pos.y].val = 7;
        if (cells[end_pos.x][end_pos.y].val != 0)
        {
            end_poses = findFreeNear(end_pos,false);
            for (Coord coord : end_poses)
            {
                if(start_pos.equals(coord) && target_id >=0)
                    return false;
                cells[coord.x][coord.y].val = 7;
            }

        }
        else
        {
            cells[end_pos.x][end_pos.y].val = 7;
        }
        return true;
    }


    public static Comparator<Coord> c_comp = new Comparator<Coord>() {
        @Override
        public int compare(Coord o1, Coord o2) {
            return Double.compare(Utils.pfGridToWorld(o1).dist(NUtils.getGameUI().map.player().rc),Utils.pfGridToWorld(o2).dist(NUtils.getGameUI().map.player().rc));
        }
    };

    private ArrayList<Coord> findFreeNear(Coord pos, boolean start)
    {
        if(!start) {
            if (target_id != -2) {
                Gob target = dummy;
                if (target == null) {
                    target = Finder.findGob(target_id);
                }

                CellsArray ca = target.ngob.getCA();
                return findFreeNearByHB(ca, target_id, dummy, start);
            }
        }
        else
        {
            if(!pfmap.cells[pos.x][pos.y].content.isEmpty()) {
                Gob gob = null;
                int i = 0;
                while(gob!=null && pfmap.cells[pos.x][pos.y].content.size()>i) {
                    Gob cand = Finder.findGob(pfmap.cells[pos.x][pos.y].content.get(i));
                    gob = cand != null ? cand : gob;
                }
                if (gob != null && gob.ngob != null) {
                    CellsArray ca = gob.ngob.getCA();
                    return findFreeNearByHB(ca, target_id, dummy, start);
                }
            }
        }
        return new ArrayList<>(Arrays.asList(pos));
    }

    private void checkAndAdd(Coord pos, ArrayList<Coord> coords, AtomicBoolean check)
    {
        if (pfmap.getCells()[pos.x][pos.y].val == 0)
        {
            pfmap.getCells()[pos.x][pos.y].val = 7;
            coords.add(pos);
        }
        else if (target_id!=-2 && check!=null)
        {
            if(!pfmap.getCells()[pos.x][pos.y].content.contains(target_id))
                check.set(false);
        }
    }


    @Override
    public Results run(NGameUI gui) throws InterruptedException
    {
        while(true)
        {
            LinkedList<Graph.Vertex> path = construct();

            if (path != null) {
                boolean needRestart = false;
                    for (Graph.Vertex vert : path) {
                        Coord2d targetCoord = Utils.pfGridToWorld(vert.pos);
                        if( vert == path.getFirst())
                        {
                            Coord2d playerrc = NUtils.player ().rc;
                            double dx, dy;
                            if (Math.min (dx = Math.abs (targetCoord.x - playerrc.x), dy = Math.abs (targetCoord.y - playerrc.y)) < MCache.tilehsz.x)

                                if (dx < dy)
                                {
                                    targetCoord.x = playerrc.x;
                                }
                                else
                                {
                                    targetCoord.y = playerrc.y;
                                }
                        }

                        if(target_id==-1 && vert == path.getLast())
                        {
                            if(Math.abs(targetCoord.x-end.x)<Math.abs(targetCoord.y-end.y))
                            {
                                targetCoord.x = end.x;
                            }
                            else
                            {
                                targetCoord.y = end.y;
                            }
                        }
                        else if (vert == path.getLast()) {
                            if (targetCoord.dist(end) < MCache.tilehsz.x)
                                targetCoord = end;
                        }

                        if (!(new GoTo(targetCoord).run(gui)).IsSuccess()) {
                            this.begin = gui.map.player().rc;
                            needRestart = true;
                            break;
                        }
                    }
                    if (!needRestart)
                        return Results.SUCCESS();
            }
            else
            {
                if(dn)
                {
//                    if(start_pos == end_poses.get(0) && NUtils.player().rc.dist(Utils.pfGridToWorld(pfmap.cells[start_pos]))
                    return Results.SUCCESS();
                }
                return
                    Results.ERROR("Can't find path");

            }
        }
    }

    public boolean isDynamic = false;

    public LinkedList<Graph.Vertex> construct() throws InterruptedException
    {
        return construct(false);
    }

    public LinkedList<Graph.Vertex> construct(boolean test) throws InterruptedException
    {
        LinkedList<Graph.Vertex> path = new LinkedList<>();
        int mul = 1;
        while (path.size() == 0 && mul < 1000)
        {
            pfmap = new NPFMap(begin, end, mul);

            pfmap.waterMode = waterMode;
            pfmap.build();
            CellsArray dca = null;
            if(dummy!=null)
                dca = pfmap.addGob(dummy);

            start_pos = Utils.toPfGrid(begin).sub(pfmap.getBegin());
            end_pos = Utils.toPfGrid(end).sub(pfmap.getBegin());
            // Находим свободные начальные и конечные точки

            if(!fixStartEnd(test)) {
                dn = true;
                return null;
            }

            if(dca!=null)
                pfmap.setCellArray(dca);
            NPFMap.print(pfmap.getSize(), pfmap.getCells());
            Graph res = null;
            if (pfmap.getCells()[end_pos.x][end_pos.y].val == 7)
            {
                Thread th = new Thread(res = new Graph(pfmap, start_pos, end_pos));
                th.start();
                th.join();
            }
            else
            {
                LinkedList<Graph> graphs = new LinkedList<>();
                for (Coord ep : end_poses)
                {
                    graphs.add(new Graph(pfmap, start_pos, ep));
                }
                LinkedList<Thread> threads = new LinkedList<>();
                for (Graph graph : graphs)
                {
                    Thread th;
                    threads.add(th = new Thread(graph));
                    th.start();
                }
                for (Thread t : threads)
                {
                    t.join();
                }

                graphs.sort(new Comparator<Graph>()
                {
                    @Override
                    public int compare(Graph o1, Graph o2)
                    {
                        return (Integer.compare(o1.getPathLen(), o2.getPathLen()));
                    }
                });
                if (!graphs.isEmpty())
                    res = graphs.get(0);
//                for (Graph g: graphs)
//                {
//                    NPFMap.print(pfmap.getSize(), g.getVert());
//                }
            }

            if (res != null)
            {
                if(!isDynamic)
                    path = getPath(pfmap, res.path);
                else
                    path = res.path;
                NPFMap.print(pfmap.getSize(), res.getVert());
                if (!path.isEmpty()) {
                    return path;
                }
            }
            mul++;
        }
        return null;
    }


    public static boolean isAvailable(Gob target) throws InterruptedException
    {
        PathFinder pf = new PathFinder(target);
        LinkedList<Graph.Vertex> res = pf.construct();
        return res!=null || pf.dn;
    }

    public static boolean isAvailable(Gob target, boolean hardMode) throws InterruptedException
    {
        PathFinder pf = new PathFinder(target);
        pf.isHardMode = true;
        return pf.construct(true)!=null;
    }

    public PathFinder(Gob dummy, boolean virtual) {
        this(dummy);
        this.dummy = dummy;
        assert virtual;
    }


    private ArrayList<Coord> findFreeNearByHB(CellsArray ca, long target_id, Gob dummy, boolean isStart) {
        ArrayList<Coord> res = new ArrayList<>();
        if (ca != null) {
            for (int i = 0; i < ca.x_len; i++)
                for (int j = 0; j < ca.y_len; j++) {
                    int ii = i + ca.begin.x - pfmap.begin.x;
                    int jj = j + ca.begin.y - pfmap.begin.y;
                    Coord npfpos = new Coord(ii, jj);
                    if (ii > 0 && ii < pfmap.size && jj > 0 && jj < pfmap.size) {
                        if (ca.cells[i][j] != 0) {
                            for (int d = 0; d < 4; d++) {
                                Coord test_coord = npfpos.add(Coord.uecw[d]);
                                if(test_coord.x<pfmap.size && test_coord.x>=0 && test_coord.y<pfmap.size && test_coord.y>=0)
                                if (pfmap.cells[test_coord.x][test_coord.y].val == 0 || pfmap.cells[test_coord.x][test_coord.y].val == 7) {
                                    if(isStart || pfmap.cells[npfpos.x][npfpos.y].content.size()==1 ) {
                                        pfmap.getCells()[test_coord.x][test_coord.y].val = 7;
                                        res.add(test_coord);
                                    }
                                    else if (pfmap.cells[npfpos.x][npfpos.y].content.size()>1)
                                    {
                                        Coord2d test2d_coord = Utils.pfGridToWorld(pfmap.cells[test_coord.x][test_coord.y].pos);
                                        double dst = 9000, testdst;
                                        long res_id = -2;
                                        for(long id : pfmap.cells[npfpos.x][npfpos.y].content)
                                        {
                                            if(id>=0)
                                            {
                                                if ((testdst = Finder.findGob(id).rc.dist(test2d_coord))<dst) {
                                                    res_id = id;
                                                    dst = testdst;
                                                }
                                            }
                                            else
                                            {
                                                if ((testdst = dummy.rc.dist(test2d_coord))<dst) {
                                                    res_id = id;
                                                    dst = testdst;
                                                }
                                            }
                                            if(res_id == target_id) {
                                                pfmap.getCells()[test_coord.x][test_coord.y].val = 7;
                                                res.add(test_coord);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        }

        Coord2d player = NUtils.player().rc;
        Coord2d targerc = (dummy == null) ? Finder.findGob(target_id).rc:dummy.rc;
        Coord2d playerdir = player.sub(targerc);

        Comparator comp =  new Comparator<Coord>()
        {
            @Override
            public int compare(Coord o1, Coord o2) {
                Coord2d t01 = Utils.pfGridToWorld(pfmap.cells[o1.x][o1.y].pos).sub(targerc);
                Coord2d t02 = Utils.pfGridToWorld(pfmap.cells[o2.x][o2.y].pos).sub(targerc);

                return Double.compare(Math.acos(t01.dot(playerdir)/(playerdir.len()*t01.len())),Math.acos(t02.dot(playerdir)/(playerdir.len()*t02.len())));
            }
        };
        res.sort(comp);
        return res;
    }

    Gob dummy;

    boolean dn = false;

    boolean getDNStatus()
    {
        return dn;
    }
}
