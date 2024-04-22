package testing.util;

import arc.*;
import arc.util.*;
import mindustry.core.*;
import mindustry.game.*;
import testing.dialogs.*;
import testing.editor.*;

public class TUVars{
    public static Team curTeam = Team.sharded;
    public static TUBaseDialog activeDialog;
    public static TerrainPainter painter = new TerrainPainter();
    public static TerrainPaintbrush paintbrush = new TerrainPaintbrush();
    public static boolean foos = Structs.contains(Version.class.getDeclaredFields(), var -> var.getName().equals("foos"));

    /** Delta time that is unaffected by time control. */
    public static float delta(){
        return Core.graphics.getDeltaTime() * 60;
    }
}
