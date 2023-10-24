package tc;
public class TBool extends TType{
	public String toString(){
		return ("Bool");
	}
	public boolean equals(Object o)
	{
    	return (o instanceof TBool);
	}
}