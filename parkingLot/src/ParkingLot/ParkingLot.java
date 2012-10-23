package ParkingLot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

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
		
		System.out.println("Parking Lot ready to work.");
	}	
}

/*
 * This Thread returns car on the time. also randomly returns car too.
 */
class carReturner extends ParkingLot implements Runnable{
	@Override
	public void run() {
		while(true){
			try{
				System.out.println(startTime[0]+":"+startTime[1]+" "+currentTime[0]+":"+currentTime[1]);
				for(int i=startTime[0]; i<currentTime[0]; i++){
					try{
						//System.out.println("check carDB line 68 "+ currentTime[0]);
						if(carDB.containsKey((Integer) i)){
							int returnCars = carDB.get((Integer) i).size();
							ArrayList<Traffic> trafficArray = carDB.get((Integer) i);
							for(int j=0; j<returnCars; j++){
								returner(trafficArray.get(j));
							}
							carDB.remove((Integer) i);
							countForCarsInLot-=returnCars;
						}
					} catch(NullPointerException NE) {
						continue;
					}
				}
			} catch(NullPointerException ne) {
				continue;
			}
			
			if(carDB.containsKey((Integer) currentTime[0])==false){
				continue;
			}
			
			ArrayList<Traffic> dataOfCars = carDB.get((Integer) currentTime[0]);
			
			int flg = 0;
			while(flg<dataOfCars.size()){
				
				Integer carOutTime[] = utils.timeConverter(dataOfCars.get(flg).getExpectingOutTime());
				
				if(carOutTime[1]<currentTime[1]){
					returner(dataOfCars.get(flg));
					dataOfCars.remove(flg);
					carDB.put(currentTime[0], dataOfCars);
					countForCarsInLot--;
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
	
	private void returner(Traffic traffic){
		System.out.println(traffic.getPlateNumber()+" returned");
		Random rand = new Random();
		int theGatePos = rand.nextInt(gateSocketArray.size());
		try {
			gateSocketArray.get(theGatePos).OOS.writeObject(traffic);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
				//ThreadCarReturn.start();
				if(timekeeping.indexOf("|")<0){
					//System.out.println(timekeeping+" "+timekeeping.indexOf("|"));
					currentTime = utils.timeConverter(timekeeping);
					//System.out.println(currentTime[0]+":"+currentTime[1]);
				} else {
					int startPos = Integer.MIN_VALUE;
					if(timekeeping.indexOf("S")>-1){
						startPos = timekeeping.indexOf("S");
					}
					if(timekeeping.indexOf("E")>-1){
						startPos = timekeeping.indexOf("E");
					}
					String head = timekeeping.substring(startPos, timekeeping.indexOf("|"));
					timekeeping = timekeeping.substring(timekeeping.indexOf("|")+1);
					//System.out.println("Header: "+head+", "+timekeeping);
					
					if(head.equalsIgnoreCase("START")){
						
						//System.out.println("Header: "+head+", "+timekeeping);
						startTime = utils.timeConverter(timekeeping);
						
					} else if(head.equalsIgnoreCase("END")){
						
						//System.out.println("Header: "+head+", "+timekeeping);
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
		this.position = pos-1;
	}
	
	@Override
	public void run() {
		ObjectInputStream OIS;
		ConnectorInfo cInfo = gateSocketArray.get(position);
		try {
			OIS = new ObjectInputStream(cInfo.socket.getInputStream()); // open up the input stream.
		
			while(true)
			{
				try {
					
					if(countForCarsInLot>=PARKINGLOT_SIZE){
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						continue;
					}
					
					Traffic car = (Traffic) OIS.readObject(); // get the traffic from the T.G.
					countForCarsInLot++;
					
					//System.out.println("Generated Time: "+car.getGeneratedTime()+", Expecting Out Time: "+car.getExpectingOutTime()+", Plate Number: "+car.getPlateNumber());
					
					Integer expectOutTime[] = utils.timeConverter(car.getExpectingOutTime());
					ArrayList<Traffic> tempDataTraffic = carDB.get(expectOutTime[0]);
					
					try{ //add car to the time frame in the hash map.
						tempDataTraffic.add(car);
					} catch(NullPointerException npe){ //if the array is null. it need to defined it.
						tempDataTraffic = new ArrayList<Traffic>();
						tempDataTraffic.add(car);
					}
					
					carDB.put(expectOutTime[0], tempDataTraffic);
					//System.out.println(carDB.size());
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}
	}
	
}

