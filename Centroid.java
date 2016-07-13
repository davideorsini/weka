package weka.clusterers;

import java.util.ArrayList;
import java.util.Collections;

import weka.core.Instances;

public class Centroid extends Instance{
	private double totalCost;
	private ArrayList<Integer> instanceList = new ArrayList<Integer>();
	private int clusterID;
	
	public Centroid(int id){
		super(id);
	}
	
	public double euclideanDistance(int instanceID, Instances data){
		double cost = 0;
		for(int i=0; i<data.instance(instanceID).numAttributes(); i++){
			cost += Math.pow(data.instance(instanceID).value(i) - data.instance(this.id).value(i),2);
		}
		return cost;
	}
	
	/*public double otherDistance(int instanceID){
		
	}*/
	
	public double getTotalCost(){
		return this.totalCost;
	}
	
	public ArrayList<Integer> getInstanceList(){
		return this.instanceList;
	}
	
	public ArrayList<Integer> getAllInstances(){
		ArrayList<Integer> toRet = instanceList;
		toRet.add(getID());
		Collections.sort(toRet);
		return toRet;
	}
	
	public boolean equals(Centroid c){
		return getAllInstances().equals(c.getAllInstances());
	}
	
	public int getClusterID(){
		return this.clusterID;
	}
	
	public void setClusterID(int id){
		this.clusterID = id;
	}
	
	public void addTotalCost(double cost){
		totalCost += cost;
	}
	
	public void addInstance(int id){
		this.instanceList.add(id);
	}
}