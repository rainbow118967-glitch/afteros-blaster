package dev.afteros.crt.client;

import dev.afteros.crt.network.KeyActions;
import java.util.function.IntConsumer;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** The configurable keybindings (rebind them in Options > Controls > AfterOS). Loader-specific code registers and polls them. */
public final class KeyDefs {
    private static final String CATEGORY = "key.categories.afteros_crt";

    public static final KeyMapping SUMMON = new KeyMapping("key.afteros_crt.summon", GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping DISMISS = new KeyMapping("key.afteros_crt.dismiss", GLFW.GLFW_KEY_H, CATEGORY);
    public static final KeyMapping FIRE = new KeyMapping("key.afteros_crt.fire", GLFW.GLFW_KEY_Z, CATEGORY);
    public static final KeyMapping CHARGED_FIRE = new KeyMapping("key.afteros_crt.charged_fire", GLFW.GLFW_KEY_X, CATEGORY);
    public static final KeyMapping SWITCH_TARGET = new KeyMapping("key.afteros_crt.switch_target", GLFW.GLFW_KEY_C, CATEGORY);
    public static final KeyMapping RESTORE = new KeyMapping("key.afteros_crt.restore", GLFW.GLFW_KEY_V, CATEGORY);

    public static final KeyMapping[] ALL = {SUMMON, DISMISS, FIRE, CHARGED_FIRE, SWITCH_TARGET, RESTORE};

    private KeyDefs() {}

    /** Call every client tick: sends one packet per key press. */
    public static void poll(IntConsumer send) {
        while (SUMMON.consumeClick()) send.accept(KeyActions.SUMMON);
        while (DISMISS.consumeClick()) send.accept(KeyActions.DISMISS);
        while (FIRE.consumeClick()) send.accept(KeyActions.FIRE);
        while (CHARGED_FIRE.consumeClick()) send.accept(KeyActions.CHARGED_FIRE);
        while (SWITCH_TARGET.consumeClick()) send.accept(KeyActions.SWITCH_TARGET);
        while (RESTORE.consumeClick()) send.accept(KeyActions.RESTORE);
    }
}
