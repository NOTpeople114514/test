package btmultiplayer.config;

public class ModConfig {
    // 数据包精度选项：低(1位小数)、中(2位小数)、高(3位小数)
    public enum PrecisionLevel {
        TINY(0),
        LOW(1),
        MEDIUM(2),
        HIGH(3);

        private final int decimalPlaces;

        PrecisionLevel(int decimalPlaces) {
            this.decimalPlaces = decimalPlaces;
        }

        public int getDecimalPlaces() {
            return decimalPlaces;
        }
    }

    // 每次启动都默认为中等精度
    public PrecisionLevel packetPrecision = PrecisionLevel.MEDIUM;

    // 单例实例
    private static final ModConfig INSTANCE = new ModConfig();

    // 私有构造函数，防止外部实例化
    private ModConfig() {}

    // 获取配置实例
    public static ModConfig getInstance() {
        return INSTANCE;
    }

}
