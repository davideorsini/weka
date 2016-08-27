package weka.clusterers;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;

import weka.core.Instances;

public class Configuration{
	private ArrayList<Centroid> clusterStatus;
	private double result = 0.0;
	private int clusterCount;
	
	public Configuration(Instances data, int nClust, Random rand, DistanceType t){
		buildFirstConf(data, nClust, rand, t);
		computeCost();
	}
	
	public Configuration(Instances data, int nClust, int[] centroidsID, DistanceType t) {
		buildNextConf(data, nClust, centroidsID, t);
		computeCost();
	}
	
	private Configuration(ArrayList<Centroid> clusterStatus) {
		this.clusterStatus = clusterStatus;
		computeCost();
	}
	
	public double buildTestConfig(Instances data, Instances trainData, Instances testData, DistanceType t){
		double[] costs = new double[clusterCount];
		double totalCost = 0.0;
		for(int i=0; i<testData.numInstances(); i++){
			for(int j=0; j<clusterCount; j++){
				switch(t){
				case EUCLIDEAN: 
					costs[j] = getCentroidAt(j).euclideanDistance(i, trainData, testData);
					break;
				case LEVENSHTEIN:
					costs[j] = Centroid.computeLevenshteinDistance(testData.instance(i).stringValue(0), trainData.instance(getCentroidAt(j).getID()).stringValue(0));
					break;
				default:
					System.err.println("Undefined Distance");
				}
			}
			double cost = 0;
			double min = Double.MAX_VALUE;
			for(int jj=0; jj<clusterCount; jj++){
				//System.out.println(costs[jj]);
				if(costs[jj] < min){
					min = costs[jj];
					cost = costs[jj];
					totalCost += cost;
				}
			}
		}
		return totalCost;
	}
	
	private void buildFirstConf(Instances data, int nClust, Random rand, DistanceType t){
		clusterCount = nClust;
		clusterStatus = chooseRandomCentroid(nClust, rand, data);
		for(int i=0; i<data.numInstances(); i++){
			double[] costs = new double[nClust];
			for(int j=0; j<nClust; j++){
				while(i == getCentroidAt(j).getID() && i < data.numInstances()-1){
					i++;
				}
				switch(t){
					case EUCLIDEAN: 
						costs[j] = getCentroidAt(j).euclideanDistance(i, data);
						break;
					case LEVENSHTEIN:
						costs[j] = Centroid.computeLevenshteinDistance(data.instance(j).stringValue(0), data.instance(i).stringValue(0));
						break;
					default:
						System.err.println("Undefined Distance");
				}
					
				//System.out.println(costs[j] + " ");
			}
			
			//assegno l'istanza al cluster piu' vicino
			int index = 0;
			double cost = 0;
			double min = Double.MAX_VALUE;
			for(int jj=0; jj<nClust; jj++){
				//System.out.println(costs[jj]);
				if(costs[jj] < min){
					min = costs[jj];
					index = jj;
	//					getCentroidAt(index).addTotalCost(costs[jj]);
					cost = costs[jj];
				}
			}
			
			getCentroidAt(index).addInstance(i, cost);
		}		
	}
	
	private void buildNextConf(Instances data, int nClust, int[] centroidsID, DistanceType t){
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
				switch(t){
				case EUCLIDEAN: 
					costs[j] = getCentroidAt(j).euclideanDistance(i, data);
					break;
				case LEVENSHTEIN:
					costs[j] = Centroid.computeLevenshteinDistance(data.instance(j).stringValue(0), data.instance(i).stringValue(0));
					break;
				}	
				//System.out.println(costs[j] + " ");
			}
			int index = 0;
			double cost = 0;
			double min = Double.MAX_VALUE;
			for(int jj=0; jj<nClust; jj++){
				//System.out.println(costs[jj]);
				if(costs[jj] < min){
					min = costs[jj];
					index = jj;
//					getCentroidAt(index).addTotalCost(costs[jj]);
					cost = costs[jj];
				}
			}
			getCentroidAt(index).addInstance(i, cost);
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
		bw.write("@relation " + args[3]);
		bw.newLine();
		bw.newLine();
		for(int i=1; i<=data.numAttributes(); i++){
			bw.append("@attribute att" + i + " numeric");
			if(i == data.numAttributes()){
				bw.newLine();
				bw.append("@attribute cluster numeric");
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
		System.out.println("size mc: " + mc.size());
		BufferedWriter bw = new BufferedWriter(new FileWriter("C:/Users/dav_0/Desktop/outputString.arff"));
		bw.write("@relation " + args[3]);
		bw.newLine();
		bw.newLine();
		bw.append("@attribute att1 string");
		bw.newLine();
		bw.append("@attribute cluster numeric");
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
				if(getCentroidAt(i).alreadyExist(index)){
					mc.add(i);
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
	
	public boolean isBetterThan(Configuration c){
		//controllo che il costo totale sia minore
//		System.out.println(c.getTotalCost() + " >= " + result + "?");
		if(c.getTotalCost() < result){
			return false;
		}
		return true;		
	}
	
	public boolean isChanged(Configuration c){
		//controllo che non ci siano stati scambi tra i cluster
		boolean flag = false;
		for(int i=0; i<clusterStatus.size(); i++){
			if(getCentroidAt(i).getID() != c.getCentroidAt(i).getID()){
				if(!getCentroidAt(i).equals(c.getCentroidAt(i))){
					flag = true;
					break;
				}
			}
		}
		return flag;
	}
	
//	public boolean isChanged(Configuration c){
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
	
	public static ArrayList<Centroid> chooseRandomCentroid(int nClust, Random rand, Instances data){
//		Random randInstanceIndex = new Random(seed);
		Random randInstanceIndex = rand;
		ArrayList<Centroid> centroidList = new ArrayList<Centroid>();
		int centroidIndex;// = randInstanceIndex.nextInt(data.numInstances());
//		centroidList.add(new Centroid(centroidIndex));
		for(int i=0; i<nClust; i++){
			int j=0;
			centroidIndex = randInstanceIndex.nextInt(data.numInstances());
			while(j<centroidList.size()){
				for(int k=0; k<centroidList.size(); k++){
					if(centroidIndex != centroidList.get(k).getID()){
						j++;
					}
					else{
						centroidIndex = randInstanceIndex.nextInt(data.numInstances());
						k = 0;
						j = 0;
					}
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
	
	public Configuration clone(){
		return new Configuration(clusterStatus);
	}
}