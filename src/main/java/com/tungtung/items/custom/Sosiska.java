package com.tungtung.items.custom;

import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Sosiska {
    public static final FoodComponent Sosiska = new FoodComponent.Builder()
            .saturationModifier(2)
            .nutrition(2)
            .statusEffect(new StatusEffectInstance(StatusEffects.LUCK, 6 * 20, 1), 1.0f)
            .build();



}
