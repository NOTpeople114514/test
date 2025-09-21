package btmultiplayer.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import btmultiplayer.bluetooth.BluetoothManager;

public class ServerEventHandler {

    // 注册所有服务器生命周期事件的监听器
    public static void register() {
        // 监听服务器启动完成事件
        ServerLifecycleEvents.SERVER_STARTED.register(ServerEventHandler::onServerStarted);

        // 监听服务器停止事件
        ServerLifecycleEvents.SERVER_STOPPED.register(ServerEventHandler::onServerStopped);
    }

    // 服务器启动完成后调用
    private static void onServerStarted(MinecraftServer server) {
        BluetoothManager.getInstance().startServer(server);
    }

    // 服务器完全停止后调用
    private static void onServerStopped(MinecraftServer server) {
        BluetoothManager.getInstance().stop();
    }
}