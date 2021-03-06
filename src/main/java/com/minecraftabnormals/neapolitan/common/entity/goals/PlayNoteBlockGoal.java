package com.minecraftabnormals.neapolitan.common.entity.goals;

import java.util.EnumSet;

import com.minecraftabnormals.neapolitan.common.entity.ChimpanzeeEntity;
import com.minecraftabnormals.neapolitan.common.entity.util.ChimpanzeeAction;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NoteBlock;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

public class PlayNoteBlockGoal extends MoveToBlockGoal {
	private final ChimpanzeeEntity chimpanzee;
	private int timePlayed;
	private int noteTime;

	public PlayNoteBlockGoal(ChimpanzeeEntity chimpanzeeIn, double speed, int length) {
		super(chimpanzeeIn, speed, length, 6);
		this.chimpanzee = chimpanzeeIn;
		this.setMutexFlags(EnumSet.of(Goal.Flag.LOOK, Goal.Flag.JUMP, Goal.Flag.MOVE));
	}

	@Override
	public boolean shouldExecute() {
		return this.chimpanzee.getRNG().nextInt(400) == 0 && !this.chimpanzee.isPassenger() && super.shouldExecute();
	}

	@Override
	public boolean shouldContinueExecuting() {
		if (this.timePlayed > 320 && this.chimpanzee.getRNG().nextInt(300) == 0) {
			return false;
		} else if (this.isBlockBeingPlayed(this.chimpanzee.world, this.destinationBlock.up())) {
			return false;
		} else if (this.chimpanzee.isPassenger()) {
			return false;
		} else if (this.timePlayed > 0 && !this.getIsAboveDestination()) {
			return false;
		} else {
			return super.shouldContinueExecuting();
		}
	}

	@Override
	public void startExecuting() {
		super.startExecuting();
		this.timePlayed = 0;
		this.noteTime = 20;
	}

	@Override
	public void resetTask() {
		super.resetTask();
		this.chimpanzee.setDefaultAction();
	}

	@Override
	public void tick() {
		super.tick();

		this.chimpanzee.getLookController().setLookPosition(this.destinationBlock.getX() + 0.5D, this.destinationBlock.getY() + 0.5D, this.destinationBlock.getZ() + 0.5D, (float)(this.chimpanzee.getHorizontalFaceSpeed() + 20), (float)this.chimpanzee.getVerticalFaceSpeed());

		if (this.getIsAboveDestination() && this.chimpanzee.getNavigator().noPath() && this.chimpanzee.getAction().canBeInterrupted()) {
			this.chimpanzee.setAction(ChimpanzeeAction.DRUMMING);

			if (--this.noteTime <= 0) {
				BlockState state = this.chimpanzee.world.getBlockState(this.destinationBlock);
				int note = state.get(NoteBlock.NOTE);
				note = MathHelper.clamp(note + this.chimpanzee.getRNG().nextInt(7) - 3, 0, 24);
				this.chimpanzee.world.setBlockState(this.destinationBlock, state.with(NoteBlock.NOTE, note), 3);
				this.chimpanzee.world.addBlockEvent(this.destinationBlock, Blocks.NOTE_BLOCK, 0, 0);

				this.noteTime = 8 + this.chimpanzee.getRNG().nextInt(5);
			}

			++this.timePlayed;
		} else {
			this.chimpanzee.setDefaultAction();
		}
	}

	@Override
	protected boolean shouldMoveTo(IWorldReader worldIn, BlockPos pos) {
		return worldIn.isAirBlock(pos.up()) && worldIn.isAirBlock(pos.up().up()) && worldIn.getBlockState(pos).getBlock() == Blocks.NOTE_BLOCK && !this.isBlockBeingPlayed((World) worldIn, pos);
	}

	private boolean isBlockBeingPlayed(World worldIn, BlockPos pos) {
		return !worldIn.getEntitiesWithinAABB(ChimpanzeeEntity.class, new AxisAlignedBB(pos.up()), (chimpanzee) -> {
			return chimpanzee != this.chimpanzee && chimpanzee.getAction() == ChimpanzeeAction.DRUMMING;
		}).isEmpty();
	}
}
