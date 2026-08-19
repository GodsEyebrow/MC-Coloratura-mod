package de.oculus.coloratura.network;

import de.oculus.coloratura.ColoraturaMod;
import de.oculus.coloratura.block.entity.KlangBlockEntity;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Einfaches S2C-Paket "coloratura:entdeckt", das dem Client mitteilt, welche
 * Klangbloecke (Position + Tonhoehe) durch einen Resonanzkompass-Ping neu
 * entdeckt wurden. Der Client speichert diese in seinem "Klanggedaechtnis"
 * (ColoraturaClient) und zeigt sie fortan als Radar-Pfeile im HUD an, auch
 * wenn der Block laengst wieder ausser Sicht ist.
 *
 * Hinweis zur Fabric-API-Version: Diese Implementierung nutzt die klassische
 * ServerPlayNetworking/ClientPlayNetworking-API mit PacketByteBuf. Neuere
 * Fabric-API-Versionen (>=0.92 fuer 1.20.5+) bevorzugen das typisierte
 * CustomPayload-System - fuer 1.20.1 ist die hier gezeigte Variante korrekt.
 */
public final class ColoraturaNetworking {

	public static final Identifier ENTDECKT_PACKET = Identifier.of(ColoraturaMod.MOD_ID, "entdeckt");

	private ColoraturaNetworking() {
	}

	public static void registerServerReceivers() {
		// Aktuell werden keine C2S-Pakete benoetigt: das Anpingen laeuft ueber die
		// normale Item#use()-Serverlogik in BlindenstockItem. Dieser Platzhalter
		// existiert, damit spaetere C2S-Kommunikation (z.B. Blindmodus-Sync) hier
		// zentral registriert werden kann.
	}

	public static void sendeEntdeckungen(ServerPlayerEntity player, List<KlangBlockEntity> entdeckungen) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(entdeckungen.size());
		for (KlangBlockEntity entity : entdeckungen) {
			BlockPos pos = entity.getPos();
			buf.writeBlockPos(pos);
			buf.writeVarInt(entity.getNote());
		}
		ServerPlayNetworking.send(player, ENTDECKT_PACKET, buf);
	}
}
