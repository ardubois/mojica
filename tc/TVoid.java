package tc;

public class TVoid extends TType{
	public String toString(){
		return ("Void");
	}
	public boolean equals(Object o)
	{
    	return (o instanceof TVoid);
	}
}
