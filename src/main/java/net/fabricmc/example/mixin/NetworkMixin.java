package net.fabricmc.example.mixin;

import io.netty.buffer.EmptyByteBuf;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.network.DecoderHandler;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import net.minecraft.network.PacketByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

//TODO: STREAM MESSAGES TO A SOCKET SERVER

@Mixin(DecoderHandler.class)
public class NetworkMixin {
    private Packet<?> currentPacket;
    ByteBuf bytes;

    FileWriter fileWriter = new FileWriter("C:/Users/bendgk2/AppData/Roaming/.minecraft/mods/packets.json", true);

    public NetworkMixin() throws IOException {
        fileWriter.write("{ ");
    }

    @ModifyVariable(method = "decode(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Ljava/util/List;)V", at = @At("HEAD"),  ordinal = 0)
    private ByteBuf onByteBuf(ByteBuf buf) throws IOException {
        this.bytes = buf.copy();

        String b = "[";
        for (int i = 0; i < bytes.capacity(); i++) {
            if (i == bytes.capacity() - 1) b += String.format("\"0x%02X\"", bytes.getByte(i)) + "]";
            else b += String.format("\"0x%02X\"", bytes.getByte(i)) + ", ";
        }

        System.out.println(b);

        /*
        try {
            fileWriter.append(b).append(", ");
            fileWriter.flush();
        }

        catch (IOException ignored) { }
        */
        return buf;
    }

    @ModifyVariable(method = "decode(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Ljava/util/List;)V", at = @At("STORE"),  ordinal = 0)
    private Packet<?> onPacketIn(Packet<?> packet) {
        currentPacket = packet;
        return packet;
    }

    @Inject(method = "decode(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;Ljava/util/List;)V", at = @At("TAIL"))
    private void packetInCallback(CallbackInfo ci) {
        parsePacket(currentPacket);
    }

    private void parsePacket(Packet<?> packet) {
        StringBuilder result = new StringBuilder();
        String newLine = System.getProperty("line.separator");

        result.append(packet.getClass().getName()).append(" {");
        result.append(newLine);

        //determine fields declared in this class only (no fields of superclass)
        Field[] fields = packet.getClass().getDeclaredFields();

        //print field names paired with their values
        for ( Field field : fields  ) {
            result.append("  ");

            try {
                field.setAccessible(true);

                result.append( field.getName() );
                result.append(": ");
                //requires access to private field:
                result.append( field.get(packet) );
                result.append(",");
            } catch ( Exception ex ) {
                result.append("N/A,");
                ex.printStackTrace();
                System.out.println(ex.getMessage());
            }
            result.append(newLine);
        }
        result.append("}");

        System.out.println(result.toString());
        System.out.println("=========================================");
    }
}
