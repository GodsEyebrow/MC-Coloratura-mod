package de.oculus.coloratura.item;

import de.oculus.coloratura.block.entity.KlangBlockEntity;
import de.oculus.coloratura.block.entity.ModBlockEntities;
import de.oculus.coloratura.network.ColoraturaNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Der Resonanzkompass ist die aktive Ortungs-Mechanik: statt passiv auf die
 * periodischen Klangpulse der Klangbloecke zu warten, kann der Spieler
 * gezielt "anpingen". Alle Klangbloecke im Radius werden geortet, dem
 * Client als Liste (Position + Tonhoehe) per Paket mitgeteilt, und dort im
 * "Gedaechtnis" (siehe ColoraturaClient) fuer die Radar-HUD-Anzeige gespeichert -
 * exakt das Radar/Memory-System aus Coloratura, nur auf Minecraft uebertragen.
 */
public class ResonanzKompassItem extends Item {

	private static final int REICHWEITE = 24;
	private static final int ABKLINGZEIT_TICKS = 40; // 2 Sekunden

	public ResonanzKompassItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<net.minecraft.item.ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
			user.getItemCooldownManager().set(this, ABKLINGZEIT_TICKS);

			world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_BEACON_AMBIENT,
					SoundCategory.PLAYERS, 0.8f, 1.6f);

			List<KlangBlockEntity> gefunden = ortePings(world, user.getPos());
			if (!gefunden.isEmpty()) {
				ColoraturaNetworking.sendeEntdeckungen(serverPlayer, gefunden);
			}
		}
		return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
	}

	private List<KlangBlockEntity> ortePings(World world, Vec3d center) {
		List<KlangBlockEntity> ergebnis = new ArrayList<>();
		BlockPos.iterate(
				BlockPos.ofFloored(center.subtract(REICHWEITE, REICHWEITE, REICHWEITE)),
				BlockPos.ofFloored(center.add(REICHWEITE, REICHWEITE, REICHWEITE))
		).forEach(pos -> {
			// Hinweis: Das direkte Iterieren ueber einen wuerfelfoermigen Bereich ist fuer
			// eine Demo/Prototyp okay, bei grossem Radius aber teuer. Fuer produktiven
			// Einsatz stattdessen eine Liste aller platzierten Klangbloecke pro Chunk/Welt
			// fuehren (aehnlich wie KlangSequenzManager.registriereKlangblock) und nur
			// darueber filtern.
			if (pos.isWithinDistance(center, REICHWEITE) && world.getBlockEntity(pos) instanceof KlangBlockEntity entity) {
				ergebnis.add(entity);
			}
		});
		return ergebnis;
	}
}
