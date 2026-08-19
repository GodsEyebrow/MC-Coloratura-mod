package de.oculus.coloratura.client;

import de.oculus.coloratura.ColoraturaMod;
import de.oculus.coloratura.client.gui.KlangHudRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Client-seitiges Herzstueck, umgebaut auf das Prinzip von "Blind Touch"
 * (statt des frueheren Coloratura-Radar/Gedaechtnis-Ansatzes):
 *
 *  - Blindmodus ist jetzt der GRUNDZUSTAND (blindModusAktiv startet auf true),
 *    nicht mehr ein optionales Extra. Taste B bleibt als Testschalter erhalten,
 *    damit man beim Bauen/Debuggen auch mal etwas sehen kann.
 *  - Es gibt KEIN dauerhaftes Gedaechtnis mehr. Ein Tipp mit dem Blindenstock
 *    "ertastet" Objekte im Kegel vor einem nur fuer ENTHUELLUNG_DAUER_TICKS -
 *    danach verschwinden sie wieder aus der Anzeige, genau wie die kurze
 *    Sonar-Ripple-Enthuellung im Vorbild. Man muss also wiederholt tippen,
 *    um sich zu orientieren, statt sich auf ein einmal aufgebautes Kartenbild
 *    zu verlassen.
 */
public class ColoraturaClient implements ClientModInitializer {

	/** Wie lange (in Ticks) ein ertastetes Objekt im HUD sichtbar bleibt,
	 * bevor es wieder "vergessen" wird. 60 Ticks = 3 Sekunden. */
	public static final int ENTHUELLUNG_DAUER_TICKS = 60;

	/** BlockPos -> Tick-Zeitpunkt, bis zu dem dieses Objekt noch als "ertastet" gilt. */
	public static final Map<BlockPos, Long> AKTUELLE_ENTHUELLUNGEN = new LinkedHashMap<>();

	/** Zaehler, der bei jedem Client-Tick hochgezaehlt wird - dient als
	 * einfache "Uhrzeit" fuer den Ablauf der Enthuellungen. */
	private static long tickZaehler = 0L;

	public static long getTickZaehler() {
		return tickZaehler;
	}

	/** Blindmodus ist der Grundzustand des Mods (analog Blind Touch), nicht
	 * mehr ein optionales Extra wie zuvor beim Coloratura-Vorbild. */
	public static boolean blindModusAktiv = true;

	private static KeyBinding toggleBlindKey;

	@Override
	public void onInitializeClient() {
		toggleBlindKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.coloratura.toggle_blind",
				Type.KEYSYM,
				GLFW.GLFW_KEY_B,
				"key.categories.coloratura"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			tickZaehler++;

			while (toggleBlindKey.wasPressed()) {
				blindModusAktiv = !blindModusAktiv;
				if (client.player != null) {
					client.player.sendMessage(Text.translatable("coloratura.hud.blind_active"), true);
				}
			}

			// Abgelaufene Enthuellungen entfernen - das ist der Kern des
			// "kein dauerhaftes Gedaechtnis"-Prinzips aus Blind Touch.
			Iterator<Map.Entry<BlockPos, Long>> it = AKTUELLE_ENTHUELLUNGEN.entrySet().iterator();
			while (it.hasNext()) {
				if (it.next().getValue() <= tickZaehler) {
					it.remove();
				}
			}
		});

		HudRenderCallback.EVENT.register((drawContext, tickDelta) ->
				KlangHudRenderer.render(drawContext, MinecraftClient.getInstance()));

		ClientPlayNetworking.registerGlobalReceiver(
				de.oculus.coloratura.network.ColoraturaNetworking.ENTDECKT_PACKET,
				(client, handler, buf, responseSender) -> {
					int anzahl = buf.readVarInt();
					Map<BlockPos, Integer> neu = new LinkedHashMap<>();
					for (int i = 0; i < anzahl; i++) {
						BlockPos pos = buf.readBlockPos();
						int note = buf.readVarInt();
						neu.put(pos, note);
					}
					client.execute(() -> {
						long ablaufZeitpunkt = tickZaehler + ENTHUELLUNG_DAUER_TICKS;
						for (BlockPos pos : neu.keySet()) {
							// Jeder erneute Treffer im Kegel verlaengert die Anzeige wieder -
							// solange man den Stock auf ein Objekt gerichtet haelt, bleibt
							// es "ertastet".
							AKTUELLE_ENTHUELLUNGEN.put(pos, ablaufZeitpunkt);
						}
					});
				}
		);

		ColoraturaMod.LOGGER.info("[Coloratura] Client-Komponenten (Blindenstock-Enthuellung, HUD) bereit - Blind-Touch-Modus");
	}
}
