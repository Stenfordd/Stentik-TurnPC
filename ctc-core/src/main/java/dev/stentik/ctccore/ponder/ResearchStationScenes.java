package dev.stentik.ctccore.ponder;

import dev.stentik.ctccore.ResearchCasingBlock;
import dev.stentik.ctccore.ResearchStationBlock;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Schematic: assets/ctccore/ponder/research_station.nbt (5 x 4 x 5)
 * y=0 base plate, y=1 casing floor (1..3), y=2 core at 2,2,2 with corner pillars, y=3 casing roof with glass at 2,3,2,
 * a redstone block at 2,2,4 is not part of it (placed by the scene).
 * Every line of text here needs a matching ctccore.ponder.<scene>.text_N entry in the lang file (tools/gen_assets.py).
 */
public class ResearchStationScenes {
    private static Selection frame(SceneBuildingUtil util) {
        return util.select().fromTo(1, 1, 1, 3, 1, 3)
                .add(util.select().fromTo(1, 3, 1, 3, 3, 3))
                .add(util.select().position(1, 2, 1)).add(util.select().position(3, 2, 1))
                .add(util.select().position(1, 2, 3)).add(util.select().position(3, 2, 3));
    }

    private static void form(SceneBuilder scene, SceneBuildingUtil util, boolean formed) {
        scene.world().modifyBlocks(frame(util), s -> s.hasProperty(ResearchCasingBlock.FORMED) ? s.setValue(ResearchCasingBlock.FORMED, formed) : s, false);
        scene.world().modifyBlock(util.grid().at(2, 2, 2), s -> s.setValue(ResearchStationBlock.FORMED, formed), false);
    }

    public static void building(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("research_station", "Building the Research Station");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.9f);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos core = util.grid().at(2, 2, 2);
        Selection floor = util.select().fromTo(1, 1, 1, 3, 1, 3);
        Selection pillars = util.select().position(1, 2, 1).add(util.select().position(3, 2, 1))
                .add(util.select().position(1, 2, 3)).add(util.select().position(3, 2, 3));
        Selection roof = util.select().fromTo(1, 3, 1, 3, 3, 3);

        scene.world().showSection(floor, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(70)
                .text("Start with a 3x3 floor of Research Casing")
                .pointAt(util.vector().topOf(2, 1, 2)).placeNearTarget().attachKeyFrame();
        scene.idle(80);

        scene.world().showSection(util.select().position(core), Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(70)
                .text("The Research Station goes in the middle, on top of the floor")
                .pointAt(util.vector().centerOf(core)).placeNearTarget().attachKeyFrame();
        scene.idle(80);

        scene.world().showSection(pillars, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(70)
                .text("Put a casing pillar on each of the four corners. The middle of each side stays open")
                .pointAt(util.vector().centerOf(1, 2, 3)).placeNearTarget().attachKeyFrame();
        scene.idle(80);

        scene.world().showSection(roof, Direction.DOWN);
        scene.idle(15);
        scene.overlay().showText(70)
                .text("Finish with a 3x3 casing roof. The block in the middle must be glass")
                .pointAt(util.vector().topOf(2, 3, 2)).placeNearTarget().attachKeyFrame();
        scene.idle(80);

        form(scene, util, true);
        scene.effects().indicateSuccess(core);
        scene.idle(10);
        scene.overlay().showText(80)
                .colored(PonderPalette.GREEN)
                .text("When the frame is complete, it lights up and the station is ready")
                .pointAt(util.vector().centerOf(core)).placeNearTarget().attachKeyFrame();
        scene.idle(90);

        scene.overlay().showOutline(PonderPalette.OUTPUT, "frame", frame(util), 80);
        scene.overlay().showText(80)
                .text("Every block of the frame is a port: cables, hoppers and pipes can connect anywhere on it")
                .pointAt(util.vector().blockSurface(util.grid().at(3, 1, 2), Direction.EAST)).placeNearTarget().attachKeyFrame();
        scene.idle(90);

        scene.world().destroyBlock(util.grid().at(3, 3, 3));
        form(scene, util, false);
        scene.idle(10);
        scene.overlay().showText(70)
                .colored(PonderPalette.RED)
                .text("Take one block away and the whole station switches off")
                .pointAt(util.vector().centerOf(3, 3, 3)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        scene.world().restoreBlocks(util.select().position(3, 3, 3));
        scene.idle(5);
        form(scene, util, true);
        scene.effects().indicateSuccess(core);
        scene.idle(20);
        scene.markAsFinished();
    }

    public static void using(SceneBuilder scene, SceneBuildingUtil util) {
        scene.title("research_station_use", "Implanting chips");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.9f);
        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(0, 1, 0, 4, 3, 4), Direction.DOWN);
        form(scene, util, true);
        BlockPos core = util.grid().at(2, 2, 2);
        scene.idle(20);

        scene.overlay().showText(90)
                .text("Open the station and put in a weapon, armor or a tool, a chip and a Uranium Rod")
                .pointAt(util.vector().centerOf(core)).placeNearTarget().attachKeyFrame();
        scene.idle(100);

        scene.overlay().showText(90)
                .text("Every implant also needs 60,000 FE. The panel in the middle shows what the chip will do")
                .pointAt(util.vector().blockSurface(util.grid().at(3, 1, 2), Direction.EAST)).placeNearTarget().attachKeyFrame();
        scene.idle(100);

        scene.overlay().showText(90)
                .text("Each piece of gear holds 2 chips. A third one replaces the oldest and gives part of its material back")
                .pointAt(util.vector().centerOf(core)).placeNearTarget().attachKeyFrame();
        scene.idle(100);

        scene.world().setBlock(util.grid().at(2, 2, 3), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK.defaultBlockState(), false);
        scene.world().showSection(util.select().position(2, 2, 3), Direction.DOWN);
        scene.idle(10);
        scene.effects().indicateRedstone(util.grid().at(2, 2, 3));
        scene.overlay().showText(90)
                .colored(PonderPalette.GREEN)
                .text("A redstone signal next to the core makes it automatic: it starts as soon as everything is inside")
                .pointAt(util.vector().centerOf(2, 2, 3)).placeNearTarget().attachKeyFrame();
        scene.idle(100);
        scene.markAsFinished();
    }
}
