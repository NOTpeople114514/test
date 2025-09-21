package btmultiplayer.entity;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class BluetoothPlayerEntity extends AbstractClientPlayerEntity {
    public BluetoothPlayerEntity(ClientWorld world, GameProfile profile, double x, double y, double z) {
        super(world, profile);
        this.setPosition(x, y, z);
    }

    @Override
    public Vec3d getVelocity() {
        return Vec3d.ZERO;
    }

    @Override
    public boolean isInvisible() {
        return false;
    }

    @Override
    public boolean isInvisibleTo(PlayerEntity player) {
        return false;
    }

    @Override
    public void tick() {
        // 禁用默认的物理更新，我们将通过网络更新位置
        this.noClip = true;
        super.tick();
        this.noClip = false;
    }
}
