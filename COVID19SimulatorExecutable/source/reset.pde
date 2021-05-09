void reset() {
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
  initDistancing(int(distancingProbText.out));
  distancingProb = int(distancingProbText.out);

  // Graph
  infTimeline = new IntList();
  remTimeline = new IntList();
  casesGraph.setMaxNumber(personQuantity);
  removedGraph.setMaxNumber(personQuantity);

  // Modi
  quarantineStarted = false;
}
