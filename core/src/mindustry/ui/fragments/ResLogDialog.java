package mindustry.ui.fragments;

import arc.Core;

import arc.scene.Group;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.util.*;
import mindustry.core.ActionsHistory;
import mindustry.gen.*;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustry.world.Tile;

import static mindustry.Vars.*;

public class ResLogDialog {
    public static Table content = new Table().marginRight(13f).marginLeft(13f);
    private boolean visible = false;
    private final Interval timer = new Interval();
    //public static Tile lastreslogtile = new Tile(0, 0);
    public static Tile lastreslogtile;

    public void build(Group parent){
        content.name = "Res log";

        parent.fill(cont -> {
            cont.name = "Res item log";
            cont.visible(() -> visible);
            cont.update(() -> {
                if(!state.isGame()){
                    visible = false;
                    return;
                }
                if(visible && timer.get(60)){
                    rebuild();
                }
            });

            cont.table(Tex.buttonTrans, pane -> {
                pane.label( () -> "Resources logs for: " + ( (lastreslogtile != null) ? (lastreslogtile.x + "/" + lastreslogtile.y) : ""));
                //if(lastreslogtile != null) { pane.label(() -> lastreslogtile.x + "/" + lastreslogtile.y); }
                pane.row();
                pane.pane(content).grow().scrollX(false);
                pane.row();

                pane.table(menu -> {
                    menu.defaults().pad(5).growX().height(50f).fillY();
                    menu.name = "menu";
                    menu.button("@close", this::toggle).get().getLabel().setWrap(false);
                }).margin(0f).pad(10f).growX();

            }).touchable(Touchable.enabled).margin(14f).minWidth(1100f);
        });
    }

    public static void rebuild(){
        content.clear();
        //Queue<ActionsHistory.ItemPlayerPlan> tempreslog = ActionsHistory.playeritemsplans;
        //ActionsHistory.playeritemsplans.forEach( t -> {if(t.tile.build == lastreslogtile.build) tempreslog.add(t);});
        if(ActionsHistory.playeritemsplans == null) return;

        for(ActionsHistory.ItemPlayerPlan itlog : ActionsHistory.playeritemsplans){
            if (itlog.tile.build == null) continue;
            //player.sendMessage(lastreslogtile + " / / / /" + itlog.tile);
            if((lastreslogtile.build.getX() == itlog.tile.build.getX()) && (lastreslogtile.build.getY() == itlog.tile.build.getY())) {
                Table button = new Table();
                button.left();
                button.margin(5).marginBottom(10);
                Table table = new Table(){ };
                table.margin(8);

                table.name = itlog.tile.build.block.localizedName;

                button.add(table).size(70);

                String texttime = itlog.localTime.getHour() + ":";
                String texttimemin = itlog.localTime.getMinute() + ":";
                if(texttimemin.length() == 2) { texttimemin = "0" + texttimemin;}
                String texttimesec = itlog.localTime.getSecond() + "";
                if(texttimesec.length() == 1) { texttimesec = "0" + texttimesec;}
                texttime = texttime + texttimemin + texttimesec;

                button.button("[" + texttime +  "] - " + itlog.player.name + "[white]: " + (itlog.take ? "withdraw" : "deposite") + " " + ( Core.input.shift() ? itlog.item.localizedName :  Fonts.getUnicodeStr(itlog.item.name))+ (itlog.take ? " from " : " to ")
                                + ( Core.input.shift() ? itlog.tile.build.block.localizedName : Fonts.getUnicodeStr(itlog.tile.build.block.name) ),
                        Styles.nonetdef, () -> Core.app.setClipboardText(String.valueOf(itlog.player.name))).wrap().fillX().growY().pad(10);

                content.add(button).maxHeight(70);
                content.row();
            }
        }
        content.marginBottom(5);
    }


    public void toggle(){
        visible = !visible;
        if(visible){
            rebuild();
        }else{
            Core.scene.setKeyboardFocus(null);
        }
    }

    public boolean shown(){
        return visible;
    }

}
