/*
COVID19 Simulator
geschrieben von Chanpan Li

PS: es kann sein, dass wegen der Fenstergröße nicht alle Buttons oder Textfelder im Bild sind
*/

// === Liste aller persons ===
person[] persons;
final int[] box = {650, 150, 500, 500};

final int boxXMid = box[0] + (box[2]/2);
final int boxYMid = box[1] + (box[3]/2);

final int gapToWallCol = 2;
final int gapToWall = 2;

// personen und Zustände deren
final int personSize = 6;
int personQuantity = 200; // Personenanzahl
int personQuant = 0; // Darauf schreibt das Textfeld, damit während einer Simulation die Personenanzahl nicht geändert wird 
final float personSpeed = 0.4;

// Social Distancing
int distancingProb = 0;
final float personRepell = 2;
float personSpacing = 0;

// laufen
final float turnStep = 0.05;
final int goalGap = 10;
final int goalTolerance = 25;

// sprung
final float jumpStep = 0.4;
final float jumpSpeed = 8;

// Zustände
final int susceptible = 0, infected = 1, removed = 2;

// Zeitsteuerung
float h;
float s; // 60 frames pro sekunde
int stepCount = 0, stepCountOld = 0;
final float secPerHour = 2; // eine Stunde in der Simulation ... Sekunden in der realen Welt 
int hOld = 0;
float sOld = 0;
int framesPerSecond = 0; // für die Berechnung von Bildern pro Sekunde
float simulationSpeed = 1; // allgemeine Geschwindigkeit der Simulation

// Infektion Kontaktverfolgung | in radius ri -> lang genug zusammen? -> infiziert
IntList contacts = new IntList();
IntList contactsOld = new IntList(); // t-1

// Daten
int normalN = personQuantity;
int infectedN = 0;
int removedN = 0;
IntList infTimeline = new IntList();
IntList remTimeline = new IntList();
graph casesGraph = new graph(box[0]-250, box[1], 200, 200, personQuantity, 244, 103, 83, "aktive Fälle");
graph removedGraph = new graph(box[0]-250, box[1]+250, 200, 200, personQuantity, 70, 70, 70, "immune Fälle");

// Einstellung
float pi = 0.2; //InfektionsWahrscheinlichkeit
float ri = 15; // Infektionsradius
int startInfectedN = 1;
int infectiousTime = 1; // Ansteckungsfähigkeit in Tagen
int incubationTime = 4; // Inkubationszeit in Stunden
int firstInfectedTime; // Zeit, an dem der erste Infizierte infiziert wurde

// Modi
final int normalMode = 0;
final int meetingMode = 1;
final int groupMode = 2;
final int quarantineMode = 3;

//meeting Mode
int[] meetingBox = {boxXMid-25, boxYMid-25, 50, 50};
float meetingProb = 0.2; // Wahrscheinlichkeit für das Treffen
int meetingTime = 2; // Länge des "Meetings" in Stunden

// quarantineMode
int[]quarantineBox = {box[0]-250, box[1]+box[3]-20, 200, 150};
int testingTime = 6; // Zeit für den Corona-Test + Inkubationszeit
int quarantineDelay = 0; // Zeit vom erstem Fall bis die Quarantäne aktiviert wird in Stunden
float noSymptomsProb = 0.15; // Wahrscheinlichkeit für symptomlose Fälle

boolean quarantineStarted = false;

//groupMode
int[][] groups = {{box[0], box[1], 250, 250}, {boxXMid, box[1], 250, 250}, {box[0], boxYMid, 250, 250}, {boxXMid, boxYMid, 250, 250}};
int[][] groupMid = new int[4][2];
float changeGroupProb = 0.1; // Wahrscheinlichkeit für Gruppenwechsel

int mode = 0;

// Menu
boolean running = true;

void setup() {
  size(1200, 820);
  initPersons();
  frameRate(60);

  for (int i = 0; i < 4; i++) {
    groupMid[i][0] = groups[i][0]+(groups[i][2]/2);
    groupMid[i][1] = groups[i][1]+(groups[i][3]/2);
  }

  textAlign(CENTER);
}

void draw() {
  background(0);
  // === Box Zeichnen ===
  fill(0);
  stroke(255);
  if (mode == normalMode) {
    rect(box[0], box[1], box[2], box[3]);
  } else if (mode == meetingMode) {
    rect(box[0], box[1], box[2], box[3]);
    rect(meetingBox[0], meetingBox[1], meetingBox[2], meetingBox[3]);
  } else if (mode == groupMode) {
    for (int[] group : groups) {
      rect(group[0], group[1], group[2], group[3]);
    }
  } else if (mode == quarantineMode) {
    rect(box[0], box[1], box[2], box[3]);
    if (quarantineStarted) {
      stroke(244, 103, 83);
      fill(244, 103, 83);
      textSize(20);
      text("Quarantäne", quarantineBox[0]+(quarantineBox[2]/2), quarantineBox[1]-20);

      fill(0);
      rect(quarantineBox[0], quarantineBox[1], quarantineBox[2], quarantineBox[3]);
      stroke(255);
    } else if (infectedN != 0) {
      fill(244, 103, 83);
      textSize(20);
      if (quarantineDelay != 0) {
        text("Quarantäne startet in:", quarantineBox[0]+(quarantineBox[2]/2), quarantineBox[1]-20);
        text(firstInfectedTime + quarantineDelay - (int) h + " Stunden", quarantineBox[0]+(quarantineBox[2]/2), quarantineBox[1]);
      }
      if (firstInfectedTime + quarantineDelay < h) {
        quarantineStarted = true;
      }
    }
  }


  // === Simulation laufen lassen ===
  if (running) {
    for (int i = 0; i < simulationSpeed; i++) {
      runSimulation();
    }
  }
  drawPersons(); 
  // === Datenausgabe ===
  drawNumbers();
  casesGraph.drawGraph(infTimeline);
  removedGraph.drawGraph(remTimeline);

  // === Menu ===
  // buttons und textfelder
  resetButton.drawButton();
  startButton.drawButton();

  normalModeButton.drawButton();
  meetingModeButton.drawButton();
  groupModeButton.drawButton();
  quarantineModeButton.drawButton();

  personQuantityText.updateBox();
  personQuant = int(personQuantityText.out);

  startInfN.updateBox();
  startInfectedN = int(startInfN.out);

  piText.updateBox();
  pi = float(piText.out);

  riText.updateBox();
  ri = float(riText.out);

  incuTimeText.updateBox();
  incubationTime = int(incuTimeText.out);

  infTimeText.updateBox();
  infectiousTime = int(infTimeText.out);

  simSpeed.updateBox();
  simulationSpeed = float(simSpeed.out);

  distancingProbText.updateBox();
  if (int(distancingProbText.out) != distancingProb) {
    initDistancing(int(distancingProbText.out));
    distancingProb = int(distancingProbText.out);
  }
  personSpacing = ri;

  meetingProbText.updateBox();
  meetingProb = float(meetingProbText.out);
  meetingLenText.updateBox();
  meetingTime = int(meetingLenText.out);

  changinGrouProbText.updateBox();
  changeGroupProb = float(changinGrouProbText.out);

  testingTimeText.updateBox();
  testingTime = int(testingTimeText.out);
  quarantineDelayText.updateBox();
  quarantineDelay = int(quarantineDelayText.out);
  noSymptomsProbText.updateBox();
  noSymptomsProb = float(noSymptomsProbText.out);


  if (running) {
    play1Button.drawButton();
  } else {
    play2Button.drawButton();
  }
}

void runSimulation() {
  // === Timing ===
  s = stepCount/60.0;
  h = (1/secPerHour) * s;
  if ((int) h != hOld) {
    perHour();
    hOld = (int) h;
  }
  int randomPerson = (int) random(0, personQuantity);
  if (mode == meetingMode) {
    // Person geht zum Treffpunkt mit der Wahrscheinlichkeit von meetingProb pro Simulationsschritt
    if ((int) random(0, 100) < meetingProb * 100) {
      persons[randomPerson].jumpToTemp(box[0]+(box[2]/2), box[1]+(box[3]/2), meetingTime);
    }
  } else if (mode == groupMode) {
    // Ändert die Gruppe einer Person mit der Wahrscheinlichkeit von changeGroupProb  pro Simulationsschritt
    if ((int) random(0, 100) < changeGroupProb * 100) {
      persons[randomPerson].groupN = (int) random(0, 4);
      persons[randomPerson].jumpTo(groupMid[persons[randomPerson].groupN][0], groupMid[persons[randomPerson].groupN][1]);
      persons[randomPerson].goalX = groupMid[persons[randomPerson].groupN][0];
      persons[randomPerson].goalY = groupMid[persons[randomPerson].groupN][1];
    }
  }

  movePersons();
  stepCount++;
}

void perHour() { // Wird einmal pro Stunde in der Simulation ausgeführt
  for (int personIndex : contacts) {
    if (contactsOld.hasValue(personIndex)) {
      persons[personIndex].personInfected();
    }
  }
  contactsOld = contacts;
  contacts = new IntList();
  if (infectedN != 0) {
    infTimeline.append(infectedN);
    remTimeline.append(removedN);
  } else if (infTimeline.size() != 0 && infTimeline.get(infTimeline.size()-1) != 0) {
    infTimeline.append(0);
    remTimeline.append(personQuantity-normalN);
  }
  // Berechnung für Bilder Pro Sekunde
  framesPerSecond = int((stepCount-stepCountOld) / (millis() / 1000.0 - sOld+0.001)) +1; // +0.001, damit keine zero-division entsteht
  sOld = millis()/ 1000.0;
  stepCountOld = stepCount;
}

void finishSimulation() { // Berechnet die Simulation sehr schnell, bis keien Infizierten mehr gibt, ohne es anzuzeigen
  while (infectedN !=0) {
    runSimulation();
  }
}
