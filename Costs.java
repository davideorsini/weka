package weka.clusterers;

public class Costs{
	public int instance;
	public double cost;
	
	public Costs(double cost, int instance){
		this.cost = cost;
		this.instance = instance;
	}
	
	public double getCost(){
		return this.cost;
	}
	
	public int getInstance(){
		return this.instance;
	}
}