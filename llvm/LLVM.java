package llvm;

import ast.*;
import tc.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.io.FileWriter; 

public class LLVM{

    private static Hashtable<String,TFun> funTypes = new Hashtable<String,TFun>(); 

    public static String target = "x86_64-pc-linux-gnu";
    

    public static void genLLVM(String fileName,Prog prog)
    {
        try{
                funTypes = TypeChecker.genFunTypes(prog.fun);
                String codemain = genLLVMMain(prog.main);
                String codefuns = genLLVMFuns(prog.fun);
                String code = genHeader() + codefuns + codemain;
                FileWriter fw = new FileWriter(fileName+".ll");
		        fw.write(code);
		        fw.close();
                
        } catch(Exception e)
            { System.out.println("Problema gerando llvm: "+e);}
        

    }

    public static String genHeader()
    {
        String header = "target triple = \"x86_64-pc-linux-gnu\"\n"
                       +"@.str = private unnamed_addr constant [5 x i8] c\"%lf\\0A\\00\", align 1\n\n"
                       +"@.str.true  = private unnamed_addr constant [6 x i8] c\"True\\0A\\00\", align 1\n"
                       +"@.str.false = private unnamed_addr constant [7 x i8] c\"False\\0A\\00\", align 1\n"
                       +"@.str.scanf = private unnamed_addr constant [4 x i8] c\"%lf\\00\", align 1\n"
                       +"declare i32 @printf(i8* noundef, ...) #1\n\n"
                       +"declare double @fmod(double noundef, double noundef) #1\n\n"
                       +"declare i32 @__isoc99_scanf(i8* noundef, ...) #1\n\n";


        String printBool = "define void @printBool__(i32 %0)  {\n"
            +"\t%2 = alloca i32, align 4\n" 
            +"\tstore i32 %0, i32* %2, align 4\n"
            +"\t%3 = load i32, i32* %2, align 4\n"
            +"\t%4 = icmp ne i32 %3, 0\n"
            +"\tbr i1 %4, label %5, label %7\n"  
        +"5:\n"                                                
            +"\t%6 = call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([6 x i8], [6 x i8]* @.str.true, i64 0, i64 0))\n"
            +"\tbr label %9\n"
        +"7:\n"                                                
            +"\t%8 = call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([7 x i8], [7 x i8]* @.str.false, i64 0, i64 0))\n"
            +"\tbr label %9\n"

        +"9:\n"                                                
            +"\tret void\n"
        +"}\n\n";

    String and__ = "define i32 @and__(i32 %0, i32 %1){\n"
            +"\t%3 = alloca i32, align 4\n"
            +"\t%4 = alloca i32, align 4\n"
            +"\tstore i32 %0, i32* %3, align 4\n"
            +"\tstore i32 %1, i32* %4, align 4\n"
            +"\t%5 = load i32, i32* %3, align 4\n"
            +"\t%6 = icmp ne i32 %5, 0\n"
            +"\tbr i1 %6, label %7, label %10\n"
    +"7:\n" 
            +"\t%8 = load i32, i32* %4, align 4\n"
            +"\t%9 = icmp ne i32 %8, 0\n"
            +"\tbr label %10\n"
    +"10:\n"
            +"\t%11 = phi i1 [ false, %2 ], [ %9, %7 ]\n"
            +"\t%12 = zext i1 %11 to i32\n"
            +"\tret i32 %12\n"
    +"}\n\n";

    String or__ = "define i32 @or__(i32 %0, i32 %1){\n"
        +"\t%3 = alloca i32, align 4\n"
        +"\t%4 = alloca i32, align 4\n"
        +"\tstore i32 %0, i32* %3, align 4\n"
        +"\tstore i32 %1, i32* %4, align 4\n"
        +"\t%5 = load i32, i32* %3, align 4\n"
        +"\t%6 = icmp ne i32 %5, 0\n"
        +"\tbr i1 %6, label %10, label %7\n"
    +"7:\n"
        +"\t%8 = load i32, i32* %4, align 4\n"
        +"\t%9 = icmp ne i32 %8, 0\n"
        +"\tbr label %10\n"
    +"10:\n"
        +"\t%11 = phi i1 [ true, %2 ], [ %9, %7 ]\n"
        +"\t%12 = zext i1 %11 to i32\n"
        +"\tret i32 %12\n"
    +"}\n\n";

    String fmodf__ = "define dso_local double @fmodf__(double %0, double %1) #0 {\n"
        +"\t%3 = alloca double\n"
        +"\t%4 = alloca double\n"
        +"\tstore double %0, double* %3\n"
        +"\tstore double %1, double* %4\n"
        +"\t%5 = load double, double* %3\n"
        +"\t%6 = load double, double* %4\n"
        +"\t%7 = call double @fmod(double noundef %5, double noundef %6) #2\n"
        +"\tret double %7\n"
        +"}\n\n";

    String readFloat__ = 
    "define dso_local double @readFloat__() #0 {\n"
        +"\t%1 = alloca double\n"
        +"%2 = call i32 (i8*, ...) @__isoc99_scanf(i8* noundef getelementptr inbounds ([4 x i8], [4 x i8]* @.str.scanf, i64 0, i64 0), double* noundef %1)\n"
        +"%3 = load double, double* %1\n"
        +"ret double %3\n"
      +"}\n\n";

        return header + and__ + or__ + fmodf__ + printBool  + readFloat__;
    }
    public static String initVars(ArrayList<VarDecl> vars){
	String llvm = "";
	for (VarDecl vardecl : vars) {
		switch (vardecl.type) {
			case "Bool":
			    llvm += "\t%" + vardecl.var + " = alloca i32\n";
                llvm += "\tstore i32 0, i32* %" + vardecl.var +"\n";
				break;
			case "Float":
				llvm += "\t%" + vardecl.var + " = alloca double\n";
                llvm += "\tstore double 0.0, double* %" + vardecl.var + "\n";
				break;
		}
	}
    return llvm;
  }

    public static String genLLVMFuns(ArrayList<Fun> funs)  throws Exception{
        String llvm = "";
        for (Fun fun : funs) {
            llvm += genLLVMFun(fun);
        }
        return llvm;
    }

    public static String genLLVMFun(Fun fun)  throws Exception{
        RegGen reg = new RegGen();
        Hashtable<String,TType> env = new Hashtable<String,TType>();
	    TypeChecker.initEnv(env,fun.vars);
        TypeChecker.initEnvParamFormal(env,fun.params);
        SymbolTable table = new SymbolTable(env);
        table.addParamFormal(fun.params);
        String vars = initVars(fun.vars);
	    String codeBlock = genLLVMBlock(table,reg,fun.body);
        String typeReturn = toLLVMtype(TypeChecker.toTType(fun.retorno));
        String codeFun = "define "+ typeReturn+ " @"+fun.nome;
        ArrayList<ParamFormalFun> params = fun.params;
        String listParam = "";
        if (params.size()==0)
        { listParam = "";}
        else{
            for (int i = 0 ; i<params.size()-1;i++){
                ParamFormalFun param = params.get(i);
                listParam += toLLVMtype(TypeChecker.toTType( param.type)) +" %"+ param.var +", "; 
            }
            ParamFormalFun param = params.get(params.size()-1);
            listParam += toLLVMtype(TypeChecker.toTType(param.type)) +" %"+ param.var ;
        }
        codeFun += "(" + listParam + "){\nnentry:\n" + vars + codeBlock + "\n";
        if(fun.retorno.equals("Void")) 
                codeFun += "\tret i32 0\n}\n\n";
        else
                codeFun += "\n}\n\n";  
        return codeFun;
        
         
    }
    public static String genLLVMMain(Main main) throws Exception
    {
        RegGen reg = new RegGen();
        Hashtable<String,TType> env = new Hashtable<String,TType>();
	    TypeChecker.initEnv(env,main.vars);
        String vars = initVars(main.vars);
        SymbolTable table = new SymbolTable(env);
	    String codeBlock = genLLVMBlock(table,reg,main.coms);
        return "define i32 @main() {\nentry:\n" + vars + codeBlock + "\tret i32 0\n}";
    }

    public static String genLLVMBlock(SymbolTable env, RegGen reg, ArrayList<Comando> coms)
    {
        String llvm = "";
        for(int i=0;i<coms.size();i++){
		    String codeb = genLLVMCom(env,reg,coms.get(i));
            llvm += codeb;
	    }
        return llvm;
    }

    public static String genLLVMCom(SymbolTable env, RegGen reg, Comando com)
    {
        String llvm = "";
        LLVMResult llvmExp;
        if(com instanceof CAtribuicao){
            CAtribuicao atrib = (CAtribuicao) com;
            llvmExp = genLLVMExp(env,reg,atrib.exp);
            TType typeVar = env.get(atrib.var);
            llvm = llvmExp.code + "\tstore " + toLLVMtype(typeVar) + " " + llvmExp.result + ", " + toLLVMtype(typeVar) + "* %" + atrib.var + "\n" ;
	    } else if(com instanceof CPrint){
		    CPrint print = (CPrint) com;
            llvmExp = genLLVMExp(env,reg,print.exp);
            TType typeExp = null;
            try{
            typeExp= TypeChecker.typeCheckExp(env.env,print.exp);
            }catch(Exception e){}
            if (typeExp instanceof TFloat)
            { 
                
                //String regDouble = reg.genReg();
                String regPrint = reg.genReg();
                llvm = llvmExp.code;
                //llvm += "\t" + regFloat +" = load float, float* " + llvmExp.result + "\n";
               // llvm += "\t" + regDouble + " = fpext double " + llvmExp.result + " to double\n";
                llvm += "\t" + regPrint + " = call i32 (i8*, ...) @printf(i8* noundef getelementptr inbounds ([5 x i8], [5 x i8]* @.str, i64 0, i64 0), double noundef " + llvmExp.result + ")\n";
	        }else
            {
                llvm = llvmExp.code + "\tcall void @printBool__(i32 " + llvmExp.result +")\n";
            }
        }else if(com instanceof CReturn){
            CReturn print = (CReturn) com;
            llvmExp = genLLVMExp(env,reg,print.exp);
            TType typeExp = null;
            try{
            typeExp= TypeChecker.typeCheckExp(env.env,print.exp);
            }catch(Exception e){}
            String typellvm = toLLVMtype(typeExp);
            llvm = llvmExp.code + "\tret " + typellvm + " " + llvmExp.result + "\n";
        } else if(com instanceof CChamadaFun){
		    CChamadaFun chamada = (CChamadaFun) com;
		    TFun tipoFuncao = funTypes.get(chamada.fun);
            LLVMResult chamadacode = genLLVMfunCall(env, reg, chamada.fun, tipoFuncao, chamada.args); 
            //llvm.result = chamadacode.result;
            //llvm.code = chamadacode.code;
            llvm = chamadacode.code;
        }else if(com instanceof CReadInput){
		    CReadInput read = (CReadInput) com;
            String regFloat = reg.genReg();
            llvm = "\t" + regFloat + " = call double @readFloat__()\n";
            llvm += "\tstore double " + regFloat + ", double* %" + read.var + "\n";
            
        } else if (com instanceof CIf){
            CIf cif = (CIf) com;
            llvmExp = genLLVMExp(env,reg,cif.exp);
            llvm = llvmExp.code;
            String regBool = reg.genReg();
            String regThen =reg.genReg();
            String codeBlock = genLLVMBlock(env, reg, cif.bloco);
            String regSaida = reg.genReg();
            llvm += "\t" + regBool +" = icmp eq i32  " + llvmExp.result + ",1\n";
            llvm += "\tbr i1 " + regBool + ", label " + regThen + ", label " + regSaida +"\n";
            String labelThen = regThen.substring(1);
            llvm += labelThen +":\n";
            llvm += codeBlock;
            llvm += "\tbr label " + regSaida + "\n";
            String labelSaida = regSaida.substring(1);
            llvm += labelSaida + ":\n";

        } else if (com instanceof CWhile){
            CWhile cwhile = (CWhile) com;
                  
           // llvm = llvmExp.code;
            String regTeste = reg.genReg();
            String labelTeste = regTeste.substring(1);
            llvmExp = genLLVMExp(env,reg,cwhile.exp);
            String regBool = reg.genReg();
            String regThen =reg.genReg();
            String codeBlock = genLLVMBlock(env, reg, cwhile.bloco);
            String regSaida = reg.genReg();
            llvm += "\tbr label " + regTeste+ "\n";
            llvm += labelTeste + ":\n";
            llvm += llvmExp.code; 
            llvm += "\t" + regBool +" = icmp eq i32  " + llvmExp.result + ",1\n";
            llvm += "\tbr i1 " + regBool + ", label " + regThen + ", label " + regSaida +"\n";
            String labelThen = regThen.substring(1);
            llvm += labelThen +":\n";
            llvm += codeBlock;
            llvm += "\tbr label " + regTeste + "\n";
            String labelSaida = regSaida.substring(1);
            llvm += labelSaida + ":\n";
        }
        return llvm;
    }

    static LLVMResult genLLVMfunCall(SymbolTable  env, RegGen reg, String fnome, TFun tipoFuncao, ArrayList<Exp> paramAtual) {
	    ArrayList<TParamFormalFun> paramFormal = tipoFuncao.params;
	
        LLVMResult llvm = new LLVMResult();
        String chamadaArgs = "";

        if (paramFormal.size()==0)
        {
            chamadaArgs = "";
            llvm = new LLVMResult();

        }else{
            for(int i=0; i< paramFormal.size()-1; i++){
                LLVMResult exp = genLLVMExp(env,reg,paramAtual.get(i));
                TType tipoFormal = paramFormal.get(i).type;
                String llvmType =  toLLVMtype(tipoFormal);
                llvm.code += exp.code;
                chamadaArgs +=  llvmType + " " + exp.result + ", ";
            }

            LLVMResult exp = genLLVMExp(env,reg,paramAtual.get(paramAtual.size()-1));
            TType tipoFormal = paramFormal.get(paramFormal.size()-1).type;
            String llvmType = toLLVMtype(tipoFormal);
            llvm.code += exp.code;
            chamadaArgs +=  llvmType + " " + exp.result ;
        }

        String regSaida = reg.genReg();
        llvm.result = regSaida;
        String tipoSaida = toLLVMtype(tipoFuncao.retorno);
        llvm.code += "\t"+ regSaida + " = call "+tipoSaida+" @"+fnome+"("+chamadaArgs+")\n";
	return llvm;
 }

    public static LLVMResult genLLVMExp(SymbolTable env, RegGen reg,Exp exp)
    {
        LLVMResult llvm = new LLVMResult();
    
        if (exp instanceof EFloat){
            llvm.code = "";
            llvm.result = "" + ((EFloat) exp).value;
	    } else if(exp instanceof EVar) {
            String var = ((EVar)exp).var;
            //%1 = load float,float %v1
            TType typeVar = env.get(var);
            if(env.isParamFormal(var))
            {
                llvm.code = "";
                llvm.result = "%"+var;
                //llvm.code = "\t" + llvm.result +" = alloca " + toLLVMtype(typeVar) +"\n";
                //llvm.code += "\tstore " + toLLVMtype(typeVar) + " %" +var + ", " + toLLVMtype(typeVar) + "* " + llvm.result + "\n";
            }
            else
            {
                llvm.result = reg.genReg();
                llvm.code = "\t" + llvm.result +" = load " + toLLVMtype(typeVar) +", " + toLLVMtype(typeVar) + "* %" + var + "\n";
            }
            //llvm.code = "\t" + llvm.result +" = alloca " + toLLVMtype(typeVar) +"\n";
            //llvm.code += "\tstore " + toLLVMtype(typeVar) + " %" +var + ", " + toLLVMtype(typeVar) + "* " + llvm.result + "\n";
            
            
	    } else if(exp instanceof ETrue) {
            llvm.code = "";
            llvm.result = "1";
	    } else if (exp instanceof EFalse){
		    llvm.code = "";
            llvm.result = "0";
        } else if (exp instanceof EOpExp){
            EOpExp opexp = (EOpExp) exp;
		    LLVMResult llvm1 = genLLVMExp(env,reg,opexp.arg1);
            LLVMResult llvm2 = genLLVMExp(env,reg,opexp.arg2);
            switch (opexp.op) { 
			    case "+":
			        llvm.result = reg.genReg();
                    llvm.code += llvm1.code + llvm2.code;
                    llvm.code += "\t" + llvm.result +" = fadd double " + llvm1.result + ", " + llvm2.result + "\n";
				    break;
                case "/":
                    llvm.result = reg.genReg();
                    llvm.code += llvm1.code + llvm2.code;
                    llvm.code += "\t" + llvm.result +" = fdiv double " + llvm1.result + ", " + llvm2.result + "\n";
				    break;
                case "-":
                    llvm.result = reg.genReg();
                    llvm.code += llvm1.code + llvm2.code;
                    llvm.code += "\t" + llvm.result +" = fsub double " + llvm1.result + ", " + llvm2.result + "\n";
				    break;
			    case "*":
                    llvm.result = reg.genReg();
                    llvm.code += llvm1.code + llvm2.code;
                    llvm.code += "\t" + llvm.result +" = fmul double " + llvm1.result + ", " + llvm2.result + "\n";
                    break;
                case "<":
                    String cmpReg1 = reg.genReg();
                    llvm.result = reg.genReg();
                    llvm.code += llvm1.code + llvm2.code;
                    llvm.code += "\t" + cmpReg1 +" = fcmp olt double " + llvm1.result + ", " + llvm2.result + "\n";
                    llvm.code += "\t" + llvm.result +" = zext i1  " + cmpReg1 + " to i32\n";
                    break; 
                case "<=":
                    String cmpReg2 = reg.genReg();
                    llvm.result = reg.genReg();
                    llvm.code += llvm1.code + llvm2.code;
                    llvm.code += "\t" + cmpReg2 +" = fcmp ole double " + llvm1.result + ", " + llvm2.result + "\n";
                    llvm.code += "\t" + llvm.result +" = zext i1  " + cmpReg2 + " to i32\n";
                    break;
                case ">":
                    String cmpReg3 = reg.genReg();
                    llvm.result = reg.genReg();
                    llvm.code += llvm1.code + llvm2.code;
                    llvm.code += "\t" + cmpReg3 +" = fcmp ogt double " + llvm1.result + ", " + llvm2.result + "\n";
                    llvm.code += "\t" + llvm.result +" = zext i1  " + cmpReg3 + " to i32\n";
                    break; 
			    case ">=":
                    String cmpReg4 = reg.genReg();
                    llvm.result = reg.genReg();
                    llvm.code += llvm1.code + llvm2.code;
                    llvm.code += "\t" + cmpReg4 +" = fcmp oge double " + llvm1.result + ", " + llvm2.result + "\n";
                    llvm.code += "\t" + llvm.result +" = zext i1  " + cmpReg4 + " to i32\n";
                    break;
                case "=":
                    String eqReg = reg.genReg();
                    TType typearg = null;
                        try{
                                typearg = TypeChecker.typeCheckExp(env.env,opexp.arg1);
                        }catch (Exception e){}
                    llvm.result = reg.genReg();
                    llvm.code += llvm1.code + llvm2.code;
                    if(typearg instanceof TFloat)
                        llvm.code += "\t" + eqReg +" = fcmp oeq double " + llvm1.result + ", " + llvm2.result + "\n";
                      else
                        llvm.code += "\t" + eqReg +" = icmp eq i32 " + llvm1.result + ", " + llvm2.result + "\n";
                    llvm.code += "\t" + llvm.result +" = zext i1  " + eqReg + " to i32\n";
                    break;
                case "&":
                    llvm.code += llvm1.code + llvm2.code;
                    String regAnd = reg.genReg();
                    llvm.result = regAnd;
                    llvm.code += "\t"+ regAnd + " = call i32 @and__(i32 " + llvm1.result +", i32 " + llvm2.result + ")\n";
                    break;
                case "|":
                    llvm.code += llvm1.code + llvm2.code;
                    String regOr = reg.genReg();
                    llvm.result = regOr;
                    llvm.code += "\t"+ regOr + " = call i32 @or__(i32 " + llvm1.result +", i32 " + llvm2.result + ")\n";
                    break;
                case "%":
                    llvm.code += llvm1.code + llvm2.code;
                    String regfmod = reg.genReg();
                    llvm.result = regfmod;
                    llvm.code += "\t"+ regfmod + " = call double @fmodf__(double " + llvm1.result +", double " + llvm2.result + ")\n";
                    break;
                

            }
        }else if(exp instanceof EChamadaFun){
		    EChamadaFun chamada = (EChamadaFun) exp;
		    TFun tipoFuncao = funTypes.get(chamada.fun);
            LLVMResult chamadacode = genLLVMfunCall(env, reg, chamada.fun, tipoFuncao, chamada.args); 
            llvm.result = chamadacode.result;
            llvm.code = chamadacode.code;
        }
        return llvm;
    }

    public static String toLLVMtype(TType type){
	
	    String resposta = "";
	
	    if(type instanceof TFloat){
		    resposta = "double";
	    } else if(type instanceof TBool){
            resposta = "i32";
        } else if(type instanceof TVoid){
            resposta = "i32";
        }
        return resposta;
    }

}// end class

class RegGen{
    int reg=0;
   
    RegGen(){
        this.reg = -1;
    }

    public String genReg(){
        reg++;
        return ("%" + reg);
    }

}

class LLVMResult{
    public String result = "";;
    public String code ="";
}


class SymbolTable{

	public Hashtable<String,TType>  env = new Hashtable<String,TType>();
	public Hashtable<String,TType>  paramFormal = new Hashtable<String,TType>();

	SymbolTable(Hashtable<String,TType> env)
	{
		this.env = env;
        this.paramFormal = new Hashtable<String,TType>();
	}

	public TType get(String var)
	{
		return env.get(var);
	}

	public void put(String var, TType type)
	{
		env.put(var,type);
	}

    public boolean isParamFormal(String var)
    {
        TType type = paramFormal.get(var);
        return (!(type==null));
    }
    public void addParamFormal(ArrayList<ParamFormalFun> vars) {
	
        for (ParamFormalFun vardecl : vars) {
            switch (vardecl.type) {
                case "Bool":
                        paramFormal.put(vardecl.var,new TBool());
                     break;
                case "Float":
                        paramFormal.put(vardecl.var,new TFloat());
                      break;
                
            }
        }
      }
    
}







