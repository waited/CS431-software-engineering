package Manager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import utils.ConnectionData;
import utils.ConnectorInfo;

public class Manager {

	private static 	int 	initTotalTokens;
	private static 	double 	initTotalBudget;
	
	protected 	static 	ServerSocket serverSocket;
	protected 	static 	ArrayList<ConnectorInfo> gateSocketArray;
	protected	static	ArrayList<GateReceiver> gateReceiverArray;
	
	final public static int NUM_OF_GATES = 6;
	final public static int INIT_NUM_OF_TOKENS_PER_GATE = 30;
	final public static double INIT_BUDGET_PER_GATE = 100.00;
	
	/**
	 * Distribute initTotalToken and initTotalBudget amongst all the gates 
	 * @param gates
	 */
	private static void initDistribution(ArrayList<ConnectorInfo> gateSenders, ArrayList<GateReceiver> gateReceivers) {

		GateReceiver currGateReceiver;
		ConnectorInfo currGateSender;
		
		if(gateSenders.size() != gateReceivers.size()) {
			System.err.println("MANAGER: WARNING: Number of gate receivers doesn't equal the number of gate senders");
			return;
		}
		
		initTotalBudget = NUM_OF_GATES * INIT_BUDGET_PER_GATE;
		initTotalTokens = NUM_OF_GATES * INIT_NUM_OF_TOKENS_PER_GATE;
		
		for(int i = 0; i < NUM_OF_GATES; i++) {
			currGateReceiver = gateReceivers.get(i);
			currGateReceiver.init_numTokens = INIT_NUM_OF_TOKENS_PER_GATE;
			currGateReceiver.init_budget = INIT_BUDGET_PER_GATE;
			
			currGateSender = gateSenders.get(i);
			currGateSender.PW.print('b'); //next output is a double representing the budget;
			currGateSender.PW.print(INIT_BUDGET_PER_GATE);
			currGateSender.PW.print('t'); //next output is a int representing the number of tokens;
			currGateSender.PW.print(INIT_NUM_OF_TOKENS_PER_GATE);
			for(int j = 0; j < INIT_NUM_OF_TOKENS_PER_GATE; j++) {
				//TODO Create a token and send it
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Socket socket;
		int conn_count = 0;
		
		gateSocketArray = new ArrayList<ConnectorInfo>();
		gateReceiverArray = new ArrayList<GateReceiver>();
		
		try {
			serverSocket = new ServerSocket(ConnectionData.portForManager);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println("MANAGER: Socket failed to initialize.");
		}
		
		while(conn_count <= NUM_OF_GATES){
			try {
				socket = serverSocket.accept();
				ConnectorInfo gi = new ConnectorInfo(socket);
				GateReceiver g_receive = new GateReceiver(socket, conn_count);
				gateSocketArray.add(gi);
				gateReceiverArray.add(g_receive);
				conn_count++;
				System.out.println("MANAGER: Connection detected!");
			} catch (IOException e) {
				System.err.println("MANAGER: Failed making connection.");
			}
		}
		
		initDistribution(gateSocketArray, gateReceiverArray);
		
		for(int i = 0; i < gateReceiverArray.size(); i++) {
			gateReceiverArray.get(i).run();
		}
		
		
	}

}

/**
 * Object for manager to handle gate info
 * @author petejodo
 *
 */
class GateReceiver extends Manager implements Runnable {
	
	protected boolean RUNNING = true;
	
	Socket socket;
	int gateNumber;
	ObjectInputStream ois;
	
	private char command;
	private double message;
	
	protected int init_numTokens;
	protected double init_budget;
	
	private int curr_numTokens;
	private double curr_budget;
	
	public GateReceiver(Socket socket, int gateNumber) {
		this.socket = socket;
		this.gateNumber = gateNumber;
		init_numTokens = 0;
		init_budget = 0;
		
		try {
			ois = new ObjectInputStream(socket.getInputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.println("MANAGER: Failed to set up input stream for Gate #" + this.gateNumber);
		}
		
		while(RUNNING) {
			try {
				command = ois.readChar();
			} catch (IOException e) {
				System.err.println("MANAGER [Gate " + this.gateNumber + "]: Now availabe stream yet. Waiting half a sec before rechecking");
				
			}
		}
	}
	
	@Override
	public void run() {
		
	}
}