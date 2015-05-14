package stm.benchmark.tpcc;

import java.util.Random;

import stm.transaction.AbstractObject;

public class TpccHistory extends AbstractObject implements java.io.Serializable {	
	public int H_C_ID;
	public int H_C_D_ID;
	public int H_C_W_ID;
	public int H_D_ID;
	public int H_W_ID;
	public String H_DATE;
	public Double H_AMOUNT;
	public String H_DATA;
	
	private Random random = new Random();
	
	private String id;

	public TpccHistory() {
		// no argument constructor for kryo serialization	
	}

	public TpccHistory(String id, int c_id, int d_id) {

		this.id = id;

		this.H_W_ID = random.nextInt(100);
		this.H_D_ID = d_id;
		this.H_C_ID = c_id;
		this.H_DATE = Integer.toString(random.nextInt(100));
		this.H_AMOUNT = 10.0;
		this.H_DATA = Integer.toString(random.nextInt(100));
	}

	public String getId() {
		return id;
	}
	
//	public TpccHistory deepcopy() {
//		TpccHistory newObject = new TpccHistory("", 0, 0);
//		newObject.id = this.id;
//		newObject.H_W_ID = this.H_W_ID;
//		newObject.H_D_ID = this.H_D_ID;
//		newObject.H_C_ID = this.H_C_ID;
//		newObject.H_DATE = this.H_DATE;
//		newObject.H_AMOUNT = this.H_AMOUNT;
//		newObject.H_DATA = this.H_DATA;
//		return newObject;
//	}
}