package com.minecraftabnormals.neapolitan.common.block;

import com.minecraftabnormals.neapolitan.core.other.NeapolitanCriteriaTriggers;
import com.minecraftabnormals.neapolitan.core.registry.NeapolitanItems;
import net.minecraft.block.*;
import net.minecraft.entity.CreatureAttribute;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.RavagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.IPlantable;

import javax.annotation.Nullable;
import java.util.Random;

public class StrawberryBushBlock extends BushBlock implements IPlantable, IGrowable {
	public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 6);
	public static final EnumProperty<StrawberryType> TYPE = EnumProperty.create("type", StrawberryType.class);
	private static final VoxelShape[] SHAPE_BY_AGE = new VoxelShape[]{
			Block.makeCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 2.0D, 14.0D),
			Block.makeCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 6.0D, 14.0D),
			Block.makeCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 8.0D, 14.0D),
			Block.makeCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 10.0D, 14.0D),
			Block.makeCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 12.0D, 14.0D),
			Block.makeCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 14.0D, 14.0D),
			Block.makeCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 14.0D, 14.0D)
	};

	public StrawberryBushBlock(Properties properties) {
		super(properties);
		this.setDefaultState(this.stateContainer.getBaseState().with(AGE, 0).with(TYPE, StrawberryType.NONE));
	}

	@Override
	public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
		int age = state.get(AGE);
		boolean fullyGrown = age == this.getMaxAge();
		if (!fullyGrown && player.getHeldItem(handIn).getItem() == Items.BONE_MEAL) {
			return ActionResultType.PASS;
		} else if (fullyGrown) {
			int strawberryCount = 1 + worldIn.rand.nextInt(2);
			Item strawberry = state.get(TYPE) == StrawberryType.WHITE ? NeapolitanItems.WHITE_STRAWBERRIES.get() : NeapolitanItems.STRAWBERRIES.get();
			spawnAsEntity(worldIn, pos, new ItemStack(strawberry, strawberryCount));
			worldIn.playSound(null, pos, SoundEvents.ITEM_SWEET_BERRIES_PICK_FROM_BUSH, SoundCategory.BLOCKS, 1.0F, 0.8F + worldIn.rand.nextFloat() * 0.4F);
			worldIn.setBlockState(pos, state.with(AGE, 1).with(TYPE, StrawberryType.NONE), 2);
			if (player instanceof ServerPlayerEntity)
				NeapolitanCriteriaTriggers.HARVEST_STRAWBERRIES.trigger((ServerPlayerEntity) player, state);
			return ActionResultType.func_233537_a_(worldIn.isRemote);
		} else {
			return super.onBlockActivated(state, worldIn, pos, player, handIn, hit);
		}
	}

	protected int getBonemealAgeIncrease(World worldIn) {
		return MathHelper.nextInt(worldIn.rand, 2, 5);
	}

	public void tick(BlockState state, ServerWorld worldIn, BlockPos pos, Random rand) {
		super.tick(state, worldIn, pos, rand);
		if (!worldIn.isAreaLoaded(pos, 1))
			return;
		if (worldIn.getLightSubtracted(pos, 0) >= 13) {
			int age = this.getAge(state);
			int maxAgeForPos = worldIn.getBlockState(pos.down()).isIn(Blocks.COARSE_DIRT) ? 2 : this.getMaxAge();
			int growthChance = !worldIn.isRaining() ? 7 : 5;
			if (age < maxAgeForPos) {
				if (net.minecraftforge.common.ForgeHooks.onCropsGrowPre(worldIn, pos, state, rand.nextInt(growthChance) == 0)) {
					if (age != 5) {
						worldIn.setBlockState(pos, this.withAge(age + 1), 2);
					} else {
						worldIn.setBlockState(pos, this.withAge(age + 1).with(TYPE, this.isWhite(worldIn, pos) ? StrawberryType.WHITE : StrawberryType.RED), 2);
					}
					net.minecraftforge.common.ForgeHooks.onCropsGrowPost(worldIn, pos, state);
				}
			}
		}

	}

	public void onEntityCollision(BlockState state, World worldIn, BlockPos pos, Entity entityIn) {
		if (worldIn.rand.nextInt(15) == 0) {
			if (entityIn.lastTickPosX != entityIn.getPosX() || entityIn.lastTickPosZ != entityIn.getPosZ()) {
				double d0 = Math.abs(entityIn.getPosX() - entityIn.lastTickPosX);
				double d1 = Math.abs(entityIn.getPosZ() - entityIn.lastTickPosZ);
				if (d0 >= (double) 0.003F || d1 >= (double) 0.003F) {
					worldIn.playSound((PlayerEntity) null, pos, SoundEvents.BLOCK_GRASS_STEP, SoundCategory.BLOCKS, 1.5F, 0.8F + worldIn.rand.nextFloat() * 0.4F);
				}
			}
		}
		if (entityIn instanceof RavagerEntity && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(worldIn, entityIn)) {
			worldIn.destroyBlock(pos, true, entityIn);
		}
		if (entityIn instanceof LivingEntity) {
			LivingEntity entity = (LivingEntity) entityIn;
			if (entity.getCreatureAttribute() == CreatureAttribute.ARTHROPOD && state.get(AGE) > 0) {
				entity.addPotionEffect(new EffectInstance(Effects.INVISIBILITY, 3, 0, false, false, false));
			}
		}
		super.onEntityCollision(state, worldIn, pos, entityIn);
	}

	@Nullable
	@Override
	public PathNodeType getAiPathNodeType(BlockState state, IBlockReader world, BlockPos pos, @Nullable MobEntity entity) {
		if (entity instanceof CreeperEntity)
			return PathNodeType.DANGER_OTHER;
		return super.getAiPathNodeType(state, world, pos, entity);
	}

	protected IItemProvider getSeedsItem() {
		return NeapolitanItems.STRAWBERRY_PIPS.get();
	}

	public ItemStack getItem(IBlockReader worldIn, BlockPos pos, BlockState state) {
		return new ItemStack(this.getSeedsItem());
	}

	protected int getAge(BlockState state) {
		return state.get(this.getAgeProperty());
	}

	public BlockState withAge(int age) {
		return this.getDefaultState().with(this.getAgeProperty(), age);
	}

	public boolean isMaxAge(BlockState state) {
		return state.get(this.getAgeProperty()) >= this.getMaxAge();
	}

	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
		builder.add(AGE, TYPE);
	}

	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		return SHAPE_BY_AGE[state.get(this.getAgeProperty())];
	}

	public IntegerProperty getAgeProperty() {
		return AGE;
	}

	public int getMaxAge() {
		return 6;
	}

	private boolean isWhite(ServerWorld worldIn, BlockPos pos) {
		return (pos.getY() >= 200 && worldIn.getDimensionKey() == World.OVERWORLD) || worldIn.getDimensionKey() == World.THE_END;
	}

	@Override
	public boolean canGrow(IBlockReader block, BlockPos pos, BlockState state, boolean isClient) {
		return !this.isMaxAge(state);
	}

	@Override
	public boolean canUseBonemeal(World world, Random rand, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void grow(ServerWorld worldIn, Random rand, BlockPos pos, BlockState state) {
		int age = Math.min(this.getAge(state) + this.getBonemealAgeIncrease(worldIn), this.getMaxAge());
		if (age != 6) {
			worldIn.setBlockState(pos, this.withAge(age), 2);
		} else {
			worldIn.setBlockState(pos, this.withAge(age).with(TYPE, this.isWhite(worldIn, pos) ? StrawberryType.WHITE : StrawberryType.RED), 2);
		}
	}

	public static enum StrawberryType implements IStringSerializable {
		NONE("none"), RED("red"), WHITE("white");

		private final String name;

		private StrawberryType(String name) {
			this.name = name;
		}

		@Override
		public String getString() {
			return this.name;
		}
	}
}
