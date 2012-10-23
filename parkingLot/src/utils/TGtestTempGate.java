package utils;

import java.io.*;
import java.net.*;
import java.util.*;


public class TGtestTempGate {

	/**
	 * @param Templary testing gate for getting traffic from traffic generator. This program will be replace with Actual gate program.
	 */
	
	static Scanner sc;
	static Socket socketForTG, socketForTime, socketForParkingLot;
	
	public static void main(String[] args) throws EOFException {
		
		ArrayList<Traffic> cars = new ArrayList<Traffic>();
		ObjectInputStream OIS = null;
		ObjectOutputStream OOSforLot = null;
		
		System.out.print("login?");
		sc = new Scanner(System.in);
		String input = sc.nextLine();
		if(input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes"))
		{
				//System.out.println(ConnectionData.ipTG+" "+ConnectionData.portForTG);
			try {
				socketForTG = new Socket(ConnectionData.ipTG, ConnectionData.portForTG);
				socketForTime = new Socket(ConnectionData.ipTG, ConnectionData.portForTime);
				socketForParkingLot = new Socket(ConnectionData.ipLot, ConnectionData.portForLot);
				System.out.println("Connected");
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				OIS = new ObjectInputStream(socketForTG.getInputStream()); // open up the input stream.
				OOSforLot = new ObjectOutputStream(socketForParkingLot.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException npe) {
				npe.printStackTrace();
			}
			
			timeReader TR = new timeReader();
			Thread thread = new Thread(TR);
			thread.start();
			
			carReturned CR = new carReturned();
			Thread thread1 = new Thread(CR);
			thread1.start();
			
			while(true)
			{
				try {
					Traffic car = (Traffic) OIS.readObject(); // get the traffic from the T.G.
					System.out.println("1. Generated Time: "+car.getGeneratedTime()+", Expecting Out Time: "+car.getExpectingOutTime()+", Plate Number: "+car.getPlateNumber());
					cars.add(car);
					OOSforLot.writeObject(car);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}	
	}
}

class carReturned extends TGtestTempGate implements Runnable{
	
	ObjectInputStream OISforLot = null;
	
	public carReturned(){
		try{
			OISforLot = new ObjectInputStream(socketForParkingLot.getInputStream());
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	@Override
	public void run() {
		
		while(true)
		{
			try {
				Traffic car = (Traffic) OISforLot.readObject(); // get the traffic from the T.G.
				System.out.println("2. Generated Time: "+car.getGeneratedTime()+", Expecting Out Time: "+car.getExpectingOutTime()+", Plate Number: "+car.getPlateNumber());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		
	}
	
}

class timeReader extends TGtestTempGate implements Runnable{
	BufferedReader time = null;
	
	public timeReader(){
		try {
			time = new BufferedReader(new InputStreamReader(socketForTime.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void run() {
		try {
			while(true){
				String timekeeping = time.readLine();
				if(timekeeping.indexOf("|")<0){
					System.out.println("time: "+ timekeeping);
				} else {
					//System.out.println("1. "+timekeeping);
					int startPos = Integer.MIN_VALUE;
					if(timekeeping.indexOf("S")>-1){
						startPos = timekeeping.indexOf("S");
					}
					if(timekeeping.indexOf("E")>-1){
						startPos = timekeeping.indexOf("E");
					}
					String head = timekeeping.substring(startPos, timekeeping.indexOf("|"));
					timekeeping = timekeeping.substring(timekeeping.indexOf("|")+1);
					System.out.println("Header: "+head+", "+timekeeping);
					
				}
			}
			} catch (IOException e) {
				e.printStackTrace();
			}
				
	}
	
}
