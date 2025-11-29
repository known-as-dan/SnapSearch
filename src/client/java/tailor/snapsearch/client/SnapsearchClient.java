package tailor.snapsearch.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SnapsearchClient implements ClientModInitializer {

    public static KeyBinding openSearchKey;

    public static KeyBinding.Category SNAPSEARCH_KEYBINDING_CATEGORY = new KeyBinding.Category(net.minecraft.util.Identifier.of("snapsearch"));

    @Override
    public void onInitializeClient() {
        // Register a keybinding (default: G)
        openSearchKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.snapsearch.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                SNAPSEARCH_KEYBINDING_CATEGORY
        ));

        // Tick event to detect key press
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSearchKey.wasPressed()) {
                client.setScreen(new SearchScreen()); // Open our search GUI
            }
        });
    }
}