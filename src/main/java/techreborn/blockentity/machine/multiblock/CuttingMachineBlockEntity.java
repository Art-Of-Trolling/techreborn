/*
 * This file is part of TechReborn, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2020 TechReborn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package techreborn.blockentity.machine.multiblock;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;
import reborncore.client.screen.BuiltScreenHandlerProvider;
import reborncore.client.screen.builder.BuiltScreenHandler;
import reborncore.client.screen.builder.ScreenHandlerBuilder;
import reborncore.common.blockentity.MultiblockWriter;
import reborncore.common.fluid.FluidValue;
import reborncore.common.recipes.RecipeCrafter;
import reborncore.common.util.RebornInventory;
import reborncore.common.util.Tank;
import techreborn.blockentity.machine.GenericMachineBlockEntity;
import techreborn.config.TechRebornConfig;
import techreborn.init.*;
import techreborn.utils.FluidUtils;

public class CuttingMachineBlockEntity extends GenericMachineBlockEntity implements BuiltScreenHandlerProvider {

	public static final FluidValue TANK_CAPACITY = FluidValue.BUCKET.multiply(16);
	public Tank tank;
	int ticksSinceLastChange;

	public CuttingMachineBlockEntity() {
			super(TRBlockEntities.CUTTING_MACHINE, "CuttingMachine", TechRebornConfig.cuttingMachineMaxInput, TechRebornConfig.cuttingMachineMaxEnergy, TRContent.Machine.CUTTING_MACHINE.block, 4);
			final int[] inputs = new int[]{0, 1};
			final int[] outputs = new int[]{2};
			this.inventory = new RebornInventory<>(5, "CuttingMachineBlockEntity", 64, this);
			this.crafter = new RecipeCrafter(ModRecipes.CUTTING_MACHINE, this, 1, 1, this.inventory, inputs, outputs);
			this.tank = new Tank("CuttingMachineBlockEntity", CuttingMachineBlockEntity.TANK_CAPACITY, this);
			this.ticksSinceLastChange = 0;
		}


	@Override
	public void writeMultiblock(MultiblockWriter writer) {
		BlockState advanced = TRContent.MachineBlocks.ADVANCED.getCasing().getDefaultState();
		BlockState industrial = TRContent.MachineBlocks.INDUSTRIAL.getCasing().getDefaultState();
		writer.translate(1, -1, -1)
				.fill(0, 0, 0, 3, 1, 3, advanced)
				.ring(Direction.Axis.Y, 3, 1, 3, (view, pos) -> view.getBlockState(pos) == industrial, industrial, (view, pos) -> view.getBlockState(pos).getMaterial() == Material.WATER, Blocks.WATER.getDefaultState())
				.fill(0, 2, 0, 3, 3, 3, advanced);
	}

	// TilePowerAcceptor
	@Override
	public void tick() {
		if (world == null){
			return;
		}
		ticksSinceLastChange++;
		// Check cells input slot 2 time per second
		if (!world.isClient && ticksSinceLastChange >= 10) {
			if (!inventory.getStack(1).isEmpty()) {
				FluidUtils.drainContainers(tank, inventory, 1, 3);
				FluidUtils.fillContainers(tank, inventory, 1, 3);
			}
			ticksSinceLastChange = 0;
		}

		super.tick();
	}

	@Override
	public void fromTag(BlockState blockState, final CompoundTag tagCompound) {
		super.fromTag(blockState, tagCompound);
		tank.read(tagCompound);
	}

	@Override
	public CompoundTag toTag(final CompoundTag tagCompound) {
		super.toTag(tagCompound);
		tank.write(tagCompound);
		return tagCompound;
	}

	// TileMachineBase
	@Nullable
	@Override
	public Tank getTank() {
		return tank;
	}

	// IContainerProvider
	@Override
	public BuiltScreenHandler createScreenHandler(int syncID, final PlayerEntity player) {
		// fluidSlot first to support automation and shift-click
		return new ScreenHandlerBuilder("cuttingmachine").player(player.inventory).inventory().hotbar().addInventory()
				.blockEntity(this)
				.fluidSlot(1, 34, 35)
				.slot(0, 84, 43)
				.outputSlot(2, 126, 43)
				.outputSlot(3, 34, 55)
				.energySlot(4, 8, 72)
				.sync(tank).syncEnergyValue().syncCrafterValue().addInventory().create(this, syncID);
	}

}