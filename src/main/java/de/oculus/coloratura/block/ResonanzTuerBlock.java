package de.oculus.coloratura.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * Die Resonanztuer wird nicht per Hand geoeffnet, sondern ausschliesslich durch
 * das erfolgreiche Loesen eines Klang-Raetsels (siehe KlangSequenzManager).
 * Solange OFFEN=false ist sie eine solide Wand; ist sie offen, hat sie keine
 * Kollision mehr (VoxelShapes.empty()), sodass man hindurchgehen kann.
 */
public class ResonanzTuerBlock extends Block {

	public static final BooleanProperty OFFEN = BooleanProperty.of("offen");

	public ResonanzTuerBlock(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState().with(OFFEN, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(OFFEN);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return state.get(OFFEN) ? VoxelShapes.empty() : VoxelShapes.fullCube();
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return getOutlineShape(state, world, pos, context);
	}

	public static void oeffnen(World world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (state.isOf(de.oculus.coloratura.block.ModBlocks.RESONANZ_TUER)) {
			world.setBlockState(pos, state.with(OFFEN, true), Block.NOTIFY_ALL);
			world.playSound(null, pos, net.minecraft.sound.SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
					net.minecraft.sound.SoundCategory.BLOCKS, 1.0f, 0.8f);
		}
	}

	public static void schliessen(World world, BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		if (state.isOf(de.oculus.coloratura.block.ModBlocks.RESONANZ_TUER)) {
			world.setBlockState(pos, state.with(OFFEN, false), Block.NOTIFY_ALL);
		}
	}
}
