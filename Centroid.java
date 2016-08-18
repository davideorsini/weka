package weka.clusterers;

import java.util.ArrayList;
import java.util.Collections;

import weka.core.Instances;

public class Centroid extends Instance{
	private double totalCost;
	private ArrayList<Integer> instanceList = new ArrayList<Integer>();
	private int clusterID;
	private int numOfElments = 0;
	
	public Centroid(int id){
		super(id);
	}
	
	public double euclideanDistance(int instanceID, Instances data){
//		System.out.println(instanceID);
		double cost = 0;
		for(int i=0; i<data.instance(instanceID).numAttributes(); i++){
			cost += Math.pow(data.instance(instanceID).value(i) - data.instance(this.id).value(i),2);
		}
		return Math.sqrt(cost);
	}

	private static int minimum(int a, int b, int c) {                            
        return Math.min(Math.min(a, b), c);                                      
    }                                                                            
                                                                                 
    public int computeLevenshteinDistance(String a, String b) {      
        int[][] distance = new int[a.length() + 1][b.length() + 1];        
                                                                                 
        for (int i = 0; i <= a.length(); i++)                                 
            distance[i][0] = i;                                                  
        for (int j = 1; j <= b.length(); j++)                                 
            distance[0][j] = j;                                                  
                                                                                 
        for (int i = 1; i <= a.length(); i++)                                 
            for (int j = 1; j <= b.length(); j++)                             
                distance[i][j] = minimum(                                        
                        distance[i - 1][j] + 1,                                  
                        distance[i][j - 1] + 1,                                  
                        distance[i - 1][j - 1] + ((a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1));
                                                                                 
        return distance[a.length()][b.length()];                           
    }
	
	public double getTotalCost(){
		return this.totalCost;
	}
	
	public ArrayList<Integer> getInstanceList(){
		return this.instanceList;
	}
	
	public ArrayList<Integer> getAllInstances(){
		ArrayList<Integer> toRet = instanceList;
		if(alreadyExist(getID())){
			return toRet;
		}
		toRet.add(getID());
		Collections.sort(toRet);
		return toRet;
	}
	
	public boolean equals(Centroid c){
//		return getAllInstances().equals(c.getAllInstances());
		ArrayList<Integer> a = getAllInstances();
		ArrayList<Integer> b = c.getAllInstances();
		if(a.size() != b.size()){
			return false;
		}
		else{
			for(int i=0; i< a.size(); i++){
				if(a.get(i) != b.get(i)){
					return false;
				}
			}
			return true;
		}
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
		instanceList.add(id);		
		numOfElments++;
	}
	
	public boolean alreadyExist(int id){
		for(Integer i : instanceList){
			if(i == id){
				return true;
			}
		}
		return false;
	}
	
	public int getID(int i){
		if(instanceList.size() == 0){
			return clusterID;
		}
		return instanceList.get(i);
	}
	
	public int getNumElements(){
		return this.numOfElments + 1;
	}
}