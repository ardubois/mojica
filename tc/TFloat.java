package tc;

public class TFloat extends TType{

	public String toString(){
		return ("Float");
	}
	public boolean equals(Object o)
	{   return (o instanceof TFloat);
	}
}