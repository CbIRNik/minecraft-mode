package com.infdimmod.items.custom.portalgun;

import com.mojang.serialization.Codec;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.RegistryByteBuf;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import java.util.List;

public record PortalGunComponents(String portalcode) {

    public record PortalEntry(String code, double x, double y, double z) {
        public static final Codec<PortalEntry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("code").forGetter(PortalEntry::code),
                        Codec.DOUBLE.fieldOf("x").forGetter(PortalEntry::x),
                        Codec.DOUBLE.fieldOf("y").forGetter(PortalEntry::y),
                        Codec.DOUBLE.fieldOf("z").forGetter(PortalEntry::z)
                ).apply(instance, PortalEntry::new)
        );
        public static final PacketCodec<RegistryByteBuf, PortalEntry> PACKET_CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, PortalEntry::code,
                PacketCodecs.DOUBLE, PortalEntry::x,
                PacketCodecs.DOUBLE, PortalEntry::y,
                PacketCodecs.DOUBLE, PortalEntry::z,
                PortalEntry::new
        );
    }

    // код измерения
    public static final Codec<PortalGunComponents> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("pcode", "").forGetter(PortalGunComponents::portalcode)
            ).apply(instance, PortalGunComponents::new)
    );

    public static final ComponentType<PortalGunComponents> PORTALCODETYPE =
            ComponentType.<PortalGunComponents>builder()
                    .codec(CODEC)
                    .build();

    //координаты
    public record PortalCoords(double x, double y, double z) {
        public static final Codec<PortalCoords> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.DOUBLE.fieldOf("x").forGetter(PortalCoords::x),
                        Codec.DOUBLE.fieldOf("y").forGetter(PortalCoords::y),
                        Codec.DOUBLE.fieldOf("z").forGetter(PortalCoords::z)
                ).apply(instance, PortalCoords::new)
        );
    }

    public static final ComponentType<PortalCoords> PORTAL_COORDS =
            ComponentType.<PortalCoords>builder()
                    .codec(PortalCoords.CODEC)
                    .build();

    // режим
    public static final ComponentType<Boolean> PORTAL_GUN_MODE =
            ComponentType.<Boolean>builder().codec(Codec.BOOL).build();

    // хистори
    public static final ComponentType<List<PortalEntry>> PORTAL_HISTORY =
            ComponentType.<List<PortalEntry>>builder()
                    .codec(PortalEntry.CODEC.listOf())
                    .build();

    // избранное
    public static final ComponentType<List<PortalEntry>> PORTAL_FAVORITES =
            ComponentType.<List<PortalEntry>>builder()
                    .codec(PortalEntry.CODEC.listOf())
                    .build();

    public static void register() {
        Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of("infdimmod", "portal_gun_code"),
                PORTALCODETYPE
        );
        Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of("infdimmod", "portal_gun_coords"),
                PORTAL_COORDS
        );
        Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of("infdimmod", "portal_gun_mode"),
                PORTAL_GUN_MODE
        );
        Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of("infdimmod", "portal_history"),
                PORTAL_HISTORY
        );
        Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of("infdimmod", "portal_favorites"),
                PORTAL_FAVORITES
        );
    }
}
