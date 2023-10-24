package ast;

import java.util.ArrayList;

public class CReturn extends Comando{
	public int linha;
	public Exp exp;
	
	
	public CReturn(int linha,Exp exp)
	{
	  this.linha = linha;
	  this.exp = exp;
	  
	} 

}
