package weka.clusterers;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.*;

import weka.core.Instances;

public class Configuration{
	private ArrayList<Medoid> clusterStatus;
	private double result = 0.0;
	private int clusterCount;
	
	public Configuration(Instances data, int nClust, Random rand, DistanceType t){
		buildFirstConf(data, nClust, rand, t);
		computeCost();
	}
	
	public Configuration(Instances data, int nClust, int[] medoidsID, DistanceType t) {
		buildNextConf(data, nClust, medoidsID, t);
		computeCost();
	}
	
	private Configuration(ArrayList<Medoid> clusterStatus) {
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
					costs[j] = getMedoidAt(j).euclideanDistance(i, trainData, testData);
					break;
				case LEVENSHTEIN:
					costs[j] = Medoid.computeLevenshteinDistance(testData.instance(i).stringValue(0), trainData.instance(getMedoidAt(j).getID()).stringValue(0));
					break;
				default:
					System.err.println("Undefined Distance");
				}
			}
			double cost = 0;
			double min = Double.MAX_VALUE;
			for(int jj=0; jj<clusterCount; jj++){
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
		clusterStatus = chooseRandomMedoid(nClust, rand, data);
		for(int i=0; i<data.numInstances(); i++){
			double[] costs = new double[nClust];
			for(int j=0; j<nClust; j++){
				switch(t){
					case EUCLIDEAN: 
						costs[j] = Manager.euclideanDistance(data.instance(getMedoidAt(j).id), data.instance(i), data.numAttributes());
						break;
					case LEVENSHTEIN:
						costs[j] = Medoid.computeLevenshteinDistance(data.instance(j).stringValue(0), data.instance(i).stringValue(0));
						break;
					default:
						System.err.println("Undefined Distance");
				}
			}
			
			//assegno l'istanza al cluster piu' vicino
			int index = 0;
			double cost = 0;
			double min = Double.MAX_VALUE;
			for(int jj=0; jj<nClust; jj++){
				if(costs[jj] < min){
					min = costs[jj];
					index = jj;
					cost = costs[jj];
				}
			}
			getMedoidAt(index).addTotalCost(costs[index]);
			getMedoidAt(index).addInstance(i, cost);
		}		
	}
	
	private void buildNextConf(Instances data, int nClust, int[] medoidsID, DistanceType t){
		clusterCount = nClust;
		clusterStatus = new ArrayList<Medoid>();
		for(int i=0; i<medoidsID.length; i++){
			clusterStatus.add(new Medoid(medoidsID[i]));
		}
		for(int i=0; i<data.numInstances(); i++){
			double[] costs = new double[nClust];
			for(int j=0; j<nClust; j++){
				switch(t){
					case EUCLIDEAN: 
						costs[j] = Manager.euclideanDistance(data.instance(getMedoidAt(j).id), data.instance(i), data.numAttributes());
					break;
					case LEVENSHTEIN:
						costs[j] = Medoid.computeLevenshteinDistance(data.instance(getMedoidAt(j).id).stringValue(0), data.instance(i).stringValue(0));
					break;
					default:
						System.err.println("Distance Error");
					break;
				}
			}
			double min = Double.MAX_VALUE;
			int index = 0;
			for(int j=0; j<nClust; j++){
				if(costs[j] < min){
					min = costs[j];
					index = j;
				}
			}
			getMedoidAt(index).addTotalCost(costs[index]);
			getMedoidAt(index).addInstance(i, costs[index]);
		}
	}
	
	private void computeCost() {
		this.result = 0.0;
		clusterCount = clusterStatus.size();
		for(int i=0; i<clusterCount; i++){
			result += getMedoidAt(i).getTotalCost();
		}
	}
	
	public void printStatus(Instances data){
		System.out.println();
		for(int i=0; i<clusterCount; i++){
			System.out.print("[" + i + " - " + data.instance(getMedoidAt(i).getID()).value(0) + "]" + " ");
			for(int j=0; j<getMedoidAt(i).getInstanceList().size(); j++){
				System.out.print(data.instance(getMedoidAt(i).getInstanceList().get(j)).value(0) + " ");
			}
			System.out.println();
		}
		System.out.println();
	}
	
	public void outputARFF(Instances data, String[] args) throws Exception{
		ArrayList<Integer> mc = membershipCluster(data);
		BufferedWriter bw = new BufferedWriter(new FileWriter(System.getProperty("user.home")+"/Desktop/output.arff"));
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
		BufferedWriter bw = new BufferedWriter(new FileWriter(System.getProperty("user.home")+"/Desktop/outputString.arff"));
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
				if(getMedoidAt(i).alreadyExist(index)){
					mc.add(i);
				}
			}
		}
		return mc;
	}
	
	public double getTotalCost(){	
		return result;
	}
	
	public Medoid getMedoidAt(int index){
		return this.clusterStatus.get(index);
	}
	
	public boolean isBetterThan(Configuration c){
		//controllo che il costo totale sia minore
		if(c.getTotalCost() < result){
			return false;
		}
		return true;		
	}
	
	public boolean isChanged(Configuration c){
		//controllo che non ci siano stati scambi tra i cluster
		boolean flag = false;
		for(int i=0; i<clusterCount; i++){
			if(getMedoidAt(i).getID() != c.getMedoidAt(i).getID()){
				if(!getMedoidAt(i).equals(c.getMedoidAt(i))){
					flag = true;
					break;
				}
			}
		}
		return flag;
	}
	
	public static ArrayList<Medoid> chooseRandomMedoid(int nClust, Random rand, Instances data){
		Random randInstanceIndex = rand;
		ArrayList<Medoid> medoidList = new ArrayList<Medoid>();
		int medoidIndex;
		for(int i=0; i<nClust; i++){
			int j=0;
			medoidIndex = randInstanceIndex.nextInt(data.numInstances());
			while(j<medoidList.size()){
				for(int k=0; k<medoidList.size(); k++){
					if(medoidIndex != medoidList.get(k).getID()){
						j++;
					}
					else{
						medoidIndex = randInstanceIndex.nextInt(data.numInstances());
						k = 0;
						j = 0;
					}
				}
			}
			medoidList.add(new Medoid(medoidIndex));
			medoidList.get(i).setClusterID(i);
		}
		return medoidList;
	}
	
	public int retClusterCount(){
		return this.clusterCount;
	}
	
	public Configuration clone(){
		return new Configuration(clusterStatus);
	}
}