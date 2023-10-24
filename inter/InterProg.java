package inter;

import ast.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;

public class InterProg{

	private static ArrayList<Fun> funDefinitions = new ArrayList<Fun>() ;

  public static void interpret(Prog prog) throws Exception{
  
	Hashtable<String,IValue> state = new Hashtable<String,IValue>();

	Main main = prog.main;

	funDefinitions = prog.fun;

	initState(state, main.vars);
    
	interpretBlock(state, main.coms);
  	
  }

  public static void interpretBlock(Hashtable<String,IValue> state, ArrayList<Comando> coms) throws Exception{
	for (Comando com : coms) {
		interpretComando(state,com);
	}
  }
 
  public static void interpretComando(Hashtable<String,IValue> state, Comando com) throws Exception{
	if (com instanceof CPrint){
		CPrint print = (CPrint) com;
		IValue result = interpretExp(state,print.exp); 
		System.out.println(""+result);
	} else if(com instanceof CIf){
		CIf cif = (CIf) com;
		if(iboolToBoolean((IBool)interpretExp(state,cif.exp))) {
			interpretBlock(state,cif.bloco);
		}
	} else if(com instanceof CAtribuicao){
		CAtribuicao atrib = (CAtribuicao) com;
		IValue exp = interpretExp(state,atrib.exp);
		state.put(atrib.var,exp);
	} else if(com instanceof CReadInput){
		CReadInput readinput = (CReadInput) com;
		Scanner scanner = new Scanner(System.in);
        float number = scanner.nextFloat();
		state.put(readinput.var,new IFloat(number));
	}  else if(com instanceof CReturn){
		CReturn creturn = (CReturn) com;
		IValue exp = interpretExp(state,creturn.exp);
		state.put("__return__",exp);
	} else if(com instanceof CWhile){
		CWhile cwhile = (CWhile) com;
		boolean condwhile = iboolToBoolean((IBool)interpretExp(state,cwhile.exp));
		while (condwhile){
			interpretBlock(state,cwhile.bloco);
			condwhile = iboolToBoolean((IBool)interpretExp(state,cwhile.exp));
		}
		
	} else if(com instanceof CChamadaFun){
		CChamadaFun chamada = (CChamadaFun) com;
        Fun fun = getFunctionDefinition(chamada.fun);
		Hashtable<String,IValue> statefun = newStateFun (state,fun.params,chamada.args);
		initState(statefun,fun.vars);
        interpretBlock(statefun,fun.body);
	}
  }

  public static IValue interpretExp(Hashtable<String,IValue> state, Exp exp) throws Exception{
	IValue result = null;
	if (exp instanceof EFloat){
		//System.out.println("Aqui");
		result = new IFloat(((EFloat)exp).value);
	} else if(exp instanceof EVar) {
		result = state.get(((EVar)exp).var);
	} else if(exp instanceof ETrue) {
		result = new ITrue();
	} else if (exp instanceof EFalse){
		result = new IFalse();
	} else if (exp instanceof EOpExp){
		EOpExp opexp = (EOpExp) exp;
		IFloat farg1;
		IFloat farg2; 
		IBool barg1;
		IBool barg2;
		switch (opexp.op) {
			case "+":
				farg1 = (IFloat) interpretExp(state, opexp.arg1);
				farg2 = (IFloat) interpretExp(state, opexp.arg2);
				result = new IFloat(farg1.value+farg2.value);
				break;
			case "-":
				farg1 = (IFloat) interpretExp(state, opexp.arg1);
				farg2 = (IFloat) interpretExp(state, opexp.arg2);
				result = new IFloat(farg1.value-farg2.value);
				break;
			case "*":
				farg1 = (IFloat) interpretExp(state, opexp.arg1);
				farg2 = (IFloat) interpretExp(state, opexp.arg2);
				result = new IFloat(farg1.value*farg2.value);
				break;
			case "/":
				farg1 = (IFloat) interpretExp(state, opexp.arg1);
				farg2 = (IFloat) interpretExp(state, opexp.arg2);
				result = new IFloat(farg1.value/farg2.value);
				break;
			case "%":
				farg1 = (IFloat) interpretExp(state, opexp.arg1);
				farg2 = (IFloat) interpretExp(state, opexp.arg2);
				result = new IFloat(farg1.value%farg2.value);
				break;
			case "<":
				farg1 = (IFloat) interpretExp(state, opexp.arg1);
				farg2 = (IFloat) interpretExp(state, opexp.arg2);
				if(farg1.value<farg2.value)
					result = new ITrue();
				else
					result = new IFalse();
				break;
			case "<=":
				farg1 = (IFloat) interpretExp(state, opexp.arg1);
				farg2 = (IFloat) interpretExp(state, opexp.arg2);
				if(farg1.value<=farg2.value)
					result = new ITrue();
				else
					result = new IFalse();
				break;
			case ">":
				farg1 = (IFloat) interpretExp(state, opexp.arg1);
				farg2 = (IFloat) interpretExp(state, opexp.arg2);
				if(farg1.value>farg2.value)
					result = new ITrue();
				else
					result = new IFalse();
				break;
			case ">=":
				farg1 = (IFloat) interpretExp(state, opexp.arg1);
				farg2 = (IFloat) interpretExp(state, opexp.arg2);
				if(farg1.value>=farg2.value)
					result = new ITrue();
				else
					result = new IFalse();
				break;
			case "|":
				barg1 = (IBool) interpretExp(state, opexp.arg1);
				barg2 = (IBool) interpretExp(state, opexp.arg2);
				if(iboolToBoolean(barg1)||iboolToBoolean(barg2))
					result = new ITrue();
				else
					result = new IFalse();
				break;
			case "&":
				barg1 = (IBool) interpretExp(state, opexp.arg1);
				barg2 = (IBool) interpretExp(state, opexp.arg2);
				if(iboolToBoolean(barg1)&&iboolToBoolean(barg2))
					result = new ITrue();
				else
					result = new IFalse();
				break;
			case "=":
				IValue arg1 =  interpretExp(state, opexp.arg1);
				IValue arg2 =  interpretExp(state, opexp.arg2);
				if(arg1 instanceof IBool && arg2 instanceof IBool)
				{
				   if(iboolToBoolean((IBool) arg1)==iboolToBoolean((IBool)arg2))
					  result = new ITrue();
				   else
					  result = new IFalse();
				} else if(arg1 instanceof IFloat && arg2 instanceof IFloat) {
					farg1 = (IFloat) arg1;
				    farg2 = (IFloat) arg2;
					if(farg1.value==farg2.value)
						result = new ITrue();
					else
						result = new IFalse();
				} else throw new Exception ("Comparando valores de tipos diferentes");
				break;
			default:
				throw new Exception("Invalid operator: " + opexp.op);
		}

	}  else if(exp instanceof EChamadaFun){
		EChamadaFun chamada = (EChamadaFun) exp;
        Fun fun = getFunctionDefinition(chamada.fun);
		Hashtable<String,IValue> statefun = newStateFun (state,fun.params,chamada.args);
		initState(statefun,fun.vars);
        interpretBlock(statefun,fun.body);
		result = statefun.get("__return__");
	}

	return result;
  }

  public static boolean iboolToBoolean(IBool value){
	if (value instanceof ITrue)
	  return true;
	else return false;
  }

  public static Fun getFunctionDefinition(String name){
	 for (Fun fun : funDefinitions) {
		if (fun.nome.equals(name))
			return fun;
	 }
	 return null;
  }

  public static Hashtable<String,IValue> newStateFun (Hashtable<String,IValue> state, ArrayList<ParamFormalFun> paramFormais, ArrayList<Exp> paramAtuais) throws Exception{
	if (paramAtuais.size() != paramAtuais.size())
			throw new Exception("Chamada de função com aridade errada!");
		
	Hashtable<String,IValue> funstate = new Hashtable<String,IValue>();

	for(int i=0;i<paramAtuais.size(); i++)
	{
		ParamFormalFun paraf = paramFormais.get(i);
		Exp param = paramAtuais.get(i);
		IValue valor = interpretExp(state,param);
		funstate.put(paraf.var,valor);
	}

	return funstate;
  }
  public static void initState(Hashtable<String,IValue> state, ArrayList<VarDecl> vars) throws Exception{
	
	for (VarDecl vardecl : vars) {
		switch (vardecl.type) {
			case "Bool":
				state.put(vardecl.var,new IFalse());
				break;
			case "Float":
				state.put(vardecl.var,new IFloat(0));
				break;
			default:
				throw new Exception("Invalid type: " + vardecl.type);
		}
	}
  }

}


class IValue{}
class IFloat extends IValue{
	float value;
	IFloat(float value) {
	  	this.value = value;
	}
	public String toString(){
		return (""+value);
	}
}
class IBool extends IValue{}
class ITrue extends IBool{
	public String toString(){
		return ("True");
	}
}
class IFalse extends IBool{
	public String toString(){
		return ("False");
	}
}
class IVoid extends IValue{}
