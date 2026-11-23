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
        // Voidsteel itself comes from the KubeJS scripts of the pack (namespace ctc)
        helper.forComponents(rl("ctc:voidsteel_ore"), rl("ctc:raw_voidsteel"), rl("ctc:crushed_voidsteel"), rl("ctc:voidsteel_ingot"),
                        rl("ctc:molten_voidsteel_bucket"))
                .addStoryBoard("voidsteel_processing", VoidsteelScenes::processing);
        helper.forComponents(chipIds())
                .addStoryBoard("chip_assembly", VoidsteelScenes::chips);
        helper.forComponents(rl("ctccore:cracked_voidsteel_helmet"), rl("ctccore:cracked_voidsteel_chestplate"),
                        rl("ctccore:cracked_voidsteel_leggings"), rl("ctccore:cracked_voidsteel_boots"),
                        rl("ctc:unfinished_cracked_voidsteel_helmet"), rl("ctc:unfinished_cracked_voidsteel_chestplate"),
                        rl("ctc:unfinished_cracked_voidsteel_leggings"), rl("ctc:unfinished_cracked_voidsteel_boots"),
                        rl("ctc:voidsteel_sword"), rl("ctc:voidsteel_rapier"), rl("ctc:voidsteel_battle_axe"),
                        rl("ctc:unfinished_voidsteel_sword"), rl("ctc:unfinished_voidsteel_rapier"), rl("ctc:unfinished_voidsteel_battle_axe"),
                        rl("ctccore:void_tipped_arrow"), rl("ctccore:diamond_tipped_arrow"),
                        rl("ctccore:voidforge_template"), rl("ctccore:void_sigil"))
                .addStoryBoard("voidsteel_finishing", VoidsteelScenes::finishing);
    }

    private static ResourceLocation rl(String id) {
        return ResourceLocation.parse(id);
    }

    private static ResourceLocation[] chipIds() {
        java.util.List<ResourceLocation> out = new java.util.ArrayList<>();
        for (String s : new String[]{"blank_chip", "chip_socket", "chip_scrap"}) out.add(rl("ctccore:" + s));
        for (String s : new String[]{"copper", "iron", "terra", "kinetic", "ember", "lunar", "martian", "mercury", "gravity", "dust",
                "acid", "flare", "void", "harbinger", "ignis", "leviathan", "remnant", "maledictus", "guardian", "lumber", "vein",
                "quarry", "harvest"}) out.add(rl("ctccore:" + s + "_chip"));
        for (String s : new String[]{"incomplete_blank_chip", "incomplete_chip", "incomplete_chip_socket"}) out.add(rl("ctc:" + s));
        return out.toArray(new ResourceLocation[0]);
    }
}
