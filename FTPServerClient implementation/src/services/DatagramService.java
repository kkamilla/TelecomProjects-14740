/*
 *  A Stub that provides datagram send and receive functionality
 *  
 *  Feel free to modify this file to simulate network errors such as packet
 *  drops, duplication, corruption etc. But for grading purposes we will
 *  replace this file with out own version. So DO NOT make any changes to the
 *  function prototypes
 */
package services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Random;

import datatypes.Datagram;

public class DatagramService {

	private int count;
	
	private int port;
	private int verbose;
	private DatagramSocket socket;	

	public DatagramService(int port, int verbose) throws SocketException {
		super();
		this.port = port;
		this.verbose = verbose;
		
		socket = new DatagramSocket(port);
	}

	public void sendDatagram(Datagram datagram) throws IOException {

		ByteArrayOutputStream bStream = new ByteArrayOutputStream(1500);
		ObjectOutputStream oStream = new ObjectOutputStream(bStream);
		oStream.writeObject(datagram);
		oStream.flush();

		// Create Datagram Packet
		byte[] data = bStream.toByteArray();
		InetAddress IPAddress = InetAddress.getByName(datagram.getDstaddr());
		DatagramPacket packet = new DatagramPacket(data, data.length,
				IPAddress, datagram.getDstport());

		count ++;	

		Random random1 = new Random();
		int num = random1.nextInt(5) + 1;
		
		if(count%num==3) {
			System.out.println("Test case for duplicate packets");
			sendDuplicates(num,packet);
			System.out.println(num + " packets duplicated");
		}  
		else if(count%num==4) {
			System.out.println("Test case for delayed packets");
			int delay = num*1000+6000; 			//Base = 7000ms, with random numbers chosen to a maximum of 120000 
			sendWithDelay(packet,delay); 
			System.out.println("Packet sent with " + delay + " milliseconds delay");			
		}
		else if(count%num==5) {
			//drop packet
			System.out.println("Test case: Packet dropped");
		}
		else if(count%num==6) {
			//Introduce checksum error
			System.out.println("Test case for checksum error");
			datagram.setChecksum((short) 2);
		}		
		
		else {		
			socket.send(packet);
		}
	}

	public Datagram receiveDatagram() throws IOException,
			ClassNotFoundException {

		byte[] buf = new byte[1500];
		DatagramPacket packet = new DatagramPacket(buf, buf.length);

		socket.receive(packet);

		ByteArrayInputStream bStream = new ByteArrayInputStream(
				packet.getData());
		ObjectInputStream oStream = new ObjectInputStream(bStream);
		Datagram datagram = (Datagram) oStream.readObject();

		return datagram;
	}

	//Send duplicate datagrams - input parameter n is used to specify number of duplicates	
	private void sendDuplicates(int n, DatagramPacket datagram) throws IOException {
		for(int i = 0; i<n; i++)
			socket.send(datagram);
	}
	
	//Delay a datagram
	private void sendWithDelay(DatagramPacket datagram,int delay) throws IOException {
		
		try {
				Thread.sleep(delay);			//introduce delay before sending datagram
			
			} catch (InterruptedException e) {
				
				e.printStackTrace();
			
			}
		//Send datagram
		socket.send(datagram);
	}

}
