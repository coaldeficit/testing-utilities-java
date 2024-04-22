package testing.buttons;

import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import blui.ui.*;
import mindustry.gen.*;
import testing.ui.*;

import static testing.ui.TUDialogs.*;

public class Fields{
    public static void addButton(Table t){
        ImageButton b = new ImageButton(Tex.alphaaaa, TUStyles.tuImageStyle);
        BLElements.boxTooltip(b, "@tu-tooltip.button-fields");
        b.clicked(fieldEditor::show);

        t.add(b);
    }
}
