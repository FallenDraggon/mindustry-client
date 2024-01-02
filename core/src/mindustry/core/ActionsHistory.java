package mindustry.core;

import arc.ApplicationListener;
import arc.graphics.Color;
import arc.graphics.Colors;
import arc.input.KeyCode;
import arc.scene.ui.Dialog;
import arc.scene.ui.layout.Table;
import arc.struct.Queue;
import arc.struct.Seq;
import arc.util.Align;
import mindustry.game.Teams;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.world.Tile;

import java.time.LocalTime;

import static arc.util.Strings.stripColors;

public class ActionsHistory extends Logic {
    public static Queue<BlockPlayerPlan> blocksplayersplans = new Queue<>();
    public static Queue<ItemPlayerPlan> playeritemsplans = new Queue<>();
    public static Queue<BlockConfigPlayerPlan> blockconfplayersplans = new Queue<>();
    public static Queue<UnitsKilledByPlayers> deathunitsplan = new Queue<>();
    public static Queue<UnitsKilledByControllPlayers> deathunitscontrolplan = new Queue<>();
    public static final Seq<Player> playeratmap = new Seq<>();
    private static Table reslog = new Table();

    public static void clearactionhistory() {
        blocksplayersplans.clear();
        blockconfplayersplans.clear();
        deathunitsplan.clear();
        deathunitscontrolplan.clear();
        playeratmap.clear();
        playeritemsplans.clear();
    }

    public static String cuteNickNames(String name) {
        if (stripColors(name).length() < 16) {
            return name;
        } else {
            for(int i = 0; i < name.length(); i++){
                int ml = 0;
                //parseColorMarkup(name);
                String istr = name.charAt(i) + "";
                if(istr.equals("[")){
                    parseColorMarkupAH(name, i, i+4);
                }
            }
            return "test pidor";
        }
    }


    //Blocks built/destroy by players
    public static class BlockPlayerPlan{
        public final short x, y, rotation, block;
        public final String lastacs;
        public final Object config;
        public boolean wasbreaking;

        public BlockPlayerPlan(int x, int y, short rotation, short block, Object config, String lastacs, boolean wasbreaking){
            this.x = (short)x;
            this.y = (short)y;
            this.rotation = rotation;
            this.block = block;
            this.config = config;
            this.lastacs = lastacs;
            this.wasbreaking = wasbreaking;
        }
    }

    //Blocks touched by players
    public static class BlockConfigPlayerPlan{
        public final short x, y, block;
        public final String lastacs;

        public BlockConfigPlayerPlan(int x, int y, short block, String lastacs){
            this.x = (short)x;
            this.y = (short)y;
            this.block = block;

            this.lastacs = lastacs;
        }
    }

    public static class UnitsKilledByPlayers{
        public Player kplayer;
        public Unit kunit;
        public final float x, y;
        public LocalTime localTime;

        public UnitsKilledByPlayers(Player kplayer, Unit kunit, float x, float y, LocalTime localTime){
            this.kplayer = kplayer;
            this.kunit = kunit;
            this.x = x;
            this.y = y;
            this.localTime = localTime;
        }
    }

    public static class UnitsKilledByControllPlayers{
        public String kplayer;
        public Unit kunit;
        public final float x, y;
        public LocalTime localTime;

        public UnitsKilledByControllPlayers(String kplayer, Unit kunit, float x, float y, LocalTime localTime){
            this.kplayer = kplayer;
            this.kunit = kunit;
            this.x = x;
            this.y = y;
            this.localTime = localTime;
        }
    }
    public static class UnitAtControl{
        public UnitType type;
        public float procX;
        public float procY;
        public int count;
        public UnitAtControl(UnitType type, float procX, float procY, int count){
            this.type = type;
            this.procX = procX;
            this.procY = procY;
            this.count = count;
        }
    }
    public static class ItemPlayerPlan{
        public Player player;
        public Tile tile;
        public Item item;
        public boolean take;
        public LocalTime localTime;
        public ItemPlayerPlan(Player player, Tile tile, Item item, boolean take, LocalTime localTime){
            this.player = player;
            this.tile = tile;
            this.item = item;
            this.take = take;
            this.localTime = localTime;
        }
    }

    private static int parseColorMarkupAH(CharSequence str, int start, int end) {
        if (start >= end) {
            return -1;
        } else {
            int i;
            char ch;
            switch (str.charAt(start)) {
                case '#':
                    for(i = start + 1; i < end; ++i) {
                        ch = str.charAt(i);
                        if (ch == ']') {
                            if (i >= start + 2 && i <= start + 9) {
                                return i - start;
                            }
                            break;
                        }

                        if ((ch < '0' || ch > '9') && (ch < 'a' || ch > 'f') && (ch < 'A' || ch > 'F')) {
                            break;
                        }
                    }

                    return -1;
                case '[':
                    return -2;
                case ']':
                    return 0;
                default:
                    for(i = start + 1; i < end; ++i) {
                        ch = str.charAt(i);
                        if (ch == ']') {
                            Color namedColor = Colors.get(str.subSequence(start, i).toString());
                            if (namedColor == null) {
                                return -1;
                            }

                            return i - start;
                        }
                    }

                    return -1;
            }
        }
    }
}
