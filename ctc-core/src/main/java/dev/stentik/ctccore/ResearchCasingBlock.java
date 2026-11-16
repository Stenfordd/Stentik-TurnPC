package dev.stentik.ctccore;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Frame of the Research Station. Once the multiblock is complete it lights up and passes power and items to the core. */
public class ResearchCasingBlock extends Block {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public ResearchCasingBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }
}
