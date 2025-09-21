package btmultiplayer.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import btmultiplayer.entity.BluetoothPlayerEntity;
import btmultiplayer.config.ModConfig;

import static btmultiplayer.BluetoothMultiplayerMod.MAIN_UUID;
import  static btmultiplayer.BluetoothMultiplayerMod.LOGGER;

public class PacketHandler {
    private static BluetoothPlayerEntity remotePlayer;
    private static final GameProfile REMOTE_PLAYER_PROFILE = new GameProfile(
            UUID.fromString(MAIN_UUID),
            Text.translatable("bluetoothmultiplayer.remote_player.name").getString()
    );

    // 获取配置实例
    private static final ModConfig config = ModConfig.getInstance();

    // 根据配置创建格式化器
    private static DecimalFormat getPositionFormat() {
        int decimalPlaces = config.packetPrecision.getDecimalPlaces();
        String pattern = "0." + "0".repeat(decimalPlaces);
        DecimalFormat df = new DecimalFormat(pattern);
        df.setRoundingMode(RoundingMode.HALF_UP);
        return df;
    }

    // 线程安全的位置更新队列
    private static final ConcurrentLinkedQueue<PositionUpdate> positionUpdates = new ConcurrentLinkedQueue<>();

    // 位置更新数据结构
    private static class PositionUpdate {
        double x;
        double y;
        double z;
        float yaw, pitch;

        PositionUpdate(double x, double y, double z, float yaw, float pitch) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    // 发送位置数据包（添加精度控制）
    public static String createPositionPacket(double x, double y, double z, float yaw, float pitch) {
        DecimalFormat df = getPositionFormat();

        // 按照配置的精度格式化位置数据
        String xStr = df.format(x);
        String yStr = df.format(y);
        String zStr = df.format(z);

        // 旋转角度也应用相同的精度控制
        String yawStr = df.format(yaw);
        String pitchStr = df.format(pitch);

        return "POSITION|" + xStr + "|" + yStr + "|" + zStr + "|" + yawStr + "|" + pitchStr;
    }

    public static void handleReceivedPacket(String data) {
        if (data == null || data.isEmpty()) return;

        String[] parts = data.split("\\|");
        if (parts.length < 1) return;

        switch (parts[0]) {
            case "POSITION":
                handlePositionPacket(parts);
                break;
            case "DISCONNECT":
                removeRemotePlayer();
                break;
            default:
                LOGGER.info("Unknown packet type: {}", parts[0]);
                // 未知数据包类型
        }
    }

    private static void handlePositionPacket(String[] parts) {
        if (parts.length < 6) {
            LOGGER.error("INVALID POSITION PACKET FORMAT");
            // 位置数据包格式无效
            return;
        }

        try {
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = Float.parseFloat(parts[4]);
            float pitch = Float.parseFloat(parts[5]);

            // 将位置更新加入队列
            positionUpdates.add(new PositionUpdate(x, y, z, yaw, pitch));

            // 触发客户端主线程处理更新
            processPositionUpdates();
        } catch (NumberFormatException e) {
            LOGGER.error(
                    "ERROR PARSING POSITION PACKET:{}", e.getMessage());
            // 解析位置数据包时出错
        }
    }

    // 在客户端主线程处理位置更新
    public static void processPositionUpdates() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            PositionUpdate update;
            while ((update = positionUpdates.poll()) != null) {
                updateRemotePlayerPosition(
                        update.x, update.y, update.z,
                        update.yaw, update.pitch
                );
            }
        });
    }

    private static void updateRemotePlayerPosition(double x, double y, double z, float yaw, float pitch) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        ClientWorld world = client.world;

        // 创建实体（如果不存在）
        if (remotePlayer == null || remotePlayer.isRemoved()) {
            remotePlayer = new BluetoothPlayerEntity(world, REMOTE_PLAYER_PROFILE, x, y, z);
            remotePlayer.setYaw(yaw);
            remotePlayer.setPitch(pitch);
            world.spawnEntity(remotePlayer);
            LOGGER.info("Spawned New Remote Player");
            // 生成了新的远程玩家
        } else {
            // 平滑更新位置
            Vec3d currentPos = remotePlayer.getPos();
            Vec3d targetPos = new Vec3d(x, y, z);
            Vec3d smoothedPos = currentPos.lerp(targetPos, 0.2f);

            remotePlayer.updatePosition(smoothedPos.x, smoothedPos.y, smoothedPos.z);

            // 平滑更新旋转角度
            float currentYaw = remotePlayer.getYaw();
            float currentPitch = remotePlayer.getPitch();
            float smoothedYaw = lerpAngle(currentYaw, yaw, 0.2f);
            float smoothedPitch = lerp(currentPitch, pitch, 0.2f);

            remotePlayer.setYaw(smoothedYaw);
            remotePlayer.setPitch(smoothedPitch);

            // 强制刷新实体状态
            remotePlayer.setOnGround(true); // 避免实体被判定为下落
            remotePlayer.velocityDirty = true;
        }
    }

    // 移除远程玩家实体
    public static void removeRemotePlayer() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            if (remotePlayer != null && !remotePlayer.isRemoved()) {
                remotePlayer.discard();
                remotePlayer = null;
                LOGGER.info("Remote Player Disconnected");
                // 远程玩家断开连接
            }
        });
    }

    // 角度插值（处理0-360度环绕问题）
    private static float lerpAngle(float from, float to, float delta) {
        float difference = (to - from) % 360;
        if (difference > 180) {
            difference -= 360;
        } else if (difference < -180) {
            difference += 360;
        }
        return from + difference * delta;
    }

    // 线性插值
    private static float lerp(float from, float to, float delta) {
        return from + (to - from) * delta;
    }
}
