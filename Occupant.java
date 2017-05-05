public class Occupant extends Thread {
  
  int id;
  int floorOn;
  int floorNext;
  boolean floorPrev;
  
  public Occupant(int i, int o, int n, boolean p) {
    id = i;
    floorOn = o;
    floorNext = n;
    floorPrev = p;
  }
  
  public int getID() {
    return id;
  }
  
  public int getFloorOn() {
    return floorOn;
  }
  
  public int getFloorNext() {
    return floorNext;
  }
}