package de.oculus.coloratura.block;

import de.oculus.coloratura.ColoraturaMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public final class ModBlocks {

	public static final Block KLANGBLOCK = new KlangBlock(
			AbstractBlock.Settings.create()
					.strength(1.5f)
					.sounds(BlockSoundGroup.AMETHYST_BLOCK)
					.nonOpaque()
	);

	public static final Block RESONANZ_TUER = new ResonanzTuerBlock(
			AbstractBlock.Settings.create()
					.strength(3.0f)
					.sounds(BlockSoundGroup.DEEPSLATE)
	);

	private ModBlocks() {
	}

	public static void register() {
		register("klangblock", KLANGBLOCK);
		register("resonanz_tuer", RESONANZ_TUER);

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register(entries -> {
			entries.add(KLANGBLOCK);
			entries.add(RESONANZ_TUER);
		});
	}

	private static void register(String path, Block block) {
		Identifier id = Identifier.of(ColoraturaMod.MOD_ID, path);
		Registry.register(Registries.BLOCK, id, block);
		Registry.register(Registries.ITEM, id, new BlockItem(block, new net.minecraft.item.Item.Settings()));
	}
}
