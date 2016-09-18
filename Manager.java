package weka.clusterers;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
//import java.util.Comparator;
import java.util.Random;

import weka.core.Instances;
import weka.core.Instance;
import weka.core.converters.ArffLoader.ArffReader;

public class Manager {

	private static Random rand;
//	private static int MAX_THREADS = 4;
	public static Instances data;
	private static int nClust;
	private static double costCV;
	private static int seed;
	private static double I;
	private static int minClust, maxClust;
	private static DistanceType t;
	private static GoodnessType cgt;
	private final int n_test = 20;
	private static int k;
	
	/*INPUT PARAMETERS
	 * "EuclideanDistance" 2 10 "C:/Users/dav_0/Desktop/pam_big.arff" 1 1 10 fileN
	 * "LevenshteinDistance" 2 10 "C:/Users/dav_0/Desktop/stringTest.arff" 1 1 10 fileN
	 */

	public Manager(String[] args) throws Exception {
		switch (args[0]) {
		case "EuclideanDistance":
			t = DistanceType.EUCLIDEAN;
			break;
		case "LevenshteinDistance":
			t = DistanceType.LEVENSHTEIN;
			break;
		}
		
		cgt = GoodnessType.ELEMENT_QTY;
		
		String filePath = args[3];
		seed = Integer.parseInt(args[4]);
		
		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		ArffReader arff = new ArffReader(reader);
		data = arff.getData();
		
		if(Integer.parseInt(args[1]) < 2){
			minClust = 2;
		}
		else{
			minClust = Integer.parseInt(args[1]);
		}
		
		if(Integer.parseInt(args[2]) > data.numInstances()){
			maxClust = data.numInstances()/2;
		}
		else{
			maxClust = Integer.parseInt(args[2]);
		}
		
		I = Double.parseDouble(args[5]);

		k = Integer.parseInt(args[6]);
		
		int fileN = Integer.parseInt(args[7]);
		
		rand = new Random(seed);
		
		Configuration optimalNClust = null;
		
		// variabile start per il calcolo del tempo di esecuzione
		double startTime = System.nanoTime();
		
		ArrayList<CrossValidationValues> retCrossV = new ArrayList<CrossValidationValues>();
		
		retCrossV = crossValidation(data, t, rand, k);
		CrossValidationValues val;
		val = mostFrequentK(retCrossV);
		nClust = val.getK();
		costCV = val.getCostCV();
		
		double cg = 0.0;

		optimalNClust = clusterization(data, nClust, rand, t);

		printCluster(optimalNClust, nClust);
		cg = clusterGoodness(optimalNClust, cgt);

		// variabile fine calcolo del tempo di esecuzione
		double endTime = System.nanoTime();
		double time = (endTime - startTime) / 1000000000;
		System.out.println("Execution time: " + time + " s");
		outputFile(time, optimalNClust, seed, nClust, I, t, cgt, cg, n_test, k, costCV, fileN, filePath);
		switch(t){
			case EUCLIDEAN:
				optimalNClust.outputARFF(data, args);
				break;
			case LEVENSHTEIN:
				optimalNClust.outputStringARFF(data, args);
				break;
			default:
				System.err.println("Error output file");
		}
	}
	
	public static CrossValidationValues mostFrequentK(ArrayList<CrossValidationValues> retCrossV){
		System.out.println();
		for(int i=0; i<retCrossV.size(); i++){
			System.out.print(retCrossV.get(i).getK() + " ");
		}
		System.out.println();
		Collections.sort(retCrossV, new KComparator());
		int index = 0;
		int nc = retCrossV.get(0).getK();
		int tmp = 0;
		int counter = 0;
		
		for(int i=0; i<retCrossV.size(); i++){
			System.out.print(retCrossV.get(i).getK() + " ");
		}
		
		for(int i=0; i<retCrossV.size(); i++){
			if(nc == retCrossV.get(i).getK()){
				tmp++;
			}
			else{
				if(tmp > counter){	//c'era anche l'uguale
					index = i-1;
					counter = tmp;
				}
				/*if(tmp == counter){
					if(retCrossV.get(i).getCostCV() <= retCrossV.get(index).getCostCV()){
						index = i-1;
						counter = tmp;
					}
				}*/
				nc = retCrossV.get(i).getK();
				tmp = 1;
			}
		}
		return retCrossV.get(index);
	}
	
	static class KComparator implements Comparator<CrossValidationValues>{
		@Override public int compare(CrossValidationValues a, CrossValidationValues b){
			return a.getK() < b.getK() ? -1 :
				a.getK() == b.getK() ? 0 : 1;
		}
	}
	
	public ArrayList<CrossValidationValues> crossValidation(Instances data, DistanceType t, Random rand, int k) throws Exception{
		int tot = data.numInstances();
		int test_qty = tot / k;
		int train_qty = tot - test_qty;
		int[] instances2train = new int[train_qty];
		int[] instances2test = new int[test_qty];
		
		ArrayList<CrossValidationValues> valuesCV = new ArrayList<CrossValidationValues>();
		
		for(int n=0; n<k; n++){
			//istance per il file di training
			for(int i=0; i<train_qty; i++){
				int val = rand.nextInt(data.numInstances());
				while(alreadyExist(instances2train, val)){
					val = rand.nextInt(data.numInstances());
				}
				instances2train[i] = val;
			}
			
			//istanze per il file di test
			for(int i=0; i<test_qty; i++){
				int val = rand.nextInt(data.numInstances());
				while(alreadyExist(instances2train, val)){
					val = rand.nextInt(data.numInstances());
					for(int j=0; j<i; j++){
						if(instances2test[j] == val){
							val = rand.nextInt(data.numInstances());
						}
					}
				}
				instances2test[i] = val;
			}
			
			String[] path = new String[2];
			try{
				switch(t){
					case EUCLIDEAN:
						path = createTrainingTestArff(data, instances2train, instances2test, "numeric", t);
						break;
					case LEVENSHTEIN:
						path = createTrainingTestArff(data, instances2train, instances2test, "string", t);
						break;
					default:
						System.err.println("Training/Test file error");
						break;
				}
			}
			catch(Exception e){
				System.err.println(e);
			}
			
			//ricerca n cluster ottimale
			double cost = Double.MAX_VALUE;
			int nc = 0;
			
			BufferedReader trainReader = new BufferedReader(new FileReader(path[0]));
			ArffReader trainArff = new ArffReader(trainReader);
			Instances trainData = trainArff.getData();
			BufferedReader testReader = new BufferedReader(new FileReader(path[1]));
			ArffReader testArff = new ArffReader(testReader);
			Instances testData = testArff.getData();
			
			Configuration optimal = null;
	
			for (int m = minClust; m <= maxClust; m++) {
				optimal = clusterization(trainData, m, rand, t);
				double cv = 0.0;
				cv = optimal.buildTestConfig(data, trainData, testData, t);
				System.out.println("cv: " + cv + " cost: " + cost + " nc: " + nc);
				if(cv < cost){
					cost = cv;
					nc = m;
				}
			}
			System.out.println();
			valuesCV.add(new CrossValidationValues(nc, cost));
		}
		return valuesCV;
	}
	
	public static double euclideanDistance(Instance a, Instance b, int nAttr){
		double cost = 0;
		for(int i=0; i<nAttr; i++){
			cost += Math.pow(a.value(i) - b.value(i),2);
		}
		return Math.sqrt(cost);
	}
	
	private static int minimum(int a, int b, int c) {                            
        return Math.min(Math.min(a, b), c);                                      
    }                                                                            
                                                                                 
    public static int computeLevenshteinDistance(String a, String b) {      
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
	
	public static int[] bestMedoids(Instances data, Configuration c, ArrayList<Integer> sizes, int nClust, DistanceType t){
		int[] medoids = new int[nClust];
		Thread[] threads = new Thread[nClust];
		for (int i = 0; i < nClust; i++) {
			threads[i] = new Thread(new Task(i, data,  sizes.get(i), c, t, medoids, I));
			threads[i].start();
		}
		try{
			for (int i=0; i < nClust; i++) {
				threads[i].join();
			}
		}
		catch(InterruptedException e){
			System.err.println(e);
		}
		return medoids;
	}
	
	public Configuration clusterization(Instances data, int nClust, Random rand, DistanceType t) throws Exception{
		Configuration firstConfig;
		Configuration currentConfig = null;
		Configuration bestConfig = null;
		int[] medoids = new int[nClust];
		ArrayList<Integer> sizes;
		boolean flag = true;
		
		for(int n=0; n<n_test; n++){
			firstConfig = new Configuration(data, nClust, rand, t);
			if (bestConfig == null) {
				bestConfig = firstConfig.clone();
			}
			flag = true;			
			while (flag) {
				sizes = new ArrayList<Integer>();
				for (int i = 0; i < nClust; i++) {
					sizes.add(firstConfig.getMedoidAt(i).getAllInstances().size());
				}
				medoids = bestMedoids(data, firstConfig,sizes, nClust, t);
				currentConfig = new Configuration(data, nClust, medoids, t); 
				flag = currentConfig.isChanged(firstConfig);
				firstConfig = currentConfig.clone();
			}
			if(currentConfig.isBetterThan(bestConfig)){
				bestConfig = currentConfig.clone();
			}
		}
		return bestConfig;
	}
	
	public static boolean alreadyExist(int[] instances, int a){
		boolean flag = false;
		for(int i=0; i<instances.length; i++){
			if(instances[i] == a){
				flag = true;
				break;
			}
		}
		return flag;
	}
	
	public static String[] createTrainingTestArff(Instances data, int[] instances2train, int[] instances2test, String type, DistanceType t) throws IOException{
		String[] path = new String[2];
		path[0] = System.getProperty("user.home") + "/Desktop/training.arff"; //trainPath
		path[1] = System.getProperty("user.home") + "/Desktop/test.arff"; //testPath 
		for(int f=0; f<2; f++){
			BufferedWriter bw = new BufferedWriter(new FileWriter(path[f]));
			if(f == 0){
				bw.write("@relation training"); 
			}
			else{
				bw.write("@relation test");
			}
			bw.newLine();
			bw.newLine();
			for(int i=0; i<data.numAttributes(); i++){
				bw.append("@attribute att" + (i+1) + " " + type);
				bw.newLine();
			}
			bw.newLine();
			bw.append("@data");
			bw.newLine();
			bw.append("%");
			bw.newLine();
			int len = 0;
			if(f == 0){
				len = instances2train.length;
			}
			else{
				len = instances2test.length;
			}
			bw.append("% " + len);
			bw.newLine();
			bw.append("%");
			bw.newLine();
			for(int i=0; i<len; i++){
				for(int j=0; j<data.numAttributes(); j++){
					switch(t){
						case EUCLIDEAN:
							if(f == 0){
								bw.append("" + data.instance(instances2train[i]).value(data.attribute(j)));
							}
							else{
								bw.append("" + data.instance(instances2test[i]).value(data.attribute(j)));
							}
							if(j != data.numAttributes()-1){
								bw.append(",");
							}
							break;
						case LEVENSHTEIN:
							if(f == 0){
								bw.append("" + data.instance(instances2train[i]).stringValue(data.attribute(j)));
							}
							else{
								bw.append("" + data.instance(instances2test[i]).stringValue(data.attribute(j)));
							}
							if(j != data.numAttributes()-1){
								bw.append(",");
							}
							break;
						default:
							System.err.println("Error Distance File");
							break;
					}
				}
				if(i != len-1){
					bw.newLine();
				}
			}
			bw.close();
		}
		return path;
	}

	public static double clusterGoodness(Configuration c, GoodnessType cgt) {
		switch(cgt){
			case ELEMENT_QTY:
				double min = Double.MAX_VALUE;
				double max = 0.0;
				int val = 0;
				for (int i = 0; i < c.retClusterCount(); i++) {
					val = c.getMedoidAt(i).getNumElements();
					if (val < min) {
						min = val;
					}
					if (val > max) {
						max = val;
					}
				}
				// System.out.println("Cluster Goodness: " + max/min);
				return max / min;
			default:
				System.err.println("GoodnessType Error");
				return -0.1;
		}
	}

	public static void printCluster(Configuration c, int nClust) {
		for (int i = 0; i < nClust; i++) {
			System.out.println("Cluster " + "[" + i +"]");
			System.out.println("Medoid " + "[" + c.getMedoidAt(i).id + "]");
			System.out.print("Elements [ ");
			for (int l : c.getMedoidAt(i).getAllInstances()) {
				System.out.print(l + " ");
			}
			System.out.print("]");
			System.out.println();
			System.out.println("Num. of elements " + c.getMedoidAt(i).getNumElements());
			System.out.println();
		}
	}

	public static void printStatus(ArrayList<Medoid> cluster) {
		System.out.print("Medoids: ");
		for (int i = 0; i < cluster.size(); i++) {
			System.out.print(cluster.get(i).getID() + " ");
		}
	}

	public static void outputFile(double time, Configuration c, int seed, int nClust, double I, DistanceType t, GoodnessType cgt, double cg, int n_test, int k, double costCV, int fileN, String filePath) throws Exception{
		BufferedWriter bw = new BufferedWriter(new FileWriter(System.getProperty("user.home")+"/Desktop/outputValues"+fileN+".txt"));
		bw.write("Dati Clusterizzazione");
		bw.newLine();
		bw.newLine();
		bw.append("File ARFF: " + filePath);
		bw.newLine();
		bw.append("Seed: " + seed);
		bw.newLine();
		bw.append("Distance Type: " + t.toString());
		bw.newLine();
		bw.append("GoodnessType: " + cgt.toString());
		bw.newLine();
		bw.append("I: " + I);
		bw.newLine();
		bw.append("k: " + k);
		bw.newLine();
		bw.append("Numero test: " + n_test);
		bw.newLine();
		bw.append("Numero Cluster: " + nClust);
		bw.newLine();
		bw.append("Medoidi: ");
		for(int i=0; i<nClust; i++){
			bw.append("{" + c.getMedoidAt(i).getID() + "} ");
		}
		bw.newLine();
		bw.append("Elementi: ");
		for(int i=0; i<nClust; i++){
			bw.append("(" + c.getMedoidAt(i).getNumElements() + ") ");
		}
		bw.newLine();
		bw.append("Cross-Validation cost: " + costCV);
		bw.newLine();
		bw.append("Likelihood: -" + Math.log(costCV)/Math.log(2));
		bw.newLine();
		bw.append("Clusters Goodness: " + cg);
		bw.newLine();
		bw.append("Execution time: " + time);
		bw.newLine();
		
		bw.close();
	}
}