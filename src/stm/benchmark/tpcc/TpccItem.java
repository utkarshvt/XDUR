package stm.benchmark.tpcc;

import java.io.Serializable;
import java.util.List;
import java.util.Random;

import stm.transaction.AbstractObject;


public class TpccItem extends AbstractObject implements java.io.Serializable {	
	public String I_IM_ID;
	public String I_NAME;
	public float I_PRICE;
	public String I_DATA;
	
	private Random random = new Random();
	private String id;

	public TpccItem() {
		// no argument constructor for kryo serialization	
	}

	public TpccItem(String id) {

		this.id = id;

		this.I_IM_ID = Integer.toString(random.nextInt(100));
		this.I_NAME = Integer.toString(random.nextInt(100));
		this.I_PRICE = random.nextFloat();
		this.I_DATA = Integer.toString(random.nextInt(100));
	}


	public String getId() {
		return id;
	}

//	public TpccItem deepcopy() {
//		TpccItem newObject = new TpccItem("");
//		newObject.id = this.id;
//		newObject.I_IM_ID = this.I_IM_ID;
//		newObject.I_NAME = this.I_NAME;
//		newObject.I_PRICE = this.I_PRICE;
//		newObject.I_DATA = this.I_DATA;
//		return newObject;
//	}
}