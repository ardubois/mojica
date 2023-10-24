package ast;

import java.util.ArrayList;

public class CPrint extends Comando{
	public int linha;
	public Exp exp;
	
	
	public CPrint(int linha,Exp exp)
	{
	  this.linha = linha;
	  this.exp = exp;
	  
	} 

}
