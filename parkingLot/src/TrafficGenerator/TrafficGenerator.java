package TrafficGenerator;

import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import utils.ConnectorInfo;
import utils.Traffic;

public class TrafficGenerator {

	/**
	 * @param Simulating program for parking lot. this program makes virtual traffic as user input.
	 */
	
	protected static ServerSocket serverSocket;
	protected static ServerSocket timeKeeperSocket;
	protected static ArrayList<ConnectorInfo> gateSocketArray;
	protected static ArrayList<ConnectorInfo> timeKeeperArray;
	protected static String timekeeper = "";
 	protected static Thread accepterThread, senderThread, TKaccepterThread;
	protected static ArrayList<String> plateDB;
	protected static final String START = "START", END = "END"; 
	
	public static void main(String[] args) {
		
		gateSocketArray = new ArrayList<ConnectorInfo>(); // array of connected gates.
		timeKeeperArray = new ArrayList<ConnectorInfo>(); //array of time keeper.
		plateDB = new ArrayList<String>(); // array of generated plate number use for avoiding duplicated plate number. 
		Scanner SC;
		String startTime, endTime, duration;
		int startHour=0, startMin=0, endHour=0, endMin=0, durationInSec=0;
		System.out.println("Traffic Generator is running now.\nThis program should run before gate is run. It will be geberate some amount of traffic with user onput.");
		
		/*
		 * get user input and convert that to usable variables.
		 */
		while(true) 
		{
			try{
			System.out.println("Please Type time of traffic generating begin in 24 hours time period. ex: 8:00");
			SC = new Scanner(System.in);
			startTime = SC.next();
			
			System.out.println("Please Type time of traffic generating end in 24 hours time period. ex: 20:00");
			SC = new Scanner(System.in);
			endTime = SC.next();
			
			System.out.println("Please Type time duration of traffic generating in second. ex: 10 (It means generation car every 10 second.)");
			SC = new Scanner(System.in);
			duration = SC.next();

			startHour = Integer.valueOf(startTime.substring(0, startTime.indexOf(":")));
			startMin = Integer.valueOf(startTime.substring(startTime.indexOf(":")+1));
			
			endHour = Integer.valueOf(endTime.substring(0, endTime.indexOf(":")));
			endMin = Integer.valueOf(endTime.substring(endTime.indexOf(":")+1));
			
			durationInSec = Integer.valueOf(duration);
			
			} catch(StringIndexOutOfBoundsException SIOOBE) {
				System.err.println("Wrong user input made. Error 01, Please follow the format of example");
				continue;
			} catch(NumberFormatException NFE) {
				System.err.println("Wrong user input made. Error 02, Please enter Integer Number with the format.");
				continue;
			}
			
			if(startHour>=endHour){
				if(startHour==endHour&&startMin<endMin){
					break;
				}
				System.err.println("End Time should be smaller than Start Time.");
				continue;
			}
			break;
		}
		
		/*
		 * actual program begins.
		 */
		try{
			serverSocket = new ServerSocket(utils.ConnectionData.portForTG); //open up port number.
		} catch (IOException e) {
			System.out.println("socket open error for main port, please change port number.");
			System.exit(1);
		}
		try{
			timeKeeperSocket = new ServerSocket(utils.ConnectionData.portForTime); //open up port number for time sending.
		} catch (IOException e) {
			System.out.println("socket open error for timeKeeper port, please change port number.");
			System.exit(1);
		}
			
		gateAccepter accepter = new gateAccepter(); //Thread for get connection.
		accepterThread = new Thread(accepter);
		accepterThread.start();
		
		timeKeeperAccepter TKaccepter = new timeKeeperAccepter(); //Thread for time keeping connection accept.
		TKaccepterThread = new Thread(TKaccepter);
		TKaccepterThread.start();
		
		int durationPointer = ((((endHour*60)+endMin)-((startHour*60)+startMin))/durationInSec)*60;
		//System.out.println(startHour+":"+startMin+" "+endHour+":"+endMin+" "+duration+" "+durationPointer);
		trafficSender sender = new trafficSender(startHour, startMin, durationPointer, durationInSec); // Thread for sending car to gates.
		senderThread = new Thread(sender);
		senderThread.start();
		
	}
	
	/*
	 * Generating plate number which is identification of car.
	 */
	protected static String plateNumberGenerator(){
		String alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		String plate = "";
		Random random = new Random();
		while(true){
			for(int i=0; i<7; i++){
				if(i<3){
					plate+=alpha.charAt(random.nextInt(26));
				} else if (i==3) {
					plate+="-";
				} else {
					plate+=random.nextInt(10);
				}
			}
			if(plateDB.indexOf(plate)==-1){
				plateDB.add(plate);
				break;
			}
		}
		return plate;
	}
	
	/*
	 * notice the current time to every connections.
	 */
	protected static void timeSender(){
		
		for(int i=0; i<timeKeeperArray.size(); i++){
			timeKeeperArray.get(i).PW.println(timekeeper);
		}
	}
	
	/*
	 * notice time with start message and end message. such as start|HH:mm:ss
	 */
	protected static void timeSender(String note){
		for(int i=0; i<timeKeeperArray.size(); i++){
			timeKeeperArray.get(i).PW.println(note+"|"+timekeeper);
		}
	}
	
	protected static void socketCloser(){
		for(int i=0; i<timeKeeperArray.size(); i++){
			try {
				timeKeeperArray.get(i).socket.close();
				timeKeeperArray.get(i).OOS.flush();
				timeKeeperArray.get(i).OOS.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		for(int i=0; i<gateSocketArray.size(); i++){
			try {
				gateSocketArray.get(i).socket.close();
				gateSocketArray.get(i).OOS.flush();
				gateSocketArray.get(i).OOS.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

/*
 * sending traffic with random generation and send to gates.
 */
class trafficSender extends TrafficGenerator implements Runnable{
	int startHour, startMin, startSec = 0, duration, initialDuration;
	Random random = new Random();
	int defaultMaxCar = 3; // per second.
	final int defaultPeckTimeBegin = 10, defaultPeakTimeEnd = 14; // which is 10AM to 2PM.
	int defaultMaxCarInPeckTime = 2; //if the value is 2 that means it generate 2 times car then non-peak time.
	int defaultLotClosedTime = 22; // in Hour.
	final int percentageOfGeneratingCar = 30; //in percent.
	
	public trafficSender(int startHour, int startMin, int duration, int initalDuration) {
		this.startHour = startHour;
		this.startMin = startMin;
		this.duration = duration; // if 8:00 to 9:00 and send car every 10 seconds, the duration is 360.
		this.initialDuration = initalDuration;
		//defaultMaxCar *= initalDuration; //makes maximum defaultMaxCar cars per minute. 
		defaultMaxCarInPeckTime *= defaultMaxCar; // makes 2 times more traffic than non-peak time.
	}
	@Override
	public void run() {
		
		Scanner SC;
		Calendar calendar=Calendar.getInstance();
		SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss"); // time format as HH:mm:ss.
		SimpleDateFormat SDFofOut = new SimpleDateFormat("HH:mm:ss"); //time format as HH:mm.
		SimpleDateFormat SDFofHour = new SimpleDateFormat("HH"); // time format as HH.
		
		while(true){
			
			System.out.println("Start to send traffic? yes or no"); // get user input to sending all the traffic.
			SC = new Scanner(System.in);
			String userInput = SC.next();
			
			if(userInput.equalsIgnoreCase("yes") || userInput.equalsIgnoreCase("y")){
				break;
			}
		}
		
		calendar.set(0,0,0,startHour,startMin,startSec);
		
		timeSender(START);
		for(int i=0; i<duration+2; i++){
			
			int currentPercenetageOfGeneratingCar = random.nextInt(101);
			
			if(percentageOfGeneratingCar>=currentPercenetageOfGeneratingCar){
				
				Date date = calendar.getTime();
				String generatedTime = SDF.format(date);
				
				int currentHour = Integer.valueOf(SDFofHour.format(date));
				int ptr = 0;
				
				/*
				 * adjusting traffic stream about peck time.
				 */
				if(currentHour >= defaultPeckTimeBegin && currentHour <= defaultPeakTimeEnd){ 
					ptr = random.nextInt(defaultMaxCarInPeckTime+1);
				} else {
					ptr = random.nextInt(defaultMaxCar+1);
				}
				
				
				for(int j=0; j<ptr+1; j++){
					
					int outTimeInHour=0, outTimeInMin=0;
					
					while(true){ //Generate expecting out time.
						outTimeInHour = random.nextInt(defaultLotClosedTime+1);
						if(outTimeInHour<startHour){
							continue;
						} else {
							if(outTimeInHour != defaultLotClosedTime){
								outTimeInMin = random.nextInt(60);
							} else if(outTimeInHour == startHour) {
								while(true)
								{
									outTimeInMin = random.nextInt(60);
									if(outTimeInMin<=startMin){
										continue;
									} else {
										break;
									}
								}
							} else {
								outTimeInMin = 0;
							}
							break;
						}
					}
					
					//System.out.println(outTimeInMin);
					Calendar tempCal = Calendar.getInstance();
					tempCal.set(0,0,0,outTimeInHour,outTimeInMin,0); //set out time.
					date = tempCal.getTime();
					String expectingOutTime = SDFofOut.format(date);
					
					String plateNumber = plateNumberGenerator();
					Traffic taraffic = new Traffic(generatedTime, expectingOutTime, plateNumber);
					timekeeper = generatedTime;
					
					int splitter = 0;
					if(gateSocketArray.size() > 0){
						splitter = random.nextInt(gateSocketArray.size());
					}
					
					while(gateSocketArray.size() <= 0){ // if no connection made, i will keep wait until any gate connect.
						try {
							//System.out.println(gateSocketArray.size());
							splitter = random.nextInt(gateSocketArray.size());
						} catch (IllegalArgumentException IAE) {
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
							}
							continue;
						}
						
					}
					
					
					try {
						//System.out.println(Splitter);
						gateSocketArray.get(splitter).OOS.writeObject(taraffic); //sending out the object to gate.
						timeSender(); // send time to every one to need to know.				
					} catch (SocketException se) {
						try {
							gateSocketArray.get(splitter).socket.close(); // if one of gate disconnected, the socket will drop.
						} catch (IOException e) {
							e.printStackTrace();
						}
						gateSocketArray.remove(splitter); 
					} catch (IOException e) {
						e.printStackTrace();
					} catch (NullPointerException npe) {
						npe.printStackTrace();
				}
			}
				
		}
			int minutePointer = initialDuration*i;
			calendar.set(0,0,0,startHour,startMin,startSec+minutePointer);
		}
	
		timeSender(END); //sending the end time to send traffic.
		System.out.println("All the cars were sent.");
		//socketCloser(); // closing all the sockets.
	}
	
	
}

/*
 * Keep running and get connection and add it the array of gateSocketArray.
 */
class gateAccepter extends TrafficGenerator implements Runnable{

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
				System.out.println("Connection detected.");
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException NPE) {
				continue;
			}
		}
	}
	
}

/*
 * Keep running and get connection and add it the array of timeKeeperArray for sending time.
 */
class timeKeeperAccepter extends TrafficGenerator implements Runnable{

	Socket socket;
	
	@Override
	public void run() {
		
		boolean flg = true;
		while(flg)
		{
			try {
				socket = timeKeeperSocket.accept();
				ConnectorInfo GI = new ConnectorInfo(socket);
				timeKeeperArray.add(GI);
				System.out.println("time keeper Connection detected.");
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException NPE) {
				continue;
			}
		}
	}
	
}
