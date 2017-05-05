import java.util.Random;
import java.util.concurrent.Semaphore;

public class ElevatorSynch extends Thread {
  public static int id;
  public static int occupant;
  public static int occupantid;
  public static int current;
  public static int n;
  public static int m;
  public static int k;
  public static int t;
  public static int[] next = new int[3];
  public static int[] totalPeople = new int[5];
  public static int[] goTo = new int[5];
  public static boolean goUp;
  public static boolean[] call = new boolean[5];
  public static Semaphore mtx = new Semaphore(1, false);
  public static Semaphore[] floors = new Semaphore[5];
  public static Occupant[] occupants = new Occupant[20];
  public static Random rand = new Random();
  public static ElevatorSynch es;
  
  public ElevatorSynch(int a, int b) {
    id = a;
    occupant = b;
  }
  
  public void run() {
    while (true) {
      Synch(id);
    }
  }
  
  public static void Synch(int n) {
    System.out.println("Elevator on floor: " + current);
    if (goUp && current == m - 1) {
      goUp = false;
    }
    if (!goUp && current == 0) {
      goUp = true;
    }
    for (int i = 0; i < 20; i++) {
      occupants[i].floorPrev = false;
    }
    floorCheck();
    simulate();
    loader();
    semCheck();
  }
  
  public static int call(int i) {
    Random y = new Random();
    int pnd = i;
    while (pnd == i) {
      pnd = y.nextInt(4);
    }
    goTo[pnd]++;
    return pnd;
  }
  
  public static void floorCheck() {
    for (int i = 0; i < n; i++) {
      if (occupants[i].floorNext != -1) {
        System.out.println("Occupant " + occupants[i].id + " is in elevator, requesting floor " + occupants[i].floorNext);
      } else {
        System.out.println("Occupant " + occupants[i].id + " is on floor " + occupants[i].floorOn);
      }
    }
    for (int i = 0; i < m; i++) {
      System.out.println("People on floor " + i + ": " + totalPeople[i] + " and " + goTo[i] + " people requesting floor");
    }
    System.out.println("Occupants: " + occupant);
  }
  
  public static void simulate() {
    for (int i = 0; i < m; i++) {
      if (totalPeople[i] != 0) {
        call[i] = true;
      } else if (goTo[i] != 0) {
        call[i] = true;
      } else {
        call[i] = false;
      }
    }
  }
  
  public static void loader() {
    System.out.println("Doors open");
    if (goTo[current] != 0 && occupant >= goTo[current]) {
      totalPeople[current] = goTo[current] + totalPeople[current];
      occupant = occupant - goTo[current];
      goTo[current] = 0;
      for (int i = 0; i < n; i++) {
        try {
          mtx.acquire();
        } catch (InterruptedException e) {}
        System.out.println("Occupant leaving elevator");
        if (occupants[i].floorOn == -1 && current == occupants[i].floorNext) {
          occupants[i].floorOn = current;
          occupants[i].floorNext = -1;
          occupants[i].floorPrev = true;
          try {
            occupants[i].sleep(1000 * t);
          } catch (InterruptedException e) {}
        }
        mtx.release();
      }
    } else if (goTo[current] != 0 && occupant < goTo[current]) {
      if (occupant != 0) {
        totalPeople[current] = occupant + totalPeople[current];
        goTo[current] = goTo[current] - occupant;
        occupant = 0;
        for (int i = 0; i < n; i++) {
          try {
            mtx.acquire();
          } catch (InterruptedException e) {}
          if (occupants[i].floorOn == -1 && current == occupants[i].floorNext) {
            occupants[i].floorOn = current;
            occupants[i].floorNext = -1;
            occupants[i].floorPrev = true;
            try {
              occupants[i].sleep(1000 * t);
            } catch (InterruptedException e) {}
          }
          mtx.release();
        }
      }
    }
    int available = totalPeople[current];
    int removed = 0;
    int numRemoved = 0;
    for (int i = 0; i < 20; i++) {
      if (occupants[i].floorOn == current && occupants[i].floorPrev) {
        available--;
        removed++;
      }
    }
    if (call[current] && k > occupant) {
      if (available > k - occupant) {
        for (int i = 0; i < (k - occupant); i++) {
          try {
            mtx.acquire();
          } catch (InterruptedException e) {}
          next[i] = call(current);
          mtx.release();
        }
        numRemoved = k - occupant;
        totalPeople[current] = (totalPeople[current] - (k - occupant)) + removed;
        occupant = 3;
      } else {
        if (available == k - occupant) {
          for (int i = 0; i < (k - occupant); i++) {
            try {
              mtx.acquire();
            } catch (InterruptedException e) {}
            next[i] = call(current);
            mtx.release();
          }
          numRemoved = available;
          totalPeople[current] = removed;
          occupant = 3;
        } else if (available < k - occupant) {
          for (int i = 0; i < totalPeople[current]; i++) {
            try {
              mtx.acquire();
            } catch (InterruptedException e) {}
            next[i] = call(current);
            mtx.release();
          }
          numRemoved = available;
          occupant = occupant + available;
          totalPeople[current] = removed;
        }
      }
      int numLeft = 0;
      for (int i = 0; i < n; i++) {
        if (!occupants[i].floorPrev && occupants[i].floorOn == current) {
          occupants[i].floorOn = -1;
          occupants[i].floorNext = next[numLeft];
          System.out.println("Occupant " + occupants[i].id + " requesting floor " + occupants[i].floorNext);
          numLeft++;
          if (numRemoved == numLeft) {
            break;
          }
        }
      }
    }
  }
  
  public static void semCheck() {
    int checker = current;
    if (goUp) {
      for (int i = 1 + current; i < m; i++) {
        if (call[i]) {
          try {
            floors[i].acquire();
          } catch (InterruptedException e) {}
          current = i;
          floors[i].release();
          break;
        }
      }
    }
    if (!goUp) {
      for (int i = current - 1; i > -1; i--) {
        if (call[i]) {
          try {
            floors[i].acquire();
          } catch (InterruptedException e) {}
          current = i;
          floors[i].release();
          break;
        }
      }
    }
    if (current != checker) {
      System.out.println("Going to floor " + current);
      try {
        es.sleep(Math.abs(1000 * (checker - current)));
      } catch (InterruptedException e) {}
    } else {
      System.out.println("Elevator resting");
    }
  }
  
  public static void main(String[] args) {
    System.out.println("Initializing");
    current = 0;
    n = 20;
    m = 5;
    k = 3;
    t = 2;
    goUp = true;
    
    for (int i = 0; i < 5; i++) {
      floors[i] = new Semaphore(1, false);
    }
    for (int i = 0; i < n; i++) {
      occupants[i] = new Occupant(0, 0, -1, false);
    }
    for (int i = 0; i < n; i++) {
      occupants[i].id = i;
    }
    for (int i = 0; i < k; i++) {
      next[i] = 0;
    }
    Random x = new Random();
    int rp = n;
    int lp = 0;
    for (int i = 0; i < m; i++) {
      if (i == m - 1) {
        for (int j = lp; j < n; j++) {
          occupants[j].floorOn = i;
        }
        totalPeople[i] = rp;
      } else { 
        int ppl = x.nextInt(rp);
        totalPeople[i] = ppl;
        for (int j = lp; j < ppl + lp; j++) {
          occupants[j].floorOn = i;
        }
        lp = ppl + lp;
        rp = rp - ppl;
      }
    }
    es = new ElevatorSynch(0, 0);
    es.start();
  }
}