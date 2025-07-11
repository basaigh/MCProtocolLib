package org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.With;
import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandNode;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandParser;
import org.geysermc.mcprotocollib.protocol.data.game.command.CommandType;
import org.geysermc.mcprotocollib.protocol.data.game.command.properties.CommandProperties;
import org.geysermc.mcprotocollib.protocol.data.game.command.properties.DoubleProperties;
import org.geysermc.mcprotocollib.protocol.data.game.command.properties.EntityProperties;
import org.geysermc.mcprotocollib.protocol.data.game.command.properties.FloatProperties;
import org.geysermc.mcprotocollib.protocol.data.game.command.properties.IntegerProperties;
import org.geysermc.mcprotocollib.protocol.data.game.command.properties.LongProperties;
import org.geysermc.mcprotocollib.protocol.data.game.command.properties.ResourceProperties;
import org.geysermc.mcprotocollib.protocol.data.game.command.properties.ScoreHolderProperties;
import org.geysermc.mcprotocollib.protocol.data.game.command.properties.StringProperties;
import org.geysermc.mcprotocollib.protocol.data.game.command.properties.TimeProperties;

import java.util.OptionalInt;

@Data
@With
@AllArgsConstructor
public class ClientboundCommandsPacket implements MinecraftPacket {
    private static final int FLAG_TYPE_MASK = 0x03;
    private static final int FLAG_EXECUTABLE = 0x04;
    private static final int FLAG_REDIRECT = 0x08;
    private static final int FLAG_SUGGESTION_TYPE = 0x10;

    private static final int NUMBER_FLAG_MIN_DEFINED = 0x01;
    private static final int NUMBER_FLAG_MAX_DEFINED = 0x02;

    private static final int ENTITY_FLAG_SINGLE_TARGET = 0x01;
    private static final int ENTITY_FLAG_PLAYERS_ONLY = 0x02;

    private final @NonNull CommandNode[] nodes;
    private final int firstNodeIndex;

    public ClientboundCommandsPacket(ByteBuf in) {
        this.nodes = new CommandNode[MinecraftTypes.readVarInt(in)];
        for (int i = 0; i < this.nodes.length; i++) {
            byte flags = in.readByte();
            CommandType type = CommandType.from(flags & FLAG_TYPE_MASK);
            boolean executable = (flags & FLAG_EXECUTABLE) != 0;

            int[] children = new int[MinecraftTypes.readVarInt(in)];
            for (int j = 0; j < children.length; j++) {
                children[j] = MinecraftTypes.readVarInt(in);
            }

            OptionalInt redirectIndex;
            if ((flags & FLAG_REDIRECT) != 0) {
                redirectIndex = OptionalInt.of(MinecraftTypes.readVarInt(in));
            } else {
                redirectIndex = OptionalInt.empty();
            }

            String name = null;
            if (type == CommandType.LITERAL || type == CommandType.ARGUMENT) {
                name = MinecraftTypes.readString(in);
            }

            CommandParser parser = null;
            CommandProperties properties = null;
            Key suggestionType = null;
            if (type == CommandType.ARGUMENT) {
                parser = CommandParser.from(MinecraftTypes.readVarInt(in));
                switch (parser) {
                    case DOUBLE -> {
                        byte numberFlags = in.readByte();
                        double min = -Double.MAX_VALUE;
                        double max = Double.MAX_VALUE;
                        if ((numberFlags & NUMBER_FLAG_MIN_DEFINED) != 0) {
                            min = in.readDouble();
                        }

                        if ((numberFlags & NUMBER_FLAG_MAX_DEFINED) != 0) {
                            max = in.readDouble();
                        }

                        properties = new DoubleProperties(min, max);
                    }
                    case FLOAT -> {
                        byte numberFlags = in.readByte();
                        float min = -Float.MAX_VALUE;
                        float max = Float.MAX_VALUE;
                        if ((numberFlags & NUMBER_FLAG_MIN_DEFINED) != 0) {
                            min = in.readFloat();
                        }

                        if ((numberFlags & NUMBER_FLAG_MAX_DEFINED) != 0) {
                            max = in.readFloat();
                        }

                        properties = new FloatProperties(min, max);
                    }
                    case INTEGER -> {
                        byte numberFlags = in.readByte();
                        int min = Integer.MIN_VALUE;
                        int max = Integer.MAX_VALUE;
                        if ((numberFlags & NUMBER_FLAG_MIN_DEFINED) != 0) {
                            min = in.readInt();
                        }

                        if ((numberFlags & NUMBER_FLAG_MAX_DEFINED) != 0) {
                            max = in.readInt();
                        }

                        properties = new IntegerProperties(min, max);
                    }
                    case LONG -> {
                        byte numberFlags = in.readByte();
                        long min = Long.MIN_VALUE;
                        long max = Long.MAX_VALUE;
                        if ((numberFlags & NUMBER_FLAG_MIN_DEFINED) != 0) {
                            min = in.readLong();
                        }

                        if ((numberFlags & NUMBER_FLAG_MAX_DEFINED) != 0) {
                            max = in.readLong();
                        }

                        properties = new LongProperties(min, max);
                    }
                    case STRING -> properties = StringProperties.from(MinecraftTypes.readVarInt(in));
                    case ENTITY -> {
                        byte entityFlags = in.readByte();
                        properties = new EntityProperties((entityFlags & ENTITY_FLAG_SINGLE_TARGET) != 0,
                                (entityFlags & ENTITY_FLAG_PLAYERS_ONLY) != 0);
                    }
                    case SCORE_HOLDER -> properties = new ScoreHolderProperties(in.readBoolean());
                    case TIME -> properties = new TimeProperties(in.readInt());
                    case RESOURCE_OR_TAG, RESOURCE_OR_TAG_KEY, RESOURCE, RESOURCE_KEY, RESOURCE_SELECTOR -> properties = new ResourceProperties(MinecraftTypes.readResourceLocation(in));
                    default -> {
                    }
                }

                if ((flags & FLAG_SUGGESTION_TYPE) != 0) {
                    suggestionType = MinecraftTypes.readResourceLocation(in);
                }
            }

            this.nodes[i] = new CommandNode(type, executable, children, redirectIndex, name, parser, properties, suggestionType);
        }

        this.firstNodeIndex = MinecraftTypes.readVarInt(in);
    }

    @Override
    public void serialize(ByteBuf out) {
        MinecraftTypes.writeVarInt(out, this.nodes.length);
        for (CommandNode node : this.nodes) {
            int flags = node.getType().ordinal() & FLAG_TYPE_MASK;
            if (node.isExecutable()) {
                flags |= FLAG_EXECUTABLE;
            }

            if (node.getRedirectIndex().isPresent()) {
                flags |= FLAG_REDIRECT;
            }

            if (node.getSuggestionType() != null) {
                flags |= FLAG_SUGGESTION_TYPE;
            }

            out.writeByte(flags);

            MinecraftTypes.writeVarInt(out, node.getChildIndices().length);
            for (int childIndex : node.getChildIndices()) {
                MinecraftTypes.writeVarInt(out, childIndex);
            }

            if (node.getRedirectIndex().isPresent()) {
                MinecraftTypes.writeVarInt(out, node.getRedirectIndex().getAsInt());
            }

            if (node.getType() == CommandType.LITERAL || node.getType() == CommandType.ARGUMENT) {
                MinecraftTypes.writeString(out, node.getName());
            }

            if (node.getType() == CommandType.ARGUMENT) {
                MinecraftTypes.writeVarInt(out, node.getParser().ordinal());
                switch (node.getParser()) {
                    case DOUBLE -> {
                        DoubleProperties properties = (DoubleProperties) node.getProperties();

                        int numberFlags = 0;
                        if (properties.getMin() != -Double.MAX_VALUE) {
                            numberFlags |= NUMBER_FLAG_MIN_DEFINED;
                        }

                        if (properties.getMax() != Double.MAX_VALUE) {
                            numberFlags |= NUMBER_FLAG_MAX_DEFINED;
                        }

                        out.writeByte(numberFlags);
                        if ((numberFlags & NUMBER_FLAG_MIN_DEFINED) != 0) {
                            out.writeDouble(properties.getMin());
                        }

                        if ((numberFlags & NUMBER_FLAG_MAX_DEFINED) != 0) {
                            out.writeDouble(properties.getMax());
                        }
                    }
                    case FLOAT -> {
                        FloatProperties properties = (FloatProperties) node.getProperties();

                        int numberFlags = 0;
                        if (properties.getMin() != -Float.MAX_VALUE) {
                            numberFlags |= NUMBER_FLAG_MIN_DEFINED;
                        }

                        if (properties.getMax() != Float.MAX_VALUE) {
                            numberFlags |= NUMBER_FLAG_MAX_DEFINED;
                        }

                        out.writeByte(numberFlags);
                        if ((numberFlags & NUMBER_FLAG_MIN_DEFINED) != 0) {
                            out.writeFloat(properties.getMin());
                        }

                        if ((numberFlags & NUMBER_FLAG_MAX_DEFINED) != 0) {
                            out.writeFloat(properties.getMax());
                        }
                    }
                    case INTEGER -> {
                        IntegerProperties properties = (IntegerProperties) node.getProperties();

                        int numberFlags = 0;
                        if (properties.getMin() != Integer.MIN_VALUE) {
                            numberFlags |= NUMBER_FLAG_MIN_DEFINED;
                        }

                        if (properties.getMax() != Integer.MAX_VALUE) {
                            numberFlags |= NUMBER_FLAG_MAX_DEFINED;
                        }

                        out.writeByte(numberFlags);
                        if ((numberFlags & NUMBER_FLAG_MIN_DEFINED) != 0) {
                            out.writeInt(properties.getMin());
                        }

                        if ((numberFlags & NUMBER_FLAG_MAX_DEFINED) != 0) {
                            out.writeInt(properties.getMax());
                        }
                    }
                    case LONG -> {
                        LongProperties properties = (LongProperties) node.getProperties();

                        int numberFlags = 0;
                        if (properties.getMin() != Long.MIN_VALUE) {
                            numberFlags |= NUMBER_FLAG_MIN_DEFINED;
                        }

                        if (properties.getMax() != Long.MAX_VALUE) {
                            numberFlags |= NUMBER_FLAG_MAX_DEFINED;
                        }

                        out.writeByte(numberFlags);
                        if ((numberFlags & NUMBER_FLAG_MIN_DEFINED) != 0) {
                            out.writeLong(properties.getMin());
                        }

                        if ((numberFlags & NUMBER_FLAG_MAX_DEFINED) != 0) {
                            out.writeLong(properties.getMax());
                        }
                    }
                    case STRING -> MinecraftTypes.writeVarInt(out, ((StringProperties) node.getProperties()).ordinal());
                    case ENTITY -> {
                        EntityProperties properties = (EntityProperties) node.getProperties();
                        int entityFlags = 0;
                        if (properties.isSingleTarget()) {
                            entityFlags |= ENTITY_FLAG_SINGLE_TARGET;
                        }

                        if (properties.isPlayersOnly()) {
                            entityFlags |= ENTITY_FLAG_PLAYERS_ONLY;
                        }

                        out.writeByte(entityFlags);
                    }
                    case SCORE_HOLDER -> out.writeBoolean(((ScoreHolderProperties) node.getProperties()).isAllowMultiple());
                    case TIME -> out.writeInt(((TimeProperties) node.getProperties()).getMin());
                    case RESOURCE_OR_TAG, RESOURCE_OR_TAG_KEY, RESOURCE, RESOURCE_KEY, RESOURCE_SELECTOR -> MinecraftTypes.writeResourceLocation(out, ((ResourceProperties) node.getProperties()).getRegistryKey());
                    default -> {
                    }
                }

                if (node.getSuggestionType() != null) {
                    MinecraftTypes.writeResourceLocation(out, node.getSuggestionType());
                }
            }
        }

        MinecraftTypes.writeVarInt(out, this.firstNodeIndex);
    }

    @Override
    public boolean shouldRunOnGameThread() {
        return true;
    }
}
