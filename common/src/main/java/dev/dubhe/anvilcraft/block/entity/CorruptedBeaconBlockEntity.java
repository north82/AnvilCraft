package dev.dubhe.anvilcraft.block.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class CorruptedBeaconBlockEntity extends BlockEntity {
    List<BeaconBeamSection> beamSections = Lists.newArrayList();
    private List<BeaconBeamSection> checkingBeamSections = Lists.newArrayList();
    int levels;
    private int lastCheckY;

    public CorruptedBeaconBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static @NotNull CorruptedBeaconBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new CorruptedBeaconBlockEntity(type, pos, blockState);
    }

    @SuppressWarnings("unused")
    public static void tick(Level level, @NotNull BlockPos pos, BlockState state, @NotNull CorruptedBeaconBlockEntity blockEntity) {
        int m;
        BlockPos blockPos;
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        if (blockEntity.lastCheckY < j) {
            blockPos = pos;
            blockEntity.checkingBeamSections = Lists.newArrayList();
            blockEntity.lastCheckY = blockPos.getY() - 1;
        } else {
            blockPos = new BlockPos(i, blockEntity.lastCheckY + 1, k);
        }
        BeaconBeamSection beaconBeamSection = blockEntity.checkingBeamSections.isEmpty() ? null : blockEntity.checkingBeamSections.get(blockEntity.checkingBeamSections.size() - 1);
        int l = level.getHeight(Heightmap.Types.WORLD_SURFACE, i, k);
        for (m = 0; m < 10 && blockPos.getY() <= l; ++m) {
            block18:
            {
                BlockState blockState;
                block16:
                {
                    float[] fs;
                    block17:
                    {
                        blockState = level.getBlockState(blockPos);
                        Block block = blockState.getBlock();
                        if (!(block instanceof BeaconBeamBlock beaconBeamBlock)) break block16;
                        fs = beaconBeamBlock.getColor().getTextureDiffuseColors();
                        if (blockEntity.checkingBeamSections.size() > 1) break block17;
                        beaconBeamSection = new BeaconBeamSection(fs);
                        blockEntity.checkingBeamSections.add(beaconBeamSection);
                        break block18;
                    }
                    if (beaconBeamSection == null) break block18;
                    if (Arrays.equals(fs, beaconBeamSection.color)) {
                        beaconBeamSection.increaseHeight();
                    } else {
                        beaconBeamSection = new BeaconBeamSection(new float[]{(beaconBeamSection.color[0] + fs[0]) / 2.0f, (beaconBeamSection.color[1] + fs[1]) / 2.0f, (beaconBeamSection.color[2] + fs[2]) / 2.0f});
                        blockEntity.checkingBeamSections.add(beaconBeamSection);
                    }
                    break block18;
                }
                if (beaconBeamSection != null && (blockState.getLightBlock(level, blockPos) < 15 || blockState.is(Blocks.BEDROCK))) {
                    beaconBeamSection.increaseHeight();
                } else {
                    blockEntity.checkingBeamSections.clear();
                    blockEntity.lastCheckY = l;
                    break;
                }
            }
            blockPos = blockPos.above();
            ++blockEntity.lastCheckY;
        }
        m = blockEntity.levels;
        if (level.getGameTime() % 80L == 0L) {
            if (!blockEntity.beamSections.isEmpty()) {
                blockEntity.levels = updateBase(level, i, j, k);
            }
            if (blockEntity.levels > 0 && !blockEntity.beamSections.isEmpty()) {
                CorruptedBeaconBlockEntity.applyEffects(level, pos);
                CorruptedBeaconBlockEntity.playSound(level, pos, SoundEvents.BEACON_AMBIENT);
            }
        }
        if (blockEntity.lastCheckY >= l) {
            blockEntity.lastCheckY = level.getMinBuildHeight() - 1;
            boolean bl = m > 0;
            blockEntity.beamSections = blockEntity.checkingBeamSections;
            if (!level.isClientSide) {
                boolean bl2;
                bl2 = blockEntity.levels > 0;
                if (!bl && bl2) {
                    playSound(level, pos, SoundEvents.BEACON_ACTIVATE);
                } else if (bl && !bl2) {
                    playSound(level, pos, SoundEvents.BEACON_DEACTIVATE);
                }
            }
        }
    }

    private static int updateBase(Level level, int x, int y, int z) {
        int k;
        int i = 0;
        int j = 1;
        while (j <= 4 && (k = y - j) >= level.getMinBuildHeight()) {
            boolean bl = true;
            block1:
            for (int l = x - j; l <= x + j && bl; ++l) {
                for (int m = z - j; m <= z + j; ++m) {
                    if (level.getBlockState(new BlockPos(l, k, m)).is(BlockTags.BEACON_BASE_BLOCKS)) continue;
                    bl = false;
                    continue block1;
                }
            }
            if (!bl) break;
            i = j++;
        }
        return i;
    }

    @Override
    public void setRemoved() {
        if (this.level == null) return;
        playSound(this.level, this.worldPosition, SoundEvents.BEACON_DEACTIVATE);
        super.setRemoved();
    }

    private static void applyEffects(@NotNull Level level, BlockPos pos) {
        if (level.isClientSide) return;
        AABB aABB = new AABB(pos).expandTowards(0.0, level.getHeight(), 0.0);
        List<Player> list = level.getEntitiesOfClass(Player.class, aABB);
        for (Player player : list) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 0, true, true));
        }
    }

    public static void playSound(@NotNull Level level, BlockPos pos, SoundEvent sound) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    public List<BeaconBeamSection> getBeamSections() {
        return this.levels == 0 ? ImmutableList.of() : this.beamSections;
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Override
    public void setLevel(@NotNull Level level) {
        super.setLevel(level);
        this.lastCheckY = level.getMinBuildHeight() - 1;
    }

    public static class BeaconBeamSection {
        final float[] color;
        @Getter
        private int height;

        public BeaconBeamSection(float[] color) {
            this.color = color;
            this.height = 1;
        }

        protected void increaseHeight() {
            ++this.height;
        }

        public float[] getColor() {
            return new float[]{
                    Math.max(0.0f, 1 - color[0] - 0.3f),
                    Math.max(0.0f, 1 - color[1] - 0.3f),
                    Math.max(0.0f, 1 - color[2] - 0.3f)
            };
        }
    }
}
