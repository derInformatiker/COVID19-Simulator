void initPersons() {
  persons = new person[personQuantity];
  for (int i = 0; i < persons.length; i++) {

    persons[i] = new person(0, 0, personSize, i);
  }
  //chooseInfected(startInfectedN);
}

void movePersons() { // Führt bei allen Personen move() aus
  noStroke();
  fill(255);
  for (int i = 0; i < persons.length; i++) {
    persons[i].move();
  }
}

void drawPersons() { // Führt bei allen Personen drawPerson() aus
  noStroke();
  for (int i = 0; i < persons.length; i++) {
    persons[i].drawPerson();
  }
}

void initDistancing(int p) { // Führt bei allen Personen initDistancing() aus
  for (int i = 0; i < persons.length; i++) {
    persons[i].initDistancing(p);
  }
}

void chooseInfected(int n) { // Wählt Zufällige, die Infiziert werden
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
    vecX = random(-1, 1.3);
    vecY = random(-1, 1.3);
    speed = personSpeed;
    step = turnStep;
    randomPos(box);

    if (mode == groupMode) {
      groupN = (int) random(0, 4);
      randomPos(groups[groupN]);
    }
    goalX = int(x + random(-20, 20));
    goalY = int(y + random(-20, 20));
    goalX = constrain(goalX, box[0]+goalGap, box[0]+box[2]-goalGap);
    goalY = constrain(goalY, box[1]+goalGap, box[1]+box[3]-goalGap);
  }

  void move() {
    if (h > returnTime && returnJump) {
      this.jumpTo(oldX, oldY); 
      returnJump = false;
    }
    if (jumpX == 0 && jumpY == 0) {
      float stepX = map(abs(goalX-x), 0, box[2], 0.1, 1) * step;
      float stepY = map(abs(goalY-y), 0, box[3], 0.1, 1) * step;

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
      if (int(x) == int(jumpX)) {
        vecJumpX = 0;
      } else if (x < jumpX) {
        vecJumpX += jumpStep;
      } else if (x > jumpX) {
        vecJumpX -= jumpStep;
      }

      if (int(y) == int(jumpY)) {
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

  void checkCollision(int[] b) {
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

  void drawPerson() {
    if (radiusAnimationSize != -1) {
      stroke(244, 103, 83);
      noFill();
      ellipse(x, y, radiusAnimationSize, radiusAnimationSize);
      noStroke();
      if (running) {
        radiusAnimationSize += framesPerSecond/600.0;
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

  void jumpTo(int toX, int toY) {
    jumpX = toX;
    jumpY = toY;
    vecJumpX = 0;
    vecJumpY = 0;
  }

  void jumpToTemp(int toX, int toY, int hours) {
    if (!returnJump) {
      jumpTo(toX, toY);
      oldX = goalX;
      oldY = goalY;
      returnTime = h + hours;
      returnJump = true;
    }
  }

  void writeContacts() {
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

  void personInfected() {
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
  void changeGoal() {
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

  void randomPos(int[] walls) {
    x = random(walls[0]+gapToWall, walls[0]+walls[2]-gapToWall);
    y = random(walls[1]+gapToWall, walls[1]+walls[3]-gapToWall);

    if (x > meetingBox[0] && x < meetingBox[0] + meetingBox[2] && y > meetingBox[1] && y < meetingBox[1] + meetingBox[3]) {
      x += meetingBox[2];
      y += meetingBox[3];
    }
  }

  void initDistancing(int p) {
    if ((int) random(0, 100) < p) {
      distancing = true;
    } else {
      distancing = false;
    }
  }
}
