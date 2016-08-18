package weka.clusterers;

public class Main{
	public static void main(String[] args) throws Exception{
		if(args[0].equals("numeric")){
			Manager manager = new Manager(args);
		}
		if(args[0].equals("string")){
			Manager1 manager = new Manager1(args);
		}
	}
}