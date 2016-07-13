package weka.clusterers;
public class Instance{
	protected int id;
	
	public Instance(int id){
		this.id = id;
	}
	
//	public double[] getCoord(){
//		usare le funzioni weka per parsree le coordinate
//	}
	
	protected int getID(){
		return this.id;
	}
}