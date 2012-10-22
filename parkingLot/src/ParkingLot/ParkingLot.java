package ParkingLot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import utils.*;


public class ParkingLot {

	protected static final int PARKINGLOT_SIZE = 180;
	protected static int countForCarsInLot;
	protected static HashMap<Integer, ArrayList<Traffic>> carDB;
	protected static Integer currentTime[] = new Integer[2], startTime[] = new Integer[2], endTime[] = new Integer[2]; // addr 0 is hour, addr 1 is minutes 
	
	protected static ArrayList<ConnectorInfo> gateSocketArray;
	protected static ServerSocket serverSocket;

	protected static Socket socketForTime;
	protected static BufferedReader in = null;
	
	protected static Thread ThreadGateAccepter, ThreadTimeReciver, ThreadCarReturn, ThreadTrafficReceiver;
	
	public static void main(String[] args) {
		countForCarsInLot = 0;
		carDB = new HashMap<Integer, ArrayList<Traffic>>();
		gateSocketArray = new ArrayList<ConnectorInfo>();
		
		try {
			serverSocket = new ServerSocket(ConnectionData.portForLot);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		gateAccepter accepter = new gateAccepter(); //Thread for get connection.
		ThreadGateAccepter = new Thread(accepter);
		ThreadGateAccepter.start();
		
		timeAccepter timeAcc = new timeAccepter();
		ThreadTimeReciver = new Thread(timeAcc);
		ThreadTimeReciver.start();
		
		carReturner carReturn =  new carReturner();
		ThreadCarReturn = new Thread(carReturn);
		ThreadCarReturn.start();
		
	}	
}

/*
 * This Thread returns car on the time. also randomly returns car too.
 */
class carReturner extends ParkingLot implements Runnable{
	
	@Override
	public void run() {
		while(true){
			for(int i=startTime[0]; i<currentTime[0]; i++){
				if(carDB.containsKey((Integer) i)){
					carDB.remove((Integer) i);
				}
			}
			ArrayList<Traffic> dataOfCars = carDB.get((Integer) currentTime[0]);
			
			int flg = 0;
			while(flg<=dataOfCars.size()){
				
				Integer carOutTime[] = utils.timeConverter(dataOfCars.get(flg).getExpectingOutTime());
				
				if(carOutTime[1]<currentTime[1]){
					dataOfCars.remove(flg);
				} else {
					flg++;
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				continue;
			}
		}
	}
	
}

/*
 * keep update time as integer array hour and minute.
 */
class timeAccepter extends ParkingLot implements Runnable{

	protected static Socket socketForTime;
	protected static BufferedReader in = null;
	
	@Override
	public void run() {
		try {
			socketForTime = new Socket(ConnectionData.ipTG, ConnectionData.portForTime);
			in = new BufferedReader(new InputStreamReader(socketForTime.getInputStream()));
			while(true){
				String timekeeping = in.readLine();
				if(timekeeping.indexOf("|")<0){
					System.out.println(timekeeping);
					currentTime = utils.timeConverter(timekeeping); 
				} else {
					String head = timekeeping.substring(0, timekeeping.indexOf("|"));
					timekeeping = timekeeping.substring(timekeeping.indexOf("|")+1);
					System.out.println("Header: "+head+", "+timekeeping);
					if(head.equalsIgnoreCase("START")){
						
						System.out.println("Header: "+head+", "+timekeeping);
						startTime = utils.timeConverter(timekeeping);
						
					} else if(head.equalsIgnoreCase("END")){
						
						System.out.println("Header: "+head+", "+timekeeping);
						endTime = utils.timeConverter(timekeeping);
						
					} else {
						System.err.println("Wrong time header recived on Parking Lot.");
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}

/*
 * Keep running and get connection and add it the array of gateSocketArray.
 */
class gateAccepter extends ParkingLot implements Runnable{

	Socket socket;
	
	@Override
	public void run() {
		
		boolean flg = true;
		while(flg)
		{
			try {
				socket = serverSocket.accept();
				ConnectorInfo GI = new ConnectorInfo(socket);
				gateSocketArray.add(GI);
				
				trafficReceiver TR = new trafficReceiver(gateSocketArray.size()); // making same number of thread as number of gates for receiving traffic.
				ThreadTrafficReceiver = new Thread(TR);
				ThreadTrafficReceiver.start();
				
				System.out.println("Connection detected.");
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException NPE) {
				continue;
			}
		}
	}
	
}

class trafficReceiver extends ParkingLot implements Runnable{

	int position = Integer.MIN_VALUE;
	
	public trafficReceiver(int pos) {
		this.position = pos;
	}
	
	@Override
	public void run() {
		
		ConnectorInfo cInfo = gateSocketArray.get(position);
		try {
			cInfo.OIS = new ObjectInputStream(cInfo.socket.getInputStream()); // open up the input stream.
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}
		
		while(true)
		{
			try {
				Traffic car = (Traffic) cInfo.OIS.readObject(); // get the traffic from the T.G.
				System.out.println("Generated Time: "+car.getGeneratedTime()+", Expecting Out Time: "+car.getExpectingOutTime()+", Plate Number: "+car.getPlateNumber());
				Integer expectOutTime[] = utils.timeConverter(car.getExpectingOutTime());
				ArrayList<Traffic> tempDataTraffic = carDB.get(expectOutTime[0]);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
}

