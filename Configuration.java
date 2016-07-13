package weka.clusterers;
import java.util.*;

public class Configuration{
	private ArrayList<Centroid> clusterStatus;
	private double result = 0.0;
	private int clusterCount;
	
	public Configuration(ArrayList<Centroid> clusterStatus){
		this.clusterStatus = clusterStatus;
		this.result = 0.0;
		for(Centroid c : clusterStatus){
			result += c.getTotalCost();
		}
		clusterCount = clusterStatus.size();
	}
	
	public double getTotalCost(){	
		return result;
	}
	
	public Centroid getCentroidAt(int index){
		return this.clusterStatus.get(index);
	}
	
	public boolean isBetterThan(Configuration c){
		//controllo che il costo totale sia minore
		if(c.getTotalCost() >= result){
			return false;
		}
		
		//controllo che non ci siano stati scambi tra i cluster
		boolean flag = true;
		for(int i=0; i<clusterStatus.size(); i++){
			if(!clusterStatus.get(i).equals(c.getCentroidAt(i))){
				flag = false;
				break;
			}
		}
		return flag;
		
	}
	
	public int retClusterCount(){
		return this.clusterCount;
	}
	
	public Configuration clone(){
		return new Configuration(clusterStatus);
	}
}