package com.infdimmod.network;

import com.infdimmod.InfDimMod;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class Networking {
    public static final Identifier SET_PORTAL_VALUE = Identifier.of(InfDimMod.MOD_ID, "set_portal_value");
    public static final Identifier SYNC_PORTAL_VALUE = Identifier.of(InfDimMod.MOD_ID, "sync_portal_value");

    public static PacketByteBuf makeBuffer() {
        return new PacketByteBuf(Unpooled.buffer());
    }
}
