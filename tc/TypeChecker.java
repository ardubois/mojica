package tc;

import ast.*;

import java.util.ArrayList;
import java.util.Hashtable;

public class TypeChecker{

	private static Hashtable<String,TFun> funTypes = new Hashtable<String,TFun>(); 

public static void typeCheckProg(Prog prog) throws TypeException{
  
	Main main = prog.main;
	funTypes = genFunTypes(prog.fun);
	try{
		typeCheckMain(main);
	} catch(TypeException e)
	{ throw new TypeException("ERRO main:\n"+e);}

	typeCheckFuns(prog.fun);
}

static void typeCheckFuns(ArrayList<Fun> funs) throws TypeException{
	
	for (Fun fun : funs) {
		try{
			typeCheckFun(fun);
		} catch(TypeException e)
		{ throw new TypeException("ERRO função " + fun.nome + ":\n"+e);}
	}

}

static void typeCheckFun(Fun fun) throws TypeException{
	Hashtable<String,TType> env = new Hashtable<String,TType>();
	initEnvParamFormal(env,fun.params);
	initEnv(env,fun.vars);
	TType typeBloco = typeCheckBlock(env,fun.body);
	if(!typeBloco.equals(toTType(fun.retorno)))
	     throw new TypeException("Função "+fun.nome+" declarada com tipo de retorno "+ toTType(fun.retorno)+ " está retornando tipo " +typeBloco + ".");
}
static void typeCheckMain(Main main) throws TypeException{
	Hashtable<String,TType> env = new Hashtable<String,TType>();
	initEnv(env,main.vars);
	TType typeBloco = typeCheckBlock(env,main.coms);
	if(!typeBloco.equals(new TVoid()))
	     throw new TypeException("Bloco de comandos do main não deve retornar valor.");
}

public static void initEnvParamFormal(Hashtable<String,TType> state, ArrayList<ParamFormalFun> vars) throws TypeException{
	
	for (ParamFormalFun vardecl : vars) {
		switch (vardecl.type) {
			case "Bool":
			    if(state.get(vardecl.var)==null)
					state.put(vardecl.var,new TBool());
				else
					throw new TypeException("Varável "+vardecl.var+" já existe.");
				break;
			case "Float":
				if(state.get(vardecl.var)==null)
					state.put(vardecl.var,new TFloat());
				else
					throw new TypeException("Varável "+vardecl.var+" já existe.");
				break;
			default:
				throw new TypeException("Tipo inválido para declaração de variáveis: " + vardecl.type);
		}
	}
  }

public static void initEnv(Hashtable<String,TType> state, ArrayList<VarDecl> vars) throws TypeException{
	
	for (VarDecl vardecl : vars) {
		switch (vardecl.type) {
			case "Bool":
			    if(state.get(vardecl.var)==null)
					state.put(vardecl.var,new TBool());
				else
					throw new TypeException("Varável "+vardecl.var+" já existe.");
				break;
			case "Float":
				if(state.get(vardecl.var)==null)
					state.put(vardecl.var,new TFloat());
				else
					throw new TypeException("Varável "+vardecl.var+" já existe.");
				break;
			default:
				throw new TypeException("Tipo inválido para declaração de variáveis: " + vardecl.type);
		}
	}
  }

public static Hashtable<String,TFun> genFunTypes (ArrayList<Fun> funs) throws TypeException{

	Hashtable<String,TFun> funTypes = new Hashtable<String,TFun>(); 

	for (Fun fun : funs) {
		if(funTypes.get(fun.nome)!= null)
		  throw new TypeException("Função " + fun.nome + " possui mais de uma definição.");
		funTypes.put(fun.nome, new TFun(toTType(fun.retorno), toTParamFormalFun(fun.params))) ; 
	}
    return funTypes;	
}

static ArrayList<TParamFormalFun> toTParamFormalFun(ArrayList<ParamFormalFun> map) throws TypeException{
	ArrayList<TParamFormalFun> newMap = new ArrayList<TParamFormalFun>();
	for (ParamFormalFun para : map) {
		newMap.add(new TParamFormalFun(toTType(para.type),para.var));
	}
	return newMap;
}

public static TType toTType(String stype) throws TypeException{
	TType ttype=null;
	switch(stype){
		case "Float" : 
			ttype = new TFloat();
			break;
		case "Bool" : 
			ttype = new TBool();
			break;
		case "Void" : 
			ttype = new TVoid();
			break;
		default:
			throw new TypeException("IMPOSSIBLE: Invalid type: " + stype + ".");

	
	}
	return ttype;
}

static TType typeCheckBlock(Hashtable<String,TType>  env, ArrayList<Comando> coms) throws TypeException{

	if(coms.size()==0)
		{ throw new TypeException("Bloco de comandos deve possuir pelo menos um comando.");}
	for(int i=0;i<coms.size()-1;i++){
		TType typeCom = typeCheckCom(env,coms.get(i));
		if(!typeCom.equals(new TVoid()))
			throw new TypeException("Comandos internos em um bloco devem possuir tipo void.");
	}

	Comando ultimo = coms.get(coms.size()-1);
	return typeCheckCom(env,ultimo);

}


static TType typeCheckCom(Hashtable<String,TType>  env, Comando com) throws TypeException{
	
	TType resposta = new TVoid();
	
	if(com instanceof CAtribuicao){
		resposta = typeCheckAtribuicao(env, (CAtribuicao) com);
	} else if(com instanceof CPrint){
		resposta = typeCheckPrint(env,(CPrint) com);
	} else if(com instanceof CReadInput){
		resposta = typeCheckReadInput(env,(CReadInput) com);
	} else if(com instanceof CReturn){
		resposta = typeCheckReturn(env,(CReturn) com);
	} else if(com instanceof CWhile){
		resposta = typeCheckWhile(env,(CWhile) com);
	} else if(com instanceof CIf){
		resposta = typeCheckIf(env,(CIf) com);
	}  else if(com instanceof CChamadaFun){
		CChamadaFun chamada = (CChamadaFun) com;
		 TFun tipoFuncao = funTypes.get(chamada.fun);
		 if (tipoFuncao == null){
			throw new TypeException("LINHA " + chamada.linha + ": função " + chamada.fun + " não definida.");
		 }
		resposta =  typeCheckFunCallCom(env,chamada,tipoFuncao,chamada.args);
		
	}
	
   return resposta;
}

static TType typeCheckFunCallCom(Hashtable<String,TType>  env, CChamadaFun cchamada, TFun tipoFuncao, ArrayList<Exp> paramAtual) throws TypeException{
	ArrayList<TParamFormalFun> paramFormal = tipoFuncao.params;
	if(paramFormal.size() != paramAtual.size())
		throw new TypeException("LINHA " + cchamada.linha + ": Chamada de função com o número de argumentos errado.");
	try{
		for(int i=0; i< paramFormal.size(); i++){
			TType tipoFormal = paramFormal.get(i).type;
			TType tipoAtual = typeCheckExp(env,paramAtual.get(i));
			if(!(tipoFormal.equals(tipoAtual)))
				throw new TypeException("LINHA " + cchamada.linha + ": " +"Erro na chamada de função, parâmetro formal possui tipo "+ tipoFormal + " e o argumento atual " + expToString(paramAtual.get(i))+ " possui tipo "+ tipoAtual + ".");
		}
	}catch(TypeExceptionExp e){
        throw new TypeException("LINHA " + cchamada.linha + ": " + e);
	}
	return tipoFuncao.retorno;
 }
 

static TType typeCheckIf(Hashtable<String,TType>  env, CIf cif) throws TypeException{
	TType resposta = new TVoid();
	try{
		TType typeExp = typeCheckExp(env,cif.exp);
		if(!typeExp.equals(new TBool()))
	     throw new TypeException("LINHA " + cif.linha + ": Expressão argumento do if deve possuir tipo Bool e a expressão " + expToString(cif.exp) + " possui tipo "+ typeExp+".");
		TType typeBloco = typeCheckBlock(env,cif.bloco);
		if(!typeBloco.equals(new TVoid()))
	     throw new TypeException("LINHA " + cif.linha + ": Bloco de comandos do if não deve retornar valor.");
		
	}catch(TypeExceptionExp e){
        throw new TypeException("LINHA " + cif.linha + ": " + e);
	}
	return resposta;
}


static TType typeCheckWhile(Hashtable<String,TType>  env, CWhile cwhile) throws TypeException{
	TType resposta = new TVoid();
	try{
		TType typeExp = typeCheckExp(env,cwhile.exp);
		if(!typeExp.equals(new TBool()))
	     throw new TypeException("LINHA " + cwhile.linha + ": Expressão argumento do while deve possuir tipo Bool e a expressão " + expToString(cwhile.exp) + " possui tipo "+ typeExp+".");
		TType typeBloco = typeCheckBlock(env,cwhile.bloco);
		if(!typeBloco.equals(new TVoid()))
	     throw new TypeException("LINHA " + cwhile.linha + ": Bloco de comandos do while não deve retornar valor." + typeBloco);
		
	}catch(TypeExceptionExp e){
        throw new TypeException("LINHA " + cwhile.linha + ": " + e);
	}
	return resposta;
}

static TType typeCheckReturn(Hashtable<String,TType>  env, CReturn creturn) throws TypeException{
	TType typeExp = null;
	try{
		typeExp = typeCheckExp(env,creturn.exp);
		
	}catch(TypeExceptionExp e){
        throw new TypeException("LINHA " + creturn.linha + ": " + e);
	}
	return typeExp;
}

static TType typeCheckReadInput(Hashtable<String,TType>  env, CReadInput read) throws TypeException{
	
	TType typeVar = env.get(read.var);
	if(typeVar == null)
	     throw new TypeException("LINHA " + read.linha + ": Variável " + read.var + " não declarada.");
	if(!typeVar.equals(new TFloat()))
	     throw new TypeException("LINHA " + read.linha + ": Variável " + read.var + " deve possuir tipo Float.");
	
	return new TVoid();
}



static TType typeCheckPrint(Hashtable<String,TType>  env, CPrint print) throws TypeException{
try{
		TType typeExp = typeCheckExp(env,print.exp);
		if(!(typeExp.equals(new TFloat()) || typeExp.equals(new TBool())))
			throw new TypeException("LINHA " + print.linha + ": output apenas para tipo Float ou Bool.");
	
	}catch(TypeExceptionExp e){
        throw new TypeException("LINHA " + print.linha + ": " + e);
	}
	return new TVoid();
}

static TType typeCheckAtribuicao(Hashtable<String,TType>  env, CAtribuicao atrib) throws TypeException{
	
	TType typeVar = env.get(atrib.var);
	if(typeVar == null)
	     throw new TypeException("LINHA " + atrib.linha + ": Variável " + atrib.var + " não declarada.");
	try{
		TType typeExp = typeCheckExp(env,atrib.exp);
		if(!typeVar.equals(typeExp))
			throw new TypeException("LINHA " + atrib.linha + ": Variável " + atrib.var + " possui tipo " + typeVar + " e expressão pussi tipo " + typeExp +".");
	
	}catch(TypeExceptionExp e){
        throw new TypeException("LINHA " + atrib.linha + ": " + e);
	}
	return new TVoid();
}

public static TType typeCheckExp(Hashtable<String,TType>  env, Exp exp) throws TypeExceptionExp{
	if (exp instanceof EFloat){
		//System.out.println("Aqui");
		return (new TFloat());
	} else if(exp instanceof EVar) {
	  String varName = ((EVar)exp).var;
	  TType typeVar = env.get(varName);
	  if(typeVar != null){
	   return typeVar;
	  }
	  throw new TypeExceptionExp("Variável " + varName + " não foi declarada.");
	} else if(exp instanceof ETrue) {
		return new TBool();
	} else if (exp instanceof EFalse){
		return new TBool();
	} else if (exp instanceof EOpExp){
		EOpExp opexp = (EOpExp) exp;
		TType targ1 = typeCheckExp(env,opexp.arg1);
		TType targ2 = typeCheckExp(env,opexp.arg2);
 		TType topexp;
		switch (opexp.op) {
			case "+":
			case "-":
			case "*":
			case "/":
			case "%":
			  	if(!(targ1 instanceof TFloat))
			   	{
			     throw new TypeExceptionExp("A expressão " + expToString(opexp.arg1) + " possui tipo " + targ1 + " e deveria ter tipo Float.");
			   	}
			  	if(!(targ2 instanceof TFloat))
			   	{
			     throw new TypeExceptionExp("A expressão " + expToString(opexp.arg2) + " possui tipo " + targ2 + " e deveria ter tipo Float.");
			   	} 
				topexp = new TFloat(); 
				break;
			case "<":
			case "<=":
			case ">":
			case ">=":
				if(!(targ1 instanceof TFloat))
			   	{
			     throw new TypeExceptionExp("A expressão " + expToString(opexp.arg1) + " possui tipo " + targ1 + " e deveria ter tipo Float.");
			   	}
			  	if(!(targ2 instanceof TFloat))
			   	{
			     throw new TypeExceptionExp("A expressão " + expToString(opexp.arg2) + " possui tipo " + targ2 + " e deveria ter tipo Float.");
			   	} 
				topexp = new TBool();
				break;
			case "|":
			case "&":
			  	if(!(targ1 instanceof TBool))
			   	{
			     throw new TypeExceptionExp("A expressão " + expToString(opexp.arg1) + " possui tipo " + targ1 + " e deveria ter tipo Bool.");
			   	}
			  	if(!(targ2 instanceof TBool))
			   	{
			     throw new TypeExceptionExp("A expressão " + expToString(opexp.arg2) + " possui tipo " + targ2 + " e deveria ter tipo Bool.");
			   	} 
				topexp = new TBool();
			    break;
			case "=":
			    if(!targ1.equals(targ2))
			      {
			        throw new TypeExceptionExp("A expressão " + expToString(opexp.arg1) + " possui tipo " + targ1 + " e a expressão " + expToString(opexp.arg2) + " possui tipo " + targ2 + ", por isso não podem ser comparadas.");
			      }
			    if(!(targ1 instanceof TBool || targ1 instanceof TFloat))
			    {
			      throw new TypeExceptionExp("A expressão " + expToString(opexp.arg1) + " possui tipo " + targ1 + ". O tipo " + targ1 + " não é comparável.");
			    }
			    if(!(targ2 instanceof TBool || targ2 instanceof TFloat))
			    {
			      throw new TypeExceptionExp("A expressão " + expToString(opexp.arg2) + " possui tipo " + targ2 + ". O tipo " + targ2 + " não é comparável.");
			    }
			    topexp = new TBool();
				break;
			default:
				throw new TypeExceptionExp("IMPOSSIBLE: Invalid operator: " + opexp.op);
		 	}
			return topexp;
	}else if(exp instanceof EChamadaFun){
		 EChamadaFun chamada = (EChamadaFun) exp;
		 TFun tipoFuncao = funTypes.get(chamada.fun);
		 if (tipoFuncao == null){
			throw new TypeExceptionExp("Função " + chamada.fun + " não definida.");
		 }
		 return typeCheckFunCallExp(env,tipoFuncao,chamada.args);
    }
	return new TType();
}

 static TType typeCheckFunCallExp(Hashtable<String,TType>  env,TFun tipoFuncao, ArrayList<Exp> paramAtual) throws TypeExceptionExp{
	ArrayList<TParamFormalFun> paramFormal = tipoFuncao.params;
	if(paramFormal.size() != paramAtual.size())
		throw new TypeExceptionExp("Chamada de função com o número de argumentos errado.");
	for(int i=0; i< paramFormal.size(); i++){
		TType tipoFormal = paramFormal.get(i).type;
		TType tipoAtual = typeCheckExp(env,paramAtual.get(i));
		if(!(tipoFormal.equals(tipoAtual)))
			throw new TypeExceptionExp("Errono na chamada de função, parâmetro formal possui tipo "+ tipoFormal + " e o argumento atual " + expToString(paramAtual.get(i))+ " possui tipo "+ tipoAtual + ".");
	}
	return tipoFuncao.retorno;
 }
 
 static String expToString(Exp exp){


	if (exp instanceof EFloat){
		//System.out.println("Aqui");
		return ("" + ((EFloat)exp).value);
	} else if(exp instanceof EVar) {
		return ((EVar)exp).var;
	} else if(exp instanceof ETrue) {
		return "True";
	} else if (exp instanceof EFalse){
		return "False";
	} else if (exp instanceof EOpExp){
		EOpExp opexp = (EOpExp) exp;
		return ("(" + expToString(opexp.arg1) + " " + opexp.op + " " + expToString(opexp.arg2) + ")");
	}  else if(exp instanceof EChamadaFun){
		 EChamadaFun chamada = (EChamadaFun) exp;
		 String args = "";
		 int sizeArgs = chamada.args.size();
		 if(sizeArgs != 0){
										for (int i=0; i< sizeArgs-1;i++) 
										 {
										    args = args + expToString(chamada.args.get(i)) + ", ";
										 }
										args = args + expToString(chamada.args.get(sizeArgs-1)); 
				}
				return args;
				}
		   
	return "";
}

}

///////////////////////
