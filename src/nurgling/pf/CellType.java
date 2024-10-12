package nurgling.pf;

import java.awt.*;

public class CellType {
    public static final CellType NonWater = new CellType((byte) 0, (byte) -1, -1);
    public static final CellType Bog = new CellType((byte) 1, (byte) -1, -1);
    public static final CellType Shallow = new CellType((byte) 2, (byte) -1, -1);
    public static final CellType ShallowOcean = new CellType((byte) 3, (byte) -1, -1);
    public static final CellType Deep = new CellType((byte) 4, (byte) -1, -1);
    public static final CellType DeepOcean = new CellType((byte) 5, (byte) -1, -1);
    public static final CellType OpenSea = new CellType((byte) 6, (byte) -1, -1);

    public static final CellType Placeable = new CellType((byte) -1, (byte) 0, -1);
    public static final CellType Passable = new CellType((byte) -1, (byte) 1, -1);
    public static final CellType Blocked = new CellType((byte) -1, (byte) 2, -1);
    public static final CellType Forbidden = new CellType((byte) -1, (byte) 3, -1);

    public static final CellType UnknownFloor = new CellType((byte) -1, (byte) -1, 1);
    public static final CellType PavedFloor = new CellType((byte) -1, (byte) -1, 1);
    public static final CellType GrassFloor = new CellType((byte) -1, (byte) -1, 0.8);
    public static final CellType ForestFloor = new CellType((byte) -1, (byte) -1, 0.6);
    public static final CellType SubmergedFloor = new CellType((byte) -1, (byte) -1, 0.2);

    public static final CellType Default = new CellType();

    CellType() {
        landType = 0;
        obstructionState = 0;
        speedK = 1;
        includeWater = false;
        includeLand = false;
        pfVisited = false;
        caLayed = false;
    }

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

        if (other.obstructionState != -1) {
            obstructionState = other.obstructionState;
            //overlayColor = obstructionState;
        }
        if (other.speedK != -1)
            speedK = other.speedK;
    }

    void copy(CellType other) {
        landType = other.landType;
        obstructionState = other.obstructionState;
        speedK = other.speedK;
        includeWater = other.includeWater;
        includeLand = other.includeLand;
        pfVisited = other.pfVisited;
        caLayed = other.caLayed;
    }

//        private byte overlayColor=-1;
//        private byte landColor=-1;

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

    public Color getClr() {
        switch (obstructionState) {
            case 2:
                return Color.RED;
            case 3:
                return Color.BLACK;
        }
        switch (landType) {
            case -1:
                return Color.WHITE;
            case 1:
                return new Color(111, 83, 11);
            case 2:
                return new Color(60, 88, 227);
            case 3:
                return new Color(102, 174, 244);
            case 4:
                return new Color(20, 53, 170);
            case 5:
                return new Color(21, 116, 205);
            case 6:
                return new Color(24, 62, 85);
        }
        switch (obstructionState) {
            case 0:
                return Color.GREEN.brighter();
            case 1:
                return Color.GREEN;
            default:
                return Color.WHITE;
        }
    }

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
