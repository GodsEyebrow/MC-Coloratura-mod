package de.oculus.coloratura.puzzle;

import de.oculus.coloratura.ColoraturaMod;
import de.oculus.coloratura.block.ResonanzTuerBlock;
import de.oculus.coloratura.block.entity.KlangBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.*;

/**
 * Verwaltet den Fortschritt aller Klang-Raetsel-Gruppen serverseitig.
 *
 * Prinzip: jede Gruppe (gruppeId) hat eine Menge von Klangbloecken mit
 * sequenzIndex 0..n-1. Werden sie in exakt dieser Reihenfolge aktiviert
 * (rein am Klang/Tonhoehe erkennbar - keine visuelle Kennzeichnung noetig),
 * oeffnet sich die verknuepfte Resonanztuer. Eine falsche Reihenfolge setzt
 * die gesamte Gruppe zurueck (alle Bloecke "erloeschen" wieder), analog zu
 * Coloraturas musikalischen Raetseln, bei denen man auf das Gehoer angewiesen ist.
 *
 * Die Verknuepfung von Tuer <-> Gruppe erfolgt einfachheitshalber ueber eine
 * In-Memory-Map (siehe registriereTuer). Fuer echte Persistenz ueber
 * Weltneustarts hinweg sollte das in eine PersistentState-Implementierung
 * ausgelagert werden (siehe Minecraft-Wiki "PersistentState" fuer das Muster).
 */
public final class KlangSequenzManager {

	private record GruppenSchluessel(ServerWorld world, String gruppeId) {
	}

	private static final Map<GruppenSchluessel, Integer> FORTSCHRITT = new HashMap<>();
	private static final Map<GruppenSchluessel, List<BlockPos>> TUEREN = new HashMap<>();

	private KlangSequenzManager() {
	}

	public static void init() {
		FORTSCHRITT.clear();
		TUEREN.clear();
	}

	/** Verknuepft eine Resonanztuer manuell mit einer Raetsel-Gruppe (z.B. per Command). */
	public static void registriereTuer(ServerWorld world, String gruppeId, BlockPos tuerPos) {
		GruppenSchluessel key = new GruppenSchluessel(world, gruppeId);
		TUEREN.computeIfAbsent(key, k -> new ArrayList<>()).add(tuerPos.toImmutable());
	}

	public static void onAktivierung(ServerWorld world, String gruppeId, int sequenzIndex, PlayerEntity player) {
		GruppenSchluessel key = new GruppenSchluessel(world, gruppeId);
		int erwarteterIndex = FORTSCHRITT.getOrDefault(key, 0);

		if (sequenzIndex == erwarteterIndex) {
			int neuerFortschritt = erwarteterIndex + 1;
			FORTSCHRITT.put(key, neuerFortschritt);
			player.sendMessage(Text.translatable("coloratura.message.sequence_correct"), true);

			int gesamtLaenge = anzahlBloeckeInGruppe(world, gruppeId);
			if (gesamtLaenge > 0 && neuerFortschritt >= gesamtLaenge) {
				gruppeAbgeschlossen(world, key, player);
			}
		} else if (sequenzIndex != 0) {
			// Falscher Ton zur falschen Zeit -> Sequenz zuruecksetzen.
			// (sequenzIndex == 0 startet immer neu, auch mitten in einem Fehlversuch)
			zuruecksetzenGruppe(world, gruppeId);
			FORTSCHRITT.put(key, 0);
			player.sendMessage(Text.translatable("coloratura.message.sequence_wrong"), true);
		} else {
			FORTSCHRITT.put(key, 1);
		}
	}

	private static void gruppeAbgeschlossen(ServerWorld world, GruppenSchluessel key, PlayerEntity player) {
		player.sendMessage(Text.translatable("coloratura.message.sequence_complete"), true);
		List<BlockPos> tueren = TUEREN.getOrDefault(key, List.of());
		for (BlockPos tuerPos : tueren) {
			ResonanzTuerBlock.oeffnen(world, tuerPos);
		}
	}

	private static void zuruecksetzenGruppe(ServerWorld world, String gruppeId) {
		// Einfache, robuste Implementierung: durchsucht die geladenen Bloecke des
		// registrierten Gebiets. Fuer grosse Welten sollte man stattdessen beim
		// Setup direkt eine Liste der BlockPos pro Gruppe fuehren (siehe TODO unten).
		for (BlockEntity be : bekannteBloeckeInGruppe(world, gruppeId)) {
			if (be instanceof KlangBlockEntity klangBlock) {
				klangBlock.zuruecksetzen();
			}
		}
	}

	// TODO: Fuer den produktiven Einsatz durch eine richtige Registrierung der
	// Klangbloecke pro Gruppe ersetzen (z.B. in KlangBlockEntity.markDirty()
	// selbst eintragen/austragen), statt bei jedem Reset/Zaehlen die Welt zu
	// durchsuchen. Als Platzhalter fuer dieses Grundgeruest:
	private static final Map<GruppenSchluessel, List<BlockPos>> KLANGBLOECKE = new HashMap<>();

	public static void registriereKlangblock(ServerWorld world, String gruppeId, BlockPos pos) {
		GruppenSchluessel key = new GruppenSchluessel(world, gruppeId);
		KLANGBLOECKE.computeIfAbsent(key, k -> new ArrayList<>()).add(pos.toImmutable());
	}

	private static int anzahlBloeckeInGruppe(ServerWorld world, String gruppeId) {
		return KLANGBLOECKE.getOrDefault(new GruppenSchluessel(world, gruppeId), List.of()).size();
	}

	private static List<BlockEntity> bekannteBloeckeInGruppe(ServerWorld world, String gruppeId) {
		List<BlockPos> positionen = KLANGBLOECKE.getOrDefault(new GruppenSchluessel(world, gruppeId), List.of());
		List<BlockEntity> result = new ArrayList<>();
		for (BlockPos pos : positionen) {
			BlockEntity be = world.getBlockEntity(pos);
			if (be != null) {
				result.add(be);
			}
		}
		return result;
	}
}
