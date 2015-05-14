package stm.benchmark.tpcc;

import java.util.Random; 

import stm.transaction.AbstractObject;


public class TpccCustomer extends AbstractObject implements java.io.Serializable {	
	public String C_FIRST;
	public String C_MIDDLE;
	public String C_LAST;
	public String C_STREET_1;
	public String C_STREET_2;
	public String C_CITY;
	public String C_STATE;
	public String C_ZIP;
	public String C_PHONE;
	public String C_SINCE;
	public String C_CREDIT;
	public Double C_CREDIT_LIM;
	public Double C_DISCOUNT;
	public Double C_BALANCE;
	public Double C_YTD_PAYMENT;
	public int C_PAYMENT_CNT;
	public int C_DELIVERY_CNT;
	public String C_DATA;
	private Random random = new Random();
	
	private String id;

	private String genCredit(){
		if (random.nextInt(100) < 90) return "GC"; 
		else return "BC";
	}
	
	public TpccCustomer() {
		// no argument constructor for kryo serialization	
	}

	public TpccCustomer(String id) {

		this.id = id;
		
		this.C_FIRST = Integer.toString(random.nextInt(100));
		this.C_MIDDLE = "OE";
		this.C_LAST = Integer.toString(random.nextInt(100));
		this.C_STREET_1 = Integer.toString(random.nextInt(100));
		this.C_STREET_2 = Integer.toString(random.nextInt(100));
		this.C_CITY = Integer.toString(random.nextInt(100));
		this.C_STATE = Integer.toString(random.nextInt(100));
		this.C_ZIP = Integer.toString(random.nextInt(100)) + "11111";
		this.C_PHONE = Integer.toString(random.nextInt(100));
		this.C_SINCE = Integer.toString(random.nextInt(100));
		this.C_CREDIT = genCredit();
		this.C_CREDIT_LIM = (double)50000.0;
		this.C_DISCOUNT = (double)(random.nextInt(5000) * 0.0001);
		this.C_BALANCE = (double)10.0;
		this.C_YTD_PAYMENT = (double)10.0;
		this.C_PAYMENT_CNT = 1;
		this.C_DELIVERY_CNT = 0;
		this.C_DATA = Integer.toString(random.nextInt(100));
	}

	public String getId() {
		return id;
	}

//	public TpccCustomer deepcopy() {
//		TpccCustomer newObject = new TpccCustomer("");
//		newObject.id = this.id;
//		newObject.C_FIRST = this.C_FIRST;
//		newObject.C_MIDDLE = this.C_MIDDLE;
//		newObject.C_LAST = this.C_LAST;
//		newObject.C_STREET_1 = this.C_STREET_1;
//		newObject.C_STREET_2 = this.C_STREET_2;
//		newObject.C_CITY = this.C_CITY;
//		newObject.C_STATE = this.C_STATE;
//		newObject.C_ZIP = this.C_ZIP;
//		newObject.C_PHONE = this.C_PHONE;
//		newObject.C_SINCE = this.C_SINCE;
//		newObject.C_CREDIT = this.C_CREDIT;
//		newObject.C_CREDIT_LIM = this.C_CREDIT_LIM;
//		newObject.C_DISCOUNT = this.C_DISCOUNT;
//		newObject.C_BALANCE = this.C_BALANCE;
//		newObject.C_YTD_PAYMENT = this.C_YTD_PAYMENT;
//		newObject.C_PAYMENT_CNT = this.C_PAYMENT_CNT;
//		newObject.C_DELIVERY_CNT = this.C_DELIVERY_CNT;
//		newObject.C_DATA = this.C_DATA;
//		return newObject;
//	}
}