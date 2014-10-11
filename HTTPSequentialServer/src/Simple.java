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

public class Simple {
	private static ServerSocket srvSock;

	public static void main(String args[]) {
		String buffer = null;
		//String path;
		int port = 80;
		String path=args[1];
		String pathreq=new String();
		File f = new File(path);
		if (f.isDirectory()) { 
            // if directory, implicitly add 'index.html'
            path =path+"index.html";
            pathreq="index.html";
           
        }
	   
	    FileInputStream fstream = null;
	  
	  
		BufferedReader inStream = null;
		DataOutputStream outStream = null;

		/* Parse parameter and do args checking */
		if (args.length!=2) {
            System.err.println("Usage: java Simple <port_number> <www_path>");
            System.exit(-1);
		}
		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception e) {
			System.err.println("Usage: java Simple <port_number> <www_path>");
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
			Socket clientSock;
			try {
				/*
				 * Get a sock for further communication with the client. This
				 * socket is sure for this client. Further connections are still
				 * accepted on srvSock
				 */
				clientSock = srvSock.accept();
				System.out.println("Accpeted new connection from "
						+ clientSock.getInetAddress() + ":"
						+ clientSock.getPort());
			} catch (IOException e) {
				continue;
			}
			try {
				inStream = new BufferedReader(new InputStreamReader(
						clientSock.getInputStream()));
				outStream = new DataOutputStream(clientSock.getOutputStream());
				/* Read the data send by the client */
				buffer = inStream.readLine();
				System.out.println("Read from client "
						+ clientSock.getInetAddress() + ":"
						+ clientSock.getPort() + " " + buffer);
				
				
				http_request_processor(buffer, outStream,pathreq);
				outStream.flush();
				/* Interaction with this client complete, close() the socket */
				clientSock.close();
			} catch (IOException e) {
				clientSock = null;
				continue;
			}
		}
	}


public static void http_request_processor(String input, DataOutputStream output,String origfile) {
  int method = 0; //1 for GET, 2 for HEAD, 0 not supported
  String path = new String(); 
  
  try {
    //These are the two types of request that are handled
    //GET /index.html HTTP/1.0
    //HEAD /index.html HTTP/1.0
    String temp = input; 
    String userinp = new String(temp);
  
    userinp.toUpperCase(); 
   
  //Check for HTTP version (report 505 exception for versions other than 1.0 and 1.1)
    int index = userinp.indexOf("HTTP/");
   
    String http_version = userinp.substring(index + 5, userinp.length());
   
    if (!((http_version.compareTo("1.0")== 0) || (http_version.compareTo("1.1")== 0)))
    {
        output.writeBytes(create_header(505, 0));
        output.close(); 	//close output stream
        return;    	
    }
    
    
    if (userinp.startsWith("GET")) { 
      method = 1;
    } 
    if (userinp.startsWith("HEAD")) { 
      method = 2;
    } 

    if (method == 0) { // for not supported methods i.e other than GET and HEAD
      try {
    	  
        output.writeBytes(create_header(501, 0));
        
        output.close();
        return;
      }
      catch (Exception e3) { 
    	  System.out.println("error:" + e3.getMessage());
      } 
    }
  
    int start = 0;
    int end = 0;
    for (int a = 0; a < userinp.length(); a++) {
      if (userinp.charAt(a) == ' ' && start != 0) {
        end = a;
        break;
      }
      if (userinp.charAt(a) == ' ' && start == 0) {
        start = a;
      }
    }
    path = userinp.substring(start + 2, end); 
   
  }
  catch (Exception e) {
	  System.out.println("erorr: " + e.getMessage());
  } //catch any exception
  if (path.compareTo("") == 0) {
	  path="index.html";
  } 
  String pathwww="www/"+path;
  
  System.out.println("\nClient requested:" + new File(pathwww).getAbsolutePath() + "\n");
  FileInputStream requestedfile = null;
  File f = new File(pathwww);
  FileInputStream fstreamout = null;
		try {
			fstreamout = new FileInputStream(f);
		} catch (FileNotFoundException e1) {
			// catch block
			e1.printStackTrace();
		}

  try {

    //trying to open the file,if found
	  
    	File fout = new File(pathwww);
    	  FileInputStream fstreamoutr = null;
    			try {
    				fstreamoutr = new FileInputStream(fout);
    				
    				//if it is a HEAD request,don't print the body and now it is 200: OK
    			    output.writeBytes(create_header(200, 5));
    			} catch (FileNotFoundException e1) {
    				// catch block
    				output.writeBytes(create_header(404, 0));
    				e1.printStackTrace();
    			}
    	if ( path.compareTo(origfile) == 0 )
			    {
	  requestedfile = fstreamoutr;
  }}
  catch (Exception e) {
    try {
      //if the file was not found, send a 404
      output.writeBytes(create_header(404, 0));
     
      output.close(); //close the stream
    }
    catch (Exception e2) {}
    ;
    System.out.println("error: " + e.getMessage());
  } 
  try {
    int type_is = 0;
    //find out what the filename ends with to write content type
    if (path.endsWith(".zip")) {
      type_is = 3;
    }
    if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
      type_is = 1;
    }
    if (path.endsWith(".gif")) {
      type_is = 2;
      
    }
    
    
   
  //1 is GET and it prints the body and the header
    if (method == 1) { 
    	
        //read the file from filestream object, and print out through the client-outputstream byte by byte.
      
    	FileInputStream fis =new FileInputStream(new File(pathwww).getAbsolutePath()) ;
     int i;
     while((i=fis.read())>-1)
    	 output.write(i);
    
    output.flush();
      
    }
    
//cleaning up the files, close open handles
    output.close();
    requestedfile.close();
  }

  catch (Exception e) {
	   System.out.println("error: " + e.getMessage());
  }

}





//This method makes the HTTP header for the response

private static String create_header(int return_code, int file_type) {
  String headerstring = "HTTP/1.0 ";
 
  switch (return_code) {
    case 200:
    	headerstring = headerstring + "200 OK";
      break;
    
    case 404:
    	headerstring = headerstring + "404 File Not Found";
      break;
    case 500:
    	headerstring = headerstring + "500 Not Found";
      break;
    case 501:
    	headerstring = headerstring + "501 Not Implemented";
      break;
    case 505:
    	headerstring = headerstring + "505 HTTP Version Not Supported";
      break;
  }

  headerstring = headerstring + "\r\n";
  headerstring = headerstring + "Server: Simple \r\n"; //server name
  headerstring = headerstring + "Connection: close\r\n"; //as it is non-persistent connection
  //write the Content-Type for the header
  switch (file_type) {
  
    case 0:
      break;
    case 1:
    	headerstring = headerstring + "Content-Type: image/jpeg\r\n";
      break;
    case 2:
    	headerstring = headerstring + "Content-Type: image/gif\r\n";
    case 3:
    	headerstring = headerstring + "Content-Type: application/x-zip-compressed\r\n";
    default:
    	headerstring = headerstring + "Content-Type: text/html\r\n";
      break;
  }
  headerstring = headerstring + "\r\n"; //this is the end of the header
  return headerstring;
}}


