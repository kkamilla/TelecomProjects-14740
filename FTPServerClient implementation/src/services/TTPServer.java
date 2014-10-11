/*
 
 */
package services;


import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.LinkedList;

import datatypes.Datagram;

public class TTPServer {
	private DatagramService ds;
	private int windowsize;
	private int retranstime;
	//Hashmap of TTGoBackN type to store all the client open connections
	private HashMap<String, TTPGoBackN> openCon= new HashMap<String, TTPGoBackN>();
	//queue used as buffer to store data
	
	private LinkedList<byte[]> buffer = new LinkedList<byte[]>(); 

	public TTPServer(int N, int time) {
		super();
		windowsize = N;
		retranstime = time;
	}

	public void addData(byte[] data) {
		buffer.add(data);
	}

	

	
	public byte[] receive()  {
		Datagram request=null;
		try {
			request = ds.receiveDatagram();
		} catch (IOException e1) {
			
			e1.printStackTrace();
		} catch (ClassNotFoundException e1) {
			
			e1.printStackTrace();
		} 
		byte[] data = (byte[]) request.getData();
		
		String clientKey = request.getSrcaddr() + ":" + request.getSrcport();
		TTPGoBackN server_end = null;

		if (data[8] == (byte)1) {
			if(!openCon.containsKey(clientKey)) {
				server_end = new TTPGoBackN(windowsize, retranstime,ds);
				openCon.put(clientKey, server_end);
				Thread reqThread = new Thread(new TTPReqHandler(server_end,request, this));
				reqThread.start();
				System.out.println("Received SYN from:" + clientKey);
			}
			else {
				System.out.println("Duplicate SYN found.");
				Thread reqThread = new Thread(new TTPReqHandler(openCon.get(clientKey),request, this));
				reqThread.start();			
			}
		} 
		else if (data[8]== (byte)8) {
			if(openCon.containsKey(clientKey)) {
				Thread reqThread = new Thread(new TTPReqHandler(openCon.get(clientKey),request, this));
				reqThread.start();
				openCon.remove(clientKey);
				System.out.println("Connection at:" + clientKey + " closed.");
			}
		}
		else {
			if(openCon.containsKey(clientKey)) {
				System.out.println("Received request from an already present client.");
				Thread reqThread = new Thread(new TTPReqHandler(openCon.get(clientKey),request, this));
				reqThread.start();
			}
		}
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (!buffer.isEmpty()) {
			System.out.println("Request passed to FTP");
			return buffer.pop();
		} else
			return null;
	}	
	public void open(int srcPort, int verbose) throws SocketException {
		ds = new DatagramService(srcPort, verbose);
	}
	public void send(byte[] data) throws IOException {
		System.out.println("TTPServer received FTP 	data");
		ByteBuffer buf = ByteBuffer.allocate(2);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		buf.put(data[4]);
		buf.put(data[5]);
		
		short conport = buf.getShort(0);

		byte[] datapart = new byte[data.length - 6];
		System.arraycopy(data, 6, datapart, 0, data.length - 6);
		//short[] shorts = new short[data.length/2];
		// to turn bytes to shorts as little endian. 
		//ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
		String addresskey=(data[0]&0xff) + ":"+(data[1]&0xff) + ':'+(data[2]&0xff) + ':'+(data[3]&0xff)+":"+conport ;


		Thread reqThread = new Thread(new TTPReqHandler(openCon.get(addresskey.toString()),datapart));
		
		reqThread.start();
	
	
	}	
	
}



class TTPReqHandler implements Runnable {
	private TTPGoBackN goBack;
	private Datagram datagram;
	private TTPServer serv;
	private byte[] data;

	public TTPReqHandler(TTPGoBackN gb, Datagram dg, TTPServer sv) {
		super();
		goBack = gb;
		datagram = dg;
		serv = sv;
	}
	public TTPReqHandler(TTPGoBackN gb, byte[] d) {
		super();
		goBack = gb;
		data = d;
	}

	@Override
	public void run() {
		if (datagram != null) {
			goBack.respond(datagram,serv);
		} 
		else if (data != null) {
			goBack.sendD(data);
		}
		
	}	
	
}





