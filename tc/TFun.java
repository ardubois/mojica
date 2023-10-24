package tc;

import java.util.ArrayList;

public class TFun extends TType{
		   public TType retorno;
		   public ArrayList<TParamFormalFun> params;
		   
		   public TFun(TType retorno, ArrayList<TParamFormalFun> params){
		        this.retorno = retorno;
		        this.params = params;
		   }
}
