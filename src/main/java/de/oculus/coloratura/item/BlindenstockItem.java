package de.oculus.coloratura.item;

import de.oculus.coloratura.block.entity.KlangBlockEntity;
import de.oculus.coloratura.network.ColoraturaNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Der Blindenstock ist die zentrale Mechanik nach dem Vorbild "Blind Touch":
 * anders als der fruehere Resonanzkompass (Coloratura-Prinzip: 360-Grad-Ping,
 * dauerhaftes Gedaechtnis) funktioniert der Stock wie in Blind Touch ueber
 * einen kurzen, KEGELFOERMIGEN Schwung direkt vor dem Spieler - man "tippt"
 * mit dem Stock in Blickrichtung und ertastet nur, was in diesem schmalen
 * Bereich vor einem liegt. Objekte ausserhalb des Kegels oder ausserhalb der
 * kurzen Reichweite (wie bei einem echten Blindenstock) bleiben unentdeckt.
 *
 * Die Entdeckung ist ausserdem NICHT dauerhaft (kein Radar-Gedaechtnis mehr):
 * der Client zeigt die getroffenen Objekte nur fuer ein paar Sekunden an,
 * genau wie die kurze "Sonar-Ripple"-Enthuellung in Blind Touch. Danach
 * verschwindet die Anzeige wieder, und man muss erneut tippen.
 */
public class BlindenstockItem extends Item {

	/** Reichweite eines echten Gehstocks - bewusst kurz, im Gegensatz zum
	 * frueheren 24-Block-Kompass-Radius. */
	private static final double REICHWEITE = 6.0;

	/** Oeffnungswinkel des Tast-Kegels in Grad (gesamt, also +/- 30 Grad
	 * um die Blickrichtung). Ein echter Stock-Schwung ist eng, kein Rundum-Sonar. */
	private static final double KEGEL_HALBWINKEL_GRAD = 35.0;

	/** Kurze Abklingzeit - man tippt in Blind Touch schnell hintereinander,
	 * nicht alle paar Sekunden wie beim alten Kompass-Ping. */
	private static final int ABKLINGZEIT_TICKS = 8;

	public BlindenstockItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<net.minecraft.item.ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
			user.getItemCooldownManager().set(this, ABKLINGZEIT_TICKS);

			// Kurzes, trockenes "Tipp"-Geraeusch des Stocks selbst (nicht zu verwechseln
			// mit den Klaengen der ertasteten Objekte).
			world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_WOOD_HIT,
					SoundCategory.PLAYERS, 0.5f, 1.8f);

			List<KlangBlockEntity> ertastet = tasteKegelAb(world, user);
			if (!ertastet.isEmpty()) {
				ColoraturaNetworking.sendeEntdeckungen(serverPlayer, ertastet);
			}
		}
		return TypedActionResult.success(user.getStackInHand(hand), world.isClient);
	}

	/**
	 * Prueft alle KlangBlockEntities in einem Wuerfel um den Spieler und
	 * behaelt nur jene, die (a) innerhalb der kurzen Stock-Reichweite UND
	 * (b) innerhalb des engen Blickrichtungs-Kegels liegen - der Kern des
	 * Unterschieds zum alten Rundum-Kompass.
	 */
	private List<KlangBlockEntity> tasteKegelAb(World world, PlayerEntity user) {
		List<KlangBlockEntity> ergebnis = new ArrayList<>();
		Vec3d spielerPos = user.getEyePos();
		Vec3d blickrichtung = user.getRotationVector().normalize();
		double kegelKosinusGrenze = Math.cos(Math.toRadians(KEGEL_HALBWINKEL_GRAD));

		BlockPos.iterate(
				BlockPos.ofFloored(spielerPos.subtract(REICHWEITE, REICHWEITE, REICHWEITE)),
				BlockPos.ofFloored(spielerPos.add(REICHWEITE, REICHWEITE, REICHWEITE))
		).forEach(pos -> {
			if (!(world.getBlockEntity(pos) instanceof KlangBlockEntity entity)) {
				return;
			}

			Vec3d zumBlock = Vec3d.ofCenter(pos).subtract(spielerPos);
			double distanz = zumBlock.length();
			if (distanz > REICHWEITE || distanz < 0.001) {
				return;
			}

			double kosinusWinkel = blickrichtung.dotProduct(zumBlock.normalize());
			if (kosinusWinkel >= kegelKosinusGrenze) {
				ergebnis.add(entity);
			}
		});
		return ergebnis;
	}
}
