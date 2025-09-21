// TODO 经过测试，1.19.4到1.20.2可以运行，1.20.3及以上会崩溃
package btmultiplayer.gui;

import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.text.Text;

import javax.bluetooth.RemoteDevice;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import btmultiplayer.bluetooth.BluetoothManager;
import btmultiplayer.bluetooth.DeviceDiscoveryListener;

@Environment(EnvType.CLIENT)
public class BluetoothScreen extends Screen implements DeviceDiscoveryListener {
    private DeviceEntryList deviceList;
    private final List<RemoteDevice> discoveredDevices = new ArrayList<>();
    private ButtonWidget discoverButton;
    private final Screen parent;

    public BluetoothScreen(Screen parent) {
        super(Text.translatable("bluetoothmultiplayer.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int listLeft = 20;
        int listWidth = width - 40;
        int listTop = 40;
        int listHeight = height - 100;

        deviceList = new DeviceEntryList(client, listWidth, listHeight, listTop);
        deviceList.setLeftPos(listLeft);

        discoverButton = ButtonWidget.builder(Text.translatable(
                                "bluetoothmultiplayer.button.discover"),
                        button -> onDiscoverClicked())
                .dimensions(width / 2 - 100, height - 50, 200, 20)
                .build();

        ButtonWidget backButton = ButtonWidget.builder(Text.translatable(
                                "bluetoothmultiplayer.button.back"),
                        button -> onBackClicked())
                .dimensions(width / 2 - 100, height - 25, 200, 20)
                .build();

        addDrawableChild(deviceList);
        addDrawableChild(discoverButton);
        addDrawableChild(backButton);
    }

    private void onDiscoverClicked() {
        discoveredDevices.clear();
        deviceList.setScrollAmount(0);
        discoverButton.setMessage(Text.translatable(
                "bluetoothmultiplayer.button.discovering"));
        discoverButton.active = false;

        BluetoothManager.getInstance().discoverDevices(this);
    }

    private void onBackClicked() {
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private void connectToDevice(RemoteDevice device) {
        boolean success = BluetoothManager.getInstance().connectToDevice(device);
        if (success && client != null) {
            client.setScreen(parent);
        }
    }

    @Override
    public void onDevicesDiscovered(List<RemoteDevice> devices) {
        MinecraftClient.getInstance().execute(() -> {
            discoveredDevices.addAll(devices);
            deviceList.setScrollAmount(0);
            discoverButton.setMessage(Text.translatable(
                    "bluetoothmultiplayer.button.discover"));
            discoverButton.active = true;
        });
    }

    public void onDiscoveryFailed(String message) {
        MinecraftClient.getInstance().execute(() -> {
            discoverButton.setMessage(Text.translatable(
                    "bluetoothmultiplayer.discovery.failed", message));
            discoverButton.active = true;
        });
    }

    // 修改了内部类的实现，避免重写final方法
    public class DeviceEntryList extends EntryListWidget<DeviceEntryList.DeviceEntry> {

        public DeviceEntryList(MinecraftClient client, int width, int height, int top) {
            super(client, width, height, top, 30, 20);
        }

        @Override
        public int getEntryCount() {
            return discoveredDevices.size();
        }

        @Override
        public DeviceEntry getEntry(int index) {
            return new DeviceEntry(discoveredDevices.get(index));
        }

        @Override
        protected int getScrollbarPositionX() {
            return this.getRowRight() - 6;
        }

        @Override
        public int getRowWidth() {
            return this.width - 10;
        }

        @Override
        public void appendNarrations(NarrationMessageBuilder builder) {
            builder.put(NarrationPart.TITLE, Text.translatable(
                    "bluetoothmultiplayer.list.title"));
        }

        public class DeviceEntry extends EntryListWidget.Entry<DeviceEntry> {
            private final RemoteDevice device;
            // 添加tooltip文本存储
            private Text tooltip;

            public DeviceEntry(RemoteDevice device) {
                this.device = device;
                // 初始化tooltip
                try {
                    String deviceName = device.getFriendlyName(false);
                    if (deviceName == null || deviceName.trim().isEmpty()) {
                        deviceName = Text.translatable("bluetoothmultiplayer.device.unknown").getString();
                    }
                    this.tooltip = Text.literal(deviceName + "\n" + device.getBluetoothAddress());
                } catch (IOException e) {
                    this.tooltip = Text.translatable("bluetoothmultiplayer.device.unknown")
                            .append("\n").append(device.getBluetoothAddress());
                }
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
                               int mouseX, int mouseY, boolean hovered, float tickDelta) {
                String deviceName;
                try {
                    deviceName = device.getFriendlyName(false);
                    if (deviceName == null || deviceName.trim().isEmpty()) {
                        deviceName = Text.translatable("bluetoothmultiplayer.device.unknown").getString();
                    }
                } catch (IOException e) {
                    deviceName = Text.translatable("bluetoothmultiplayer.device.unknown").getString();
                }

                String address = device.getBluetoothAddress();
                context.drawTextWithShadow(textRenderer,
                        Text.literal(deviceName + " (" + address + ")"),
                        x + 2, y + 2, 0xFFFFFF);

                // 使用新的方式显示tooltip，而不是重写renderTooltip
                if (hovered) {
                    context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
                }
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                connectToDevice(device);
                return true;
            }

            public void appendNarrations(NarrationMessageBuilder builder) {
                builder.put(NarrationPart.TITLE, Text.literal(device.getBluetoothAddress()));
            }
        }
    }
}
