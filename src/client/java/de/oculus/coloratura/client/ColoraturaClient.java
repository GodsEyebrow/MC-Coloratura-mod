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
import java.util.Map;

/**
 * Client-seitiges Herzstueck: verwaltet
 *  - den Blindmodus (Taste B) - schwaerzt den Bildschirm komplett,
 *  - das Klanggedaechtnis (welche Klangquellen wurden je entdeckt + ihre Tonhoehe),
 *  - und delegiert das eigentliche Zeichnen an KlangHudRenderer.
 *
 * Genau wie in Coloratura soll man im Blindmodus vollstaendig auf 3D-Audio
 * angewiesen sein; die Radar-Pfeile im HUD entsprechen dem "Objective Button"/
 * Memory-System aus dem Vorbild - ein reduziertes, nicht-visuelles Hilfsmittel,
 * das nur Richtung/Distanz zu bereits entdeckten Klaengen zeigt, aber niemals
 * das Bild selbst.
 */
public class ColoraturaClient implements ClientModInitializer {

	/** BlockPos -> Tonhoehe (note), aller bisher entdeckten Klangquellen. */
	public static final Map<BlockPos, Integer> KLANG_GEDAECHTNIS = new LinkedHashMap<>();

	public static boolean blindModusAktiv = false;

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
			while (toggleBlindKey.wasPressed()) {
				blindModusAktiv = !blindModusAktiv;
				if (client.player != null) {
					client.player.sendMessage(
							Text.translatable(blindModusAktiv
									? "coloratura.hud.blind_active"
									: "coloratura.hud.blind_active"),
							true
					);
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
						boolean neuEntdeckt = false;
						for (Map.Entry<BlockPos, Integer> entry : neu.entrySet()) {
							if (!KLANG_GEDAECHTNIS.containsKey(entry.getKey())) {
								neuEntdeckt = true;
							}
							KLANG_GEDAECHTNIS.put(entry.getKey(), entry.getValue());
						}
						if (neuEntdeckt && client.player != null) {
							client.player.sendMessage(
									Text.translatable("coloratura.message.discovered",
											KLANG_GEDAECHTNIS.size()),
									true
							);
						}
					});
				}
		);

		ColoraturaMod.LOGGER.info("[Coloratura] Client-Komponenten (Blindmodus, HUD, Klanggedaechtnis) bereit");
	}
}
