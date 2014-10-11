/*
 
 */
package services;


import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Timer;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import datatypes.Datagram;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class TTPGoBackN {
	static int ACKflag=0;
	static int FINCLOSEflag=2;
	 static int SYNflag=1;
	 static int FINflag=3;
	 static int SYNACKflag=6;
	 static int FINACKflag=7;
	 static int EODATAflag=4;
	 static int DATAflag=5;
	
	
	private DatagramService ds;
	private Datagram datagram;
	private Datagram recDatagram;
	private int base;
	private int nextSeqNum;
	private int windowSize;
	
	private int ackNum;
	private int expectedSeqNum;
	private int nextFragment;
	private int retransmissiontime;
	private Timer clock;
	private ConcurrentHashMap<Integer,Datagram> unackPackets;
	private LinkedList<Datagram> sendBuffer;
	
	public TTPGoBackN(int windowsz, int rettime) {
		datagram = new Datagram();
		sendBuffer = new LinkedList<Datagram>();
		recDatagram = new Datagram();
		unackPackets = new ConcurrentHashMap<Integer,Datagram>();
		
		retransmissiontime = rettime;
		clock = new Timer(retransmissiontime,listener);
		clock.setInitialDelay(retransmissiontime);

		Random rand = new Random();
		nextSeqNum = rand.nextInt(135536);
		windowSize = windowsz;

	}

	public TTPGoBackN(int window, int retranstime, DatagramService ds) {
		this(window, retranstime);
		this.ds = ds;
	}

	private short calChecksum(byte[] payld) throws IOException {
		int length = payld.length;
		int i = 0;
		int word16;

		int sum = 0;
        //if the payload has more than one byte then merge every two bytes to 16 bits word to sum them  
	
		if (length>1) {	
			for (i=0;i<length;i=i+2){
				word16 =((payld[i]<<8)&0xFF00)|(payld[i+1]&0xFF);
				sum = sum + word16;
			}

            //if there is a carry
			while ((sum>>16)==1)
				sum = (sum & 0xFFFF)+(sum >> 16);
				

		}
        //if there is only one byte in the payload
		if (length== 1) {
			sum =sum+ (payld[i] << 8 & 0xFF00);
			//if there is a carry then add the carry
			while ((sum >>16)==1)
				sum = (sum & 0xFFFF)+(sum >> 16);
				
		}
		sum = ~sum;
		sum = sum & 0xFFFF;
		return (short) sum;
	}
	
	public void open(String src, String dest, short srcPort, short destPort, int verbose) {

		try {
			this.ds = new DatagramService(srcPort, verbose);
		} catch (SocketException e) {
			
			e.printStackTrace();
		}

		datagram.setSrcaddr(src);
		datagram.setDstaddr(dest);
		datagram.setSrcport((short) srcPort);
		datagram.setDstport((short) destPort);
		datagram.setSize((short) 9);
		datagram.setData(createHeader(TTPGoBackN.SYNflag));
		datagram.setChecksum((short) -1);
		try {
			this.ds.sendDatagram(datagram);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		System.out.println("SYN sent to IP:" + datagram.getDstaddr() + "with port no:" + datagram.getDstport() + " and ISN:" + nextSeqNum);

		base = nextSeqNum;
		clock.start();

		unackPackets.put(nextSeqNum, new Datagram(datagram.getSrcaddr(), datagram.getDstaddr(), datagram.getSrcport(), datagram.getDstport(), datagram.getSize(), datagram.getChecksum(), datagram.getData()));
		nextSeqNum++;

		try {
			receiveD();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
	
	private byte[] createHeader(int flag) {
		byte[] header = new byte[9];
		byte[] ackByteArr = ByteBuffer.allocate(4).putInt(ackNum).array();
		byte[] seqByteArr = ByteBuffer.allocate(4).putInt(nextSeqNum).array();
		

		switch (flag) {

		case 0:
			for (int i = 0; i < 4; i++) {
				header[i] = seqByteArr[i];
			}
			for (int i = 4; i < 8; i++) {
				header[i] = ackByteArr[i - 4];
			}
			header[8] = (byte) 2;
			break;
			

		case 5:
			for (int i = 0; i < 4; i++) {
				header[i] = seqByteArr[i];
			}
			for (int i = 4; i < 8; i++) {
				header[i] = (byte) 0;
			}
			header[8] = (byte) 0;
			
			break;
		case 4:
			for (int i = 0; i < 4; i++) {
				header[i] = seqByteArr[i];
			}
			for (int i = 4; i < 8; i++) {
				header[i] = (byte) 0;
			}
			header[8] = (byte) 16;
			break;
		case 1:
			for (int i = 0; i < 4; i++) {
				header[i] = seqByteArr[i];
			}
			for (int i = 4; i < 8; i++) {
				header[i] = (byte) 0;
			}
			header[8] = (byte) 1;
			break;
		
		case 6:
			for (int i = 0; i < 4; i++) {
				header[i] = seqByteArr[i];
			}
			for (int i = 4; i < 8; i++) {
				header[i] = ackByteArr[i - 4];
			}
			header[8] = (byte) 3;
			break;

		case 3:
			for (int i = 0; i < 4; i++) {
				header[i] = seqByteArr[i];
			}
			for (int i = 4; i < 8; i++) {
				header[i] = (byte) 0;
			}
			header[8] = (byte) 4;
			break;
		
		case 7:
			for (int i = 0; i < 4; i++) {
				header[i] = seqByteArr[i];
			}
			
			for (int i = 4; i < 8; i++) {
				header[i] = ackByteArr[i - 4];
			}
			header[8] = (byte) 6;
			break;

		case 2:
			for (int i = 0; i < 4; i++) {
				header[i] = seqByteArr[i];
			}
			for (int i = 4; i < 8; i++) {
				header[i] = ackByteArr[i - 4];
			}
			header[8] = (byte) 8;
			break;
		}
		return header;
	}
	
	public void close()  {
		datagram.setData(createHeader(TTPGoBackN.FINflag));
		datagram.setChecksum((short)-1);
		datagram.setSize((short)9);

		try {
			ds.sendDatagram(datagram);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		System.out.println("FIN sent with seq no:" + nextSeqNum);

		unackPackets.put(nextSeqNum, new Datagram(datagram.getSrcaddr(), datagram.getDstaddr(), datagram.getSrcport(), datagram.getDstport(), datagram.getSize(), datagram.getChecksum(), datagram.getData()));
		nextSeqNum++;

		if(base == nextSeqNum)
			clock.restart();

	}

	
	 private void sendAck()  {
		
		datagram.setChecksum((short)-1);
		datagram.setData(createHeader(TTPGoBackN.ACKflag));
		datagram.setSize((short)9);
		try {
			ds.sendDatagram(datagram);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		System.out.println("ACK sent with no:" + ackNum);
	}

	 
	
	 private void donotsend_data(byte[] b) {
		System.out.println("Window full,try again later.");
	}

	 private void sendFrag(Datagram datagram)  {
		try {
			ds.sendDatagram(datagram);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		System.out.println("Data sent to " + datagram.getDstaddr() + ":" + datagram.getDstport() + " with seq no " + nextFragment);

		if (base == nextSeqNum) 
		{
			clock.restart();
		}

		unackPackets.put(nextFragment, new Datagram(datagram.getSrcaddr(), datagram.getDstaddr(), datagram.getSrcport(), datagram.getDstport(), datagram.getSize(), datagram.getChecksum(), datagram.getData()));
	}
	
	 private void sendAckFinClose()  {
		 datagram.setChecksum((short)-1);
		 datagram.setSize((short)9);
		datagram.setData(createHeader(TTPGoBackN.FINCLOSEflag));
		
		try {
			ds.sendDatagram(datagram);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		System.out.println("ACK sent for final connection closing with No:" + ackNum);

		clock.removeActionListener(listener);
		clock.addActionListener(delClient);
		clock.restart();
		nextSeqNum++;
	}
	
	 public void sendD(byte[] b) {

			if (nextSeqNum < base + windowSize) {

				int length = b.length;
				byte[] fragment = {};
				int y = 0;
				int x;
				int sizeCont = 0;
                
				//total size of the payload is taken as 1300 bytes = 1291 bytes(actual data)+9 bytes(flags+acknum+seqnum)
				if (length > 1291) 
				{

					do {
						
						sizeCont = Math.min(1291,length);
						fragment = new byte[sizeCont];
						x = y;
						for (int i = x; i < x + sizeCont; y++, i++) {
							int index=i % 1291;
							fragment[index] = b[i];
						}

						if (length > 1291)
							try {
								encapsulate(fragment, false);
							} catch (IOException e) {
								
								e.printStackTrace();
							}
						else
							try {
								encapsulate(fragment, true);
							} catch (IOException e) {
								
								e.printStackTrace();
							}

						length =length- 1291;

					} while (length > 0);
				} 
				
				else {
					
					try {
						encapsulate(b, true);
					} catch (IOException e) {
						
						e.printStackTrace();
					}
				}
			} else {
				donotsend_data(b);
			}
		}
	 
	 private void encapsulate (byte[] fragment, boolean lFragment) throws IOException  {

			byte[] header = new byte[9];
			if (lFragment) {
				header = createHeader(TTPGoBackN.EODATAflag);
			} 
			
			else {
				header = createHeader(TTPGoBackN.DATAflag);
			}

			byte[] headerAndData = new byte[fragment.length + header.length];
			System.arraycopy(header, 0, headerAndData, 0, header.length);
			System.arraycopy(fragment, 0, headerAndData, header.length, fragment.length);

			datagram.setSize((short)headerAndData.length);
			try {
				datagram.setChecksum(calChecksum(headerAndData));
			} catch (IOException e) {
				
				e.printStackTrace();
			}
			datagram.setData(headerAndData);
			

			if (nextSeqNum < base + windowSize) {
				nextFragment = nextSeqNum;
				sendFrag(datagram);
			} 
			
			else {
				sendBuffer.add(new Datagram(datagram.getSrcaddr(), datagram.getDstaddr(), datagram.getSrcport(), datagram.getDstport(), datagram.getSize(), datagram.getChecksum(), datagram.getData()));
				System.out.println("Window full,packet no:" + nextSeqNum + " queued");
			}
			nextSeqNum++;
		} 
	
	 public byte[] receiveD()  throws IOException {
			if (ds != null) {
				try {
					recDatagram = ds.receiveDatagram();
				} catch (ClassNotFoundException e) {
					
					e.printStackTrace();
				} catch (IOException e) {
					
					e.printStackTrace();
				} 
			}

			byte[] d = (byte[]) recDatagram.getData();
			byte[] ftpdata = null;

			if (recDatagram.getSize() > 9) {
				int value = 0;
				value = java.nio.ByteBuffer.wrap(new byte[] {d[0], d[1], d[2], d[3]}).getInt();
				if((value) == expectedSeqNum) 
				{
					try {
						if (calChecksum(d) != recDatagram.getChecksum()) {
							System.out.println("checksum error found.");
							sendAck();
						} else {
							System.out.println("checksum correct, verified.");
							 value = 0;
							value = java.nio.ByteBuffer.wrap(new byte[] {d[0], d[1], d[2], d[3]}).getInt();
							
							ackNum = value;
							System.out.println("Received data with Seq no " + ackNum);

							if(d[8]==16) {
								ftpdata = new byte[d.length - 9];
								for (int i=0; i < ftpdata.length; i++) {
									ftpdata[i] = d[i+9];
								}
								sendAck();
								expectedSeqNum++;
							}
							else if(d[8]== 0) {
								sendAck();
								expectedSeqNum++;
								ArrayList<Byte> dataList = reassemble(d);
								ftpdata = new byte[dataList.size()];
								for (int i=0;i<dataList.size();i++) {
									ftpdata[i] = (byte)dataList.get(i);
								}
							}
						}
					} catch (IOException e) {
						
						e.printStackTrace();
					}
				}
				else 
				{
					sendAck();
				}
			} 
			else 
			{
				
				if(d[8]== (byte)2) {
					int value = 0;
					value = java.nio.ByteBuffer.wrap(new byte[]{ d[4], d[5], d[6], d[7]}).getInt();
					
					base = value + 1;
					System.out.println("Received ACK for packet no:" + value);
					//@SuppressWarnings("rawtypes")
					//Iterator keyset = unackPackets.keySet().iterator();
					
					//while (keyset.hasNext()) {
						
						//@SuppressWarnings("rawtypes")
						//Map.Entry entry = (Map.Entry) keyset.next();
					    //Integer key = (Integer)entry.getKey();
					
						//if (key< base) {
							//unackPackets.remove(key);
						//}
					Set<Integer> keys = unackPackets.keySet();
					for (Integer i: keys) {
						if (i< base) {
							unackPackets.remove(i);
						}
					}
					}	
					
				}
				if(d[8]== (byte)6) {
					int value = 0;
					value = java.nio.ByteBuffer.wrap(new byte[]{ d[4], d[5], d[6], d[7]}).getInt();
					
					
					base = value + 1;
					 value = 0;
					value = java.nio.ByteBuffer.wrap(new byte[]{ d[0], d[1], d[2], d[3]}).getInt();
					
					ackNum = value;
					expectedSeqNum =  ackNum + 1;
			

					if (ds!=null) {
						sendAckFinClose();
					}
				}
				
				
				if (d[8] == (byte)3) {
					int value = 0;
					value = java.nio.ByteBuffer.wrap(new byte[]{ d[0], d[1], d[2], d[3]}).getInt();
					
					ackNum = value;
					expectedSeqNum =  ackNum + 1;
					 value = 0;
					value = java.nio.ByteBuffer.wrap(new byte[]{ d[4], d[5], d[6], d[7]}).getInt();
					
					base = value + 1;
					clock.stop();
					System.out.println("Received SYNACK with seq no:" + ackNum + " and Acknowledgement No " + (base-1));
					sendAck();
				}
				if(base == nextSeqNum) {
					clock.stop();
				} else {
					clock.restart();
				}
			
			return ftpdata;
}
	
	 private ArrayList<Byte> reassemble(byte[] d) throws IOException{
			ArrayList<Byte> reasData = new ArrayList<Byte>();

			for(int i=9;i < d.length;i++) {
				reasData.add(d[i]);
			}

			while(true) {
				try {
					recDatagram = ds.receiveDatagram();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					
					e.printStackTrace();
				} 
				byte[] datapayload = (byte[]) recDatagram.getData();
                
                int value = 0;
				value = java.nio.ByteBuffer.wrap(new byte[] {datapayload[0], datapayload[1], datapayload[2], datapayload[3]}).getInt();
				
                if(value == expectedSeqNum) {
					

					for(int i=9;i < datapayload.length;i++) {
						reasData.add(datapayload[i]);
					}
					 value = 0;
					value = java.nio.ByteBuffer.wrap(new byte[] {datapayload[0], datapayload[1], datapayload[2], datapayload[3]}).getInt();
					
					ackNum = value;
					sendAck();
					
					expectedSeqNum++;
					nextSeqNum++;

					
					 if(datapayload[8]==16)
					{
						break;
					}
					if(datapayload[8]==0) 
					{
						continue;
					}
				}
				else 
				{
					sendAck();
				}
			}
			return reasData;
		}
	
	 
	 
	 public void respond(Datagram request, TTPServer ttp) {

			byte[] datapayload = (byte[]) request.getData();
			byte[] ftpdata = null;
			byte[] clientAdd = new byte[6];

			if (request.getSize() > 9) {
				int value = 0;
				value = java.nio.ByteBuffer.wrap(new byte[] {datapayload[0], datapayload[1], datapayload[2], datapayload[3]}).getInt();
				
				if(value == expectedSeqNum) {
					try {
						if (calChecksum(datapayload) != request.getChecksum()) {
							System.out.println("checksum error.");
							sendAck();
						} else {
							System.out.println("checksum verified.");
							int val = 0;
							val = java.nio.ByteBuffer.wrap(new byte[] {datapayload[0], datapayload[1], datapayload[2], datapayload[3]}).getInt();
							
							ackNum = val;

							String[] temp = datagram.getDstaddr().split("\\.");				
							for (int i=0;i<4;i++) {
								clientAdd[i] = (byte) (Integer.parseInt(temp[i]));
							}
							clientAdd[4] = (byte)(datagram.getDstport() & 0xFF);
							clientAdd[5] = (byte) ((datagram.getDstport() >> 8) & 0xFF);
								
							if(datapayload[8]==16) {
								System.out.println("Received request from " + datagram.getDstaddr() + ":" + datagram.getDstport());
								ftpdata = new byte[datapayload.length - 9];
								for (int i=0; i < ftpdata.length; i++) {
									ftpdata[i] = datapayload[i+9];
								}
								sendAck();
								expectedSeqNum++;
							}
							else if(datapayload[8]== 0) {
								ArrayList<Byte> dataList = reassemble(datapayload);
								ftpdata = new byte[dataList.size()];
								System.arraycopy(dataList, 0, ftpdata, 0, ftpdata.length);
							}
						}
					}  catch (IOException e) {
					
						e.printStackTrace();
					}
				}
				else {
					sendAck();
				}
			} else {
				if (datapayload[8]==(byte)1) {
					int value = 0;
					value = java.nio.ByteBuffer.wrap(new byte[] {datapayload[0], datapayload[1], datapayload[2], datapayload[3]}).getInt();
					
					ackNum = value;

					datagram.setSrcaddr(request.getDstaddr());
					datagram.setSrcport((short) request.getDstport());
					datagram.setDstaddr(request.getSrcaddr());
					
					datagram.setDstport((short) request.getSrcport());
					datagram.setChecksum((short)-1);
					datagram.setSize((short) 9);
					datagram.setData(createHeader(TTPGoBackN.SYNACKflag));
					
					try {
						this.ds.sendDatagram(datagram);
					} catch (IOException e) {
						
						e.printStackTrace();
					}
					System.out.println("SYNACK sent to " + datagram.getDstaddr() + ":" + datagram.getDstport() + " with Seq no " + nextSeqNum);
					value = 0;
					value = java.nio.ByteBuffer.wrap(new byte[] {datapayload[0], datapayload[1], datapayload[2], datapayload[3]}).getInt();
					
					expectedSeqNum = value + 1;

					base = nextSeqNum;
					clock.start();

					unackPackets.put(nextSeqNum, new Datagram(datagram.getSrcaddr(), datagram.getDstaddr(), datagram.getSrcport(), datagram.getDstport(), datagram.getSize(), datagram.getChecksum(), datagram.getData()));
					nextSeqNum++;

				}
				if (datapayload[8] == (byte)3) {
					int value = 0;
					value = java.nio.ByteBuffer.wrap(new byte[] {datapayload[0], datapayload[1], datapayload[2], datapayload[3]}).getInt();
					
					ackNum = value;
					expectedSeqNum =  ackNum + 1;
					int val = 0;
					System.out.println("printing");
					val = java.nio.ByteBuffer.wrap(new byte[] {datapayload[4], datapayload[5], datapayload[6], datapayload[7]}).getInt();
					System.out.println(val);
					base = val + 1;
					clock.stop();
					System.out.println("Received SYNACK with seq no:" + ackNum);
					sendAck();
				}
				if(datapayload[8]== (byte)2) {
					int value = 0;
					value = java.nio.ByteBuffer.wrap(new byte[] {datapayload[4], datapayload[5], datapayload[6], datapayload[7]}).getInt();
					
					base = value + 1;
					System.out.println("Received ACK for packet no:" + value);
					Set<Integer> keys = unackPackets.keySet();
					for (Integer i: keys) {
						if (i< base) {
							unackPackets.remove(i);
						}
					}
					//@SuppressWarnings("rawtypes")
					//Iterator keyset = unackPackets.keySet().iterator();
					
				//	while (keyset.hasNext()) {
						
					//	@SuppressWarnings("rawtypes")
						//Map.Entry entry = (Map.Entry) keyset.next();
					   // Integer key = (Integer)entry.getKey();
					
						//if (key< base) {
							//unackPackets.remove(key);
						//}
					}				

					for (int i = ((base+windowSize)-nextFragment);i>0;i--) {
						try {
							Thread.sleep(1500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if (!sendBuffer.isEmpty()) {
							nextFragment++;
							System.out.println("Queued packet going to be sent");
							
							sendFrag(sendBuffer.pop());					
						}
					}				
				}
				if(datapayload[8] == (byte)4){
					unackPackets.clear();
					int value = 0;
					value = java.nio.ByteBuffer.wrap(new byte[] {datapayload[0], datapayload[1], datapayload[2], datapayload[3]}).getInt();
					
					ackNum = value;
					expectedSeqNum =  ackNum + 1;
					datagram.setSize((short) 9);
					datagram.setData(createHeader(TTPGoBackN.FINACKflag));
					datagram.setChecksum((short)-1);
					try {
						ds.sendDatagram(datagram);
					} catch (IOException e) {
						
						e.printStackTrace();
					}

					System.out.println("FINACK sent to " + datagram.getDstaddr() + ":" + datagram.getDstport());

					clock.restart();
					unackPackets.put(nextSeqNum, new Datagram(datagram.getSrcaddr(), datagram.getDstaddr(), datagram.getSrcport(), datagram.getDstport(), datagram.getSize(), datagram.getChecksum(), datagram.getData()));
					nextSeqNum++;
				}
				if(datapayload[8] == (byte)8) {
					int value = 0;
					value = java.nio.ByteBuffer.wrap(new byte[] {datapayload[4], datapayload[5], datapayload[6], datapayload[7]}).getInt();
					
					base = value + 1;
					System.out.println("Received ACK for packet no:" + value);
				}
				if(base == nextSeqNum) {
					clock.stop();
				} 
				else {
					clock.restart();
				}
			
			if (ftpdata != null) {
				byte[] totalData = new byte[clientAdd.length + ftpdata.length];
				
				System.arraycopy(clientAdd, 0, totalData, 0, clientAdd.length);
				System.arraycopy(ftpdata, 0, totalData, clientAdd.length, ftpdata.length);
				ttp.addData(totalData);
				System.out.println("Data written to TTP buffer");
			}
		}

	
	 ActionListener listener = new ActionListener(){
			public void actionPerformed(ActionEvent event){
				System.out.println("Timeout for Packet " + base);
				
				clock.restart();
			}
		};
	 
		ActionListener delClient = new ActionListener(){
			public void actionPerformed(ActionEvent event){
				ds = null;
				clock.stop();
				System.out.println("TTP client connection closed");
			}
		}; 
	 
	 
}


