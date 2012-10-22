package utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class ConnectorInfo {
	
	/**
	 * @param Hold gate information and open up socket and streams.
	 */
	
	public Socket socket;
	public PrintWriter PW;
	public ObjectOutputStream OOS;
	public ObjectInputStream OIS;
	
	public ConnectorInfo(Socket socket){
		this.socket = socket;
		try {
			//DIS = new DataInputStream(socket.getInputStream());
			PW = new PrintWriter(socket.getOutputStream(), true);
			OOS = new ObjectOutputStream(socket.getOutputStream());
			OIS = new ObjectInputStream(socket.getInputStream());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException npe) {
			npe.printStackTrace();
		}
		
	}

}
