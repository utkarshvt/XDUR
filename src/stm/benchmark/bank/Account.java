package stm.benchmark.bank;

import java.nio.ByteBuffer;

import stm.transaction.AbstractObject;

public class Account extends AbstractObject implements java.io.Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2584138666421024340L;
	private int amount;
	private String Id;
	
	public Account(int initialAmount, String Id) {
		super();
		amount = initialAmount;
		this.Id = Id;
	}
	
	public Account(int initialAmount) {
		super();
		amount = initialAmount;
		this.Id = "";
	}

	// No-args constructor to satisfy Kryo
	public Account() {
		super();
	}
	
	@Override
	public String getId() {
		return Id;
	}

	public void setId(String Id) {
		this.Id = Id;
	}
	
	public int getAmount() {
		return amount;
	}

	public void setAmount(int value) {
		amount = value;
	}
	
	public void withdraw(int amountToTransfer) {
		amount -= amountToTransfer;
	}

	public void deposit(int amountToTransfer) {
		amount += amountToTransfer;
	}
}
