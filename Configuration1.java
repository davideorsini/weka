package weka.clusterers;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

import weka.core.Instances;

public class Configuration1{
	private ArrayList<Centroid> clusterStatus;
	private double result = 0.0;
	private int clusterCount;
	
	public Configuration1(Instances data, int nClust, int seed){
		buildFirstConf(data, nClust, seed);
		computeCost();
	}
	
	public Configuration1(Instances data, int nClust, int[] centroidsID) {
		buildNextConf(data, nClust, centroidsID);
		computeCost();
	}
	
	private Configuration1(ArrayList<Centroid> clusterStatus) {
		this.clusterStatus = clusterStatus;
		computeCost();
	}
	
	private void buildFirstConf(Instances data, int nClust, int seed){
		clusterCount = nClust;
		clusterStatus = chooseRandomCentroid(nClust, seed, data);
		for(int i=0; i<data.numInstances(); i++){
			double[] costs = new double[nClust];
			for(int j=0; j<nClust; j++){
				while(i == getCentroidAt(j).getID() && i < data.numInstances()-1){
					i++;
				}
				costs[j] = getCentroidAt(j).computeLevenshteinDistance(data.instance(j).stringValue(0), data.instance(i).stringValue(0));
				//System.out.println(costs[j] + " ");
			}
			
			//assegno l'istanza al cluster piu' vicino
			int index = 0;
			double min = Double.MAX_VALUE;
			for(int jj=0; jj<nClust; jj++){
				//System.out.println(costs[jj]);
				if(costs[jj] < min){
					min = costs[jj];
					index = jj;
					getCentroidAt(index).addTotalCost(costs[jj]);
				}
			}
			
			getCentroidAt(index).addInstance(i);
		}		
	}
	
	private void buildNextConf(Instances data, int nClust, int[] centroidsID){
		clusterCount = nClust;
		clusterStatus = new ArrayList<Centroid>();
		for(Integer i : centroidsID){
			clusterStatus.add(new Centroid(i));
		}
//		for(int i=0;i<nClust;i++)
			//	System.out.println(clusterStatus.get(i).getID());
//			System.err.println(data.numInstances());
		for(int i=0; i<data.numInstances(); i++){
			double[] costs = new double[nClust];
			for(int j=0; j<nClust; j++){
				while(i == getCentroidAt(j).getID() && i < data.numInstances()-1){
					i++;
				}
				
				costs[j] = getCentroidAt(j).computeLevenshteinDistance(data.instance(j).stringValue(0), data.instance(i).stringValue(0));
				//System.out.println(costs[j] + " ");
			}
			int index = 0;
			double min = Double.MAX_VALUE;
			for(int jj=0; jj<nClust; jj++){
				//System.out.println(costs[jj]);
				if(costs[jj] < min){
					min = costs[jj];
					index = jj;
					getCentroidAt(index).addTotalCost(costs[jj]);
				}
			}
			getCentroidAt(index).addInstance(i);
		}
	}
	
	private void computeCost() {
		this.result = 0.0;
		for(Centroid c : clusterStatus){
			result += c.getTotalCost();
		}
		clusterCount = clusterStatus.size();
	}
	
	public void printStatus(){
		System.out.println();
		for(int i=0; i<clusterCount; i++){
			System.out.print("[" + i + "]" + " ");
			for(int j=0; j<getCentroidAt(i).getInstanceList().size(); j++){
				System.out.print(getCentroidAt(i).getInstanceList().get(j) + " ");
			}
			System.out.println();
		}
		System.out.println();
	}
	
	public void outputARFF(Instances data, String[] args) throws Exception{
		ArrayList<Integer> mc = membershipCluster(data);
		BufferedWriter bw = new BufferedWriter(new FileWriter("C:/Users/dav_0/Desktop/output.arff"));
		bw.write("@relation clustered");
		bw.newLine();
		bw.newLine();
		for(int i=1; i<data.numAttributes()+1; i++){
			bw.append("@attribute att" + i + " numeric");
			if(i == data.numAttributes()){
				bw.newLine();
				bw.append("@attribute att" + (i+1) + " cluster");
			}
			bw.newLine();
		}
		bw.newLine();
		bw.append("@data");
		bw.newLine();
		bw.append("%");
		bw.newLine();
		bw.append("% " + data.numInstances());
		bw.newLine();
		bw.append("%");
		bw.newLine();
		for(int i=0; i<data.numInstances(); i++){
			for(int j=0; j<data.numAttributes(); j++){
				bw.append(data.instance(i).value(data.attribute(j)) + ",");
			}
			bw.append(mc.get(i).toString());
			if(i <= data.numInstances()-2){
				bw.newLine();
			}
		}
		bw.close();
	}
	
	public void outputStringARFF(Instances data, String[] args) throws Exception{
		ArrayList<Integer> mc = membershipCluster(data);
		BufferedWriter bw = new BufferedWriter(new FileWriter("C:/Users/dav_0/Desktop/outputString.arff"));
		bw.write("@relation clustered");
		bw.newLine();
		bw.newLine();
		bw.append("@attribute att1 string");
		bw.newLine();
		bw.append("@attribute att2 cluster");
		bw.newLine();
		bw.newLine();
		bw.append("@data");
		bw.newLine();
		bw.append("%");
		bw.newLine();
		bw.append("% " + data.numInstances());
		bw.newLine();
		bw.append("%");
		bw.newLine();
		for(int i=0; i<data.numInstances(); i++){
			for(int j=0; j<data.numAttributes(); j++){
				bw.append(data.instance(i).stringValue(data.attribute(j)) + ",");
			}
			bw.append(mc.get(i).toString());
			if(i <= data.numInstances()-2){
				bw.newLine();
			}
		}
		bw.close();
	}
	
	public ArrayList<Integer> membershipCluster(Instances data){
		ArrayList<Integer> mc = new ArrayList<Integer>();
		for(int index=0; index<data.numInstances(); index++){
			for(int i=0; i< clusterStatus.size(); i++){
				for(int j=0; j<getCentroidAt(i).getInstanceList().size(); j++){
					if(data.instance(index).stringValue(data.attribute(0)).equals(data.instance(getCentroidAt(i).getAllInstances().get(j)).stringValue(0))){
						mc.add(i);
					}
				}
			}
		}
		return mc;
	}
	
	public double getTotalCost(){	
		return result;
	}
	
	public Centroid getCentroidAt(int index){
		return this.clusterStatus.get(index);
	}
	
	public boolean isBetterThan(Configuration1 c){
		//controllo che il costo totale sia minore
//		System.out.println(c.getTotalCost() + " >= " + result + "?");
		if(c.getTotalCost() < result){
			return false;
		}
		return true;		
	}
	
	public boolean isChanged(Configuration1 c){
		//controllo che non ci siano stati scambi tra i cluster
		boolean flag = false;
		for(int i=0; i<clusterStatus.size(); i++){
			if(!getCentroidAt(i).equals(c.getCentroidAt(i))){
				flag = true;
				break;
			}
		}
		return flag;
	}
	
//	public boolean isChanged(Configuration1 c){
//		//controllo che non ci siano stati scambi tra i cluster
//		boolean flag = false;
//		for(int i=0; i<clusterStatus.size(); i++){
//			if(getCentroidAt(i).getID() != c.getCentroidAt(i).getID()){
//				flag = true;
//			}
//		}
//		return flag;
//	}
	
//	public boolean isChanged(Configuration1 c){
//		//controllo che non ci siano stati scambi tra i cluster
//		for(int i=0; i<clusterStatus.size(); i++){
//			for(int j=0; j<clusterStatus.get(i).getNumElements(); j++){
//				if(clusterStatus.get(i).getAllInstances().get(j) != c.getCentroidAt(i).getAllInstances().get(j)){
//					return true;
//				}
//			}
//		}
//		return false;
//	}
	
	public static ArrayList<Centroid> chooseRandomCentroid(int nClust, int seed, Instances data){
		Random randInstanceIndex = new Random(seed);
		ArrayList<Centroid> centroidList = new ArrayList<Centroid>();
		int centroidIndex = randInstanceIndex.nextInt(data.numInstances());
		centroidList.add(new Centroid(centroidIndex));
		for(int i=0; i<nClust-1; i++){
			int j=0;
			centroidIndex = randInstanceIndex.nextInt(data.numInstances());
			while(j<centroidList.size()){
				if(centroidIndex != centroidList.get(j).getID()){
					j++;
				}
				else{
					centroidIndex = randInstanceIndex.nextInt(data.numInstances());
				}
			}
			centroidList.add(new Centroid(centroidIndex));
			centroidList.get(i).setClusterID(i);
		}
		//printStatus(centroidList);
		return centroidList;
	}
	
	public int retClusterCount(){
		return this.clusterCount;
	}
	
	public Configuration1 clone(){
		return new Configuration1(clusterStatus);
	}
}