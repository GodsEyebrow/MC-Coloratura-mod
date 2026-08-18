package de.oculus.coloratura.block.entity;

import de.oculus.coloratura.block.KlangBlock;
import de.oculus.coloratura.puzzle.KlangSequenzManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Herzstueck des Mods.
 *
 * "note"        - Tonhoehe (0..24, wie ein Notenblock) = Identitaet dieses Klangs.
 *                 Zwei Klangbloecke mit unterschiedlicher note klingen hoerbar
 *                 unterschiedlich - das ist die einzige Information, die man zur
 *                 Loesung der Raetsel braucht (kein visueller Hinweis noetig).
 * "gruppeId"    - Verknuepft mehrere Klangbloecke + eine Resonanztuer zu einem
 *                 gemeinsamen Raetsel. Wird per NBT/Command gesetzt (siehe unten).
 * "sequenzIndex"- Position dieses Blocks in der korrekten Aktivierungsreihenfolge
 *                 innerhalb seiner Gruppe (0 = zuerst).
 * "aktiviert"   - Ob dieser Block in der aktuellen Raetsel-Runde schon "dran war".
 *
 * Ambientes Verhalten: alle PULS_INTERVALL Ticks spielt der Block seinen Ton leise
 * ab. Minecraft berechnet Lautstaerke/Stereo-Richtung fuer den Spieler automatisch
 * anhand der Entfernung/Position - das ist exakt das 3D-Audio-Prinzip aus Coloratura,
 * wir muessen es nicht selbst nachbauen.
 *
 * Setup eines Raetsels (Beispiel mit 3 Klangbloecken + einer Tuer):
 *   /data merge block <x1> <y1> <z1> {gruppeId:"raum1", sequenzIndex:0, note:5}
 *   /data merge block <x2> <y2> <z2> {gruppeId:"raum1", sequenzIndex:1, note:9}
 *   /data merge block <x3> <y3> <z3> {gruppeId:"raum1", sequenzIndex:2, note:14}
 * und die Resonanztuer registriert sich beim Laden automatisch fuer ihre gruppeId
 * (siehe ResonanzTuerBlockEntity) - dafuer muesste man das Konzept um eine eigene
 * BlockEntity fuer die Tuer erweitern; hier der Einfachheit halber ueber
 * KlangSequenzManager.registriereTuer() manuell/per Command:
 *   /coloratura link raum1 <tuerX> <tuerY> <tuerZ>
 */
public class KlangBlockEntity extends BlockEntity {

	private static final int PULS_INTERVALL = 100; // 5 Sekunden bei 20 TPS
	private static final int ABKUEHLUNG_TICKS = 20; // Spam-Schutz bei Aktivierung

	private int note = 0;
	private String gruppeId = "";
	private int sequenzIndex = -1;
	private boolean aktiviert = false;

	private int pulsTimer = 0;
	private int abkuehlung = 0;

	public KlangBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.KLANGBLOCK_ENTITY, pos, state);
	}

	public static void tick(World world, BlockPos pos, BlockState state, KlangBlockEntity entity) {
		if (entity.abkuehlung > 0) {
			entity.abkuehlung--;
		}

		entity.pulsTimer++;
		if (entity.pulsTimer >= PULS_INTERVALL) {
			entity.pulsTimer = 0;
			entity.pulsAbspielen(world, pos);
		}
	}

	private void pulsAbspielen(World world, BlockPos pos) {
		float tonhoehe = notenNummerZuPitch(note);
		// Leiser ambienter Puls - dient als "Radar-Ping", den man aus der Ferne hoert
		// und dessen Richtung/Distanz einem sagt, wo sich der Block befindet.
		world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.BLOCKS, 0.6f, tonhoehe);
	}

	/**
	 * Wird ausgeloest, wenn ein Spieler den Block per Rechtsklick "anspielt".
	 * Das ist die Kernhandlung des Klang-Raetsels: der Spieler hoert den Ton,
	 * merkt sich (bzw. das Spiel merkt sich per HUD) die Tonhoehe und muss die
	 * Bloecke einer Gruppe in aufsteigender sequenzIndex-Reihenfolge aktivieren.
	 */
	public void aktivierenDurchSpieler(PlayerEntity player) {
		if (world == null || world.isClient) {
			return;
		}
		if (abkuehlung > 0) {
			return;
		}
		abkuehlung = ABKUEHLUNG_TICKS;

		aktiviert = true;
		markDirty();
		world.setBlockState(pos, getCachedState().with(KlangBlock.AKTIVIERT, true), 3);

		float tonhoehe = notenNummerZuPitch(note);
		world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.BLOCKS, 1.0f, tonhoehe);

		if (!gruppeId.isEmpty() && sequenzIndex >= 0) {
			KlangSequenzManager.onAktivierung((net.minecraft.server.world.ServerWorld) world, gruppeId, sequenzIndex, player);
		}
	}

	/** Setzt diesen Block zurueck, z.B. wenn die Sequenz seiner Gruppe fehlschlaegt. */
	public void zuruecksetzen() {
		aktiviert = false;
		if (world != null && !world.isClient) {
			markDirty();
			world.setBlockState(pos, getCachedState().with(KlangBlock.AKTIVIERT, false), 3);
		}
	}

	private static float notenNummerZuPitch(int note) {
		// Analog zum vanilla Notenblock: 2^((note-12)/12)
		return (float) Math.pow(2.0, (note - 12) / 12.0);
	}

	public int getNote() {
		return note;
	}

	public String getGruppeId() {
		return gruppeId;
	}

	public int getSequenzIndex() {
		return sequenzIndex;
	}

	// Hinweis: die genaue Signatur von writeNbt/readNbt hat sich zwischen
	// Minecraft-Versionen mehrfach geaendert (in 1.20.1 ohne RegistryWrapper-Parameter,
	// in spaeteren Versionen mit). Bitte gegen die tatsaechliche BlockEntity-Basisklasse
	// im Loom-Mappings-Cache abgleichen und ggf. anpassen.
	@Override
	public void writeNbt(NbtCompound nbt) {
		super.writeNbt(nbt);
		nbt.putInt("note", note);
		nbt.putString("gruppeId", gruppeId);
		nbt.putInt("sequenzIndex", sequenzIndex);
		nbt.putBoolean("aktiviert", aktiviert);
	}

	@Override
	public void readNbt(NbtCompound nbt) {
		super.readNbt(nbt);
		note = nbt.getInt("note");
		gruppeId = nbt.getString("gruppeId");
		sequenzIndex = nbt.getInt("sequenzIndex");
		aktiviert = nbt.getBoolean("aktiviert");
	}
}
