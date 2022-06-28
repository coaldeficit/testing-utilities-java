package testing.dialogs;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.TextureAtlas.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.TreeElement.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.Content.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.effect.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.legacy.*;
import mindustry.world.meta.*;
import testing.ui.*;

import java.lang.reflect.*;

import static arc.Core.*;
import static mindustry.Vars.*;

public class FieldEditor extends BaseDialog{
    static Seq<Class<?>> skipFields = Seq.with(ModContentInfo.class, Stats.class);
    static Seq<String> skipFieldNames = Seq.with("name", "iconId", "outineColor");
    static TextField search;
    static Table selection = new Table(), fields = new Table();
    static ContentType selectedType = ContentType.block;
    static UnlockableContent selectedContent;
    static float height = 50f;

    public FieldEditor(){
        super("a weird sort of editor");

        shouldPause = false;
        addCloseButton();
        closeOnBack();
        shown(() -> {
            rebuildSelection();
            rebuildFields();
        });
        onResize(() -> {
            rebuildSelection();
            rebuildFields();
        });

        //ContentType Selector
        cont.table(c -> {
            c.defaults().size(60f);
            contentButton(c, new TextureRegionDrawable(Icon.crafting), ContentType.block);
            contentButton(c, new TextureRegionDrawable(Icon.units), ContentType.unit);
            contentButton(c, new TextureRegionDrawable(Icon.distribution), ContentType.item);
            contentButton(c, new TextureRegionDrawable(Icon.liquid), ContentType.liquid);
            contentButton(c, new TextureRegionDrawable(StatusEffects.burning.fullIcon), ContentType.status);
            contentButton(c, new TextureRegionDrawable(Icon.terrain), ContentType.sector);
        });

        cont.row();
        cont.table(s -> { //Search Bar
            s.image(Icon.zoom).padRight(8);
            search = s.field(null, text -> rebuildSelection()).growX().get();
            search.setMessageText("@players.search");
        }).fillX().padBottom(4).row();

        cont.row();
        cont.pane(all -> {
            all.add(selection).top().center(); //Content Selection
            all.row();
            //TODO field search
            all.add(fields); //Field Editor
        }).expandY().top();
    }

    void contentButton(Table table, Drawable icon, ContentType type){
        ImageButton b = table.button(icon, () -> {
            selectedType = type;
            rebuildSelection();
        }).get();
        b.getStyle().checked = Tex.buttonOver;
        b.update(() -> {
            b.setChecked(selectedType == type);
        });
    }

    void rebuildSelection(){
        selection.clear();
        String text = search.getText();

        selection.label(() -> bundle.get("tu-menu.selection") + (selectedContent != null ? selectedContent.localizedName : bundle.get("none"))).padBottom(6);
        selection.row();

        Seq<UnlockableContent> array = content.getBy(selectedType).<UnlockableContent>as().select(u -> shouldShow(u) && (text.isEmpty() || u.localizedName.toLowerCase().contains(text.toLowerCase())));
        selection.table(list -> {
            list.left();

            float iconMul = 1.25f;
            int cols = (int)Mathf.clamp((graphics.getWidth() - Scl.scl(30)) / Scl.scl(32 + 10) / iconMul, 1, 22 / iconMul);
            int count = 0;

            for(UnlockableContent u : array){
                Image image = new Image(u.uiIcon).setScaling(Scaling.fit);
                list.add(image).size(8 * 4 * iconMul).pad(3);

                ClickListener listener = new ClickListener();
                image.addListener(listener);
                if(!mobile){
                    image.addListener(new HandCursorListener());
                    image.update(() -> image.color.lerp(listener.isOver() || selectedContent == u ? Color.white : Color.lightGray, Mathf.clamp(0.4f * Time.delta)));
                }else{
                    image.update(() -> image.color.lerp(selectedContent == u ? Color.white : Color.lightGray, Mathf.clamp(0.4f * Time.delta)));
                }

                image.clicked(() -> {
                    if(input.keyDown(KeyCode.shiftLeft) && Fonts.getUnicode(u.name) != 0){
                        app.setClipboardText((char)Fonts.getUnicode(u.name) + "");
                        ui.showInfoFade("@copied");
                    }else{
                        if(selectedContent == u){
                            selectedContent = null;
                        }else{
                            selectedContent = u;
                        }
                        rebuildFields();
                    }
                });
                TUElements.boxTooltip(image, u.localizedName);

                if((++count) % cols == 0){
                    list.row();
                }
            }
        }).growX().left().padBottom(10);
    }

    boolean shouldShow(UnlockableContent u){
        return switch(selectedType){
            case block -> u != Blocks.air && !(u instanceof ConstructBlock) && !(u instanceof LegacyBlock);
            case unit -> !((UnitType)u).internal;
            case status -> u != StatusEffects.none;
            default -> true;
        };
    }

    void rebuildFields(){
        fields.clear();

        if(selectedContent == null) return;

        Class<?> c = selectedContent.getClass();

        for(Field field : c.getFields()){
            addField(selectedContent, fields, field);
        }
    }

    /*void setup(){
        cont.clear();

        TreeElement t = new TreeElement();

        for(ContentType type : editableContent){
            t.add(new TreeElementNode(new Label(type.name())).children(tp -> {
                for(UnlockableContent un : Vars.content.getBy(type).<UnlockableContent>as().select(c -> !c.isHidden())){
                    tp.get(new TreeElementNode(new Label(un.emoji() + " " + un.localizedName)).children(cp -> {
                        Table all = new Table();
                        Class<?> c = un.getClass();

                        for(Field field : c.getFields()){
                            addField(un, all, field);
                        }
                        cp.get(new TreeElementNode(all).hoverable(false));
                    }));
                }
            }));
        }

        cont.pane(t).top().left().grow();
    }*/

    void addField(Object obj, Table all, Field field){
        if(skipFields.contains(field.getType()) || skipFieldNames.contains(field.getName())) return;

        Class<?> fieldType = field.getType();
        String head = Strings.insertSpaces(Strings.capitalize(field.getName()))/* + "\n[lightgray][[" + fieldType.getSimpleName() + "][] "*/;
        Table res = new Table();
        res.left();
        res.defaults().fillX();
        makeFieldEditor(obj, fieldType, field, res, null);

        all.defaults().padBottom(2);
        all.add(head).left().uniformY().padRight(8);
        all.add(res).growX().uniformY();

        all.row();
    }

    /**
     * @author Anuke
     * Modified by me (MEEP) to add more functionaliyy.
     * */
    void makeFieldEditor(Object content, Class<?> type, Field field, Table table, TreeElement tree){
        if(type == int.class){
            table.field(Reflect.get(content, field) + "", out -> {
                if(Strings.canParseInt(out)){
                    Reflect.set(content, field, Strings.parseInt(out));
                }
            }).size(250f, height).valid(Strings::canParseInt);
        }else if(type == float.class){
            table.field(Reflect.get(content, field) + "", out -> {
                if(Strings.canParseFloat(out)){
                    Reflect.set(content, field, Strings.parseFloat(out));
                }
            }).size(250f, height).valid(Strings::canParseFloat);
        }else if(type == String.class){
            table.field(Reflect.get(content, field), out -> Reflect.set(content, field, out)).size(250f, height);
        }else if(type == boolean.class){
            table.check("", Reflect.get(content, field), val -> Reflect.set(content, field, val)).left();
        }else if(type == Color.class){
            Color out = Reflect.get(content, field);
            if(out == null) return;
            table.table(Tex.pane, in -> {
                in.stack(new Image(Tex.alphaBg), new Image(Tex.whiteui){{
                    update(() -> setColor(out));
                }}).grow();
            }).margin(4).size(height).padRight(10).get().tapped(() -> {
                ui.picker.show(out, out::set);
            });
        }else if(type == TextureRegion.class){
            TextureRegion[] out = {Reflect.get(content, field)};
            if(out[0] == null) out[0] = Core.atlas.find("error");
            table.image(() -> out[0]).padRight(4).size(50f).scaling(Scaling.fit).visible(() -> out[0].found());
            table.field(out[0].found() && out[0] instanceof AtlasRegion ? ((AtlasRegion)out[0]).name : "", res -> {
                if(!res.isEmpty()){
                    out[0] = Core.atlas.find(res);
                    Reflect.set(content, field, out[0]);
                }
            }).valid(t -> Core.atlas.has(t) || t.isEmpty()).size(250f, height).padLeft(4f);
        }else if(UnlockableContent.class.isAssignableFrom(type)){
            UnlockableContent[] c = {Reflect.get(content, field)};
            ContentType cType = getType(type);
            table.image(() -> c[0] != null ? c[0].uiIcon : new TextureRegion(Icon.none.getRegion())).padRight(4).size(50f).scaling(Scaling.fit);
            table.field(c[0] != null ? c[0].name : "", res -> {
                if(!res.isEmpty()){
                    c[0] = Vars.content.getByName(cType, res);
                    Reflect.set(content, field, c[0]);
                }else{
                    c[0] = null;
                    Reflect.set(content, field, null);
                }
            }).valid(t -> Vars.content.getByName(cType, t) != null || t.isEmpty()).size(250f, height).padLeft(4f);
        }
    }

    ContentType getType(Class<?> ucClass){
        if(Item.class.isAssignableFrom(ucClass)){
            return ContentType.item;
        }else if(Block.class.isAssignableFrom(ucClass)){
            return ContentType.block;
        }else if(Liquid.class.isAssignableFrom(ucClass)){
            return ContentType.liquid;
        }else if(StatusEffect.class.isAssignableFrom(ucClass)){
            return ContentType.status;
        }else if(UnitType.class.isAssignableFrom(ucClass)){
            return ContentType.unit;
        }else if(Weather.class.isAssignableFrom(ucClass)){
            return ContentType.weather;
        }else if(SectorPreset.class.isAssignableFrom(ucClass)){
            return ContentType.sector;
        }else if(Planet.class.isAssignableFrom(ucClass)){
            return ContentType.planet;
        }
        return null;
    }
}