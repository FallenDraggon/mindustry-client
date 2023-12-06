package mindustry.client.ui;

import arc.*;
import arc.graphics.*;
import arc.input.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.Vars;
import mindustry.ai.types.LogicAI;
import mindustry.client.*;
import mindustry.content.Blocks;
import mindustry.content.Items;
import mindustry.content.UnitTypes;
import mindustry.core.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.game.Teams;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.ConstructBlock.*;

import java.util.Iterator;

import static arc.Core.*;
import static mindustry.Vars.*;
import static mindustry.client.ClientVars.lastSentPos;

public class ControlPanelUnit {
    public Category currentCategory = Category.distribution;
    Seq<ActionsHistory.UnitAtControl> unatproc = new Seq<>();

    ObjectFloatMap<Category> scrollPositions = new ObjectFloatMap<>();

    boolean washovered;
    float mhe = 0;

    Table blockTable, toggler;
    ScrollPane blockPane;
    StringBuilder builder = new StringBuilder();

    public ControlPanelUnit(){
        Events.on(WorldLoadEvent.class, event -> {
            Core.app.post(() -> {
                control.input.block = null;
                rebuild();
            });
        });

        Events.on(UnitCreateEvent.class, event -> {
            updateunitsatco();
            if(washovered && player.timer.get(2000)) {washovered = false; }
            if(!washovered) rebuild();

        });
        Events.on(UnitDestroyEvent.class, event -> {
            updateunitsatco();
            if(washovered && player.timer.get(2000)) {washovered = false; }
            if(!washovered) rebuild();
        });

    }


    void rebuild(){
        currentCategory = Category.turret;
        Group group = toggler.parent;
        int index = toggler.getZIndex();
        toggler.remove();
        build(group);
        toggler.setZIndex(index);
    }

    public void build(Group parent) {

        parent.fill(full -> {
            toggler = full;
            full.bottom().visible(() -> ui.hudfrag.shown);

            if (Core.settings.getBool("unitcontrollpanel")) {
                full.table(frame -> {

                    //rebuilds the category table with the correct recipes
                    Runnable rebuildCategory = () -> {
                        blockTable.clear();
                        //blockTable.top().margin(5);

                        blockPane.hovered(() -> washovered = true);

                        ButtonGroup<ImageButton> group = new ButtonGroup<>();
                        group.setMinCheckCount(0);
                        if(unatproc.size == 0){blockTable.button(Icon.refreshSmall, Styles.cleari, this::updateunitsatco);}

                        Iterator<ActionsHistory.UnitAtControl> it = unatproc.iterator();
                        while (it.hasNext()) {
                            ActionsHistory.UnitAtControl b = it.next();

                            //if(Core.settings.getInt("alertmomono") != 0){if( b.type == UnitTypes.mono && b.count > (Core.settings.getInt("alertmomono"))){if(player.timer.get(60)) player.sendMessage("[#ff]ALERT: mono " + b.count + " at " + b.procX/8 + "/" + b.procY/8 );}}

                            ImageButton button = blockTable.button(new TextureRegionDrawable(b.type.uiIcon), Styles.selecti, () -> {
                                Position pospoc = new Vec2(b.procX, b.procY);
                                Spectate.INSTANCE.spectate(pospoc);
                                updateunitsatco();
                            }).size(46f).left().group(group).name("unit-" + b.type.name).get();
                            button.resizeImage(32f);

                            blockTable.labelWrap(() -> {
                                short pX = (short) (b.procX/8);
                                short pY = (short) (b.procY/8);
                                short pc = (short) b.count;

                                builder.setLength(0);
                                builder.append(pc);
                                builder.append(" | ");
                                builder.append(pX);
                                builder.append(",");
                                builder.append(pY);
                                return builder;
                            }).growX().left().width(150f);
                            blockTable.row();
                        }
                        unatproc.clear();

                        blockTable.act(0f);
                        blockPane.setScrollYForce(scrollPositions.get(currentCategory, 0));
                        app.post(() -> {
                            blockPane.setScrollYForce(scrollPositions.get(currentCategory, 0));
                            blockPane.act(0f);
                            blockPane.layout();
                        });

                        //blockTable.setFillParent(true);
                        //blockTable.pack();
                    };


                    frame.table(Tex.pane2, blocksSelect -> {
                        blocksSelect.margin(4).marginTop(0);

                        if (unatproc.size <= 3 ) { mhe = 0;
                        } else  mhe = 194f;
                        blockPane = blocksSelect.pane(blocks -> blockTable = blocks).height(mhe).update(pane -> {
                            if (pane.hasScroll()) {
                                Element result = scene.hit(input.mouseX(), input.mouseY(), true);
                                if (result == null || !result.isDescendantOf(pane)) {
                                    scene.setScrollFocus(null);
                                }
                            }
                        }).grow().get();
                        blockPane.setStyle(Styles.smallPane);
                    }).fillY().bottom().touchable(Touchable.enabled);

                    rebuildCategory.run();
                    //blockTable.setFillParent(true);
                    blockTable.pack();
                }).maxHeight(138f).padRight(Core.settings.getInt("controlunitpaneloffset")-150);
            }
        });
    }

    public void updateunitsatco(){

        for (Unit units : state.teams.get(player.team()).units) {
            if (units.controller() instanceof LogicAI ai) {

                UnitType type = units.type;
                if(ai.controller == null) continue;
                float procY = ai.controller.tileY() * 8;
                float procX = ai.controller.tileX() * 8;
                //player.sendMessage(UnitAC.type + ":" + UnitAC.procX + " " + UnitAC.procY);

                if (unatproc.contains(t -> (t.procX == ai.controller.tileX() * 8) && (t.procY == ai.controller.tileY() * 8) && (t.type == units.type))) {
                    //player.sendMessage("contains:");

                    int idx = unatproc.indexOf(t -> (t.procX == ai.controller.tileX() * 8) && (t.procY == ai.controller.tileY() * 8) && (t.type == units.type));
                    if (idx != -1) {
                        ActionsHistory.UnitAtControl tet = unatproc.get(idx);
                        tet.count = tet.count + 1;
                        unatproc.set(idx,tet);
                    }

                } else unatproc.add(new ActionsHistory.UnitAtControl(type,procX, procY, 1));

                //button.exited(() -> {player.sendMessage("" + unitt);});
            }
        }
        unatproc.sort(s->s.count);
        unatproc.reverse();
    }
}
