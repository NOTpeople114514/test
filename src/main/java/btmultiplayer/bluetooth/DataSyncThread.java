package btmultiplayer.bluetooth;

import btmultiplayer.config.ModConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.concurrent.TimeUnit;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class DataSyncThread extends Thread {
    private volatile boolean running = true;
    private final MinecraftServer server;

    // 获取配置实例

    // 接收服务器引用而非客户端
    public DataSyncThread(MinecraftServer server) {
        this.server = server;
        setName("Bluetooth Data Sync Thread");
    }

    @Override
    public void run() {
        while (running) {
            try {
                // 每50毫秒同步一次数据
                TimeUnit.MILLISECONDS.sleep(50);

                if (BluetoothManager.getInstance().isConnected() && server != null) {
                    // 在服务器线程中安全地获取玩家数据
                    server.execute(() -> {
                        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                            // 发送玩家位置数据（使用PacketHandler格式：POSITION|uuid|x|y|z|yaw|pitch）
                            String positionData = createPositionPacket(player);
                            BluetoothManager.getInstance().sendData(positionData);
                        }
                    });
                }
            } catch (InterruptedException e) {
                running = false;
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 创建位置数据包（与PacketHandler逻辑一致，新增UUID标识玩家）
     * 格式：POSITION|玩家UUID|格式化X|格式化Y|格式化Z|格式化Yaw|格式化Pitch
     */
    private String createPositionPacket(ServerPlayerEntity player) {
        DecimalFormat df = getPositionFormat();

        // 按照配置的精度格式化位置与旋转数据
        String xStr = df.format(player.getX());
        String yStr = df.format(player.getY());
        String zStr = df.format(player.getZ());
        String yawStr = df.format(player.getYaw());
        String pitchStr = df.format(player.getPitch());

        // 组装数据包（包含玩家UUID以区分多玩家）
        return "POSITION|" + player.getUuidAsString() + "|" + xStr + "|" + yStr + "|" + zStr + "|" + yawStr + "|" + pitchStr;
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

    @Override
    public void interrupt() {
        running = false;
        super.interrupt();
    }
}