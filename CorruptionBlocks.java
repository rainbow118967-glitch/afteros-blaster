package dev.afteros.crt.gameplay;

import dev.afteros.crt.AfterOSBlaster;
import dev.afteros.crt.block.CorruptedBlock;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/** What each block turns into when corrupted. */
public final class CorruptionBlocks {
    private CorruptionBlocks() {}

    public static boolean isCorrupted(BlockState state) {
        return state.getBlock() instanceof CorruptedBlock
                || state.is(AfterOSBlaster.CORRUPTED_LOG.get())
                || state.is(AfterOSBlaster.GLITCHED_LEAVES.get());
    }

    /** The corrupted version of this state, or null if it cannot be corrupted. */
    public static BlockState convert(BlockState s) {
        if (isCorrupted(s)) {
            return null;
        }
        if (s.is(BlockTags.LEAVES)) {
            return AfterOSBlaster.GLITCHED_LEAVES.get().defaultBlockState();
        }
        if (s.is(BlockTags.LOGS)) {
            BlockState log = AfterOSBlaster.CORRUPTED_LOG.get().defaultBlockState();
            return s.hasProperty(RotatedPillarBlock.AXIS) ? log.setValue(RotatedPillarBlock.AXIS, s.getValue(RotatedPillarBlock.AXIS)) : log;
        }
        if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.MYCELIUM) || s.is(Blocks.PODZOL)) {
            return AfterOSBlaster.CORRUPTED_GRASS.get().defaultBlockState();
        }
        if (s.is(BlockTags.DIRT)) {
            return AfterOSBlaster.CORRUPTED_DIRT.get().defaultBlockState();
        }
        if (s.is(BlockTags.SAND)) {
            return AfterOSBlaster.CORRUPTED_SAND.get().defaultBlockState();
        }
        if (s.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
            return AfterOSBlaster.CORRUPTED_DEEPSLATE.get().defaultBlockState();
        }
        if (s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(Blocks.COBBLESTONE) || s.is(Blocks.MOSSY_COBBLESTONE)) {
            return AfterOSBlaster.CORRUPTED_STONE.get().defaultBlockState();
        }
        return null;
    }

    /** Small plants that get wiped when the ground under them is corrupted. */
    public static boolean isPlant(BlockState s) {
        return s.is(BlockTags.FLOWERS) || s.is(Blocks.SHORT_GRASS) || s.is(Blocks.TALL_GRASS)
                || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN);
    }
}
