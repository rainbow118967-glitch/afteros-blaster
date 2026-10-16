package dev.afteros.blaster.gameplay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

/**
 * Remembers the original block of every corrupted position so the Restoration Tool can undo it,
 * even after the world is saved and reloaded. Corruption never touches block entities, so a state is all that is needed.
 */
public class CorruptionData extends SavedData {
    private final Map<Long, BlockState> originals = new HashMap<>();

    public static CorruptionData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(CorruptionData::new, CorruptionData::load, DataFixTypes.LEVEL), "afteros_corruption");
    }

    public void remember(BlockPos pos, BlockState original) {
        if (this.originals.putIfAbsent(pos.asLong(), original) == null) {
            this.setDirty();
        }
    }

    /** Removes and returns the stored original, or null. */
    public BlockState take(BlockPos pos) {
        BlockState s = this.originals.remove(pos.asLong());
        if (s != null) {
            this.setDirty();
        }
        return s;
    }

    public void forget(BlockPos pos) {
        take(pos);
    }

    public int size() {
        return this.originals.size();
    }

    /** All remembered positions within radius of c, nearest first. */
    public List<BlockPos> near(Vec3 c, double radius) {
        double r2 = radius * radius;
        List<BlockPos> out = new ArrayList<>();
        for (long key : this.originals.keySet()) {
            BlockPos p = BlockPos.of(key);
            if (p.distToCenterSqr(c.x, c.y, c.z) <= r2) {
                out.add(p);
            }
        }
        out.sort(Comparator.comparingDouble(p -> p.distToCenterSqr(c.x, c.y, c.z)));
        return out;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        List<BlockState> palette = new ArrayList<>();
        Map<BlockState, Integer> index = new HashMap<>();
        long[] pos = new long[this.originals.size()];
        int[] idx = new int[this.originals.size()];
        int i = 0;
        for (Map.Entry<Long, BlockState> e : this.originals.entrySet()) {
            pos[i] = e.getKey();
            Integer found = index.get(e.getValue());
            if (found == null) {
                found = palette.size();
                palette.add(e.getValue());
                index.put(e.getValue(), found);
            }
            idx[i] = found;
            i++;
        }
        ListTag pal = new ListTag();
        for (BlockState s : palette) {
            pal.add(NbtUtils.writeBlockState(s));
        }
        tag.put("palette", pal);
        tag.put("pos", new LongArrayTag(pos));
        tag.put("idx", new IntArrayTag(idx));
        return tag;
    }

    public static CorruptionData load(CompoundTag tag, HolderLookup.Provider registries) {
        CorruptionData data = new CorruptionData();
        HolderGetter<Block> blocks = BuiltInRegistries.BLOCK.asLookup();
        ListTag pal = tag.getList("palette", Tag.TAG_COMPOUND);
        List<BlockState> palette = new ArrayList<>();
        for (int i = 0; i < pal.size(); i++) {
            palette.add(NbtUtils.readBlockState(blocks, pal.getCompound(i)));
        }
        long[] pos = tag.getLongArray("pos");
        int[] idx = tag.getIntArray("idx");
        for (int i = 0; i < pos.length && i < idx.length; i++) {
            if (idx[i] >= 0 && idx[i] < palette.size()) {
                data.originals.put(pos[i], palette.get(idx[i]));
            }
        }
        return data;
    }
}
