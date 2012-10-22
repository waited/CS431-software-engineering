package utils;

import java.io.Serializable;


public class Traffic implements Serializable{
	
	/**
	 * @param Traffic object which is hold information of a car.
	 */

	private String generatedTime;
	private String expectingOutTime;
	private String intoLotTime;
	private String plateNumber;
	private boolean hasToken = false;
	private Token token;
	
	public Traffic(String generatedTime, String expectingOutTime, String plateNumber){
		this.generatedTime = generatedTime;
		this.expectingOutTime = expectingOutTime;
		this.plateNumber = plateNumber;
	}
	
	public void setIntoLot(String intoLotTime){
		this.intoLotTime = intoLotTime;
	}
	
	public boolean setToken(Token token){
		if(token!=null){
			if(hasToken == false){
				hasToken = true;
				this.token = token;
				return true;
			} else {
				return false;
			}
		}
		return false;
	}
	
	public String getGeneratedTime(){
		return generatedTime;
	}
	
	public String getExpectingOutTime(){
		return expectingOutTime;
	}
	
	public String getIntoLotTime(){
		return intoLotTime;
	}
	
	public String getPlateNumber(){
		return plateNumber;
	}
	
	public boolean hasToken(){
		return hasToken;
	}
	
	public Token outFromLot(){
		hasToken = false;
		return token;
	}
	
}
