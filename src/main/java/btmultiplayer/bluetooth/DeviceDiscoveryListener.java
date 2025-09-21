package btmultiplayer.bluetooth;

import javax.bluetooth.RemoteDevice;
import java.util.List;

public interface DeviceDiscoveryListener {
    void onDevicesDiscovered(List<RemoteDevice> devices);
    // 发现设备时调用
}
