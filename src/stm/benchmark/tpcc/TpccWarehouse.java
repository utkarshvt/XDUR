package stm.benchmark.tpcc;

import java.io.Serializable;
import java.util.List;
import java.util.Random;

import stm.transaction.AbstractObject;

public class TpccWarehouse extends AbstractObject implements java.io.Serializable {	
	public String W_NAME;
	public String W_STREET_1; 
	public String  W_STREET_2; 
	public String  W_CITY; 
	public String W_STATE;
	public String W_ZIP; 
	public float W_TAX; 
	public float W_YTD; 
		
	private Random random = new Random();

	private String id;

	public TpccWarehouse() {
		// no argument constructor for kryo serialization	
	}

	public TpccWarehouse(String id) {			
		this.id = id;
		
		this.W_NAME = Integer.toString(random.nextInt(100));
		this.W_STREET_1 = Integer.toString(random.nextInt(100));
		this.W_STREET_2 = Integer.toString(random.nextInt(100));
		this.W_CITY = Integer.toString(random.nextInt(100));
		this.W_STATE = Integer.toString(random.nextInt(100));
		this.W_ZIP = Integer.toString(random.nextInt(100)) + "11111";
		this.W_TAX = (float)(random.nextInt(2000) * 0.0001);
		this.W_YTD = (float)300000.0;
	}

	public String getId() {
		return id;
	}
	
//	public TpccWarehouse deepcopy() {
//		TpccWarehouse newObject = new TpccWarehouse("");
//		newObject.id = this.id;
//		newObject.W_NAME = this.W_NAME;
//		newObject.W_STREET_1 = this.W_STREET_1;
//		newObject.W_STREET_2 = this.W_STREET_2;
//		newObject.W_CITY = this.W_CITY;
//		newObject.W_STATE = this.W_STATE;
//		newObject.W_ZIP = this.W_ZIP;
//		newObject.W_TAX = this.W_TAX;
//		newObject.W_YTD = this.W_YTD;
//		return newObject;
//	}
}