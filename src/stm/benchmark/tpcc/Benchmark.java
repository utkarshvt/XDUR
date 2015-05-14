//package paxosstm.benchmark.tpcc;
//
//import java.util.Random;
//
//
//
//public class Benchmark //extends edu.vt.rt.hyflow.benchmark.tm.Benchmark
//{
//	Tpcc tpcc = new Tpcc();
//	
//	private Random random = new Random();
//	@Override
//	protected Class[] getSharedClasses() {
//		return new Class[] { Tpcc.class };
//	}
//	
//
//	@Override
//	protected void checkSanity() {
//		// TODO Auto-generated method stub
//	}
//
//	@Override
//	protected void createLocalObjects() {
//		Integer id = Network.getInstance().getID();
//		//int nodes = Network.getInstance().nodesCount();
//			
//		if(id == 0)
//			tpcc.TpccInit();
//	}
//	
//	@Override
//	protected String getLabel() {
//		return "Tpcc-TM";
//	}
//
//	@Override
//	protected int getOperandsCount() {
//		if (calls < 2)
//			return 2;
//		else
//			return calls;
//	}
//
//	@Override
//	protected Object randomId() {
//		return random.nextInt(100);
//	}
//
//	@Override
//	protected void readOperation(Object... ids) {
//		int r = random.nextInt(3);
//		switch(r) 
//		{
//		case 0:
//			Logger.debug("Read operation: orderStatus.");
//			tpcc.orderStatus(calls);
//			break;
//		case 1:
//			Logger.debug("Read operation: delivery.");
//			tpcc.delivery(calls);
//			break;
//		case 2:
//			Logger.debug("Read operation: stock Level.");
//			tpcc.stockLevel(calls);
//			break;
//		}
//		
//	}
//
//	@Override
//	protected void writeOperation(Object... ids) {
//		int r = random.nextInt(2);
//		switch(r)
//		{
//		case 0:
//			Logger.debug("Write operation: new_order.");
//			tpcc.newOrder(calls);
//			break;
//		case 1:
//			Logger.debug("Write operation: payment.");
//			tpcc.payment(calls);
//			break;
//
//		}
//		
//	}
//	
//	
//}
