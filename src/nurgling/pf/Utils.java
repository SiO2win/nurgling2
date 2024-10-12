package nurgling.pf;

import haven.*;

public class Utils
{
    public static Coord worldToPf(Coord2d coord)
    {
        return coord.div(MCache.tilehsz).round();
    }
    public static Coord2d pfToWorld(Coord coord)
    {
        return coord.mul(MCache.tilehsz);
    }

    public static Coord tileToPf(Coord coord)
    {
        return coord.mul(2).add(1,1);
    }
    public static Coord pfToTile(Coord coord)
    {
        return coord.div(2);
    }

    public static Coord worldToTile(Coord2d coord)
    {
        return coord.div(MCache.tilesz).floor();
    }
    public static Coord2d tileToWorld(Coord coord)
    {
        return coord.mul(MCache.tilesz);
    }
}
