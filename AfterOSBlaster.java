package dev.afteros.crt;

import dev.afteros.crt.block.CorruptedBlock;
import dev.afteros.crt.block.CorruptionSeedBlock;
import dev.afteros.crt.effect.DataCorruptionEffect;
import dev.afteros.crt.entity.CorruptionFront;
import dev.afteros.crt.entity.FloatingCrt;
import dev.afteros.crt.gameplay.CorruptionCommands;
import dev.afteros.crt.network.ActionPayload;
import dev.afteros.crt.network.KeyActions;
import dev.afteros.crt.item.CorruptorItem;
import dev.afteros.crt.item.RestorationToolItem;
import dev.afteros.crt.entity.ImaginarySpace;
import dev.afteros.crt.entity.SoulmineOrbit;
import dev.afteros.crt.entity.FlameAura;
import dev.afteros.crt.entity.TelekinesisGrip;
import dev.afteros.crt.entity.ThermalFront;
import dev.afteros.crt.entity.VoidCrystalGrowth;
import dev.afteros.crt.entity.VoidPortal;
import dev.afteros.crt.entity.VoidWalker;
import dev.afteros.crt.entity.VoidCollapse;
import dev.afteros.crt.item.FlameCoreItem;
import dev.afteros.crt.item.SoulmineBladesItem;
import dev.afteros.crt.item.TelekinesisItem;
import dev.afteros.crt.item.VoidCoreItem;
import dev.afteros.crt.entity.SignalBeam;
import dev.afteros.crt.entity.SignalBolt;
import dev.afteros.crt.entity.SignalDebris;
import dev.afteros.crt.entity.SubspaceLance;
import dev.afteros.crt.item.CrtBlasterItem;
import dev.afteros.crt.item.FloatingCrtItem;
import dev.afteros.crt.item.SubspaceLanceItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

/** AfterOS // CRT Arsenal - Fabric edition (mod id afteros_crt, so it can sit next to other AfterOS jars). */
public class AfterOSBlaster implements ModInitializer {
    public static final String MOD_ID = "afteros_crt";

    /** Registered object holder with the same get() shape the rest of the code uses. */
    public static final class Ref<T> {
        private T value;

        public T get() {
            return this.value;
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static <I extends Item> Ref<I> item(String name, I item) {
        Ref<I> ref = new Ref<>();
        ref.value = Registry.register(BuiltInRegistries.ITEM, id(name), item);
        return ref;
    }

    private static Ref<Holder<MobEffect>> effect(String name, MobEffect effect) {
        Ref<Holder<MobEffect>> ref = new Ref<>();
        ref.value = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id(name), effect);
        return ref;
    }

    private static <B extends Block> Ref<B> block(String name, B block) {
        Ref<B> ref = new Ref<>();
        ref.value = Registry.register(BuiltInRegistries.BLOCK, id(name), block);
        Registry.register(BuiltInRegistries.ITEM, id(name), new BlockItem(block, new Item.Properties()));
        return ref;
    }

    private static BlockBehaviour.Properties corrupt(float hardness, float resistance, SoundType sound, boolean tool) {
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                .strength(hardness, resistance).sound(sound).lightLevel(state -> 4);
        return tool ? p.requiresCorrectToolForDrops() : p;
    }

    private static <T extends Entity> Ref<EntityType<T>> entity(String name, EntityType.Builder<T> builder) {
        Ref<EntityType<T>> ref = new Ref<>();
        ref.value = Registry.register(BuiltInRegistries.ENTITY_TYPE, id(name), builder.build(name));
        return ref;
    }

    private static Ref<SoundEvent> sound(String name) {
        Ref<SoundEvent> ref = new Ref<>();
        ref.value = Registry.register(BuiltInRegistries.SOUND_EVENT, id(name), SoundEvent.createVariableRangeEvent(id(name)));
        return ref;
    }

    // ---- effects
    public static final Ref<Holder<MobEffect>> DATA_CORRUPTION = effect("data_corruption", new DataCorruptionEffect());

    public static Holder<MobEffect> dataCorruption() {
        return DATA_CORRUPTION.get();
    }

    // ---- items
    public static final Ref<Item> PHOSPHOR_CELL = item("phosphor_cell", new Item(new Item.Properties()));
    public static final Ref<CrtBlasterItem> CRT_BLASTER =
            item("crt_blaster", new CrtBlasterItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), false));
    public static final Ref<FloatingCrtItem> FLOATING_CRT =
            item("floating_crt", new FloatingCrtItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final Ref<CrtBlasterItem> SUPER_CRT_BLASTER =
            item("super_crt_blaster", new CrtBlasterItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), true));
    public static final Ref<SubspaceLanceItem> SUBSPACE_LANCE =
            item("subspace_lance", new SubspaceLanceItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final Ref<TelekinesisItem> TELEKINESIS =
            item("telekinesis", new TelekinesisItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final Ref<SoulmineBladesItem> SOULMINE_BLADES =
            item("soulmine_blades", new SoulmineBladesItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final Ref<VoidCoreItem> VOID_CORE =
            item("void_core", new VoidCoreItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final Ref<CorruptorItem> CRT_CORRUPTOR =
            item("crt_corruptor", new CorruptorItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final Ref<RestorationToolItem> RESTORATION_TOOL =
            item("restoration_tool", new RestorationToolItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    // ---- corrupted blocks (each also registers its block item)
    public static final Ref<CorruptionSeedBlock> CORRUPTION_SEED = block("corruption_seed", new CorruptionSeedBlock(corrupt(3.0F, 1200.0F, SoundType.AMETHYST, true)));
    public static final Ref<CorruptedBlock> CORRUPTED_GRASS = block("corrupted_grass", new CorruptedBlock(corrupt(0.6F, 0.6F, SoundType.GRAVEL, false)));
    public static final Ref<CorruptedBlock> CORRUPTED_DIRT = block("corrupted_dirt", new CorruptedBlock(corrupt(0.5F, 0.5F, SoundType.GRAVEL, false)));
    public static final Ref<CorruptedBlock> CORRUPTED_STONE = block("corrupted_stone", new CorruptedBlock(corrupt(1.5F, 6.0F, SoundType.STONE, true)));
    public static final Ref<CorruptedBlock> CORRUPTED_SAND = block("corrupted_sand", new CorruptedBlock(corrupt(0.5F, 0.5F, SoundType.SAND, false)));
    public static final Ref<CorruptedBlock> CORRUPTED_DEEPSLATE = block("corrupted_deepslate", new CorruptedBlock(corrupt(3.0F, 6.0F, SoundType.DEEPSLATE, true)));
    public static final Ref<RotatedPillarBlock> CORRUPTED_LOG = block("corrupted_log", new RotatedPillarBlock(corrupt(2.0F, 2.0F, SoundType.WOOD, false)));
    public static final Ref<CorruptedBlock> GLITCHED_LEAVES = block("glitched_leaves", new CorruptedBlock(corrupt(0.2F, 0.2F, SoundType.GRASS, false)));

    public static final Ref<FlameCoreItem> FLAME_CORE =
            item("flame_core", new FlameCoreItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));

    // ---- entities
    public static final Ref<EntityType<SignalBolt>> SIGNAL_BOLT = entity("signal_bolt",
            EntityType.Builder.<SignalBolt>of(SignalBolt::new, MobCategory.MISC)
                    .noSave().sized(0.4F, 0.4F).clientTrackingRange(8).updateInterval(1));
    public static final Ref<EntityType<SignalBeam>> SIGNAL_BEAM = entity("signal_beam",
            EntityType.Builder.<SignalBeam>of(SignalBeam::new, MobCategory.MISC)
                    .noSave().sized(0.5F, 0.5F).fireImmune().clientTrackingRange(16).updateInterval(1));
    public static final Ref<EntityType<SignalDebris>> SIGNAL_DEBRIS = entity("signal_debris",
            EntityType.Builder.<SignalDebris>of(SignalDebris::new, MobCategory.MISC)
                    .noSave().sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(2));
    public static final Ref<EntityType<FloatingCrt>> FLOATING_CRT_ENTITY = entity("floating_crt",
            EntityType.Builder.<FloatingCrt>of(FloatingCrt::new, MobCategory.MISC)
                    .noSave().sized(0.9F, 0.9F).fireImmune().clientTrackingRange(10).updateInterval(1));
    public static final Ref<EntityType<SubspaceLance>> SUBSPACE_LANCE_ENTITY = entity("subspace_lance",
            EntityType.Builder.<SubspaceLance>of(SubspaceLance::new, MobCategory.MISC)
                    .noSave().sized(0.4F, 0.4F).fireImmune().clientTrackingRange(10).updateInterval(1));
    public static final Ref<EntityType<ImaginarySpace>> IMAGINARY_SPACE = entity("imaginary_space",
            EntityType.Builder.<ImaginarySpace>of(ImaginarySpace::new, MobCategory.MISC)
                    .noSave().sized(0.2F, 0.2F).fireImmune().clientTrackingRange(10).updateInterval(20));

    public static final Ref<EntityType<TelekinesisGrip>> TELEKINESIS_GRIP = entity("telekinesis_grip",
            EntityType.Builder.<TelekinesisGrip>of(TelekinesisGrip::new, MobCategory.MISC)
                    .noSave().sized(0.2F, 0.2F).fireImmune().clientTrackingRange(10).updateInterval(20));
    public static final Ref<EntityType<SoulmineOrbit>> SOULMINE_ORBIT = entity("soulmine_orbit",
            EntityType.Builder.<SoulmineOrbit>of(SoulmineOrbit::new, MobCategory.MISC)
                    .noSave().sized(0.3F, 0.3F).fireImmune().clientTrackingRange(10).updateInterval(1));
    public static final Ref<EntityType<VoidCollapse>> VOID_COLLAPSE = entity("void_collapse",
            EntityType.Builder.<VoidCollapse>of(VoidCollapse::new, MobCategory.MISC)
                    .noSave().sized(0.2F, 0.2F).fireImmune().clientTrackingRange(12).updateInterval(20));

    public static final Ref<EntityType<CorruptionFront>> CORRUPTION_FRONT = entity("corruption_front",
            EntityType.Builder.<CorruptionFront>of(CorruptionFront::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F).fireImmune().clientTrackingRange(4).updateInterval(40));

    public static final Ref<EntityType<VoidPortal>> VOID_PORTAL = entity("void_portal",
            EntityType.Builder.<VoidPortal>of(VoidPortal::new, MobCategory.MISC)
                    .noSave().sized(2.2F, 3.0F).fireImmune().clientTrackingRange(12).updateInterval(1));
    public static final Ref<EntityType<VoidWalker>> VOID_WALKER = entity("void_walker",
            EntityType.Builder.<VoidWalker>of(VoidWalker::new, MobCategory.MISC)
                    .noSave().sized(0.2F, 0.2F).fireImmune().clientTrackingRange(4).updateInterval(20));
    public static final Ref<EntityType<VoidCrystalGrowth>> VOID_CRYSTAL = entity("void_crystal",
            EntityType.Builder.<VoidCrystalGrowth>of(VoidCrystalGrowth::new, MobCategory.MISC)
                    .noSave().sized(0.2F, 0.2F).fireImmune().clientTrackingRange(4).updateInterval(20));

    public static final Ref<EntityType<FlameAura>> FLAME_AURA = entity("flame_aura",
            EntityType.Builder.<FlameAura>of(FlameAura::new, MobCategory.MISC)
                    .noSave().sized(0.2F, 0.2F).fireImmune().clientTrackingRange(4).updateInterval(20));
    public static final Ref<EntityType<ThermalFront>> THERMAL_FRONT = entity("thermal_front",
            EntityType.Builder.<ThermalFront>of(ThermalFront::new, MobCategory.MISC)
                    .noSave().sized(0.2F, 0.2F).fireImmune().clientTrackingRange(4).updateInterval(40));

    // ---- sounds (mapped to vanilla sounds in sounds.json)
    public static final Ref<SoundEvent> CHARGE = sound("charge");
    public static final Ref<SoundEvent> FIRE = sound("fire");
    public static final Ref<SoundEvent> IMPACT = sound("impact");
    public static final Ref<SoundEvent> BOOM = sound("boom");
    public static final Ref<SoundEvent> SUPER_FIRE = sound("super_fire");
    public static final Ref<SoundEvent> CHAIN_SWING = sound("chain_swing");
    public static final Ref<SoundEvent> CHAIN_THROW = sound("chain_throw");
    public static final Ref<SoundEvent> CHAIN_RETURN = sound("chain_return");
    public static final Ref<SoundEvent> SOUL_IMPACT = sound("soul_impact");
    public static final Ref<SoundEvent> SOUL_CHARGE = sound("soul_charge");
    public static final Ref<SoundEvent> SOUL_SLAM = sound("soul_slam");
    public static final Ref<SoundEvent> BLOCK_LAUNCH = sound("block_launch");
    public static final Ref<SoundEvent> FORCE_GRAB = sound("force_grab");
    public static final Ref<SoundEvent> FORCE_PUSH = sound("force_push");
    public static final Ref<SoundEvent> FORCE_PULL = sound("force_pull");
    public static final Ref<SoundEvent> FORCE_THROW = sound("force_throw");
    public static final Ref<SoundEvent> FORCE_SLAM = sound("force_slam");
    public static final Ref<SoundEvent> VOID_OPEN = sound("void_open");
    public static final Ref<SoundEvent> VOID_TRAVEL = sound("void_travel");
    public static final Ref<SoundEvent> VOID_LANCE = sound("void_lance");
    public static final Ref<SoundEvent> VOID_COLLAPSE_SOUND = sound("void_collapse");

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(ActionPayload.TYPE, ActionPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ActionPayload.TYPE, (payload, context) -> KeyActions.handle(context.player(), payload.action()));
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(entries -> {
            entries.accept(CRT_BLASTER.get());
            entries.accept(SUPER_CRT_BLASTER.get());
            entries.accept(FLOATING_CRT.get());
            entries.accept(SUBSPACE_LANCE.get());
            entries.accept(TELEKINESIS.get());
            entries.accept(SOULMINE_BLADES.get());
            entries.accept(VOID_CORE.get());
            entries.accept(FLAME_CORE.get());
            entries.accept(CRT_CORRUPTOR.get());
            entries.accept(RESTORATION_TOOL.get());
            entries.accept(PHOSPHOR_CELL.get());
        });
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
            entries.accept(CORRUPTION_SEED.get());
            entries.accept(CORRUPTED_GRASS.get());
            entries.accept(CORRUPTED_DIRT.get());
            entries.accept(CORRUPTED_STONE.get());
            entries.accept(CORRUPTED_SAND.get());
            entries.accept(CORRUPTED_DEEPSLATE.get());
            entries.accept(CORRUPTED_LOG.get());
            entries.accept(GLITCHED_LEAVES.get());
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> CorruptionCommands.register(dispatcher));
    }
}
