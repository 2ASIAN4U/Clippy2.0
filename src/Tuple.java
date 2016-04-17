public class Tuple<X, Y, Z> { 
  public final X x; 
  public final Y y; 
  public final Z z; 
  public Tuple(X x, Y y, Z z) { 
    this.x = x; 
    this.y = y; 
    this.z = z;
  }
  public String toString(){
	  return "("+x+","+y+","+z+")";
  }
  public int x(){
	  return (int)x;
  }
  public int y(){
	  return (int)y;
  }
  public int z(){
	  return (int)z;
  }
} 