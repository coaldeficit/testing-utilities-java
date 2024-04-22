package testing.util;

import arc.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import blui.ui.*;
import mindustry.core.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import testing.*;
import testing.buttons.*;
import testing.ui.*;

import static mindustry.Vars.*;

public class Setup{
    public static boolean posLabelAligned = false;

    public static TerrainPainterFragment terrainFrag;

    public static void init(){
        TUDialogs.load();

        BLSetup.addTable(table -> {
            if(mobile && Core.settings.getBool("console")){
                table.table(Tex.buttonEdge3, Console::addButtons);
                table.row();
            }
            table.table(Tex.buttonEdge3, t -> {
                Spawn.addButtons(t);
                Environment.worldButton(t);
                Effect.statusButton(t);
                Sandbox.addButtons(t);
                Fields.addButton(t);
            });
            table.row();

            table.table(Tex.pane, t -> {
                TeamChanger.addButton(t);
                Health.addButtons(t);
                Death.addButtons(t);
                LightSwitch.lightButton(t);
            });
        }, () -> !net.client() && !TestUtils.disableCampaign());

        BLSetup.addTable(table -> {
            table.table(Tex.pane, Death::seppuku);
        }, () -> !net.client() && state.isCampaign() && TestUtils.disableCampaign());

        Table miniPos = ui.hudGroup.find("minimap/position");
        Label pos = miniPos.find("position");
        pos.setText(() ->
            (Core.settings.getBool("position") ?
                player.tileX() + ", " + player.tileY() + "\n" +
                (Core.settings.getBool("tu-wu-coords", true) ? "[accent]" + fix(player.x) + ", " + fix(player.y) + "\n" : "") :
                ""
            ) +
            (Core.settings.getBool("mouseposition") ?
                "[lightgray]" + World.toTile(Core.input.mouseWorldX()) + ", " + World.toTile(Core.input.mouseWorldY()) + "\n" +
                (Core.settings.getBool("tu-wu-coords", true) ? "[#d4816b]" + fix(Core.input.mouseWorldX()) + ", " + fix(Core.input.mouseWorldY()) : "") : //accentBack is not an indexed color for [] format
                ""
            )
        );
        miniPos.getCell(miniPos.find("minimap")).top().right();
        miniPos.getCell(pos).top().right();

        terrainFrag = new TerrainPainterFragment();
        terrainFrag.build(ui.hudGroup);

        Events.on(WorldLoadEvent.class, e -> {
            if(posLabelAligned) return;
            pos.setAlignment(Align.right, Align.right);
            posLabelAligned = true;
        });
    }

    private static String fix(float f){
        return Strings.autoFixed(f, 1);
    }
}
