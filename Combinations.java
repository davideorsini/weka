package weka.clusterers;

import java.util.ArrayList;

public class Combinations{
	private int[] clustersSize; 
	private int[] currentCombination;
	private boolean flagEnded = false;
	
	/*public Combinations(int... ints){
		clustersSize = ints.clone();
		currentCombination = ints;
		printArray(clustersSize);
	}*/
	
	public Combinations(ArrayList<Integer> list) {
		clustersSize = new int[list.size()];
		currentCombination = new int[list.size()];
		for (int i=0; i<list.size(); i++) {
			clustersSize[i] = list.get(i);
			currentCombination[i] = list.get(i);
		}
		printArray(clustersSize);
	}
	
	public static void printArray(int[] array) {
		if (array == null)
			return;
		String output = "{ ";
		for (int i=0; i<array.length; i++) {
			output += array[i] + ", ";
		}
		
		output += "}";
		System.out.println(output);
	}
	
	private void decreaseUnit(int index){
		if(currentCombination[index] == 0){
			currentCombination[index] = clustersSize[index];
			if(index > 0){
				decreaseUnit(index - 1);
			}
			else{
				System.out.println("Combination deplated");
				flagEnded = true;
			}
		}
		else{
			currentCombination[index]--;
		}
	}
	
	public int[] getCombination(){
		if(flagEnded){
			return null;
		}
		
		int[] toRet = currentCombination;
		decreaseUnit(currentCombination.length-1);
		return toRet;
	}
}