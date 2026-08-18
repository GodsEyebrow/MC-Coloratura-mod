package de.oculus.coloratura.item;

import de.oculus.coloratura.ColoraturaMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {

	public static final Item RESONANZKOMPASS = new ResonanzKompassItem(
			new Item.Settings().maxCount(1)
	);

	private ModItems() {
	}

	public static void register() {
		Registry.register(Registries.ITEM, Identifier.of(ColoraturaMod.MOD_ID, "resonanzkompass"), RESONANZKOMPASS);

		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(RESONANZKOMPASS));
	}
}
