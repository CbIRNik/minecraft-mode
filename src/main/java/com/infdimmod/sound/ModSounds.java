package com.infdimmod.sound;

import com.infdimmod.InfDimMod;
import com.infdimmod.burmaldeniya.BurmaldeniyaConstants;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public final class ModSounds {
    public static final Identifier BURMALDENIYA_AMBIENT_MUSIC_ID =
            Identifier.of(InfDimMod.MOD_ID, BurmaldeniyaConstants.BURMALDENIYA_AMBIENT_MUSIC_ID);
    public static final Identifier DRUNNY_COLLIDER_AMBIENT_ID =
            Identifier.of(InfDimMod.MOD_ID, BurmaldeniyaConstants.DRUNNY_COLLIDER_AMBIENT_SOUND_ID);

    public static final SoundEvent BURMALDENIYA_AMBIENT_MUSIC = SoundEvent.of(BURMALDENIYA_AMBIENT_MUSIC_ID);
    public static final SoundEvent DRUNNY_COLLIDER_AMBIENT = SoundEvent.of(DRUNNY_COLLIDER_AMBIENT_ID);

    private ModSounds() {
    }

    public static void register() {
        Registry.register(Registries.SOUND_EVENT, BURMALDENIYA_AMBIENT_MUSIC_ID, BURMALDENIYA_AMBIENT_MUSIC);
        Registry.register(Registries.SOUND_EVENT, DRUNNY_COLLIDER_AMBIENT_ID, DRUNNY_COLLIDER_AMBIENT);
    }
}
