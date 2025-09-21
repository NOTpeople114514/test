package btmultiplayer.bluetooth;

import javax.bluetooth.*;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.MinecraftServer;
import java.util.ArrayList;
import java.util.List;

import static btmultiplayer.BluetoothMultiplayerMod.LOGGER;

public class BluetoothManager {
    private static BluetoothManager instance;
    private ServerThread serverThread;
    private ClientThread clientThread;
    private boolean isServer;
    private boolean isInitialized = false;

    public static synchronized BluetoothManager getInstance() {
        if (instance == null) {
            instance = new BluetoothManager();
        }
        return instance;
    }

    // 初始化蓝牙功能
    public void initialize() {
        if (isInitialized) {
            return;
        }

        try {
            // 检查蓝牙是否可用并进行初始化
            LocalDevice localDevice = LocalDevice.getLocalDevice();

            LOGGER.info(
                    "BLUETOOTH INITIALIZED SUCCESSFULLY！{}",
                    localDevice.getBluetoothAddress());
            // 蓝牙初始化成功
            isInitialized = true;
        } catch (BluetoothStateException e) {
            LOGGER.error(
                    "BLUETOOTH INITIALIZED FAILED: {}", e.getMessage());
            // 蓝牙初始化失败
            isInitialized = false;
        }
    }

    public void startServer(MinecraftServer server) {
        if (!isInitialized) {
            LOGGER.error(
                    "BLUETOOTH IS NOT INITIALIZED, CANNOT START THE SERVER.");
            // 蓝牙未初始化，无法启动服务器
            return;
        }

        if (serverThread == null || !serverThread.isAlive()) {
            isServer = true;
            serverThread = new ServerThread();
            serverThread.start();
            DataSyncThread dataSyncThread = new DataSyncThread(server);
            dataSyncThread.start();
        }
    }

    @Environment(EnvType.CLIENT)
    public boolean connectToDevice(RemoteDevice device) {
        if (!isInitialized) {
            LOGGER.error(
                    "BLUETOOTH IS NOT INITIALIZED, CANNOT CONNECT DEVICES");
            // 蓝牙未初始化，无法连接设备
            return false;
        }

        if (clientThread != null && clientThread.isAlive()) {
            clientThread.interrupt();
        }

        isServer = false;
        clientThread = new ClientThread(device);
        clientThread.start();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return clientThread.isConnected();
    }

    @Environment(EnvType.CLIENT)
    public void discoverDevices(DeviceDiscoveryListener listener) {
        if (!isInitialized) {
            LOGGER.error(
                    "BLUETOOTH IS NOT INITIALIZED, CANNOT DISCOVER DEVICES");
            // 蓝牙未初始化，无法发现设备
            listener.onDevicesDiscovered(new ArrayList<>());
            return;
        }

        new Thread(() -> {
            try {
                LocalDevice localDevice = LocalDevice.getLocalDevice();
                DiscoveryAgent agent = localDevice.getDiscoveryAgent();

                List<RemoteDevice> devices = new ArrayList<>();
                agent.startInquiry(DiscoveryAgent.GIAC, new DiscoveryListener() {
                    @Override
                    public void deviceDiscovered(RemoteDevice btDevice, DeviceClass cod) {
                        devices.add(btDevice);
                    }

                    @Override
                    public void inquiryCompleted(int discType) {
                        listener.onDevicesDiscovered(devices);
                    }

                    @Override
                    public void serviceSearchCompleted(int transID, int respCode) {}

                    @Override
                    public void servicesDiscovered(int transID, ServiceRecord[] servRecord) {}
                });
            } catch (BluetoothStateException e) {
                LOGGER.warn(e.getMessage());
                listener.onDevicesDiscovered(new ArrayList<>());
            }
        }).start();
    }

    public void sendData(String data) {
        if (!isInitialized) {
            LOGGER.error(
                    "BLUETOOTH IS NOT INITIALIZED, CANNOT SEND DATA");
            // 蓝牙未初始化，无法发送数据
            return;
        }

        if (isServer && serverThread != null) {
            serverThread.sendData(data);
        } else if (!isServer && clientThread != null) {
            clientThread.sendData(data);
        }
    }

    public void stop() {
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
        }
        isInitialized = false;
    }

    // 判断是否连接成功
    public boolean isConnected() {
        if (!isInitialized) {
            return false;
        }

        if (isServer) {
            return serverThread != null && serverThread.isConnected();
        } else {
            return clientThread != null && clientThread.isConnected();
        }
    }
}
