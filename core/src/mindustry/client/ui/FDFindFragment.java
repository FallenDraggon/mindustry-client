package mindustry.client.ui;

import arc.*;
import arc.input.*;
import arc.math.Mathf;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.core.NetClient;
import mindustry.gen.*;
import mindustry.world.blocks.logic.LogicBlock;
import static mindustry.Vars.*;

public class FDFindFragment {
    public Table content = new Table().marginRight(13f).marginLeft(13f);
    private boolean visible = false;
    private final Interval timer = new Interval();
    private TextField search;
    public void build(Group parent){
        content.name = "findhbase";

        parent.fill(cont -> {
            cont.name = "searchcode";
            cont.visible(() -> visible);
            cont.update(() -> {
                if(!state.isGame()){
                    visible = false;
                    return;
                }

                if(visible && timer.get(60) && !Core.input.keyDown(KeyCode.mouseLeft) && !(Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true) instanceof Image || Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true) instanceof ImageButton)){
                    rebuild();
                }
            });

            cont.table(Tex.buttonTrans, pane -> {
                search = pane.field(null, text -> rebuild()).grow().pad(8).name("search").maxTextLength(maxNameLength).get();
                search.setMessageText(Core.bundle.get("players.search"));

                pane.row();
                pane.pane(content).grow().scrollX(false);
                pane.row();

                pane.table(menu -> {
                    menu.defaults().pad(5).growX().height(50f).fillY();
                    menu.name = "menu";
                    menu.button("@close", this::toggle).get().getLabel().setWrap(false);
                }).margin(0f).pad(10f).growX();
            }).touchable(Touchable.enabled).margin(14f).minWidth(500f);
        });

        rebuild();
    }

    public void rebuild(){
        content.clear();
        content.button("run", this::findcode);
        content.marginBottom(5);
    }

    public void toggle(){
        visible = !visible;
        if(visible){
            rebuild();
            Core.scene.setKeyboardFocus(search);
        }else{
            Core.scene.setKeyboardFocus(null);
            search.clearText();
        }
    }

    public boolean shown(){
        return visible;
    }
    private void findcode() {
        String itsforme = "";
        for(Building bui : Groups.build){
            if(bui instanceof LogicBlock.LogicBuild){
                if(((LogicBlock.LogicBuild) bui).code.contains(search.getText())){
                    itsforme = itsforme + "(" + Mathf.ceil(bui.x/8) + "," + Mathf.ceil(bui.y/8)  + "); ";
                }
            }
        }
        if(itsforme.length() == 0){
            player.sendMessage("Not found");
        } else NetClient.findCoords(ui.chatfrag.addMessage(itsforme, null, null, "", itsforme));
    }
}
