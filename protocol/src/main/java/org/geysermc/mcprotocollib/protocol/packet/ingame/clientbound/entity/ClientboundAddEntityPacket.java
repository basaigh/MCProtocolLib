package org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.With;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.FallingBlockData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.GenericObjectData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.MinecartType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.ObjectData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.ProjectileData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.WardenData;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;

import java.util.UUID;

@Data
@With
@AllArgsConstructor
public class ClientboundAddEntityPacket implements MinecraftPacket {
    private static final GenericObjectData EMPTY_DATA = new GenericObjectData(0);

    private final int entityId;
    private final @NonNull UUID uuid;
    private final @NonNull EntityType type;
    private final @NonNull ObjectData data;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float headYaw;
    private final float pitch;
    private final double motionX;
    private final double motionY;
    private final double motionZ;

    public ClientboundAddEntityPacket(int entityId, @NonNull UUID uuid, @NonNull EntityType type,
                                      double x, double y, double z, float yaw, float pitch, float headYaw) {
        this(entityId, uuid, type, EMPTY_DATA, x, y, z, yaw, headYaw, pitch, 0, 0, 0);
    }

    public ClientboundAddEntityPacket(int entityId, @NonNull UUID uuid, @NonNull EntityType type, @NonNull ObjectData data,
                                      double x, double y, double z, float yaw, float pitch, float headYaw) {
        this(entityId, uuid, type, data, x, y, z, yaw, headYaw, pitch, 0, 0, 0);
    }

    public ClientboundAddEntityPacket(int entityId, @NonNull UUID uuid, @NonNull EntityType type,
                                      double x, double y, double z, float yaw, float pitch,
                                      float headYaw, double motionX, double motionY, double motionZ) {
        this(entityId, uuid, type, EMPTY_DATA, x, y, z, yaw, headYaw, pitch, motionX, motionY, motionZ);
    }

    public ClientboundAddEntityPacket(ByteBuf in) {
        this.entityId = MinecraftTypes.readVarInt(in);
        this.uuid = MinecraftTypes.readUUID(in);
        this.type = EntityType.from(MinecraftTypes.readVarInt(in));
        this.x = in.readDouble();
        this.y = in.readDouble();
        this.z = in.readDouble();
        this.pitch = in.readByte() * 360 / 256f;
        this.yaw = in.readByte() * 360 / 256f;
        this.headYaw = in.readByte() * 360 / 256f;

        int data = MinecraftTypes.readVarInt(in);
        if (this.type == EntityType.MINECART) {
            this.data = MinecartType.from(data);
        } else if (this.type == EntityType.ITEM_FRAME || this.type == EntityType.GLOW_ITEM_FRAME || this.type == EntityType.PAINTING) {
            this.data = Direction.VALUES[data];
        } else if (this.type == EntityType.FALLING_BLOCK) {
            this.data = new FallingBlockData(data & 65535, data >> 16);
        } else if (this.type.isProjectile()) {
            this.data = new ProjectileData(data);
        } else if (this.type == EntityType.WARDEN) {
            this.data = new WardenData(data);
        } else {
            if (data == 0) {
                this.data = EMPTY_DATA;
            } else {
                this.data = new GenericObjectData(data);
            }
        }

        this.motionX = in.readShort() / 8000D;
        this.motionY = in.readShort() / 8000D;
        this.motionZ = in.readShort() / 8000D;
    }

    @Override
    public void serialize(ByteBuf out) {
        MinecraftTypes.writeVarInt(out, this.entityId);
        MinecraftTypes.writeUUID(out, this.uuid);
        MinecraftTypes.writeVarInt(out, this.type.ordinal());
        out.writeDouble(this.x);
        out.writeDouble(this.y);
        out.writeDouble(this.z);
        out.writeByte((byte) (this.pitch * 256 / 360));
        out.writeByte((byte) (this.yaw * 256 / 360));
        out.writeByte((byte) (this.headYaw * 256 / 360));

        int data = 0;
        if (this.data instanceof MinecartType) {
            data = ((MinecartType) this.data).ordinal();
        } else if (this.data instanceof Direction) {
            data = ((Direction) this.data).ordinal();
        } else if (this.data instanceof FallingBlockData) {
            data = ((FallingBlockData) this.data).getId() | ((FallingBlockData) this.data).getMetadata() << 16;
        } else if (this.data instanceof ProjectileData) {
            data = ((ProjectileData) this.data).getOwnerId();
        } else if (this.data instanceof GenericObjectData) {
            data = ((GenericObjectData) this.data).getValue();
        }

        MinecraftTypes.writeVarInt(out, data);

        out.writeShort((int) (this.motionX * 8000));
        out.writeShort((int) (this.motionY * 8000));
        out.writeShort((int) (this.motionZ * 8000));
    }

    @Override
    public boolean shouldRunOnGameThread() {
        return true;
    }
}
