package de.oculus.coloratura.block;

import de.oculus.coloratura.block.entity.KlangBlockEntity;
import de.oculus.coloratura.block.entity.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Ein Klangblock ist die Kernmechanik des Mods: er sendet periodisch einen
 * positionalen Ton aus (siehe {@link KlangBlockEntity}) und laesst sich mit dem
 * Resonanzkompass oder per Rechtsklick "aktivieren", was ihn Teil eines
 * Klang-Raetsels macht (siehe puzzle-Paket).
 *
 * Die AKTIVIERT-Eigenschaft steuert rein optisch/akustisch die Rueckmeldung
 * (anderes Modell + hellerer Ton), die eigentliche Logik liegt in der BlockEntity.
 */
public class KlangBlock extends BlockWithEntity {

	public static final BooleanProperty AKTIVIERT = BooleanProperty.of("aktiviert");

	private static final VoxelShape SHAPE = VoxelShapes.cuboid(0.05, 0.0, 0.05, 0.95, 1.0, 0.95);

	public KlangBlock(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState().with(AKTIVIERT, false));
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(AKTIVIERT);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		// Hinweis: je nach genauer Mappings-Version (Yarn build) kann die Signatur
		// von onUse leicht abweichen (z.B. ohne Hand-Parameter in neueren Buildscripts).
		// Bei Compile-Fehlern bitte gegen die im Loom-Cache generierten Sources abgleichen.
		if (!world.isClient && world.getBlockEntity(pos) instanceof KlangBlockEntity entity) {
			entity.aktivierenDurchSpieler(player);
		}
		return ActionResult.SUCCESS;
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new KlangBlockEntity(pos, state);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		// checkType ist bereits von BlockWithEntity geerbt - eine eigene
		// gleichnamige Methode wuerde (wie hier zuvor) zu einer Mehrdeutigkeit
		// beim Compiler fuehren.
		return checkType(type, ModBlockEntities.KLANGBLOCK_ENTITY, KlangBlockEntity::tick);
	}
}
