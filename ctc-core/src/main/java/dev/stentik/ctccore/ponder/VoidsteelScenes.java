package dev.stentik.ctccore.ponder;

import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 2.0 Create crafting of the pack's own items. Schematics are written by tools/gen_ponders.py.
 * Every line of text needs a ctccore.ponder.<scene>.text_N entry in the lang file (gen_ponders.py writes them too).
 */
public class VoidsteelScenes {
    static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    static void drop(SceneBuilder scene, SceneBuildingUtil util, int x, int y, int z, String id) {
        scene.world().createItemEntity(util.vector().centerOf(x, y, z), new Vec3(0, 0.1, 0), stack(id));
    }

    /**
     * voidsteel_processing.nbt (9 x 6 x 7)
     * crushing wheels 1,2,3 + 3,2,3 (axis z), depot 2,1,3
     * heated mixer: burner 5,1,3 (kindled), basin 5,2,3, mixer 5,4,3, motor 5,5,3
     * superheated: burner 7,1,3 (seething), basin 7,2,3, mixer 7,4,3, motor 7,5,3
     */
    public static void processing(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("voidsteel_processing", "Processing Voidsteel");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.75f);
        scene.showBasePlate();
        scene.idle(10);

        scene.world().showSection(util.select().fromTo(1, 1, 3, 3, 2, 3), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(80)
                .text("Raw Voidsteel goes through Crushing Wheels: one piece becomes two Crushed Voidsteel")
                .pointAt(util.vector().topOf(2, 2, 3)).placeNearTarget().attachKeyFrame();
        drop(scene, util, 2, 4, 3, "ctc:raw_voidsteel");
        scene.idle(50);
        drop(scene, util, 2, 1, 3, "ctc:crushed_voidsteel");
        scene.idle(50);

        scene.overlay().showText(70)
                .colored(PonderPalette.RED)
                .text("A furnace only gets one ingot out of a raw piece, the Create way gets two")
                .pointAt(util.vector().centerOf(2, 1, 3)).placeNearTarget();
        scene.idle(80);

        scene.world().showSection(util.select().position(5, 1, 3), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(5, 2, 3), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(5, 4, 3, 5, 5, 3), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(90)
                .text("Mix 2 Crushed Voidsteel with a Chorus Fruit in a Basin over a lit Blaze Burner")
                .pointAt(util.vector().centerOf(5, 2, 3)).placeNearTarget().attachKeyFrame();
        drop(scene, util, 5, 3, 3, "ctc:crushed_voidsteel");
        scene.idle(10);
        drop(scene, util, 5, 3, 3, "minecraft:chorus_fruit");
        scene.idle(90);
        scene.overlay().showText(60)
                .colored(PonderPalette.GREEN)
                .text("Out come 2 Voidsteel Ingots")
                .pointAt(util.vector().topOf(5, 2, 3)).placeNearTarget();
        drop(scene, util, 5, 3, 3, "ctc:voidsteel_ingot");
        scene.idle(70);

        scene.world().showSection(util.select().position(7, 1, 3), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(7, 2, 3), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(7, 4, 3, 7, 5, 3), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(90)
                .text("Over a superheated burner (Blaze Cake) an ingot melts into 100 mB of Molten Voidsteel")
                .pointAt(util.vector().centerOf(7, 2, 3)).placeNearTarget().attachKeyFrame();
        drop(scene, util, 7, 3, 3, "ctc:voidsteel_ingot");
        scene.idle(100);
        scene.overlay().showText(90)
                .colored(PonderPalette.MEDIUM)
                .text("Spouts need it for chips, Voidsteel weapons and armor, Void Tipped Arrows and the Voidforge Template")
                .pointAt(util.vector().topOf(7, 2, 3)).placeNearTarget();
        scene.idle(100);
        scene.overlay().showText(70)
                .text("A Mechanical Press over the basin turns 100 mB back into an ingot")
                .pointAt(util.vector().topOf(7, 4, 3)).placeNearTarget();
        scene.idle(80);
        scene.markAsFinished();
    }

    /**
     * chip_assembly.nbt (7 x 5 x 5): depots 1,1,2 / 3,1,2 / 5,1,2, deployer 1,3,2 (down), spout 3,3,2, press 5,3,2
     * mixer side: none (fluids are explained in text)
     */
    public static void chips(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("chip_assembly", "Assembling chips");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.85f);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(1, 1, 2, 5, 1, 2), Direction.DOWN);
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(1, 3, 2, 5, 3, 2), Direction.DOWN);
        scene.idle(15);

        scene.overlay().showText(90)
                .text("Chips are made by Sequenced Assembly: a belt or depots under a Deployer, a Spout and a Mechanical Press")
                .pointAt(util.vector().topOf(3, 1, 2)).placeNearTarget().attachKeyFrame();
        scene.idle(100);

        drop(scene, util, 1, 2, 2, "create:copper_sheet");
        scene.overlay().showText(90)
                .text("Blank Chip: a Copper Sheet gets a Gold Nugget and Polished Rose Quartz from Deployers, then a press. Three rounds")
                .pointAt(util.vector().centerOf(1, 2, 2)).placeNearTarget().attachKeyFrame();
        scene.idle(100);
        drop(scene, util, 5, 2, 2, "ctccore:blank_chip");
        scene.overlay().showText(70)
                .colored(PonderPalette.RED)
                .text("About 1 in 10 fails into Chip Scrap. Crush it to get copper and gold back")
                .pointAt(util.vector().centerOf(5, 2, 2)).placeNearTarget();
        scene.idle(80);

        drop(scene, util, 1, 2, 2, "ctccore:blank_chip");
        scene.overlay().showText(100)
                .text("Every chip: Blank Chip, then its material from a Deployer, 250 mB of its fluid from a Spout, and a press")
                .pointAt(util.vector().topOf(3, 3, 2)).placeNearTarget().attachKeyFrame();
        scene.idle(110);
        drop(scene, util, 5, 2, 2, "ctccore:iron_chip");
        scene.overlay().showText(100)
                .colored(PonderPalette.MEDIUM)
                .text("Most chips use water or lava. Liquid Oxygen, Molten Mercury, Sulfuric Acid and Crude Oil come from a Mixer. JEI shows each recipe")
                .pointAt(util.vector().centerOf(3, 3, 2)).placeNearTarget();
        scene.idle(110);
        scene.overlay().showText(80)
                .text("The Chip Socket is assembled the same way: Brass Sheet, Precision Mechanism, Blank Chip, press")
                .pointAt(util.vector().centerOf(1, 3, 2)).placeNearTarget().attachKeyFrame();
        scene.idle(90);
        scene.markAsFinished();
    }

    /**
     * voidsteel_finishing.nbt (7 x 5 x 5): mechanical crafters x 1..3, y 1..3, z 2 (facing south),
     * depot 5,1,2 with spout 5,3,2
     */
    public static void finishing(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("voidsteel_finishing", "Voidsteel gear");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.85f);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(1, 1, 2, 3, 3, 2), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(90)
                .text("Voidsteel weapons and the Cracked Voidsteel armor start in Mechanical Crafters, in the usual shape")
                .pointAt(util.vector().blockSurface(util.grid().at(2, 2, 2), Direction.SOUTH)).placeNearTarget().attachKeyFrame();
        scene.idle(100);
        drop(scene, util, 2, 0, 3, "ctc:unfinished_cracked_voidsteel_chestplate");
        scene.overlay().showText(70)
                .text("What comes out is still unfinished: the void channels are empty")
                .pointAt(util.vector().centerOf(2, 1, 3)).placeNearTarget();
        scene.idle(80);

        scene.world().showSection(util.select().position(5, 1, 2), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().position(5, 3, 2), Direction.DOWN);
        scene.idle(10);
        drop(scene, util, 5, 2, 2, "ctc:unfinished_cracked_voidsteel_chestplate");
        scene.overlay().showText(90)
                .text("A Spout pours 250 mB of Molten Voidsteel into it, and the piece is done")
                .pointAt(util.vector().topOf(5, 1, 2)).placeNearTarget().attachKeyFrame();
        scene.idle(100);
        drop(scene, util, 5, 2, 2, "ctccore:cracked_voidsteel_chestplate");
        scene.idle(10);
        scene.overlay().showText(90)
                .colored(PonderPalette.MEDIUM)
                .text("Arrows under the same Spout take 25 mB each and become Void Tipped Arrows. Diamond Tipped ones come from a Mixer")
                .pointAt(util.vector().centerOf(5, 3, 2)).placeNearTarget().attachKeyFrame();
        drop(scene, util, 5, 2, 2, "ctccore:void_tipped_arrow");
        scene.idle(100);
        scene.overlay().showText(90)
                .text("The Voidforge Template and the Void Sigil are Sequenced Assembly too: Deployer, Spout and Press, two rounds")
                .pointAt(util.vector().topOf(5, 1, 2)).placeNearTarget();
        scene.idle(100);
        scene.markAsFinished();
    }
}
