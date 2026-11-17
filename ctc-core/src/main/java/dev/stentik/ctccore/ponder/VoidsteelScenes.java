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
 * Schematic: assets/ctccore/ponder/voidsteel_melting.nbt (7 x 6 x 7)
 * melting: blaze burner 2,1,3 (seething), basin 2,2,3, mixer 2,4,3, creative motor 2,5,3
 * pressing: basin 5,1,3, press 5,3,3 (facing north), creative motor 5,3,4 facing north
 */
public class VoidsteelScenes {
    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    public static void melting(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("voidsteel_melting", "Melting Voidsteel");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.8f);
        scene.showBasePlate();
        scene.idle(10);

        scene.world().showSection(util.select().position(2, 1, 3), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("Voidsteel does not melt in a furnace. It needs a Blaze Burner fed with Blaze Cake (superheated)")
                .pointAt(util.vector().centerOf(2, 1, 3)).placeNearTarget().attachKeyFrame();
        scene.idle(90);

        scene.world().showSection(util.select().position(2, 2, 3), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(2, 4, 3, 2, 5, 3), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("Put a Basin on it and a Mechanical Mixer above. Drop in Voidsteel Ore or Raw Voidsteel")
                .pointAt(util.vector().centerOf(2, 2, 3)).placeNearTarget().attachKeyFrame();
        scene.world().createItemEntity(util.vector().centerOf(2, 3, 3), new Vec3(0, 0.1, 0), stack("ctc:raw_voidsteel"));
        scene.idle(90);

        scene.overlay().showText(80)
                .colored(PonderPalette.MEDIUM)
                .text("Each piece becomes 250 mB of Molten Voidsteel. Crush the ore first: it gives two raw pieces")
                .pointAt(util.vector().topOf(2, 2, 3)).placeNearTarget().attachKeyFrame();
        scene.idle(90);

        scene.world().showSection(util.select().position(5, 1, 3), Direction.DOWN);
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(5, 3, 3, 5, 3, 4), Direction.DOWN);
        scene.idle(10);
        scene.overlay().showText(80)
                .text("Move the metal with pipes into a Basin under a Mechanical Press. Pressing needs no heat")
                .pointAt(util.vector().centerOf(5, 1, 3)).placeNearTarget().attachKeyFrame();
        scene.idle(90);

        scene.world().createItemEntity(util.vector().centerOf(5, 2, 3), new Vec3(0.05, 0.2, 0), stack("ctc:voidsteel_ingot"));
        scene.overlay().showText(70)
                .colored(PonderPalette.GREEN)
                .text("Every 250 mB is pressed into one Voidsteel Ingot")
                .pointAt(util.vector().topOf(5, 1, 3)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        scene.markAsFinished();
    }
}
