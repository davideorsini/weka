package weka.clusterers;

public class CrossValidationValues{
	private int K;
	private double costCV;
	
	public CrossValidationValues(int K, double costCV){
		this.K = K;
		this.costCV = costCV;
	}
	
	public int getK(){
		return this.K;
	}
	
	public double getCostCV(){
		return this.costCV;
	}
}