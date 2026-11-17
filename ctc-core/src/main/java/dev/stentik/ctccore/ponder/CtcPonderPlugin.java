package dev.stentik.ctccore.ponder;

import dev.stentik.ctccore.CtcCore;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.resources.ResourceLocation;

/** Ponder scenes (hold W over the block in a GUI), the same system Create uses. Ponder ships inside Create. */
public class CtcPonderPlugin implements PonderPlugin {
    public static void register() {
        PonderIndex.addPlugin(new CtcPonderPlugin());
    }

    @Override
    public String getModId() {
        return CtcCore.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(CtcCore.STATION.getId(), CtcCore.CASING.getId())
                .addStoryBoard("research_station", ResearchStationScenes::building)
                .addStoryBoard("research_station", ResearchStationScenes::using);
    }
}
