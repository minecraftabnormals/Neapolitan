package com.minecraftabnormals.neapolitan.common.potion;

import com.minecraftabnormals.neapolitan.core.registry.NeapolitanEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

import java.util.Map.Entry;

public class SugarRushEffect extends Effect {

	public SugarRushEffect() {
		super(EffectType.NEUTRAL, 6739711);
		this.addAttributesModifier(Attributes.MOVEMENT_SPEED, "7107DE5E-7CE8-4030-940E-514C1F160890", 0.0F, AttributeModifier.Operation.MULTIPLY_TOTAL);
	}

	@Override
	public double getAttributeModifierAmount(int amplifier, AttributeModifier modifier) {
		return 0.1 * (double) (amplifier + 1);
	}

	@Override
	public void applyAttributesModifiersToEntity(LivingEntity entity, AttributeModifierManager attributeMapIn, int amplifier) {
		if (!entity.world.isRemote) {
			int duration = entity.getActivePotionEffect(NeapolitanEffects.SUGAR_RUSH.get()).getDuration();
			int totalDuration = entity.getPersistentData().contains("SugarRushDuration") ? entity.getPersistentData().getInt("SugarRushDuration") : 0;
			float amount = duration >= totalDuration / 3 ? (amplifier + 2) * 0.5F : -(amplifier + 2) * 1.25F;
			for (Entry<Attribute, AttributeModifier> entry : this.getAttributeModifierMap().entrySet()) {
				ModifiableAttributeInstance iattributeinstance = attributeMapIn.createInstanceIfAbsent(entry.getKey());
				if (iattributeinstance != null) {
					AttributeModifier attributemodifier = entry.getValue();
					iattributeinstance.removeModifier(attributemodifier);
					iattributeinstance.applyPersistentModifier(new AttributeModifier(attributemodifier.getID(), this.getName() + " " + amplifier, amount * this.getAttributeModifierAmount(amplifier, attributemodifier), attributemodifier.getOperation()));
				}
			}
		}
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
