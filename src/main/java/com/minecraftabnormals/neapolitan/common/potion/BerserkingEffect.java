package com.minecraftabnormals.neapolitan.common.potion;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

import java.util.UUID;

public class BerserkingEffect extends Effect {
	private static final String ARMOR_UUID = "710D4861-7021-47DE-9F52-62F48D2B61EB";
	private static final String DAMAGE_UUID = "CE752B4A-A279-452D-853A-73C26FB4BA46";

	public BerserkingEffect() {
		super(EffectType.BENEFICIAL, 0x8DF4AE);
		this.addAttributesModifier(Attributes.ARMOR, ARMOR_UUID, 0.0F, AttributeModifier.Operation.ADDITION);
		this.addAttributesModifier(Attributes.ATTACK_DAMAGE, DAMAGE_UUID, 0.0F, AttributeModifier.Operation.ADDITION);
	}

	@Override
	public double getAttributeModifierAmount(int amplifier, AttributeModifier modifier) {
		if (modifier.getID().equals(UUID.fromString(DAMAGE_UUID)))
			return (amplifier + 1) * 0.5F;
		return amplifier + 1;
	}

	@Override
	public void performEffect(LivingEntity entity, int amplifier) {
		this.applyAttributesModifiersToEntity(entity, entity.getAttributeManager(), amplifier);
	}

	@Override
	public boolean isReady(int duration, int amplifier) {
		return true;
	}
}
