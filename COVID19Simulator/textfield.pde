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
  void updateBox() {
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

    if (float(text) > max) {
      text = str(max);
    } else if (float(text) < min) {
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
  void updateShow() {
    show = text;
  }
  void update_char(char character, int code) {
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

  void resetText() {
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

  void drawButton() {
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

  void checkClick() {
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


void buttonClicked(String name) { // der jeweilige Button ruft diese Funktion mit seinem Namen auf
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


void mousePressed() {
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

void keyPressed() {
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
  if (keyCode == LEFT && int(simSpeed.text) > 1) {
    simSpeed.text = str(float(simSpeed.text)-1);
    simSpeed.updateShow();
  } else if (keyCode == RIGHT && int(simSpeed.text) < 100) {
    simSpeed.text = str(float(simSpeed.text)+1);
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
