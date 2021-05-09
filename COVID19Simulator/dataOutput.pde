void drawNumbers() {
  int realTimeNumbersOffset = -100;
  int otherNumbersOffset = 100;
  // === Text über der Box ===
  textSize(20);
  fill(244, 103, 83);
  text("aktive Fälle = "+infectedN + ", "+ round(100*((float) infectedN/ (float) personQuantity * 100))/100.0+"%", box[0]+(box[2]/2+realTimeNumbersOffset), box[1] - 50);
  fill(70);
  text("immune Fälle = "+removedN + ", "+ round((float) removedN/ (float) personQuantity * 100)+"%", box[0]+(box[2]/2+realTimeNumbersOffset), box[1] - 25);
  fill(48, 157, 108);
  text("Gesunde = "+normalN, box[0]+(box[2]/2+realTimeNumbersOffset), box[1] - 75);
  fill(255);
  textSize(23);
  text(modeToName(mode), boxXMid, box[1] - 120);
  textSize(20);
  text("Stunden = "+(int) h, box[0]+(box[2]/2+otherNumbersOffset)+10, box[1] - 25);
  textSize(12);
  text("erreichte Simulationsgeschwindigkeit = "+round(framesPerSecond/60)+"x", box[0]+box[2] - 130, box[1]+5);
  
  // === Text über Einstellungen ===
  textAlign(CORNER);
  textSize(16);
  text("Einstellungen für Infektionsverlauf", textfieldsX, textfieldsY-10);
  text("Einstellungen für Treffpunkt-Modus", textfieldsX, textfieldsY+330 + meetingSettingsOffset-10);
  text("Einstellungen für Gruppen-Modus", textfieldsX, textfieldsY+520);
  text("Einstellungen für Quarantäne-Modus", textfieldsX, textfieldsY+590);
  textAlign(CENTER);
  
  // === Text im Menu ===
  fill(128);
  textSize(14);
  text("Leerzeichen: Pause/Weiter, S: Start, R: Reset, F: Schnelldurchlauf",boxXMid,810);
}

String modeToName(int mode) {
  if (mode == normalMode) {
    return "Normaler-Modus";
  } else if (mode == meetingMode) {
    return "Treffpunkt-Modus";
  } else if (mode == groupMode) {
    return "Gruppen-Modus";
  } else if (mode == quarantineMode) {
    return "Quarantäne-Modus";
  }
  return "";
}
