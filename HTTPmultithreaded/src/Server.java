/**
 * @file: Server.java
 * 
 * @author: Chinmay Kamat <chinmaykamat@cmu.edu>
 * 
 * @date: Feb 15, 2013 1:13:37 AM EST
 * 
 */
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public final class Server{
	private static ServerSocket srvSock;

	public static void main(String args[]) throws Exception {
		String buffer = null;
		int port = 80;
		String path=args[1];
		String pathreq=new String();
	    File f = new File(args[1]);
	    FileInputStream fstream = null;
	    if (f.isDirectory()) { 
            // if directory, implicitly add 'index.html'
            path =path+"index.html";
            pathreq="index.html";
            //f = new File(path);
        }
	    
	  
	  
		BufferedReader inStream = null;
		DataOutputStream outStream = null;

		/* Parse parameter and do args checking */
		if (args.length!=2) {
            System.err.println("Usage: java Server <port_number> <www_path>");
            System.exit(-1);
		}
		

		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception e) {
			System.err.println("Usage: java Server <port_number> <www_path>");
			System.exit(1);
		}

		if (port > 65535 || port < 1024) {
			System.err.println("Port number must be in between 1024 and 65535");
			System.exit(1);
		}

		try {
			/*
			 * Create a socket to accept() client connections. This combines
			 * socket(), bind() and listen() into one call. Any connection
			 * attempts before this are terminated with RST.
			 */
			srvSock = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println("Unable to listen on port " + port);
			System.exit(1);
		}

		while (true) {
			Socket clientSock = null;
			
			try {
				/*
				 * Getting a sock for further communication with the client.New connections are still
				 * accepted on srvSock
				 */
				clientSock = srvSock.accept();
				System.out.println("Accpeted new connection from "
						+ clientSock.getInetAddress() + ":"
						+ clientSock.getPort());
				// Constructing an object to process the HTTP request message.
		        HttpRequest request = new HttpRequest(clientSock);

		        // Creating a new thread to process the request.
		        Thread thread = new Thread(request);

		        // Starting the thread.
		        thread.start();
		        request.http_request_processor();
			} catch (IOException e) {
				continue;
			}
			try {
				inStream = new BufferedReader(new InputStreamReader(
						clientSock.getInputStream()));
				outStream = new DataOutputStream(clientSock.getOutputStream());
				/* Reading the data send by the client */
				buffer = inStream.readLine();
				System.out.println("Read from client "
						+ clientSock.getInetAddress() + ":"
						+ clientSock.getPort() + " " + buffer);
				
				
				
				outStream.flush();
				/* Interaction with this client complete, close() the socket */
				clientSock.close();
			} catch (IOException e) {
				clientSock = null;
				continue;
			}}}}



