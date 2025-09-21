package btmultiplayer;

import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import btmultiplayer.bluetooth.BluetoothManager;
import btmultiplayer.event.ServerEventHandler;

public class BluetoothMultiplayerMod implements ModInitializer {
	public static final String MOD_ID = "bluetoothmultiplayer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final String MAIN_UUID = "00001100-0000-1000-8000-00805F9B34FB";
	public static final BluetoothManager BLUETOOTH_MANAGER = new BluetoothManager();
    private static MinecraftServer serverInstance;

    public static MinecraftServer getServer() {
        return serverInstance;
    }

    @Override
	public void onInitialize() {
		// 初始化蓝牙管理器
		BLUETOOTH_MANAGER.initialize();
		ServerEventHandler.register();
	}
}