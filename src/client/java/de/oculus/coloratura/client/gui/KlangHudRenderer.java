package de.oculus.coloratura.client.gui;

import de.oculus.coloratura.client.ColoraturaClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.Map;

/**
 * Zeichnet das Blind-Touch-Prinzip:
 *  1. Der Bildschirm ist (fast) permanent geschwaerzt - Blindmodus ist der
 *     Grundzustand, nicht mehr ein Umschalt-Gimmick.
 *  2. Nur Objekte, die GERADE per Blindenstock im Kegel vor dem Spieler
 *     ertastet wurden, werden kurzzeitig als Marker angezeigt. Je naeher der
 *     Ablauf-Zeitpunkt, desto blasser wird der Marker (Fade-Out), bis er ganz
 *     verschwindet - es gibt bewusst KEIN dauerhaftes Radar mehr.
 */
public final class KlangHudRenderer {

	private static final int BLIND_ALPHA = 235; // 0-255, fast deckend

	/** Aktueller Tick-Zaehler des Clients, fuer die Berechnung des Fade-Outs. */
	private static long aktuellerTick() {
		return de.oculus.coloratura.client.ColoraturaClient.getTickZaehler();
	}

	public static void render(DrawContext context, MinecraftClient client) {
		if (client.player == null) {
			return;
		}

		if (ColoraturaClient.blindModusAktiv) {
			int width = client.getWindow().getScaledWidth();
			int height = client.getWindow().getScaledHeight();
			context.fill(0, 0, width, height, (BLIND_ALPHA << 24));
		}

		renderErtasteteObjekte(context, client);
	}

	private static void renderErtasteteObjekte(DrawContext context, MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null || ColoraturaClient.AKTUELLE_ENTHUELLUNGEN.isEmpty()) {
			return;
		}

		int width = client.getWindow().getScaledWidth();
		int height = client.getWindow().getScaledHeight();
		int centerX = width / 2;
		int centerY = height / 2;
		int radius = Math.min(width, height) / 2 - 20;

		float playerYaw = player.getYaw();

		for (Map.Entry<BlockPos, Long> entry : ColoraturaClient.AKTUELLE_ENTHUELLUNGEN.entrySet()) {
			BlockPos pos = entry.getKey();

			double dx = pos.getX() + 0.5 - player.getX();
			double dz = pos.getZ() + 0.5 - player.getZ();
			double distanz = Math.sqrt(dx * dx + dz * dz);

			double zielWinkel = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
			double relativerWinkel = MathHelper.wrapDegrees(zielWinkel - playerYaw);
			double radiant = Math.toRadians(relativerWinkel);

			int px = centerX + (int) (Math.sin(radiant) * radius);
			int py = centerY - (int) (Math.cos(radiant) * radius * 0.6);

			// Fade-Out: je naeher der Ablauf-Zeitpunkt (entry.getValue()), desto
			// blasser der Marker - das visualisiert das "Vergessen" nach kurzer Zeit.
			long verbleibendeTicks = entry.getValue() - aktuellerTick();
			float restAnteil = MathHelper.clamp(
					(float) verbleibendeTicks / (float) ColoraturaClient.ENTHUELLUNG_DAUER_TICKS, 0f, 1f);
			int alpha = (int) (255 * restAnteil);
			int farbe = (alpha << 24) | 0xE0C8FF;

			String label = Math.round(distanz) + "m";
			context.drawText(client.textRenderer, label, px - client.textRenderer.getWidth(label) / 2, py, farbe, true);
		}
	}
}
