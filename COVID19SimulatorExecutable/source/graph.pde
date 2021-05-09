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

  void drawGraph(IntList input) {
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
  void setMaxNumber(int n){
    max = n;
  }
}
