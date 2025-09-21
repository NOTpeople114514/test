package btmultiplayer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import btmultiplayer.gui.BluetoothScreen;
import btmultiplayer.network.PacketHandler;
import static btmultiplayer.BluetoothMultiplayerMod.LOGGER;

public class BluetoothMultiplayerClient implements ClientModInitializer {
    private static KeyBinding openBluetoothScreenKey;

    @Override
    public void onInitializeClient() {
        // 注册按键绑定
        openBluetoothScreenKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.bluetoothmultiplayer.open_screen",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.bluetoothmultiplayer.keys"
        ));

        // 注册客户端Tick事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openBluetoothScreenKey.wasPressed()) {

                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new BluetoothScreen(client.currentScreen));
                    // 打开界面
                    LOGGER.info("Opened Bluetooth screen");
                }
            }
            if (client.world != null && !client.isPaused()) {
                PacketHandler.processPositionUpdates();// 处理位置更新
            }
        });

        LOGGER.info("CLIENT SIDE INITIALIZED!");
    }
}