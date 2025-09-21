package btmultiplayer.bluetooth;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import javax.bluetooth.RemoteDevice;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import btmultiplayer.config.ModConfig;
import static btmultiplayer.BluetoothMultiplayerMod.*;

public class ClientThread extends Thread {
    private final RemoteDevice device;
    private StreamConnection connection;
    private DataOutputStream dataOut;
    private boolean isConnected;

    // 获取配置实例
    //private static final ModConfig config = ModConfig.getInstance();

    public ClientThread(RemoteDevice device) {
        this.device = device;
    }

    @Override
    public void run() {
        try {
            // 连接到蓝牙设备
            String connectionString = "btspp://"
                    + device.getBluetoothAddress() + ":"
                    + MAIN_UUID
                    + ";authenticate=false;encrypt=false;master=false";

            connection = (StreamConnection) Connector.open(connectionString);

            dataOut = new DataOutputStream(connection.openOutputStream());
            isConnected = true;

            // 处理 incoming 数据
            DataInputStream dataIn = new DataInputStream(connection.openInputStream());
            String message;
            while (isConnected && (message = dataIn.readUTF()) != null) {
                // 处理接收到的数据
                handleReceivedData(message);
            }

        } catch (IOException e) {
            LOGGER.warn(e.getMessage());
            isConnected = false;
        } finally {
            closeConnection();
        }
    }

    private void handleReceivedData(String data) {
        // 处理接收到的数据
        LOGGER.info("Received Data: {}", data);
        // 接收数据

        // 解析接收到的数据（与PacketHandler保持一致，使用|分隔）
        String[] dataParts = data.split("\\|");
        if (dataParts.length < 1) {
            LOGGER.warn("Invalid data format: {}", data);
            // 数据格式无效
            return;
        }

        // 按数据包类型处理（与PacketHandler格式对齐）
        switch (dataParts[0]) {
            case "POSITION":
                handlePositionPacket(dataParts);
                break;
            case "DISCONNECT":
                handleDisconnectPacket(dataParts);
                break;
            default:
                LOGGER.info("Unknown packet type: {}", dataParts[0]);
                // 未知数据包类型
        }
    }

    /**
     * 处理位置数据包（与PacketHandler格式一致：POSITION|uuid|x|y|z|yaw|pitch）
     */
    private void handlePositionPacket(String[] parts) {
        // 验证数据格式是否正确（需包含：类型+UUID+xyz+yaw+pitch，共7部分）
        if (parts.length < 7) {
            LOGGER.error(
                    "Invalid POSITION packet format. Expected at least 7 parts, got {}",
                    parts.length);
            // 数据包格式无效
            return;
        }

        try {
            // 提取数据部分（按配置精度解析）
            String playerUuid = parts[1];
            double x = Double.parseDouble(parts[2]);
            double y = Double.parseDouble(parts[3]);
            double z = Double.parseDouble(parts[4]);
            float yaw = Float.parseFloat(parts[5]);
            float pitch = Float.parseFloat(parts[6]);

            // 获取服务器实例
            MinecraftServer server = getServer();
            if (server == null) {
                LOGGER.warn(
                        "Cannot update player position - server instance is null");
                // 无法更新玩家位置 - 服务器实例为 null
                return;
            }

            // 在服务器线程中安全地更新玩家位置（添加平滑过渡）
            server.execute(() -> {
                // 查找对应的玩家实体
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerUuid);
                if (player != null) {
                    // 平滑更新位置（与PacketHandler平滑逻辑一致）
                    Vec3d currentPos = player.getPos();
                    Vec3d targetPos = new Vec3d(x, y, z);
                    Vec3d smoothedPos = currentPos.lerp(targetPos, 0.2f);

                    // 更新位置
                    player.updatePosition(smoothedPos.x, smoothedPos.y, smoothedPos.z);

                    // 平滑更新旋转角度（处理0-360度环绕问题）
                    float currentYaw = player.getYaw();
                    float currentPitch = player.getPitch();
                    float smoothedYaw = lerpAngle(currentYaw, yaw, 0.2f);
                    float smoothedPitch = lerp(currentPitch, pitch, 0.2f);

                    player.setYaw(smoothedYaw);
                    player.setPitch(smoothedPitch);

                    // 强制刷新实体状态（避免下落判定）
                    player.setOnGround(true);
                    player.velocityDirty = true;

                    LOGGER.info(
                            "Updated position for player {}: [{}, {}, {}] | Yaw: {}, Pitch: {}",
                            player.getEntityName(), x, y, z, yaw, pitch);
                    // 更新玩家位置
                } else {
                    LOGGER.warn(
                            "Could not find player with UUID: {}", playerUuid);
                    // 找不到带有 UUID 的玩家
                }
            });

        } catch (NumberFormatException e) {
            LOGGER.error(
                    "Failed to parse player coordinates: {}", e.getMessage());
            // 无法解析玩家坐标
        }
    }

    /**
     * 处理断开连接数据包
     */
    private void handleDisconnectPacket(String[] parts) {
        LOGGER.info("Received disconnect packet from remote device");
        // 远程玩家断开连接
        // 可扩展：根据parts中的UUID处理特定玩家断开逻辑
        closeConnection();
    }

    /**
     * 根据配置创建位置格式化器（与PacketHandler完全一致）
     */

    // 获取配置实例
    private static final ModConfig config = ModConfig.getInstance();

    private static DecimalFormat getPositionFormat() {
        int decimalPlaces = config.packetPrecision.getDecimalPlaces();
        String pattern = "0." + "0".repeat(decimalPlaces);
        DecimalFormat df = new DecimalFormat(pattern);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df;
    }

    /**
     * 角度插值（处理0-360度环绕问题，与PacketHandler一致）
     */
    private static float lerpAngle(float from, float to, float delta) {
        float difference = (to - from) % 360;
        if (difference > 180) {
            difference -= 360;
        } else if (difference < -180) {
            difference += 360;
        }
        return from + difference * delta;
    }

    /**
     * 线性插值（与PacketHandler一致）
     */
    private static float lerp(float from, float to, float delta) {
        return from + (to - from) * delta;
    }

    public void sendData(String data) {
        if (isConnected && dataOut != null) {
            try {
                dataOut.writeUTF(data);
                dataOut.flush();
            } catch (IOException e) {
                LOGGER.error(
                        "SEND DATA ERROR:{}", e.getMessage());
                closeConnection();
            }
        }
    }

    public void closeConnection() {
        isConnected = false;
        try {
            if (dataOut != null) dataOut.close();
            if (connection != null) connection.close();
        } catch (IOException e) {
            LOGGER.error("Close connection error: {}", e.getMessage());
        }
    }

    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public void interrupt() {
        closeConnection();
        super.interrupt();
    }
}