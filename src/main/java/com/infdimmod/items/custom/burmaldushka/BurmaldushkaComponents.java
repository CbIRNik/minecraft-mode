package com.infdimmod.items.custom.burmaldushka;

import com.infdimmod.InfDimMod;
import com.infdimmod.burmaldeniya.BurmaldeniyaConstants;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class BurmaldushkaComponents {
    public record BurmaldushkaState(int rotation, int version) {
        public static final Codec<BurmaldushkaState> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.INT.optionalFieldOf("rotation", 0).forGetter(BurmaldushkaState::rotation),
                        Codec.INT.optionalFieldOf("version", 1).forGetter(BurmaldushkaState::version)
                ).apply(instance, BurmaldushkaState::new)
        );
    }

    public static final ComponentType<BurmaldushkaState> BURMALDUSHKA_STATE =
            ComponentType.<BurmaldushkaState>builder()
                    .codec(BurmaldushkaState.CODEC)
                    .build();

    private BurmaldushkaComponents() {
    }

    public static void register() {
        Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of(InfDimMod.MOD_ID, BurmaldeniyaConstants.BURMALDUSHKA_ITEM_ID + "_state"),
                BURMALDUSHKA_STATE
        );
    }
}
