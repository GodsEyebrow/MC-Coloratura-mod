package de.oculus.coloratura.block.entity;

import de.oculus.coloratura.ColoraturaMod;
import de.oculus.coloratura.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlockEntities {

	public static final BlockEntityType<KlangBlockEntity> KLANGBLOCK_ENTITY =
			FabricBlockEntityTypeBuilder.create(KlangBlockEntity::new, ModBlocks.KLANGBLOCK).build();

	private ModBlockEntities() {
	}

	public static void register() {
		Registry.register(Registries.BLOCK_ENTITY_TYPE,
				Identifier.of(ColoraturaMod.MOD_ID, "klangblock_entity"), KLANGBLOCK_ENTITY);
	}
}
