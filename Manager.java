package weka.clusterers;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import weka.core.Instances;
import weka.core.converters.ArffLoader.ArffReader;

public class Manager {

	private int alphaLen;
	private int N;
	private int minLen;
	private int maxLen;
	private char[] alphabet;

	private static Random rand;
	private static int MAX_THREADS = 4;
	private static Instances data;
	private static int nClust;
	private static int seed;
	private static int K = 0;
	private static double delta;
	private static int minClust, maxClust;
	private static DistanceType t;
	private static GoodnessType cgt;
	private final int n_test = 5;
	
	/*INPUT PARAMETERS
	 * "ED" 2 10 "C:/Users/dav_0/Desktop/pam_big.arff" 1 20 0 
	 * "LD" 2 10 "C:/Users/dav_0/Desktop/stringTest.arff" 1 20 0
	 */

	public Manager(String[] args) throws Exception {
		switch (args[0]) {
		case "ED":
			t = DistanceType.EUCLIDEAN;
			break;
		case "LD":
			t = DistanceType.LEVENSHTEIN;
			break;
		}
		
		cgt = GoodnessType.ELEMENT_QTY;
		
		minClust = Integer.parseInt(args[1]);
		maxClust = Integer.parseInt(args[2]);

		String filePath = args[3];
		seed = Integer.parseInt(args[4]);
//		threads = new Thread[MAX_THREADS];
		if (!args[5].equalsIgnoreCase("inf")) {
			K = Integer.parseInt(args[5]);
		}
		delta = Double.parseDouble(args[6]);
		rand = new Random(seed);
		
		if(K < MAX_THREADS){
			MAX_THREADS = K;
		}
		
		if(args[0].equalsIgnoreCase("LD")){
			N = 1000;
			minLen = 3;
			maxLen = 6;
			alphaLen = 10;
			
			alphabet = alphaCreator(alphaLen);
			
			String[] s = new String[N];
			for (int i = 0; i < N; i++) {
				int len = rand.nextInt(alphaLen * maxLen);
				while (len < alphaLen * minLen || len > alphaLen * maxLen) {
					len = rand.nextInt(26 * maxLen);
				}
				s[i] = "";
				for (int j = 0; j < len; j++) {
					int val = rand.nextInt(alphaLen);
					s[i] += alphabet[val];
				}
			}
			createStringARFF(s);
		}

		BufferedReader reader = new BufferedReader(new FileReader(filePath));
		ArffReader arff = new ArffReader(reader);
		data = arff.getData();
		
		Configuration optimalNClust = null;
		
		// variabile start per il calcolo del tempo di esecuzione
		double startTime = System.nanoTime();
		
		nClust = crossValidation(data, t, rand);
		
		double cg = 0.0;

		optimalNClust = clusterization(data, nClust, rand, t);
		// System.out.println("threads terminated " + count);

		// printCluster(firstConfig, nClust);
		// printCluster(bestConfig, nClust);
		printCluster(optimalNClust, nClust);
		cg = clusterGoodness(optimalNClust, cgt);
		// double p = clusterGoodness(bestConfig);

		// variabile fine calcolo del tempo di esecuzione
		double endTime = System.nanoTime();
		double time = (endTime - startTime) / 1000000000;
		System.out.println("Execution time: " + time + " s");
		outputFile(time, optimalNClust, seed, nClust, K, delta, t, cgt, cg, n_test);
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
	
	public int crossValidation(Instances data, DistanceType t, Random rand) throws Exception{
		int tot = data.numInstances();
		int train_qty = (90*tot)/100;
		int test_qty = tot - train_qty;
		int[] instances2train = new int[train_qty];
		int[] instances2test = new int[test_qty];
		
		//istance per il file di training
		for(int i=0; i<train_qty; i++){
			int val = rand.nextInt(data.numInstances());
			while(alreadyExist(instances2train, val)){
				val = rand.nextInt(data.numInstances());
			}
			instances2train[i] = val;
		}
//		System.out.println(data.numInstances() + "Train: ");
//		for(int i=0; i<train_qty; i++){
//			System.out.print(instances2train[i] + ",");
//		}
		
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
		
//		System.out.println("Test: ");
//		for(int i=0; i<test_qty; i++){
//			System.out.print(instances2test[i] + ",");
//		}
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
			double a = 0.0;
			a = optimal.buildTestConfig(data, trainData, testData, t);
			System.err.println("a: " + a + " cost: " + cost + " nc: " + nc);
			if(a < cost){
				cost = a;
				nc = m;
			}
		}
		
		return nc;
	}
	
	public Configuration clusterization(Instances data, int nClust, Random rand, DistanceType t) throws Exception{
		Configuration optimal = null;
		Configuration firstConfig;
		Configuration currentConfig;
		Configuration bestConfig = null;
		Configuration[] configs = new Configuration[MAX_THREADS];
		int[][][] randCombs = null;
		Thread[] threads = new Thread[MAX_THREADS];
		ArrayList<Integer> sizes;
//		int count = 0;
		boolean flag = true;
		
		for(int n=0; n<n_test; n++){
//			System.err.println("ntest: " + n);
			currentConfig = new Configuration(data, nClust, rand, t);
			firstConfig = currentConfig.clone();
//			printCluster(firstConfig, nClust);
			if (optimal == null) {
				optimal = currentConfig.clone();
			}
//			firstConfig.printStatus();
			bestConfig = currentConfig.clone();
			flag = true;
//			count = 0;
			while (flag) {
//				System.err.println("count: " + count);
//				printCluster(firstConfig, m);
				sizes = new ArrayList<Integer>();
				for (int i = 0; i < nClust; i++) {
					sizes.add(firstConfig.getCentroidAt(i).getAllInstances().size());
				}
				// System.err.println();
				// System.err.println();
				// firstConfig.printStatus();
				// System.out.println("Combination per thread: " + K/MAX_THREADS
				// + " resto: " + K%MAX_THREADS);
				if (delta == 0) {
					randCombs = randCombinations(nClust, sizes, firstConfig, rand);
				}
				else {
					randCombs = randCombinationsDelta(nClust, sizes, firstConfig, data, rand);
				}
//				 printRandCombs(randCombs);
				int val = K / MAX_THREADS;
				for (int i = 0; i < MAX_THREADS; i++) {
					if (i == MAX_THREADS - 1) {
						val = K / MAX_THREADS + K % MAX_THREADS;
					}
					threads[i] = new Thread(new Task(data, nClust, i, randCombs[i], val, configs, t));
					threads[i].start();
				}
				for (int i = 0; i < MAX_THREADS; i++) {
					threads[i].join();
				}
				for (int i = 0; i < MAX_THREADS; i++) {
					try {
						if (configs[i].isBetterThan(bestConfig)) {
							bestConfig = configs[i].clone();
						}
					} catch (NullPointerException e) {
						System.err.println("Trying to call method of configs[" + i + "] " + e);
					}
					
				}
//				printCluster(bestConfig, nClust);
				flag = bestConfig.isChanged(firstConfig);
				firstConfig = bestConfig.clone();
//				count++;
			}
			
			if(bestConfig.isBetterThan(optimal)){
				optimal = bestConfig.clone();
			}
		}
		return optimal;
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
		path[0] = "C:/Users/dav_0/Desktop/training.arff"; //trainPath
		path[1] = "C:/Users/dav_0/Desktop/test.arff"; //testPath 
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
					val = c.getCentroidAt(i).getNumElements();
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

	static class SizesComparator implements Comparator<Sizes>{
		@Override public int compare(Sizes a, Sizes b){
			return a.size < b.size ? -1 :
				a.size == b.size ? 0 : 1;
		}
	}
	
	public static int[][][] randCombinations(int nClust, ArrayList<Integer> sizes, Configuration firstConfig, Random rand) {
		int[][][] randCombs = new int[MAX_THREADS][(K / MAX_THREADS) + (K % MAX_THREADS)][nClust];
		int val;
		int qty = K / MAX_THREADS;
		ArrayList<Sizes> s = new ArrayList<Sizes>();
		for(int k=0; k<sizes.size(); k++){
			s.add(new Sizes(sizes.get(k), k));
		}
		Collections.sort(s, new SizesComparator());
		
		for (int i = 0; i < MAX_THREADS; i++) {
			if (i == MAX_THREADS - 1) {
				qty = (K / MAX_THREADS) + (K % MAX_THREADS);
			}
			for (int j = 0; j < qty; j++) {
				for (int h = 0; h < nClust; h++) {
					int range = s.get(h).size;
//					System.out.println(range);
					if (range == 0) {
						range = 1;
					}
					val = rand.nextInt(range);
					while(alreadyExist(randCombs[i][j], val)){
						val = rand.nextInt(range);
//						System.out.println(val);
					}
					
					randCombs[i][j][s.get(h).index] = firstConfig.getCentroidAt(s.get(h).index).getID(val);
				}
				if(alreadyExistComb(randCombs, randCombs[i][j], nClust)){
					j--;
				}
			}
		}
		return randCombs;
	}

	public static int[][][] randCombinationsDelta(int nClust, ArrayList<Integer> sizes, Configuration firstConfig, Instances data, Random rand) {
		int[][][] randCombs = new int[MAX_THREADS][(K / MAX_THREADS) + (K % MAX_THREADS)][nClust];
		int val = 0;
		int qty = K / MAX_THREADS;
		ArrayList<Sizes> s = new ArrayList<Sizes>();
		for(int k=0; k<sizes.size(); k++){
			s.add(new Sizes(sizes.get(k), k));
		}
		Collections.sort(s, new SizesComparator());
		
		for (int i = 0; i < MAX_THREADS; i++) {
			if (i == MAX_THREADS - 1) {
				qty = (K / MAX_THREADS) + (K % MAX_THREADS);
			}
			for (int j = 0; j < qty; j++) {
				for (int h = 0; h < nClust; h++) {
					int range = s.get(h).size;
					if (range == 0) {
						range = 1;
					}
					double cost = 0;
					while (cost < delta) {
						val = rand.nextInt(range);
						while(alreadyExist(randCombs[i][j], val)){
							val = rand.nextInt(range);
						}
//						System.err.println(val + " " + firstConfig.getCentroidAt(h).getID());
						switch(t){
						case EUCLIDEAN:
							for (int ii = 0; ii < data.instance(val).numAttributes(); ii++) {
								cost += Math.pow(data.instance(val).value(ii)
										- data.instance(firstConfig.getCentroidAt(s.get(h).index).getID()).value(ii), 2);
							}
							cost = Math.sqrt(cost);
							break;
						case LEVENSHTEIN:
							cost += Centroid.computeLevenshteinDistance(data.instance(val).stringValue(data.instance(val).attribute(0)),
									data.instance(firstConfig.getCentroidAt(s.get(h).index).getID()).stringValue(data.instance(firstConfig.getCentroidAt(s.get(h).index).getID()).attribute(0)));
							break;
						default:
							System.err.println("Error RandomComb Distance");
							break;
						}
//						System.err.println("cost: " + cost + " delta: " + delta);
					}
					randCombs[i][j][s.get(h).index] = firstConfig.getCentroidAt(s.get(h).index).getID(val);
				}
				if(alreadyExistComb(randCombs, randCombs[i][j], nClust)){
					j--;
				}
			}
		}
		return randCombs;
	}
	
	public static boolean alreadyExistComb(int[][][] combs, int[] comb, int nClust){
		int count = 0;
		int[] exist = new int[nClust];
		for(int i=0; i<nClust; i++){
			exist[i] = 0;
		}
		int len = K / MAX_THREADS;
		
		for(int i=0; i<MAX_THREADS; i++){
			if(i == MAX_THREADS-1){
				len = (K / MAX_THREADS) + (K % MAX_THREADS);
			}
			for(int j=0; j<len; j++){
				for(int k=0; k<nClust; k++){
					exist[k] = 0;
				}
				for(int k=0; k<nClust; k++){
					for(int x=0; x<nClust; x++){
//						System.out.println(comb[k] + " " + combs[i][j][x]);
						if(comb[k] == combs[i][j][x]){
							exist[k] = 1;
						}
						else{
							exist[k] = 0;
						}
					}
//					System.out.println();
				}
				count = 0;
				for(int m=0; m<nClust; m++){
//					System.out.print(exist[m] + " ");
					if(exist[m] == 1){
						count++; 
					}
				}
//				System.out.println();
				if(count == nClust){
					return true;
				}
			}
		}
		
		return false;
	}
	
	public static void printRandCombs(int[][][] randCombs) {
		int qty = K / MAX_THREADS;
		for (int i = 0; i < MAX_THREADS; i++) {
			if (i == MAX_THREADS - 1) {
				qty = (K / MAX_THREADS) + (K % MAX_THREADS);
			}
			for (int j = 0; j < qty; j++) {
				for (int h = 0; h < nClust; h++) {
					System.err.print(randCombs[i][j][h] + " ");
				}
				System.err.println();
			}
		}
	}

	public static void printCluster(Configuration c, int nClust) {
		for (int i = 0; i < nClust; i++) {
			System.out.println("Cluster " + "[" + i + "]");
			System.out.println("Centroid " + "[" + c.getCentroidAt(i).id + "]");
			System.out.print("Elements [ ");
			for (int l : c.getCentroidAt(i).getAllInstances()) {
				System.out.print(l + " ");
			}
			System.out.print("]");
			System.out.println();
			System.out.println("Num. of elements " + c.getCentroidAt(i).getNumElements());
			System.out.println();
		}
	}

	public static void printStatus(ArrayList<Centroid> cluster) {
		System.out.print("Centroids: ");
		for (int i = 0; i < cluster.size(); i++) {
			System.out.print(cluster.get(i).getID() + " ");
		}
	}

	public char[] alphaCreator(int alphaLen) {
		if (alphaLen < 2) {
			alphaLen = 2;
		}
		if (alphaLen > 26) {
			alphaLen = 26;
		}
		char[] alphabet = new char[alphaLen];
		for (int i = 0; i < alphaLen; i++) {
			alphabet[i] = (char) (i + 65);
			// System.out.println(alphabet[i]);
		}
		return alphabet;
	}

	public void createStringARFF(String[] s) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter("C:/Users/dav_0/Desktop/stringTest.arff"));
		bw.write("@relation clustered");
		bw.newLine();
		bw.newLine();
		bw.append("@attribute att1 string");
		bw.newLine();
		bw.newLine();
		bw.append("@data");
		bw.newLine();
		bw.append("%");
		bw.newLine();
		bw.append("% " + N);
		bw.newLine();
		bw.append("%");
		bw.newLine();
		for (int i = 0; i < N; i++) {
			bw.append(s[i]);
			if (i <= N - 2) {
				bw.newLine();
			}
		}
		bw.close();
	}

	public static void outputFile(double time, Configuration c, int seed, int nClust, int K, double delta, DistanceType t, GoodnessType cgt, double cg, int n_test) throws Exception{
		BufferedWriter bw = new BufferedWriter(new FileWriter("C:/Users/dav_0/Desktop/outputValues.arff"));
		bw.write("Dati Clusterizzazione");
		bw.newLine();
		bw.newLine();
		bw.append("Seed: " + seed);
		bw.newLine();
		bw.append("Distance Type: " + t.toString());
		bw.newLine();
		bw.append("GoodnessType: " + cgt.toString());
		bw.newLine();
		bw.append("K: " + K);
		bw.newLine();
		bw.append("Delta: " + delta);
		bw.newLine();
		bw.append("Numero test: " + n_test);
		bw.newLine();
		bw.append("Numero Cluster: " + nClust);
		bw.newLine();
		bw.append("Centroidi: ");
		for(int i=0; i<nClust; i++){
			bw.append("{" + c.getCentroidAt(i).getID() + "} ");
		}
		bw.newLine();
		bw.append("Elementi: ");
		for(int i=0; i<nClust; i++){
			bw.append("(" + c.getCentroidAt(i).getNumElements() + ") ");
		}
		bw.newLine();
		bw.append("Clusters Goodness: " + cg);
		bw.newLine();
		bw.append("Execution time: " + time);
		bw.newLine();
		
		bw.close();
	}
}