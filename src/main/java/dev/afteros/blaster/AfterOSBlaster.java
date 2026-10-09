package dev.afteros.blaster;

import dev.afteros.blaster.entity.FloatingCrt;
import dev.afteros.blaster.entity.ImaginarySpace;
import dev.afteros.blaster.entity.SoulmineOrbit;
import dev.afteros.blaster.entity.TelekinesisGrip;
import dev.afteros.blaster.entity.VoidCollapse;
import dev.afteros.blaster.item.SoulmineBladesItem;
import dev.afteros.blaster.item.TelekinesisItem;
import dev.afteros.blaster.item.VoidCoreItem;
import dev.afteros.blaster.entity.SubspaceLance;
import dev.afteros.blaster.item.SubspaceLanceItem;
import dev.afteros.blaster.entity.SignalBeam;
import dev.afteros.blaster.entity.SignalBolt;
import dev.afteros.blaster.entity.SignalDebris;
import dev.afteros.blaster.item.CrtBlasterItem;
import dev.afteros.blaster.item.FloatingCrtItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(AfterOSBlaster.MOD_ID)
public class AfterOSBlaster {
    public static final String MOD_ID = "afteros_blaster";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MOD_ID);

    // ---- items
    public static final DeferredItem<Item> PHOSPHOR_CELL =
            ITEMS.registerSimpleItem("phosphor_cell", new Item.Properties());
    public static final DeferredItem<CrtBlasterItem> CRT_BLASTER =
            ITEMS.registerItem("crt_blaster", props -> new CrtBlasterItem(props.stacksTo(1).rarity(Rarity.RARE), false));
    public static final DeferredItem<FloatingCrtItem> FLOATING_CRT =
            ITEMS.registerItem("floating_crt", props -> new FloatingCrtItem(props.stacksTo(1).rarity(Rarity.RARE)));
    public static final DeferredItem<CrtBlasterItem> SUPER_CRT_BLASTER =
            ITEMS.registerItem("super_crt_blaster", props -> new CrtBlasterItem(props.stacksTo(1).rarity(Rarity.EPIC).fireResistant(), true));

    public static final DeferredItem<SubspaceLanceItem> SUBSPACE_LANCE =
            ITEMS.registerItem("subspace_lance", props -> new SubspaceLanceItem(props.stacksTo(1).rarity(Rarity.EPIC)));

    public static final DeferredItem<TelekinesisItem> TELEKINESIS =
            ITEMS.registerItem("telekinesis", props -> new TelekinesisItem(props.stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<SoulmineBladesItem> SOULMINE_BLADES =
            ITEMS.registerItem("soulmine_blades", props -> new SoulmineBladesItem(props.stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredItem<VoidCoreItem> VOID_CORE =
            ITEMS.registerItem("void_core", props -> new VoidCoreItem(props.stacksTo(1).rarity(Rarity.EPIC)));

    // ---- entities
    public static final DeferredHolder<EntityType<?>, EntityType<SignalBolt>> SIGNAL_BOLT = ENTITIES.register("signal_bolt",
            () -> EntityType.Builder.<SignalBolt>of(SignalBolt::new, MobCategory.MISC)
                    .noSave().sized(0.4F, 0.4F).clientTrackingRange(8).updateInterval(1).build("signal_bolt"));
    public static final DeferredHolder<EntityType<?>, EntityType<SignalBeam>> SIGNAL_BEAM = ENTITIES.register("signal_beam",
            () -> EntityType.Builder.<SignalBeam>of(SignalBeam::new, MobCategory.MISC)
                    .noSave().sized(0.5F, 0.5F).fireImmune().clientTrackingRange(16).updateInterval(1).build("signal_beam"));
    public static final DeferredHolder<EntityType<?>, EntityType<SignalDebris>> SIGNAL_DEBRIS = ENTITIES.register("signal_debris",
            () -> EntityType.Builder.<SignalDebris>of(SignalDebris::new, MobCategory.MISC)
                    .noSave().sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(2).build("signal_debris"));
    public static final DeferredHolder<EntityType<?>, EntityType<FloatingCrt>> FLOATING_CRT_ENTITY = ENTITIES.register("floating_crt",
            () -> EntityType.Builder.<FloatingCrt>of(FloatingCrt::new, MobCategory.MISC)
                    .noSave().sized(0.9F, 0.9F).fireImmune().clientTrackingRange(10).updateInterval(1).build("floating_crt"));

    public static final DeferredHolder<EntityType<?>, EntityType<SubspaceLance>> SUBSPACE_LANCE_ENTITY = ENTITIES.register("subspace_lance",
            () -> EntityType.Builder.<SubspaceLance>of(SubspaceLance::new, MobCategory.MISC)
                    .noSave().sized(0.4F, 0.4F).fireImmune().clientTrackingRange(10).updateInterval(1).build("subspace_lance"));
    public static final DeferredHolder<EntityType<?>, EntityType<ImaginarySpace>> IMAGINARY_SPACE = ENTITIES.register("imaginary_space",
            () -> EntityType.Builder.<ImaginarySpace>of(ImaginarySpace::new, MobCategory.MISC)
                    .noSave().sized(0.2F, 0.2F).fireImmune().clientTrackingRange(10).updateInterval(20).build("imaginary_space"));

    public static final DeferredHolder<EntityType<?>, EntityType<TelekinesisGrip>> TELEKINESIS_GRIP = ENTITIES.register("telekinesis_grip",
            () -> EntityType.Builder.<TelekinesisGrip>of(TelekinesisGrip::new, MobCategory.MISC)
                    .noSave().sized(0.2F, 0.2F).fireImmune().clientTrackingRange(10).updateInterval(20).build("telekinesis_grip"));
    public static final DeferredHolder<EntityType<?>, EntityType<SoulmineOrbit>> SOULMINE_ORBIT = ENTITIES.register("soulmine_orbit",
            () -> EntityType.Builder.<SoulmineOrbit>of(SoulmineOrbit::new, MobCategory.MISC)
                    .noSave().sized(0.3F, 0.3F).fireImmune().clientTrackingRange(10).updateInterval(1).build("soulmine_orbit"));
    public static final DeferredHolder<EntityType<?>, EntityType<VoidCollapse>> VOID_COLLAPSE = ENTITIES.register("void_collapse",
            () -> EntityType.Builder.<VoidCollapse>of(VoidCollapse::new, MobCategory.MISC)
                    .noSave().sized(0.2F, 0.2F).fireImmune().clientTrackingRange(12).updateInterval(20).build("void_collapse"));

    // ---- sounds (mapped to vanilla sounds in sounds.json)
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARGE = sound("charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE = sound("fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT = sound("impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOOM = sound("boom");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUPER_FIRE = sound("super_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAIN_SWING = sound("chain_swing");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAIN_THROW = sound("chain_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAIN_RETURN = sound("chain_return");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUL_IMPACT = sound("soul_impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUL_CHARGE = sound("soul_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> SOUL_SLAM = sound("soul_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLOCK_LAUNCH = sound("block_launch");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORCE_GRAB = sound("force_grab");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORCE_PUSH = sound("force_push");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORCE_PULL = sound("force_pull");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORCE_THROW = sound("force_throw");
    public static final DeferredHolder<SoundEvent, SoundEvent> FORCE_SLAM = sound("force_slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_OPEN = sound("void_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_TRAVEL = sound("void_travel");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_LANCE = sound("void_lance");
    public static final DeferredHolder<SoundEvent, SoundEvent> VOID_COLLAPSE_SOUND = sound("void_collapse");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(id(name)));
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public AfterOSBlaster(IEventBus modBus, ModContainer container) {
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, BlasterConfig.SPEC);
        modBus.addListener(AfterOSBlaster::creativeTab);
    }

    private static void creativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(CRT_BLASTER);
            event.accept(SUPER_CRT_BLASTER);
            event.accept(FLOATING_CRT);
            event.accept(SUBSPACE_LANCE);
            event.accept(TELEKINESIS);
            event.accept(SOULMINE_BLADES);
            event.accept(VOID_CORE);
            event.accept(PHOSPHOR_CELL);
        }
    }
}
