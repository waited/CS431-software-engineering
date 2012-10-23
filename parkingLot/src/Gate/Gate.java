package Gate;

import java.io.*;
import java.net.*;
import java.util.*;

import utils.ConnectionData;
import utils.Traffic;
import utils.utils;

public class Gate {
	
	protected static Scanner sc;
	protected static Socket socketForManager, socketForTG, socketForTime, socketForParkingLot;
	protected static ArrayList<Traffic> queue;
	protected static Integer currentTime[] = new Integer[2], startTime[] = new Integer[2], endTime[] = new Integer[2]; // addr 0 is hour, addr 1 is minutes
	protected static int Token = 100;
	
	
	public static void main(String[] args) throws EOFException {
		
		queue = new ArrayList<Traffic>();
		
		ObjectInputStream OISforTG = null;
		ObjectOutputStream OOSforLot = null;
		
		System.out.print("login?");
		sc = new Scanner(System.in);
		String input = sc.nextLine();
		if(input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes"))
		{
				//System.out.println(ConnectionData.ipTG+" "+ConnectionData.portForTG);
			try {
				//socketForManager = new Socket(ConnectionData.ipMani, ConnectionData.portForManager);
				socketForTG = new Socket(ConnectionData.ipTG, ConnectionData.portForTG);
				socketForTime = new Socket(ConnectionData.ipTG, ConnectionData.portForTime);
				socketForParkingLot = new Socket(ConnectionData.ipLot, ConnectionData.portForLot);
				System.out.println("Connected");
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			receiverForManager RFM = new receiverForManager();
			Thread ThreadRFM = new Thread(RFM);
			ThreadRFM.start();
			
			trafficReceiver trafficRec = new trafficReceiver();
			Thread ThreadTraffic = new Thread(trafficRec);
			ThreadTraffic.start();
			
			try {
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
			//System.out.println("reach1");
			while(true)
			{
				try {
					if(Token>0 && queue.size()>0){
						Token--;
						System.out.println(Token);
						OOSforLot.writeObject(queue.get(0));
					} else if(Token==0 && queue.size()>0){
						String carOrig = queue.get(0).getGeneratedTime();
						Integer carOrigInt[] = utils.timeConverter(carOrig);
						int Orig = (carOrigInt[0]*3600)+(carOrigInt[1]*60);
						int Curren = (currentTime[0]*3600)+(currentTime[1]*60);
						
						if((Curren-Orig)>600){
							// need to something...
							//
							//
							//
							//
						} else {
							System.out.println("Token: "+Token+" queue Size: "+queue.size());
							continue;
						}
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}	
	}
}

class receiverForManager extends Gate implements Runnable{
	
	BufferedReader inFromManager = null;
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
}

class trafficReceiver extends Gate implements Runnable{
	protected static ObjectInputStream OISforTG = null;
	
	public trafficReceiver() {
		try {
			OISforTG = new ObjectInputStream(socketForTG.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NullPointerException npe){
			npe.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		try {
			while(true){
				//System.out.println("reach2");
				Traffic car = (Traffic) OISforTG.readObject(); // get the traffic from the T.G.
				queue.add(car);
				//System.out.println("Generated Time: "+car.getGeneratedTime()+", Expecting Out Time: "+car.getExpectingOutTime()+", Plate Number: "+car.getPlateNumber());
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} 
			
	}
	
}

class carReturned extends Gate implements Runnable{
	
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
				Token++;
				System.out.println("Generated Time: "+car.getGeneratedTime()+", Expecting Out Time: "+car.getExpectingOutTime()+", Plate Number: "+car.getPlateNumber());
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		
	}
	
}

class timeReader extends Gate implements Runnable{
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
			socketForTime = new Socket(ConnectionData.ipTG, ConnectionData.portForTime);
			time = new BufferedReader(new InputStreamReader(socketForTime.getInputStream()));
			while(true){
				String timekeeping = time.readLine();
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

