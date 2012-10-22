package utils;
/*
 * This Program is just for test case which is just receiving time only. it will be using same method as in manger or parking lot.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class TimeReciver {

	protected static Socket socketForTime;
	protected static BufferedReader in = null;
	public static void main(String[] args){
		try {
			socketForTime = new Socket(ConnectionData.ipTG, ConnectionData.portForTime);
			in = new BufferedReader(new InputStreamReader(socketForTime.getInputStream()));
			while(true){
				String timekeeping = in.readLine();
				if(timekeeping.indexOf(":")>=0){
					if(timekeeping.indexOf("|")<0){
						System.out.println(timekeeping);
					} else {
						String head = timekeeping.substring(0, timekeeping.indexOf("|"));
						timekeeping = timekeeping.substring(timekeeping.indexOf("|")+1);
						System.out.println("Header: "+head+", "+timekeeping);
						
					}
				
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}

			
