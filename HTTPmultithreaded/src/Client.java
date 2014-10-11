/**
 * @file: Client.java
 * 
 * @author: Chinmay Kamat <chinmaykamat@cmu.edu>
 * 
 * @date: Feb 15, 2013 1:14:09 AM EST
 * 
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
	private static int bufferSize = 8192;
	public static void main(String[] args) {
		Socket sock;
		int port = 8080;
		InetAddress addr = null;
		BufferedReader inStream = null;
		DataOutputStream outStream = null;
		String buffer = null;
	    /* Parse parameter and do args checking */
		if (args.length < 2) {
			System.err.println("Usage: java Client <server_ip> <server_port>");
			System.exit(1);
		}

		try {
			port = Integer.parseInt(args[1]);
		} catch (Exception e) {
			System.err.println("Usage: java Client <server_ip> <server_port>");
			System.exit(1);
		}

		if (port > 65535 || port < 1024) {
			System.err.println("Port number must be in between 1024 and 65535");
			System.exit(1);
		}

		try {
			/* Get the server adder in InetAddr format */
			addr = InetAddress.getByName(args[0]);
		} catch (UnknownHostException e) {
			System.err.println("Invalid address provided for server");
			System.exit(1);
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			try {
				/* Read data from the user */
				buffer = br.readLine();
				/*
				 * connect() to the server at addr:port. The server needs to be
				 * listen() in order for this to succeed. This call initiates
				 * the SYN-SYN/ACK-ACK handshake
				 */
				sock = new Socket(addr, port);
			} catch (IOException e) {
				System.err.println("Unable to reach server");
				continue;
			}


	    try {
	        
	        
	        System.out.println("Enter the extention of receiving file:");
	        String fileExt = br.readLine();
	        // TODO code application logic here
	        Socket socket = new Socket(addr,Integer.parseInt(args[1]));
	        BufferedInputStream bis = new BufferedInputStream
	                (socket.getInputStream());

	        BufferedOutputStream bout = new BufferedOutputStream
	                (socket.getOutputStream());
	        System.out.println("Enter the request:");
	        String message = br.readLine();// GET /index.html HTTP/1.0

	        System.out.println("Header read");
	        if(message!=null){
	            bout.write(message.getBytes());
	        }
	        FileOutputStream fout = new FileOutputStream("out"+fileExt);
	        String s1 = "\r\n\r\n";
	        bout.write(s1.getBytes());
	        bout.flush();
	        System.out.println("Header sent");

	        byte[] res = new byte[bufferSize];
	        int got;
	        while((got = bis.read(res))!=-1){
	            fout.write(res,0,got);
	        }
	        fout.close();
	        bout.flush();
	        socket.close();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	  }
	}}
