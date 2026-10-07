package dev.afteros.blaster;

import dev.afteros.blaster.entity.FloatingCrt;
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

    // ---- sounds (mapped to vanilla sounds in sounds.json)
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARGE = sound("charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE = sound("fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT = sound("impact");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOOM = sound("boom");
    public static final DeferredHolder<SoundEvent, SoundEvent> SUPER_FIRE = sound("super_fire");

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
            event.accept(PHOSPHOR_CELL);
        }
    }
}
