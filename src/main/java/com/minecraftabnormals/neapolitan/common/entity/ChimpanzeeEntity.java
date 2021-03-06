package com.minecraftabnormals.neapolitan.common.entity;

import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;

import com.minecraftabnormals.neapolitan.common.entity.goals.*;
import com.minecraftabnormals.neapolitan.common.entity.util.ChimpanzeeAction;
import com.minecraftabnormals.neapolitan.core.other.NeapolitanTags;
import com.minecraftabnormals.neapolitan.core.registry.NeapolitanEntities;
import com.minecraftabnormals.neapolitan.core.registry.NeapolitanItems;
import com.minecraftabnormals.neapolitan.core.registry.NeapolitanParticles;
import com.minecraftabnormals.neapolitan.core.registry.NeapolitanSounds;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.IAngerable;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.BodyController;
import net.minecraft.entity.ai.controller.LookController;
import net.minecraft.entity.ai.goal.BreedGoal;
import net.minecraft.entity.ai.goal.FollowParentGoal;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.ResetAngerGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.ClimberPathNavigator;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.RangedInteger;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.TickRangeConverter;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.IWorld;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ChimpanzeeEntity extends AnimalEntity implements IAngerable {
	private static final DataParameter<Integer> CHIMPANZEE_TYPE = EntityDataManager.createKey(ChimpanzeeEntity.class, DataSerializers.VARINT);
	private static final DataParameter<Integer> ANGER_TIME = EntityDataManager.createKey(ChimpanzeeEntity.class, DataSerializers.VARINT);
	private static final DataParameter<Integer> HUNGER = EntityDataManager.createKey(ChimpanzeeEntity.class, DataSerializers.VARINT);
	private static final DataParameter<Integer> DIRTINESS = EntityDataManager.createKey(ChimpanzeeEntity.class, DataSerializers.VARINT);
	private static final DataParameter<Integer> PALENESS = EntityDataManager.createKey(ChimpanzeeEntity.class, DataSerializers.VARINT);
	private static final DataParameter<Byte> ACTION = EntityDataManager.createKey(ChimpanzeeEntity.class, DataSerializers.BYTE);
	private static final DataParameter<Byte> CLIMBING = EntityDataManager.createKey(ChimpanzeeEntity.class, DataSerializers.BYTE);
	private static final DataParameter<Direction> FACING = EntityDataManager.createKey(ChimpanzeeEntity.class, DataSerializers.DIRECTION);

	public static final EntitySize SIZE_SITTING = EntitySize.fixed(0.6F, 1.0F);
	public static final EntitySize SIZE_SITTING_CHILD = EntitySize.fixed(0.3F, 0.5F);

	private static final RangedInteger ANGER_RANGE = TickRangeConverter.convertRange(20, 39);
	private UUID lastHurtBy;
	private int attackTimer;
	private int pickUpTimer;

	@Nullable
	private ChimpanzeeEntity groomingTarget;
	@Nullable
	private ChimpanzeeEntity groomer;

	private int climbAnim;
	private int prevClimbAnim;

	private int headShakeAnim;
	private int prevHeadShakeAnim;

	public boolean isPartying = false;
	BlockPos jukeboxPosition;

	public ChimpanzeeEntity(EntityType<? extends AnimalEntity> type, World worldIn) {
		super(type, worldIn);
		this.lookController = new ChimpanzeeEntity.LookHelperController();
		this.setCanPickUpLoot(true);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new SwimGoal(this));
		this.goalSelector.addGoal(1, new RestrictRainGoal(this));
		this.goalSelector.addGoal(2, new GrabBananaGoal(this, 1.25D));
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.25D, false));
		this.goalSelector.addGoal(4, new BreedGoal(this, 1.0D));
		this.goalSelector.addGoal(5, new OpenBunchGoal(this));
		this.goalSelector.addGoal(6, new EatBananaGoal(this));
		this.goalSelector.addGoal(7, new TemptBananaGoal(this, 1.25D));
		this.goalSelector.addGoal(8, new TemptGoal(this, 1.25D, Ingredient.fromTag(NeapolitanTags.Items.CHIMPANZEE_FOOD), false));
		this.goalSelector.addGoal(9, new FollowParentGoal(this, 1.25D));
		this.goalSelector.addGoal(10, new ShakeBundleGoal(this, 1.0D, 24, 12));
		this.goalSelector.addGoal(11, new ShareBananaGoal(this, 1.0D));
		this.goalSelector.addGoal(12, new BeGroomedGoal(this));
		this.goalSelector.addGoal(13, new GroomGoal(this, 1.0D));
		this.goalSelector.addGoal(14, new HideFromRainGoal(this, 1.1D, 24, 6));
		this.goalSelector.addGoal(15, new CryGoal(this));
		this.goalSelector.addGoal(16, new ShakeHeadGoal(this));
		this.goalSelector.addGoal(17, new PlayNoteBlockGoal(this, 1.0D, 16));
		this.goalSelector.addGoal(18, new WaterAvoidingRandomWalkingGoal(this, 1.0D));
		this.goalSelector.addGoal(19, new LookAtGoal(this, PlayerEntity.class, 6.0F));
		this.goalSelector.addGoal(20, new LookAtGoal(this, ChimpanzeeEntity.class, 6.0F));
		this.goalSelector.addGoal(21, new LookRandomlyGoal(this));

		this.targetSelector.addGoal(0, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::func_233680_b_));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setCallsForHelp());
		this.targetSelector.addGoal(2, new ResetAngerGoal<>(this, true));
	}

	public static AttributeModifierMap.MutableAttribute registerAttributes() {
		return AnimalEntity.func_233666_p_().createMutableAttribute(Attributes.MAX_HEALTH, 10.0D).createMutableAttribute(Attributes.MOVEMENT_SPEED, (double) 0.3F).createMutableAttribute(Attributes.ATTACK_DAMAGE, 3.0D);
	}

	@Override
	protected void registerData() {
		super.registerData();
		this.dataManager.register(CHIMPANZEE_TYPE, 0);
		this.dataManager.register(ANGER_TIME, 0);
		this.dataManager.register(HUNGER, 0);
		this.dataManager.register(DIRTINESS, 0);
		this.dataManager.register(PALENESS, 0);
		this.dataManager.register(ACTION, (byte) 0);
		this.dataManager.register(CLIMBING, (byte) 0);
		this.dataManager.register(FACING, Direction.DOWN);
	}

	@Override
	public void writeAdditional(CompoundNBT compound) {
		super.writeAdditional(compound);
		this.writeAngerNBT(compound);
		compound.putInt("ChimpanzeeType", this.getChimpanzeeType());
		compound.putInt("Hunger", this.getHunger());
		compound.putInt("Dirtiness", this.getDirtiness());
		compound.putInt("Paleness", this.getPaleness());
	}

	@Override
	public void readAdditional(CompoundNBT compound) {
		super.readAdditional(compound);
		this.readAngerNBT((ServerWorld) this.world, compound);
		this.setChimpanzeeType(compound.getInt("ChimpanzeeType"));
		this.setHunger(compound.getInt("Hunger"));
		this.setDirtiness(compound.getInt("Dirtiness"));
		this.setPaleness(compound.getInt("Paleness"));
	}

	@Override
	protected BodyController createBodyController() {
		return new ChimpanzeeEntity.BodyHelperController();
	}

	@Override
	protected PathNavigator createNavigator(World worldIn) {
		return new ClimberPathNavigator(this, worldIn);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return this.func_233678_J__() ? NeapolitanSounds.ENTITY_CHIMPANZEE_ANGRY.get() : NeapolitanSounds.ENTITY_CHIMPANZEE_AMBIENT.get();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
		return NeapolitanSounds.ENTITY_CHIMPANZEE_HURT.get();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return NeapolitanSounds.ENTITY_CHIMPANZEE_DEATH.get();
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState blockIn) {
		this.playSound(NeapolitanSounds.ENTITY_CHIMPANZEE_STEP.get(), 0.3F, 1.0F);
	}

	@Nullable
	@Override
	public SoundEvent getEatSound(ItemStack itemStackIn) {
		return null;
	}

	@Override
	protected float getSoundVolume() {
		return 0.4F;
	}

	@Override
	protected void updateAITasks() {
		this.func_241359_a_((ServerWorld) this.world, true);

		if (this.func_233678_J__()) {
			this.recentlyHit = this.ticksExisted;
		}

		super.updateAITasks();
	}

	@Override
	public boolean attackEntityAsMob(Entity entityIn) {
		this.swingArms();
		this.world.setEntityState(this, (byte) 4);
		float f = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
		float f1 = (int) f > 0 ? f / 2.0F + (float) this.rand.nextInt((int) f) : f;
		boolean flag = entityIn.attackEntityFrom(DamageSource.causeMobDamage(this), f1);
		if (flag) {
			this.applyEnchantments(this, entityIn);
		}

		return flag;
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (this.isInvulnerableTo(source)) {
			return false;
		} else {
			this.setAction(ChimpanzeeAction.DEFAULT);
			return super.attackEntityFrom(source, amount);
		}
	}

	public void tick() {
		if (!this.world.isRemote) {
			if (this.getAction() == ChimpanzeeAction.DEFAULT || this.getAction() == ChimpanzeeAction.CLIMBING) {
				this.setDefaultAction();
			}
		}

		super.tick();

		if (!this.world.isRemote) {
			this.setBesideClimbableBlock(this.collidedHorizontally);

			if (this.isClimbing()) {
				Direction newfacing = Direction.DOWN;

				for(Direction direction : Direction.Plane.HORIZONTAL) {
					Vector3d vector3d = this.getAllowedMovement(Vector3d.copy(direction.getDirectionVec()));

					if (Math.abs(vector3d.getCoordinate(direction.getAxis())) <= 0.2D ) {
						newfacing = direction;
						if (direction == this.dataManager.get(FACING)) {
							break;
						}
					}
				}

				this.setFacing(newfacing);
			}
		}

		if (this.world.isRemote) {
			if (this.isDirty()) {
				if (this.ticksExisted % 6 == 0) {
					double d0 = ((double) this.rand.nextFloat() + 1.0D) * 0.06D;
					double d1 = this.rand.nextInt(360) - 360.0D;
					double d2 = ((double) this.rand.nextFloat() + 1.0D) * 14.0D;
					d2 *= this.rand.nextBoolean() ? 1.0D : -1.0D;

					world.addParticle(NeapolitanParticles.FLY.get(), this.getPosXRandom(0.5D), this.getPosYEye() + this.rand.nextDouble() * 0.2D + 0.3D, this.getPosZRandom(0.5D), d0, d1, d2);
				}
			}
		}

		if (this.getAction() == ChimpanzeeAction.EATING) {
			ItemStack food = this.getFood();
			if (this.ticksExisted % 10 == 0 && !food.isEmpty()) {
				if (this.world.isRemote) {
					for (int i = 0; i < 6; ++i) {
						Vector3d vector3d = new Vector3d(((double) this.rand.nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, ((double) this.rand.nextFloat() - 0.5D) * 0.1D);
						vector3d = vector3d.rotatePitch(-this.rotationPitch * ((float) Math.PI / 180F));
						vector3d = vector3d.rotateYaw(-this.rotationYaw * ((float) Math.PI / 180F));
						double d0 = (double) (-this.rand.nextFloat()) * 0.2D;
						Vector3d vector3d1 = new Vector3d(((double) this.rand.nextFloat() - 0.5D) * 0.2D, d0, 0.8D + ((double) this.rand.nextFloat() - 0.5D) * 0.2D);
						vector3d1 = vector3d1.rotateYaw(-this.renderYawOffset * ((float) Math.PI / 180F));
						vector3d1 = vector3d1.add(this.getPosX(), this.getPosYEye(), this.getPosZ());
						this.world.addParticle(new ItemParticleData(ParticleTypes.ITEM, this.getHeldItem(this.getFoodHand())), vector3d1.x, vector3d1.y, vector3d1.z, vector3d.x, vector3d.y + 0.05D, vector3d.z);
					}
				}

				this.playSound(NeapolitanSounds.ENTITY_CHIMPANZEE_EAT.get(), 0.25F + 0.5F * (float) this.rand.nextInt(2), (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F);
			}
		} else if (this.getAction() == ChimpanzeeAction.CRYING) {
			if (this.world.isRemote) {
				if (this.ticksExisted % 2 == 0 && this.rand.nextInt(4) > 0) {
					for (int i = 0; i < 2; ++i) {
						double d0 = i == 0 ? (double) (this.rand.nextFloat()) * 0.15D + 0.1D : (double) (-this.rand.nextFloat()) * 0.15D - 0.1D;
						double d1 = ((double) this.rand.nextFloat()) * 0.1D + 0.15D;
						double d2 = i == 0 ? 0.15D : -0.15D;

						Vector3d vector3d = new Vector3d(d0, Math.random() * 0.2D + 0.1D, (double) (this.rand.nextFloat()) * 0.2D + 0.1D);
						vector3d = vector3d.rotateYaw(-this.renderYawOffset * ((float) Math.PI / 180F));
						Vector3d vector3d1 = new Vector3d(d2, d1, 0.35D);
						vector3d1 = vector3d1.rotateYaw(-this.renderYawOffset * ((float) Math.PI / 180F));
						vector3d1 = vector3d1.add(this.getPosX(), this.getPosYEye(), this.getPosZ());

						this.world.addParticle(NeapolitanParticles.TEAR.get(), vector3d1.x, vector3d1.y, vector3d1.z, vector3d.x, vector3d.y + 0.05D, vector3d.z);
					}
				}
			}
		}
	}

	@Override
	public void livingTick() {
		super.livingTick();
		if (this.attackTimer > 0) {
			--this.attackTimer;
		}

		if (this.pickUpTimer > 0) {
			--this.pickUpTimer;
		}

		if (this.jukeboxPosition == null || !this.jukeboxPosition.withinDistance(this.getPositionVec(), 3.46D) || this.world.getBlockState(jukeboxPosition).getBlock() != Blocks.JUKEBOX) {
			this.isPartying = false;
			this.jukeboxPosition = null;
		}

		this.recalculateSize();

		if (!this.world.isRemote) {
			if (!this.isHungry() && this.getHunger() >= 0) {
				this.setHunger(this.getHunger() + 1);
			}

			if (!this.isDirty() && this.getDirtiness() >= 0) {
				this.setDirtiness(this.getDirtiness() + 1);
			}

			if (this.getPaleness() >= 0) {
				if (this.isInSunlight()) {
					this.setPaleness(this.getPaleness() - 1);
				} else if (!this.needsSunlight()) {
					this.setPaleness(this.getPaleness() + 1);
				}
			}
		} else {
			this.prevClimbAnim = this.climbAnim;
			if (this.getAction() == ChimpanzeeAction.CLIMBING) {
				this.climbAnim = Math.min(this.climbAnim + 1, 4);
			} else if (this.getAction() == ChimpanzeeAction.HANGING || this.getAction() == ChimpanzeeAction.SHAKING) {
				this.climbAnim = Math.min(this.climbAnim + 1, 8);
			} else {
				this.climbAnim = Math.max(this.climbAnim - 1, 0);
			}

			this.prevHeadShakeAnim = this.headShakeAnim;
			if (this.headShakeAnim > 0) {
				--this.headShakeAnim;
			}
		}
	}

	@Override
	public ActionResultType func_230254_b_(PlayerEntity player, Hand hand) {
		ItemStack itemstack = player.getHeldItem(hand);
		if (this.isFood(itemstack) && !(this.isBreedingItem(itemstack) && !this.isHungry())) {
			if (this.getHeldItemMainhand().isEmpty()) {
				ItemStack itemstack1 = itemstack.copy();
				itemstack1.setCount(1);
				this.setHeldItem(Hand.MAIN_HAND, itemstack1);
				this.consumeItemFromStack(player, itemstack);
				this.func_241356_K__();

				return ActionResultType.func_233537_a_(this.world.isRemote);
			}
			return ActionResultType.PASS;
		}

		if (itemstack.getItem() == NeapolitanItems.BANANA_BUNCH.get()) {
			if (this.getGrowingAge() > 0 || !this.canFallInLove()) {
				ActionResultType result = itemstack.interactWithEntity(player, this, hand);
				if (result.isSuccessOrConsume()) {
					return result;
				}
			}
		}

		return super.func_230254_b_(player, hand);
	}

	public void openBunch(Hand hand) {
		if (!this.world.isRemote) {
			BananaPeelEntity bananapeel = NeapolitanEntities.BANANA_PEEL.get().create(this.world);
			bananapeel.setLocationAndAngles(this.getPosX(), this.getPosYEye(), this.getPosZ(), this.rotationYaw, 0.0F);
			bananapeel.setMotion(this.rand.nextDouble() * 0.6D - 0.3D, 0.4D, this.rand.nextDouble() * 0.6D - 0.3D);
			this.world.addEntity(bananapeel);

			this.setHeldItem(hand, new ItemStack(NeapolitanItems.BANANA.get()));
		}
	}

	@Override
	public void func_233629_a_(LivingEntity entity, boolean isFlying) {
		this.prevLimbSwingAmount = this.limbSwingAmount;
		double d0 = this.getPosX() - this.prevPosX;
		double d1 = this.isClimbing() ? this.getPosY() - this.prevPosY : 0.0D;
		double d2 = this.getPosZ() - this.prevPosZ;
		float f = MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2) * 4.0F;
		if (f > 1.0F) {
			f = 1.0F;
		}

		this.limbSwingAmount += (f - this.limbSwingAmount) * 0.4F;
		this.limbSwing += this.limbSwingAmount;
	}

	@Override
	public boolean isOnLadder() {
		return (this.getAction() == ChimpanzeeAction.DEFAULT || this.getAction() == ChimpanzeeAction.CLIMBING) && this.isBesideClimbableBlock();
	}

	public boolean isClimbing() {
		return !this.onGround && this.isBesideClimbableBlock();
	}

	@Override
	public boolean onLivingFall(float distance, float damageMultiplier) {
		return false;
	}

	@Override
	public void handleStatusUpdate(byte id) {
		if (id == 4) {
			this.swingArms();
		} else if (id == 6) {
			this.shakeHead(NeapolitanParticles.CHIMPANZEE_NEEDS_FRIEND.get());
		} else if (id == 7) {
			this.shakeHead(NeapolitanParticles.CHIMPANZEE_NEEDS_SUN.get());
		} else if (id == 8) {
			this.shakeHead(NeapolitanParticles.CHIMPANZEE_NEEDS_FOOD.get());
		} else {
			super.handleStatusUpdate(id);
		}
	}

	public static boolean canChimpanzeeSpawn(EntityType<ChimpanzeeEntity> entity, IWorld world, SpawnReason reason, BlockPos pos, Random random) {
		return random.nextInt(3) != 0;
	}

	@Override
	public ILivingEntityData onInitialSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
		this.setTypeForPosition(this, worldIn);
		this.setHunger(this.rand.nextInt(4800));
		this.setDirtiness(this.rand.nextInt(4800));
		return super.onInitialSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
	}

	public void setTypeForPosition(ChimpanzeeEntity entity, IWorld worldIn) {
		if (worldIn.getBiome(this.getPosition()).getRegistryName().getPath().contains("rainforest")) {
			entity.setChimpanzeeType(1);
		} else if (worldIn.getBiome(this.getPosition()).getRegistryName().getPath().contains("bamboo")) {
			entity.setChimpanzeeType(2);
		} else {
			entity.setChimpanzeeType(0);
		}
	}

	public boolean isMouthOpen() {
		if (this.getAction() == ChimpanzeeAction.EATING) {
			return Math.sin(Math.PI * this.ticksExisted * 0.2D) > 0;
		} else if (this.getAction() == ChimpanzeeAction.HUNCHING) {
			return false;
		} else if (this.getAction() == ChimpanzeeAction.CRYING || this.func_233678_J__() || this.isHungry() || this.isPartying()) {
			return true;
		} else {
			return false;
		}
	}

	@OnlyIn(Dist.CLIENT)
	public float getClimbingAnimationScale(float partialTicks) {
		return MathHelper.lerp(partialTicks, this.prevClimbAnim, this.climbAnim) / 8.0F;
	}

	@OnlyIn(Dist.CLIENT)
	public float getHeadShakeProgress(float partialTicks) {
		return MathHelper.lerp(partialTicks, this.prevHeadShakeAnim, this.headShakeAnim);
	}

	public boolean isBesideClimbableBlock() {
		return (this.dataManager.get(CLIMBING) & 1) != 0;
	}

	public void setBesideClimbableBlock(boolean climbing) {
		byte b0 = this.dataManager.get(CLIMBING);
		if (climbing) {
			b0 = (byte) (b0 | 1);
		} else {
			b0 = (byte) (b0 & -2);
		}

		this.dataManager.set(CLIMBING, b0);
	}

	@Override
	public EntitySize getSize(Pose pose) {
		return this.isSitting() ? this.isChild() ? SIZE_SITTING_CHILD : SIZE_SITTING : super.getSize(pose);
	}

	@Override
	public double getYOffset() {
		return this.isChild() ? -0.05D : -0.3D;
	}

	@Override
	public boolean func_230293_i_(ItemStack itemStack) {
		return this.isFood(itemStack);
	}

	@Override
	public boolean canPickUpItem(ItemStack itemstackIn) {
		if (this.pickUpTimer > 0) {
			return false;
		} else {
			EquipmentSlotType equipmentslottype = MobEntity.getSlotForItemStack(itemstackIn);
			if (!this.getItemStackFromSlot(equipmentslottype).isEmpty()) {
				return false;
			} else {
				return equipmentslottype == EquipmentSlotType.MAINHAND && super.canPickUpItem(itemstackIn);
			}
		}
	}

	@Override
	public boolean canEquipItem(ItemStack stack) {
		ItemStack itemstack = this.getItemStackFromSlot(EquipmentSlotType.MAINHAND);
		return !this.isFood(itemstack) && this.isFood(stack);
	}

	@Override
	protected void updateEquipmentIfNeeded(ItemEntity itemEntity) {
		ItemStack itemstack = itemEntity.getItem();
		if (this.canEquipItem(itemstack)) {
			int i = itemstack.getCount();
			if (i > 1) {
				ItemEntity itementity = new ItemEntity(this.world, this.getPosX(), this.getPosY(), this.getPosZ(), itemstack.split(i - 1));
				this.world.addEntity(itementity);
			}

			ItemEntity itementity1 = new ItemEntity(this.world, this.getPosX(), this.getPosYEye() - (double)0.3F, this.getPosZ(), this.getItemStackFromSlot(EquipmentSlotType.MAINHAND));
			this.world.addEntity(itementity1);

			this.triggerItemPickupTrigger(itemEntity);
			this.setItemStackToSlot(EquipmentSlotType.MAINHAND, itemstack.split(1));
			this.inventoryHandsDropChances[EquipmentSlotType.MAINHAND.getIndex()] = 2.0F;
			this.onItemPickup(itemEntity, itemstack.getCount());
			itemEntity.remove();

			this.func_241356_K__();
		}
	}

	public void eatFood() {
		if (!this.getFood().isEmpty()) {
			this.setHeldItem(this.getFoodHand(), this.getFood().onItemUseFinish(this.world, this));
		}
		this.setHunger(0);
	}

	public void swingArms() {
		this.attackTimer = 10;
	}

	public void shakeHead(IParticleData particleData) {
		this.headShakeAnim = 40;
		this.prevHeadShakeAnim = 40;

		double d0 = this.rand.nextGaussian() * 0.02D;
		double d1 = this.rand.nextGaussian() * 0.02D;
		double d2 = this.rand.nextGaussian() * 0.02D;

		this.world.addParticle(particleData, this.getPosX(), this.getPosYHeight(1.0D), this.getPosZ(), d0, d1, d2);
	}

	public Hand getFoodHand() {
		if (!this.getFood(Hand.MAIN_HAND).isEmpty()) {
			return Hand.MAIN_HAND;
		} else {
			return Hand.OFF_HAND;
		}
	}

	public ItemStack getFood() {
		if (!this.getFood(Hand.MAIN_HAND).isEmpty()) {
			return this.getFood(Hand.MAIN_HAND);
		} else if (!this.getFood(Hand.OFF_HAND).isEmpty()) {
			return this.getFood(Hand.OFF_HAND);
		}
		return ItemStack.EMPTY;
	}

	public ItemStack getFood(Hand hand) {
		ItemStack food = this.getHeldItem(hand);
		if (this.isFood(food)) {
			return food;
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean isBreedingItem(ItemStack stack) {
		return stack.getItem().isIn(NeapolitanTags.Items.CHIMPANZEE_BREEDING_ITEMS);
	}

	public boolean isFood(ItemStack stack) {
		return stack.getItem().isIn(NeapolitanTags.Items.CHIMPANZEE_FOOD);
	}

	@Override
	public ChimpanzeeEntity func_241840_a(ServerWorld world, AgeableEntity ageableEntity) {
		ChimpanzeeEntity baby = NeapolitanEntities.CHIMPANZEE.get().create(world);
		this.setTypeForPosition(baby, this.getEntityWorld());
		return baby;
	}

	public int getChimpanzeeType() {
		return MathHelper.clamp(this.dataManager.get(CHIMPANZEE_TYPE), 0, 2);
	}

	public void setChimpanzeeType(int type) {
		this.dataManager.set(CHIMPANZEE_TYPE, type);
	}

	public int getAttackTimer() {
		return this.attackTimer;
	}

	@Override
	public int getAngerTime() {
		return this.dataManager.get(ANGER_TIME);
	}

	@Override
	public void setAngerTime(int time) {
		this.dataManager.set(ANGER_TIME, time);
	}

	public int getHunger() {
		return this.dataManager.get(HUNGER);
	}

	public void setHunger(int amount) {
		this.dataManager.set(HUNGER, amount);
	}

	public boolean isHungry() {
		return this.getHunger() >= 9600;
	}

	public int getDirtiness() {
		return this.dataManager.get(DIRTINESS);
	}

	public void setDirtiness(int amount) {
		this.dataManager.set(DIRTINESS, amount);
	}

	public boolean isDirty() {
		return this.getDirtiness() >= 12000;
	}

	public void getCleaned() {
		this.setDirtiness(0);
	}

	public int getPaleness() {
		return this.dataManager.get(PALENESS);
	}

	public void setPaleness(int amount) {
		this.dataManager.set(PALENESS, amount);
	}

	public boolean needsSunlight() {
		return this.getPaleness() >= 6000;
	}

	public float getVisiblePaleness() {
		return MathHelper.clamp((this.getPaleness() - 4800.0F) / 1200.0F, 0.0F, 1.0F);
	}

	public boolean isInSunlight() {
		BlockPos blockpos = this.getRidingEntity() instanceof BoatEntity ? (new BlockPos(this.getPosX(), (double)Math.round(this.getPosY()), this.getPosZ())).up() : new BlockPos(this.getPosX(), (double)Math.round(this.getPosY()), this.getPosZ());
		return this.world.getLightFor(LightType.SKY, blockpos) > 8;
	}

	public ChimpanzeeEntity getGroomingTarget() {
		return this.groomingTarget;
	}

	public void setGroomingTarget(ChimpanzeeEntity target) {
		this.groomingTarget = target;
	}

	public ChimpanzeeEntity getGroomer() {
		return this.groomer;
	}

	public void setGroomer(ChimpanzeeEntity groomerIn) {
		this.groomer = groomerIn;
	}

	public Direction getFacing() {
		return this.dataManager.get(FACING);
	}

	public void setFacing(Direction direction) {
		this.dataManager.set(FACING, direction);
	}

	@Override
	public UUID getAngerTarget() {
		return this.lastHurtBy;
	}

	@Override
	public void setAngerTarget(UUID target) {
		this.lastHurtBy = target;
	}

	@Override
	public void func_230258_H__() {
		this.setAngerTime(ANGER_RANGE.getRandomWithinRange(this.rand));
	}

	public boolean isSitting() {
		return this.getAction().shouldSit();
	}

	public boolean isPartying() {
		return this.isPartying;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void setPartying(BlockPos pos, boolean isPartying) {
		this.jukeboxPosition = pos;
		this.isPartying = isPartying;
	}

	public ChimpanzeeAction getAction() {
		return ChimpanzeeAction.byIndex(this.dataManager.get(ACTION));
	}

	public void setAction(ChimpanzeeAction action) {
		this.dataManager.set(ACTION, (byte)action.getIndex());
	}

	public void setDefaultAction() {
		boolean flag = !this.isPassenger() && this.isClimbing();

		if (flag) {
			this.setAction(ChimpanzeeAction.CLIMBING);
		} else {
			this.setAction(ChimpanzeeAction.DEFAULT);
		}
	}

	public void setPickUpTimer(int time) {
		this.pickUpTimer = time;
	}

	class LookHelperController extends LookController {

		public LookHelperController() {
			super(ChimpanzeeEntity.this);
		}

		public void tick() {
			if (this.shouldResetPitch()) {
				ChimpanzeeEntity.this.rotationPitch = 0.0F;
			}

			if (this.isLooking) {
				this.isLooking = false;
				ChimpanzeeEntity.this.rotationYawHead = this.clampedRotate(ChimpanzeeEntity.this.rotationYawHead, this.getTargetYaw(), this.deltaLookYaw);
				ChimpanzeeEntity.this.rotationPitch = this.clampedRotate(ChimpanzeeEntity.this.rotationPitch, this.getTargetPitch(), this.deltaLookPitch);
			} else {
				ChimpanzeeEntity.this.rotationYawHead = this.clampedRotate(ChimpanzeeEntity.this.rotationYawHead, ChimpanzeeEntity.this.renderYawOffset, 10.0F);
			}

			Direction facing = ChimpanzeeEntity.this.getFacing();
			if (!ChimpanzeeEntity.this.getNavigator().noPath()) {
				ChimpanzeeEntity.this.rotationYawHead = MathHelper.func_219800_b(ChimpanzeeEntity.this.rotationYawHead, ChimpanzeeEntity.this.renderYawOffset, (float)ChimpanzeeEntity.this.getHorizontalFaceSpeed());
			} else if (ChimpanzeeEntity.this.isClimbing() && facing != Direction.DOWN) {
				ChimpanzeeEntity.this.rotationYawHead = MathHelper.func_219800_b(ChimpanzeeEntity.this.rotationYawHead, facing.getHorizontalAngle(), (float)ChimpanzeeEntity.this.getHorizontalFaceSpeed());
			}
		}
	}

	class BodyHelperController extends BodyController {
		private int bodyRotationTickCounter;
		private Direction prevFacing;

		public BodyHelperController() {
			super(ChimpanzeeEntity.this);
		}

		public void updateRenderAngles() {
			super.updateRenderAngles();

			Direction facing = ChimpanzeeEntity.this.getFacing();

			if (facing != this.prevFacing || !ChimpanzeeEntity.this.isClimbing()) {
				this.bodyRotationTickCounter = 10;
			}

			this.prevFacing = facing;

			if (facing != Direction.DOWN && ChimpanzeeEntity.this.isClimbing()) {
				int i = this.bodyRotationTickCounter;
				float f = MathHelper.clamp((float)i / 10.0F, 0.0F, 1.0F);
				float f1 = 90.0F * f;
				ChimpanzeeEntity.this.renderYawOffset = MathHelper.func_219800_b(ChimpanzeeEntity.this.renderYawOffset, facing.getHorizontalAngle(), f1);

				if (this.bodyRotationTickCounter > 0) {
					--this.bodyRotationTickCounter;
				}
			}
		}
	}
}
