package weka.clusterers;
public class Instance{
	protected int id;
	
	public Instance(int id){
		this.id = id;
	}
	
	protected int getID(){
		return this.id;
	}
}