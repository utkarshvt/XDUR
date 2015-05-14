package stm.benchmark.tpcc;

import java.util.Random; 

import stm.transaction.AbstractObject;


public class TpccDistrict extends AbstractObject implements java.io.Serializable {	
	public String  D_NAME;
	public String  D_STREET_1;
	public String  D_STREET_2;
	public String  D_CITY;
	public String  D_STATE;
	public String  D_ZIP;

	public double D_TAX;
	public double D_YTD; 
	public int D_NEXT_O_ID;
	private Random random = new Random();
	
	private String id;

	public TpccDistrict() {
		// no argument constructor for kryo serialization	
	}

	public TpccDistrict(String id) {

		this.id = id;

		this.D_NAME = Integer.toString(random.nextInt(100));
		this.D_STREET_1 = Integer.toString(random.nextInt(100));
		this.D_STREET_2 = Integer.toString(random.nextInt(100));
		this.D_CITY = Integer.toString(random.nextInt(100));
		this.D_STATE = Integer.toString(random.nextInt(100));
		this.D_ZIP = Integer.toString(random.nextInt(100)) + "11111";
		this.D_TAX = (double)(random.nextInt(2000) * 0.0001);
		this.D_YTD = (double)30000.0;
		this.D_NEXT_O_ID = 3001;
	}

	public String getId() {
		return id;
	}
	
//	public TpccDistrict deepcopy() {
//		TpccDistrict newObject = new TpccDistrict("");
//		newObject.id = this.id;
//		newObject.D_NAME = this.D_NAME;
//		newObject.D_STREET_1 = this.D_STREET_1;
//		newObject.D_STREET_2 = this.D_STREET_2;
//		newObject.D_CITY = this.D_CITY;
//		newObject.D_STATE = this.D_STATE;
//		newObject.D_ZIP = this.D_ZIP;
//		newObject.D_TAX = this.D_TAX;
//		newObject.D_YTD = this.D_YTD;
//		newObject.D_NEXT_O_ID = this.D_NEXT_O_ID;
//		return newObject;
//	}
}