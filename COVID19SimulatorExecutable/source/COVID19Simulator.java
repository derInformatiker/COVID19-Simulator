import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class COVID19Simulator extends PApplet {

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
final float personSpeed = 0.4f;

// Social Distancing
int distancingProb = 0;
final float personRepell = 2;
float personSpacing = 0;

// laufen
final float turnStep = 0.05f;
final int goalGap = 10;
final int goalTolerance = 25;

// sprung
final float jumpStep = 0.4f;
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
float pi = 0.2f; //InfektionsWahrscheinlichkeit
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
float meetingProb = 0.2f; // Wahrscheinlichkeit für das Treffen
int meetingTime = 2; // Länge des "Meetings" in Stunden

// quarantineMode
int[]quarantineBox = {box[0]-250, box[1]+box[3]-20, 200, 150};
int testingTime = 6; // Zeit für den Corona-Test + Inkubationszeit
int quarantineDelay = 0; // Zeit vom erstem Fall bis die Quarantäne aktiviert wird in Stunden
float noSymptomsProb = 0.15f; // Wahrscheinlichkeit für symptomlose Fälle

boolean quarantineStarted = false;

//groupMode
int[][] groups = {{box[0], box[1], 250, 250}, {boxXMid, box[1], 250, 250}, {box[0], boxYMid, 250, 250}, {boxXMid, boxYMid, 250, 250}};
int[][] groupMid = new int[4][2];
float changeGroupProb = 0.1f; // Wahrscheinlichkeit für Gruppenwechsel

int mode = 0;

// Menu
boolean running = true;

public void setup() {
  
  initPersons();
  frameRate(60);

  for (int i = 0; i < 4; i++) {
    groupMid[i][0] = groups[i][0]+(groups[i][2]/2);
    groupMid[i][1] = groups[i][1]+(groups[i][3]/2);
  }

  textAlign(CENTER);
}

public void draw() {
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
  personQuant = PApplet.parseInt(personQuantityText.out);

  startInfN.updateBox();
  startInfectedN = PApplet.parseInt(startInfN.out);

  piText.updateBox();
  pi = PApplet.parseFloat(piText.out);

  riText.updateBox();
  ri = PApplet.parseFloat(riText.out);

  incuTimeText.updateBox();
  incubationTime = PApplet.parseInt(incuTimeText.out);

  infTimeText.updateBox();
  infectiousTime = PApplet.parseInt(infTimeText.out);

  simSpeed.updateBox();
  simulationSpeed = PApplet.parseFloat(simSpeed.out);

  distancingProbText.updateBox();
  if (PApplet.parseInt(distancingProbText.out) != distancingProb) {
    initDistancing(PApplet.parseInt(distancingProbText.out));
    distancingProb = PApplet.parseInt(distancingProbText.out);
  }
  personSpacing = ri;

  meetingProbText.updateBox();
  meetingProb = PApplet.parseFloat(meetingProbText.out);
  meetingLenText.updateBox();
  meetingTime = PApplet.parseInt(meetingLenText.out);

  changinGrouProbText.updateBox();
  changeGroupProb = PApplet.parseFloat(changinGrouProbText.out);

  testingTimeText.updateBox();
  testingTime = PApplet.parseInt(testingTimeText.out);
  quarantineDelayText.updateBox();
  quarantineDelay = PApplet.parseInt(quarantineDelayText.out);
  noSymptomsProbText.updateBox();
  noSymptomsProb = PApplet.parseFloat(noSymptomsProbText.out);


  if (running) {
    play1Button.drawButton();
  } else {
    play2Button.drawButton();
  }
}

public void runSimulation() {
  // === Timing ===
  s = stepCount/60.0f;
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

public void perHour() { // Wird einmal pro Stunde in der Simulation ausgeführt
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
  framesPerSecond = PApplet.parseInt((stepCount-stepCountOld) / (millis() / 1000.0f - sOld+0.001f)) +1; // +0.001, damit keine zero-division entsteht
  sOld = millis()/ 1000.0f;
  stepCountOld = stepCount;
}

public void finishSimulation() { // Berechnet die Simulation sehr schnell, bis keien Infizierten mehr gibt, ohne es anzuzeigen
  while (infectedN !=0) {
    runSimulation();
  }
}
public void drawNumbers() {
  int realTimeNumbersOffset = -100;
  int otherNumbersOffset = 100;
  // === Text über der Box ===
  textSize(20);
  fill(244, 103, 83);
  text("aktive Fälle = "+infectedN + ", "+ round(100*((float) infectedN/ (float) personQuantity * 100))/100.0f+"%", box[0]+(box[2]/2+realTimeNumbersOffset), box[1] - 50);
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

public String modeToName(int mode) {
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
/*
 === Klasse für Graphen ===
 
 Konstruktor graph(x,y,w,h,max,r,g,b,t) x, y, w, h -> Position und Größe, max -> maximaler Wert in den Daten, r, g, b -> Farbe des Graphs, t -> Title / Überschrift
 drawGraph(input) -> zeichnet den Graphen mit den Daten aus input (index -> Zeit, Wert -> Höhe des Graphen)
 setMaxNumber(n) -> neuer maximaler Wert
*/

class graph {
  IntList dataList = new IntList();
  int x, y, w, h, r, g, b, max;
  String title;
  graph(int x_pos, int y_pos, int graphWidth, int graphHeight, int maxNumber, int rColor, int gColor, int bColor, String t) {
    x = x_pos;
    y = y_pos;
    max = maxNumber;

    h = graphHeight;
    w = graphWidth;

    r = rColor;
    g = gColor;
    b = bColor;

    title = t;
  }

  public void drawGraph(IntList input) {
    dataList = input;

    // Titel vom Graph
    stroke(r, g, b);
    fill(r, g, b);
    textSize(20);
    text(title, x+w/2, y-20);

    float xPoint = x;
    float yPoint = y+h;
    for (int data : dataList) {
      stroke(r, g, b);

      // Linien
      line(xPoint, yPoint, xPoint + (w / dataList.size()), map(data, 0, max, y+h, y));
      // Füllung unter der Linie
      rect(xPoint, yPoint, (w / dataList.size()), abs(yPoint-(y+h-1)));
      triangle(xPoint, yPoint, xPoint + (w / dataList.size()), map(data, 0, max, y+h, y), xPoint + (w / dataList.size()), yPoint);
      xPoint += ((float) w / dataList.size());
      yPoint = map(data, 0, max, y+h, y);
    }
    stroke(255);
    line(x+w-10, yPoint, x+w+10, yPoint);
    if(dataList.size() == 0){
      text("0%",x+w+20, yPoint);
    }else{
      text(round((float) dataList.get(dataList.size()-1)/personQuantity * 100) + "%",x+w+20, yPoint);
    }
    
    noFill();
    stroke(255);
    rect(x-2, y-2, w+3, h+3);
  }
  public void setMaxNumber(int n){
    max = n;
  }
}
public void initPersons() {
  persons = new person[personQuantity];
  for (int i = 0; i < persons.length; i++) {

    persons[i] = new person(0, 0, personSize, i);
  }
  //chooseInfected(startInfectedN);
}

public void movePersons() { // Führt bei allen Personen move() aus
  noStroke();
  fill(255);
  for (int i = 0; i < persons.length; i++) {
    persons[i].move();
  }
}

public void drawPersons() { // Führt bei allen Personen drawPerson() aus
  noStroke();
  for (int i = 0; i < persons.length; i++) {
    persons[i].drawPerson();
  }
}

public void initDistancing(int p) { // Führt bei allen Personen initDistancing() aus
  for (int i = 0; i < persons.length; i++) {
    persons[i].initDistancing(p);
  }
}

public void chooseInfected(int n) { // Wählt Zufällige, die Infiziert werden
  for (int i = 0; i < n; i++) {
    boolean noChange = true;
    if (normalN > 0) {
      while (noChange) {
        int randomPerson = (int) random(0, personQuantity);
        if (persons[randomPerson].state != infected && persons[randomPerson].state != removed) {
          persons[randomPerson].state = infected;
          persons[randomPerson].infectedT = h;
          persons[randomPerson].quarantineTimer = (int) h;
          infectedN ++;
          normalN --;
          firstInfectedTime = (int) h;
          noChange = false;
        }
      }
    }
  }
}

/*
=== Klasse für Personen (sind als Punkte dargestellt) ===
 Laufen zum zufällig ausgewähltem Ziel (goalX, goalY)
 sie können ihre Richtung nur sehr langsam ändern (vecX,vecY) -> Bewegungen sind nicht plötzlich und sie gehen nicht parallel zueinander
 
 Konstruktor - person(x,y,size,index) x,y -> position, size -> Größe, index -> index in der liste aller personen
 move() - Berechnung der nächsten Position und der besten Richtung, um sie am schnellsten zu erreichen
 drawPerson() - zeichnet die Person Grün - Gesund, Rot - Infiziert, Grau - Immun
 checkCollision(box) - Prallt bei Wänden ab (collision engine)
 jumpTo(toX,toY) - springt zu dem angegebenen Ort (auch durch Wände hindurch)
 jumpToTemp(toX,toY,hours) - ist wie jumpTo nur mit Rückkehr nach der zeit hours
 writeContacts() - Schreibt alle Kontakte als index (persons[index]) in der globalen liste contacts
 personInfected() - Wird aufgerufen, wenn die Person 1 Stunde lang mit Infizierten Personen Kontakt hatte (im Infektionsradius)
                  -> mit die Wahrscheinlichkeit von pi infiziert zu werden
 changeGoal() - ändert das Ziel der Person, wenn die Person es erreicht hat
 randomPos - die Person wird an einen zufälligen ort in der Box geschickt, ohne schon im Treffpunkt zu sein (relevant für Treffpuntk-Modus)
 initDistancing(p) - mit der Wahrscheinlichkeit von p hält die Person genug Abstand zu anderen ein
 */
class person {
  int size, index, state = susceptible, groupN;
  int goalX, goalY, jumpX, jumpY, incubationTimer, oldX, oldY, quarantineTimer;
  float vecX, vecY, vecJumpX, vecJumpY, step, speed, x, y, infectedT, radiusAnimationSize = -1, returnTime;
  boolean returnJump = false, quarantine = false, noSymptom = false, distancing = false;
  person (int x_pos, int y_pos, int size_person, int list_index) {
    x = x_pos;
    y = y_pos;
    size = size_person;
    index = list_index;
    vecX = random(-1, 1.3f);
    vecY = random(-1, 1.3f);
    speed = personSpeed;
    step = turnStep;
    randomPos(box);

    if (mode == groupMode) {
      groupN = (int) random(0, 4);
      randomPos(groups[groupN]);
    }
    goalX = PApplet.parseInt(x + random(-20, 20));
    goalY = PApplet.parseInt(y + random(-20, 20));
    goalX = constrain(goalX, box[0]+goalGap, box[0]+box[2]-goalGap);
    goalY = constrain(goalY, box[1]+goalGap, box[1]+box[3]-goalGap);
  }

  public void move() {
    if (h > returnTime && returnJump) {
      this.jumpTo(oldX, oldY); 
      returnJump = false;
    }
    if (jumpX == 0 && jumpY == 0) {
      float stepX = map(abs(goalX-x), 0, box[2], 0.1f, 1) * step;
      float stepY = map(abs(goalY-y), 0, box[3], 0.1f, 1) * step;

      if (!quarantine) {
        if (x < goalX) {
          vecX += stepX;
        } else if (x > goalX) {
          vecX -= stepX;
        }
        
        if (y < goalY) {
          vecY += stepY;
        } else if (y > goalY) {
          vecY -= stepY;
        }
      }

      if (state != removed) {

        for (int i = 0; i < persons.length; i++) {
          if ( i != index) {
            float otherX, otherY, other_size;

            otherX = persons[i].x;
            otherY = persons[i].y;
            other_size = persons[i].size;
            if (persons[i].state != removed && persons[i].distancing) {

              if (abs(x - otherX) < (size + other_size)/2 + personSpacing && abs(y - otherY) < (size + other_size)/2 + personSpacing && distancing) {
                if (otherX > x) {
                  vecX -= personRepell;
                } else {
                  vecX += personRepell;
                }

                if (otherY > y) {
                  vecY -= personRepell;
                } else {
                  vecY += personRepell;
                }
              }
            }
          }
        }
      }
      if (!quarantine) {
        if ((x-(size/2) < box[0] && vecX < 0) || (x+(size/2) > box[0]+box[2] && vecX > 0)) {
          vecX = -vecX;
        }
        if ((y-(size/2) < box[1] && vecY < 0) || (y+(size/2) > box[1]+box[3] && vecY > 0)) {
          vecY = -vecY;
        }
      }

      if (mode == meetingMode) {
        checkCollision(meetingBox);
      } else if (mode == quarantineMode) {
        checkCollision(quarantineBox);
      } else if (mode == groupMode) {
        for (int[] group : groups) {
          checkCollision(group);
        }
      }

      if (state == susceptible) {
      } else if (state == infected) {
        writeContacts();
        if (h - infectedT > infectiousTime * 24) {
          state = removed;
          infectedN --;
          removedN ++;
        }
        if (h - quarantineTimer > testingTime + incubationTime && !quarantine && mode == quarantineMode && !noSymptom && quarantineStarted) {
          jumpTo(quarantineBox[0]+(quarantineBox[2]/2), quarantineBox[1]+(quarantineBox[3]/2));
          quarantine = true;
        }
      } else if (state == removed) {
        if (quarantine) {
          jumpTo(goalX, goalY);
          quarantine = false;
        }
      }


      vecX = constrain(vecX, -1, 1);
      vecY = constrain(vecY, -1, 1);
      x += vecX * speed;
      y += vecY * speed;

      if (abs(goalX-x) < goalTolerance && abs(goalY-y) < goalTolerance) {
        changeGoal();
      }
    } else {
      if (PApplet.parseInt(x) == PApplet.parseInt(jumpX)) {
        vecJumpX = 0;
      } else if (x < jumpX) {
        vecJumpX += jumpStep;
      } else if (x > jumpX) {
        vecJumpX -= jumpStep;
      }

      if (PApplet.parseInt(y) == PApplet.parseInt(jumpY)) {
        vecJumpY = 0;
      } else if (y < jumpY) {
        vecJumpY += jumpStep;
      } else if (y > jumpY) {
        vecJumpY -= jumpStep;
      }

      vecJumpX = constrain(vecJumpX, -1, 1);
      vecJumpY = constrain(vecJumpY, -1, 1);
      x += vecJumpX * jumpSpeed;
      y += vecJumpY * jumpSpeed;

      if (abs(jumpX - x) < goalTolerance && abs(jumpY - y) < goalTolerance) {
        jumpX = 0;
        jumpY = 0;
      }
    }
  }

  public void checkCollision(int[] b) {
    if (abs(x - b[0]) < size/2 + gapToWallCol && y > b[1] && y < b[1]+b[3]) {
      if (x < b[0] && vecX > 0) {
        vecX = -vecX;
      } else if (x > b[0] && vecX < 0) {
        vecX = -vecX;
      }
    } else if (abs(x - (b[0]+b[2])) < size/2 + gapToWallCol && y > b[1] && y < b[1]+b[3]) {
      if (x < b[0]+b[2] && vecX > 0) {
        vecX = -vecX;
      } else if (x > b[0]+b[2] && vecX < 0) {
        vecX = -vecX;
      }
    }

    if (abs(y - b[1]) < size/2 + gapToWallCol && x > b[0] && x < b[0]+b[2]) {
      if (y < b[1] && vecY > 0) {
        vecY = -vecY;
      } else if (y > b[1] && vecY < 0) {
        vecY = -vecY;
      }
    } else if (abs(y - (b[1]+b[3])) < size/2 + gapToWallCol && x > b[0] && x < b[0]+b[2]) {
      if (y < b[1]+b[3] && vecY > 0) {
        vecY = -vecY;
      } else if (y > b[1]+b[3] && vecY < 0) {
        vecY = -vecY;
      }
    }
  }

  public void drawPerson() {
    if (radiusAnimationSize != -1) {
      stroke(244, 103, 83);
      noFill();
      ellipse(x, y, radiusAnimationSize, radiusAnimationSize);
      noStroke();
      if (running) {
        radiusAnimationSize += framesPerSecond/600.0f;
      }
      if (radiusAnimationSize >= ri) {
        radiusAnimationSize = -1;
      }
    }

    if (state == susceptible) {
      fill(48, 157, 108);
      noStroke();
    } else if (state == infected) {
      if (radiusAnimationSize == -1) {
        radiusAnimationSize = size;
      }
      if (noSymptom) {
        stroke(255, 255, 0, 150);
      } else {
        noStroke();
      }
      fill(244, 103, 83);
    } else if (state == removed) {
      fill(70);
      noStroke();
    }

    ellipse(x, y, size, size);
  }

  public void jumpTo(int toX, int toY) {
    jumpX = toX;
    jumpY = toY;
    vecJumpX = 0;
    vecJumpY = 0;
  }

  public void jumpToTemp(int toX, int toY, int hours) {
    if (!returnJump) {
      jumpTo(toX, toY);
      oldX = goalX;
      oldY = goalY;
      returnTime = h + hours;
      returnJump = true;
    }
  }

  public void writeContacts() {
    if (h - incubationTimer > incubationTime) {
      int otherIndex, otherGroupN;
      float otherX, otherY;

      for (int i = 0; i < persons.length; i++) {
        otherIndex = persons[i].index;

        if (otherIndex != index && persons[i].state == susceptible) {
          otherX = persons[i].x;
          otherY = persons[i].y;
          if (mode == groupMode) {
            otherGroupN = persons[i].groupN;
            if (abs(x-otherX) <= ri && abs(y-otherY) <= ri && !contacts.hasValue(otherIndex) && groupN == otherGroupN) {
              contacts.append(otherIndex);
            }
          } else {
            if (abs(x-otherX) <= ri && abs(y-otherY) <= ri && !contacts.hasValue(otherIndex)) {
              contacts.append(otherIndex);
            }
          }
        }
      }
    }
  }

  public void personInfected() {
    if ((int) random(0, 100) < pi * 100) {
      state = infected;
      infectedT = h - incubationTime;
      infectedN ++;
      normalN --;
      incubationTimer = (int) h;
      quarantineTimer = (int) h;
      if ((int) random(0, 100) < noSymptomsProb * 100 && mode == quarantineMode) {
        noSymptom = true;
      }
    }
  }
  public void changeGoal() {
    int[] groupBox = groups[groupN];
    if (mode == groupMode) {
      goalX = (int) random(groupBox[0], groupBox[0]+groupBox[2]);
      goalY = (int) random(groupBox[1], groupBox[1]+groupBox[3]);
    } else {
      goalX = (int) random(box[0], box[0]+box[2]);
      goalY = (int) random(box[1], box[1]+box[3]);
    }

    if (goalX > meetingBox[0] && goalX < meetingBox[0] + meetingBox[2] && goalY > meetingBox[1] && goalY < meetingBox[1] + meetingBox[3] && mode == meetingMode) {
      goalX += meetingBox[2];
      goalY += meetingBox[3];
    }
    goalX = constrain(goalX, box[0]+goalGap, box[0]+box[2]-goalGap);
    goalY = constrain(goalY, box[1]+goalGap, box[1]+box[3]-goalGap);
  }

  public void randomPos(int[] walls) {
    x = random(walls[0]+gapToWall, walls[0]+walls[2]-gapToWall);
    y = random(walls[1]+gapToWall, walls[1]+walls[3]-gapToWall);

    if (x > meetingBox[0] && x < meetingBox[0] + meetingBox[2] && y > meetingBox[1] && y < meetingBox[1] + meetingBox[3]) {
      x += meetingBox[2];
      y += meetingBox[3];
    }
  }

  public void initDistancing(int p) {
    if ((int) random(0, 100) < p) {
      distancing = true;
    } else {
      distancing = false;
    }
  }
}
public void reset() {
  // Daten
  personQuantity = personQuant;
  normalN = personQuantity;
  infectedN = 0;
  removedN = 0;
  stepCount = 0;
  stepCountOld = 0;

  // Personen
  contacts = new IntList();
  contactsOld = new IntList();
  
  initPersons();
  initDistancing(PApplet.parseInt(distancingProbText.out));
  distancingProb = PApplet.parseInt(distancingProbText.out);

  // Graph
  infTimeline = new IntList();
  remTimeline = new IntList();
  casesGraph.setMaxNumber(personQuantity);
  removedGraph.setMaxNumber(personQuantity);

  // Modi
  quarantineStarted = false;
}
float focus = -1;
boolean test = true;
boolean[] clicked = new boolean[14];

class Textfield {
  String text = new String();
  String alt = new String();
  String show = new String();
  String hl = new String();
  String behind = new String();
  String out = new String();
  float x, y, standard, min, max;
  int rand = 10;
  int max_char = 10;
  int mode = 0;
  int name;
  int opacity = 1000;
  Textfield (float x_pos, float y_pos, int n, float standard_num, float min_num, float max_num, String behind_tf, String alternative) {
    x = x_pos;
    y = y_pos;
    standard = standard_num;
    resetText();
    min = min_num;
    max = max_num;
    name = n;
    alt = alternative;
    behind = behind_tf;
  }
  public void updateBox() {
    rectMode(CORNER);
    textAlign(CORNER);
    strokeWeight(5);
    fill(0, 0, 0, opacity);
    textSize(12);
    if (name == focus) {
      stroke(150);
      if (stepCount/simulationSpeed % 30 == 0) {
        if (mode==0) {
          hl = "";
          mode = 1;
        } else {
          hl = "|";
          mode = 0;
        }
      }
      opacity = 200;
      show = text;
    } else {
      stroke(215);
      hl = "";
      if (text.length() == 0) {
        opacity = 100;
        show = alt;
      }
      out = text;
    }

    if (PApplet.parseFloat(text) > max) {
      text = str(max);
    } else if (PApplet.parseFloat(text) < min) {
      text = str(min);
    }

    fill(230);
    rect(x, y, max_char*10-10, 30);
    fill(0, 0, 0, opacity);
    text(show+hl, x+rand, y+20);
    fill(150);
    textSize(14);
    text(behind, x+max_char*10 +7 - 10, y+20);
    if (mousePressed && mouseX > x && mouseY > y && mouseX < max_char*10 - 10 + x && mouseY < 30+y && focus != name) {
      focus = name;
    } else if (mousePressed) {
      clicked[name] = true;
    }
  }
  public void updateShow() {
    show = text;
  }
  public void update_char(char character, int code) {
    if (name == focus) {
      if (code == 8) {
        if (text.length()>0) {
          text = text.substring(0, text.length()-1);
        }
      } else if (text.length()< max_char && code != 16 && code != 10 && (str(character).matches("[0-9]") || character == '.')) {
        text += str(character);
      }
    }
  }

  public void resetText() {
    text = str(standard);
    show = text;
    out = text;
  }
}

class button {
  float x, y;
  float w, h;
  int r, g, b;
  String name;
  button(float x_pos, float y_pos, float widthSize, float heightSize, String nameButton, int R, int G, int B) {
    x = x_pos;
    y = y_pos;
    w = widthSize;
    h = heightSize;
    name = nameButton;
    r = R;
    g = G;
    b = B;
  }

  public void drawButton() {
    textAlign(CENTER, CENTER);
    strokeWeight(5);
    textSize(12);
    if (mouseX > x && mouseX < x + w && mouseY > y && mouseY < y + h) {
      stroke(150);
    } else {
      stroke(215);
    }
    fill(r, g, b);
    rect(x, y, w, h);
    fill(70);
    text(name, x+(w/2), y+(h/2));
    strokeWeight(1);
  }

  public void checkClick() {
    if (mouseX > x && mouseX < x + w && mouseY > y && mouseY < y + h) {
      buttonClicked(name);
    }
  }
}

// === Buttons ===
button resetButton = new button(box[0]+box[2] - 90, box[1] + box[3] + 10, 90, 60, "Reset", 255, 255, 255);
button play1Button = new button(box[0] + 100, box[1] + box[3] + 10, 300, 60, "Pause", 255, 255, 255);
button play2Button = new button(box[0] + 100, box[1] + box[3] + 10, 300, 60, "Weiter", 255, 255, 255);
button startButton = new button(box[0], box[1] + box[3] + 10, 90, 60, "Start", 255, 255, 255);

button normalModeButton = new button(box[0], box[1] + box[3] + 80, box[2]/4 - 10, 60, "Normaler-Modus", 255, 255, 255);
button meetingModeButton = new button(box[0] + (box[2]/4 * 1), box[1] + box[3] + 80, box[2]/4 - 10, 60, "Treffpunkt-Modus", 255, 255, 255);
button groupModeButton = new button(box[0]  + (box[2]/4 * 2), box[1] + box[3] + 80, box[2]/4 - 10, 60, "Gruppen-Modus", 255, 255, 255);
button quarantineModeButton = new button(box[0]  + (box[2]/4 * 3), box[1] + box[3] + 80, box[2]/4, 60, "Quarantäne-Modus", 255, 255, 255);

// === Textfelder ===
int textfieldsX = 50, textfieldsY = 50;
Textfield personQuantityText = new Textfield(textfieldsX, textfieldsY, 13, personQuantity, 1, 10000, "Personenanzahl", "personQuantity");
Textfield startInfN = new Textfield(textfieldsX, textfieldsY+250, 0, startInfectedN, 1, personQuantity, "Infizierte am Anfang", "startInfectedN");
Textfield piText = new Textfield(textfieldsX, textfieldsY+50, 1, pi, 0, 1, "InfektionsWahrscheinlichkeit", "pi");
Textfield riText = new Textfield(textfieldsX, textfieldsY+100, 2, ri, 1, personSize*10, "Infektionsradius", "ri");
Textfield incuTimeText = new Textfield(textfieldsX, textfieldsY+150, 3, incubationTime, 0, 100, "Inkubationszeit in Stunden", "incubationTime");
Textfield infTimeText = new Textfield(textfieldsX, textfieldsY+200, 4, infectiousTime, 0, 100, "Ansteckungsfähigkeit in Tagen", "infectiousTime");
Textfield simSpeed = new Textfield(textfieldsX, textfieldsY+300, 5, simulationSpeed, 1, 100, "Simulationsgeschwindigkeit", "simulationSpeed");
Textfield distancingProbText = new Textfield(textfieldsX, textfieldsY+350, 12, distancingProb, 0, 100, "% sozial distancing", "distancingProb");

int meetingSettingsOffset = 80;
Textfield meetingProbText = new Textfield(textfieldsX, textfieldsY+330 + meetingSettingsOffset, 6, meetingProb, 0, 1, "Wahrscheinlichkeit für Treffen", "meetingProb");
Textfield meetingLenText = new Textfield(textfieldsX, textfieldsY+380 + meetingSettingsOffset, 7, meetingTime, 0, 1000, "Länge des Treffens", "meetingTime");

Textfield changinGrouProbText = new Textfield(textfieldsX, textfieldsY+450+meetingSettingsOffset, 8, changeGroupProb, 0, 1, "Wahrscheinlichkeit Gruppenwechsel", "changeGroupProb");

Textfield testingTimeText = new Textfield(textfieldsX, textfieldsY+600, 9, testingTime, 0, 1000, "Zeit für den Corona-Test", "testingTime");
Textfield quarantineDelayText = new Textfield(textfieldsX, textfieldsY+650, 10, quarantineDelay, 0, 1000, "Zeit, bis zum Start der Quarantäne", "quarantineDelay");
Textfield noSymptomsProbText = new Textfield(textfieldsX, textfieldsY+700, 11, noSymptomsProb, 0, personQuantity, "Wahrscheinlichkeit symptomlos", "noSymptomsProb");


public void buttonClicked(String name) { // der jeweilige Button ruft diese Funktion mit seinem Namen auf
  if (name == "Reset") {
    reset();
  } else if (name == "Pause") {
    running = false;
  } else if (name == "Weiter") {
    running = true;
  } else if (name == "Start") {
    chooseInfected(startInfectedN);
  } else if (name == "Normaler-Modus") {
    if (mode != normalMode) {
      mode = normalMode;
      reset();
    }
  } else if (name == "Treffpunkt-Modus") {
    if (mode != meetingMode) {
      mode = meetingMode;
      reset();
    }
  } else if (name == "Gruppen-Modus") {
    if (mode != groupMode) {
      mode = groupMode;
      reset();
    }
  } else if (name == "Quarantäne-Modus") {
    if (mode != quarantineMode) {
      mode = quarantineMode;
      reset();
    }
  }
}


public void mousePressed() {
  // === Buttons ===
  resetButton.checkClick();
  startButton.checkClick();

  if (running) {
    play1Button.checkClick();
  } else {
    play2Button.checkClick();
  }

  normalModeButton.checkClick();
  meetingModeButton.checkClick();
  groupModeButton.checkClick();
  quarantineModeButton.checkClick();

  for (boolean clickedNowhere : clicked) {
    if (clickedNowhere) {
      focus = -1;
    }
  }
}

public void keyPressed() {
  // === Textfelder ===
  personQuantityText.update_char(key, keyCode);
  startInfN.update_char(key, keyCode);
  piText.update_char(key, keyCode);
  riText.update_char(key, keyCode);
  incuTimeText.update_char(key, keyCode);
  infTimeText.update_char(key, keyCode);
  simSpeed.update_char(key, keyCode);
  distancingProbText.update_char(key, keyCode);

  meetingProbText.update_char(key, keyCode);
  meetingLenText.update_char(key, keyCode);
  changinGrouProbText.update_char(key, keyCode);

  testingTimeText.update_char(key, keyCode);
  quarantineDelayText.update_char(key, keyCode);
  noSymptomsProbText.update_char(key, keyCode);
  
  // === Tastenbefehle ===
  if (key == ' ') {
    if (running) {
      running = false;
    } else {
      running = true;
    }
  }
  if (keyCode == LEFT && PApplet.parseInt(simSpeed.text) > 1) {
    simSpeed.text = str(PApplet.parseFloat(simSpeed.text)-1);
    simSpeed.updateShow();
  } else if (keyCode == RIGHT && PApplet.parseInt(simSpeed.text) < 100) {
    simSpeed.text = str(PApplet.parseFloat(simSpeed.text)+1);
    simSpeed.updateShow();
  }
  if(key == 'r'){
    reset();
  }
  if(key == 's'){
    chooseInfected(startInfectedN);
  }
  
  // SEHR RECHENLASTIG!
  if(key == 'f'){
   finishSimulation(); 
  }
}
  public void settings() {  size(1200, 820); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "COVID19Simulator" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
