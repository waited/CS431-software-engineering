package utils;

import java.io.*;
import java.net.*;
import java.util.*;


public class TGtestTempGate {

	/**
	 * @param Templary testing gate for getting traffic from traffic generator. This program will be replace with Actual gate program.
	 */
	
	static Scanner sc;
	static Socket socket, socketForTime;
	
	public static void main(String[] args) throws EOFException {
		
		ArrayList<Traffic> cars = new ArrayList<Traffic>();
		ObjectInputStream OIS = null;
		
		System.out.print("login?");
		sc = new Scanner(System.in);
		String input = sc.nextLine();
		if(input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes"))
		{
				
			try {
				socket = new Socket(ConnectionData.ipTG, ConnectionData.portForTG);
				socketForTime = new Socket(ConnectionData.ipTG, ConnectionData.portForTime);
				System.out.println("Connected");
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				OIS = new ObjectInputStream(socket.getInputStream()); // open up the input stream.
				
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NullPointerException npe) {
				npe.printStackTrace();
			}
			
			timeReader TR = new timeReader();
			Thread thread = new Thread(TR);
			thread.start();
			
			while(true)
			{
				try {
					Traffic car = (Traffic) OIS.readObject(); // get the traffic from the T.G.
					System.out.println("Generated Time: "+car.getGeneratedTime()+", Expecting Out Time: "+car.getExpectingOutTime()+", Plate Number: "+car.getPlateNumber());
					cars.add(car);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
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
					System.out.println(timekeeping);
				} else {
					String head = timekeeping.substring(0, timekeeping.indexOf("|"));
					timekeeping = timekeeping.substring(timekeeping.indexOf("|")+1);
					System.out.println("Header: "+head+", "+timekeeping);
					
				}
			}
			} catch (IOException e) {
				e.printStackTrace();
			}
				
	}
	
}
