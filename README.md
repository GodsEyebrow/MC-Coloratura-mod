# Coloratura-Mod (Minecraft Fabric, 1.20.1)

Ein Minecraft-Mod nach dem Prinzip des Steam-Spiels **Coloratura** (Nakama Game
Studio): Navigation und Rätsel funktionieren primär über **3D-positionierten
Klang**, nicht über Sicht. Minecrafts Soundengine ist bereits von Haus aus
3D-positional (Lautstärke/Stereo abhängig von Distanz & Richtung) – dieser Mod
baut ein Radar-, Gedächtnis- und Rätselsystem obendrauf.

## Mechaniken

| Coloratura (Vorbild)                         | In diesem Mod                                              |
|-----------------------------------------------|--------------------------------------------------------------|
| Navigation nur durch 3D-Audio                 | **Blindmodus** (Taste `B`): Bildschirm wird geschwärzt       |
| Radar-System für entdeckte Klangquellen       | **Resonanzkompass**: pingt Klangblöcke im Umkreis an          |
| Gedächtnis-System (einmal gefunden = gespeichert) | Client-seitiges `KLANG_GEDAECHTNIS`, HUD zeigt Richtung/Distanz |
| Musikalische Rätsel                           | **Klangblöcke** müssen in richtiger Tonhöhen-Reihenfolge aktiviert werden → öffnet **Resonanztür** |

## Projektstruktur

```
src/main/java/...      Serverseitige Logik (Blöcke, BlockEntities, Rätsel, Networking)
src/client/java/...    Clientseitige Logik (HUD, Blindmodus, Klanggedächtnis, Keybinding)
src/main/resources/    fabric.mod.json, Sprachen, Modelle, Texturen
```

## Setup & Bauen

Dieses Projekt wurde **nicht** in dieser Sandbox kompiliert, da hier kein
Netzwerkzugriff auf die Fabric-/Mojang-Server besteht. Zum Bauen lokal:

1. [Java 17 (JDK)](https://adoptium.net/) installieren.
2. Projekt in IntelliJ IDEA öffnen (oder `./gradlew genSources` + eigener Editor).
3. `./gradlew build` – lädt beim ersten Mal automatisch Minecraft, Yarn-Mappings
   und Fabric API herunter (braucht Internetzugang).
4. Fertiges Mod-Jar liegt danach in `build/libs/`.
5. In Fabric Loader + [Fabric API](https://modrinth.com/mod/fabric-api) für
   Minecraft 1.20.1 werfen (`.minecraft/mods/`).

Falls sich die Minecraft-/Yarn-/Fabric-API-Versionen inzwischen geändert haben:
aktuelle Werte auf https://fabricmc.net/develop/ nachschlagen und in
`gradle.properties` eintragen.

## Ein Rätsel im Spiel aufbauen

1. Drei (oder mehr) Klangblöcke platzieren.
2. Jeden per Command mit Gruppe, Position in der Sequenz und Tonhöhe versehen:
   ```
   /data merge block <x1> <y1> <z1> {gruppeId:"raum1", sequenzIndex:0, note:5}
   /data merge block <x2> <y2> <z2> {gruppeId:"raum1", sequenzIndex:1, note:9}
   /data merge block <x3> <y3> <z3> {gruppeId:"raum1", sequenzIndex:2, note:14}
   ```
3. Eine Resonanztür platzieren und die Gruppe damit verknüpfen. In diesem
   Grundgerüst geschieht das (noch) nicht per fertigem `/coloratura`-Command,
   sondern muss serverseitig einmalig aufgerufen werden, z.B. über einen
   eigenen Command oder beim Weltstart:
   ```java
   KlangSequenzManager.registriereKlangblock(world, "raum1", pos1);
   KlangSequenzManager.registriereKlangblock(world, "raum1", pos2);
   KlangSequenzManager.registriereKlangblock(world, "raum1", pos3);
   KlangSequenzManager.registriereTuer(world, "raum1", tuerPos);
   ```
   → Als nächster Ausbauschritt würde sich ein eigener `/coloratura link`-Command
   oder ein "Setup-Werkzeug" anbieten, das das automatisch beim Platzieren
   erledigt (siehe "Nächste Schritte" unten).
4. Spieler betreten den Raum, hören die periodischen Klangpulse, aktivieren
   die Blöcke per Rechtsklick in der richtigen Reihenfolge (erkennbar nur an
   der Tonhöhe) → Tür öffnet sich.

## Bekannte Einschränkungen / Nächste Schritte

- **API-Signaturen**: Einige Minecraft-/Fabric-Methodensignaturen (z.B.
  `Block#onUse`, `BlockEntity#writeNbt`) ändern sich zwischen Versionen leicht.
  Der Code ist auf 1.20.1 (Yarn) ausgelegt; bei Abweichungen hilft ein Blick in
  die von Loom generierten Sources (`./gradlew genSources`).
- **Persistenz**: `KlangSequenzManager` hält Gruppen/Türen aktuell nur im
  Arbeitsspeicher (nicht über Weltneustarts hinweg). Für echten Einsatz sollte
  das über Minecrafts `PersistentState`-API gespeichert werden.
- **Setup-Komfort**: Gruppen/Türen aktuell nur per Code/NBT verknüpfbar. Ein
  `/coloratura link <gruppeId>`-Command oder ein Setup-Item wäre der nächste
  sinnvolle Schritt.
- **Radar-Genauigkeit**: Die HUD-Pfeile zeigen bewusst nur eine grobe Richtung
  (kein exaktes Tracking durch Wände), um dem "nur Klang, kein Wallhack"-Prinzip
  treu zu bleiben.
- **Echte Barrierefreiheit**: Für tatsächlich blinde Spieler wären zusätzlich
  Sprachausgabe (Text-to-Speech) der HUD-Meldungen und ggf. Controller-Vibration
  sinnvoll – beides ist in Vanilla-Minecraft nicht eingebaut und müsste über
  Mixins/externe Bibliotheken ergänzt werden.

## Lizenz

MIT – frei anpassbar für eigene Maps/Modpacks.
