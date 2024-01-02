package mindustry.client.ui;

import arc.Core;
import arc.input.KeyCode;
import arc.scene.ui.Dialog;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import mindustry.client.antigrief.TileRecords;
import mindustry.core.ActionsHistory;
import mindustry.gen.Tex;
import mindustry.world.Tile;


public class HistoryInfoFragment extends Table{

    public HistoryInfoFragment() {
        setBackground(Tex.wavepane);
        Image img = new Image();
        add(img);
        Label label = new Label("");
        add(label).height(126);
        visible(() -> Core.settings.getBool("tilehud"));
        var builder = new StringBuilder();
        update(() -> {
            var record  = TileRecords.INSTANCE.getHistory();
            if (record.size() < 1 ) return;
            builder.setLength(0);
            for (var item : record) {
                builder.append(item).append("\n");
            }
            label.setText(builder.length() == 0 ? "" : builder.substring(0, builder.length() - 1));
        });
    }
}