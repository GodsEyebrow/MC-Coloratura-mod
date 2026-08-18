package de.oculus.coloratura;

import de.oculus.coloratura.block.ModBlocks;
import de.oculus.coloratura.block.entity.ModBlockEntities;
import de.oculus.coloratura.item.ModItems;
import de.oculus.coloratura.network.ColoraturaNetworking;
import de.oculus.coloratura.puzzle.KlangSequenzManager;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coloratura-Mod
 * ----------------
 * Grundidee (uebernommen aus dem Steam-Spiel "Coloratura" von Nakama Game Studio):
 * Statt sich auf visuelle Hinweise zu verlassen, findet und loest man Dinge ueber
 * raeumlichen (3D-positionierten) Klang. Minecrafts Soundengine ist von Haus aus
 * bereits 3D-positional - wir nutzen das als Fundament und bauen drei Mechaniken
 * darauf auf:
 *
 *  1. Klangbloecke, die periodisch einen Ton (mit individueller Tonhoehe) aussenden
 *     -> "Radar/Gedaechtnis"-System: einmal georteter Klang wird sich clientseitig
 *        gemerkt (siehe ColoraturaClient) und kann per HUD als Richtung angezeigt werden,
 *        ohne dass man den Block je gesehen haben muss.
 *  2. Der Resonanzkompass: aktives "Anpingen" der Umgebung, das Klangbloecke in
 *     Reichweite ortet, dem Spieler die Distanz mitteilt und sie ins Gedaechtnis
 *     aufnimmt.
 *  3. Klang-Raetsel: Klangbloecke muessen in der richtigen Tonhoehen-Reihenfolge
 *     aktiviert werden (rein am Klang erkennbar), um eine Resonanztuer zu oeffnen.
 *  4. Ein Blindmodus (clientseitig) verdunkelt den Bildschirm komplett, sodass man
 *     - wie im Vorbild - ausschliesslich per Gehoer navigieren muss.
 */
public class ColoraturaMod implements ModInitializer {

	public static final String MOD_ID = "coloratura";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[Coloratura] Initialisiere Klang-Navigations-Mod");

		ModBlocks.register();
		ModItems.register();
		ModBlockEntities.register();
		ColoraturaNetworking.registerServerReceivers();
		KlangSequenzManager.init();

		LOGGER.info("[Coloratura] Fertig - Klangbloecke, Kompass und Resonanztueren registriert");
	}
}
