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
 * Zeichnet:
 *  1. Im Blindmodus: einen (fast) vollstaendig schwarzen Overlay ueber den
 *     gesamten Bildschirm, sodass Sicht praktisch keine Information mehr liefert
 *     und man sich - wie in Coloratura - auf 3D-Audio verlassen muss.
 *  2. Radar-Pfeile am Bildschirmrand fuer jede im Klanggedaechtnis gespeicherte
 *     Klangquelle: Richtung relativ zur Blickrichtung des Spielers, Groesse/Alpha
 *     grob nach Entfernung skaliert. Das ist bewusst NICHT positionsgenau (kein
 *     "Wallhack"), sondern nur eine grobe Richtungsangabe - vergleichbar mit dem
 *     reduzierten Objective-Button/Radar-System aus dem Vorbild.
 */
public final class KlangHudRenderer {

	private static final int BLIND_ALPHA = 235; // 0-255, fast deckend

	private KlangHudRenderer() {
	}

	public static void render(DrawContext context, MinecraftClient client) {
		if (client.player == null) {
			return;
		}

		if (ColoraturaClient.blindModusAktiv) {
			int width = client.getWindow().getScaledWidth();
			int height = client.getWindow().getScaledHeight();
			context.fill(0, 0, width, height, (BLIND_ALPHA << 24));
			context.drawCenteredTextWithShadow(client.textRenderer,
					Text.translatable("coloratura.hud.blind_active"),
					width / 2, 10, 0xFFFFFF);
		}

		renderRadar(context, client);
	}

	private static void renderRadar(DrawContext context, MinecraftClient client) {
		ClientPlayerEntity player = client.player;
		if (player == null || ColoraturaClient.KLANG_GEDAECHTNIS.isEmpty()) {
			return;
		}

		int width = client.getWindow().getScaledWidth();
		int height = client.getWindow().getScaledHeight();
		int centerX = width / 2;
		int centerY = height / 2;
		int radius = Math.min(width, height) / 2 - 20;

		float playerYaw = player.getYaw();

		for (Map.Entry<BlockPos, Integer> entry : ColoraturaClient.KLANG_GEDAECHTNIS.entrySet()) {
			BlockPos pos = entry.getKey();

			double dx = pos.getX() + 0.5 - player.getX();
			double dz = pos.getZ() + 0.5 - player.getZ();
			double distanz = Math.sqrt(dx * dx + dz * dz);

			// Richtung relativ zur Blickrichtung berechnen
			double zielWinkel = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
			double relativerWinkel = MathHelper.wrapDegrees(zielWinkel - playerYaw);
			double radiant = Math.toRadians(relativerWinkel);

			int px = centerX + (int) (Math.sin(radiant) * radius);
			int py = centerY - (int) (Math.cos(radiant) * radius * 0.6); // leicht abgeflacht wie ein Kompass

			// Alpha nach Distanz: nahe Klaenge leuchten kraeftiger
			int alpha = (int) MathHelper.clamp(255 - distanz * 3, 60, 255);
			int farbe = (alpha << 24) | 0xE0C8FF; // helles Violett, passend zum "Resonanz"-Thema

			String label = Math.round(distanz) + "m";
			context.drawText(client.textRenderer, label, px - client.textRenderer.getWidth(label) / 2, py, farbe, true);
		}
	}
}
