/*
 * FTP Server
 */
package applications;
import services.TTPServer;	
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FTPServer  {			

	public static void main(String[] args) throws ClassNotFoundException {
		
		byte[] fileRequest;
		
		//Initialize TTP server passing window size and retransmission interval
		TTPServer ttpServer = new TTPServer(Integer.parseInt(args[0]),Integer.parseInt(args[1]));

		try {
			//opening connection in TTPServer
			ttpServer.open(3015,10);

			while (true) {
				
				fileRequest = ttpServer.receive();
				
				if (fileRequest != null) {
					
					System.out.println("FTP Server received file request.");					
					//Start new thread to process fetching of file 					
					Thread serviceClient = new Thread(new fileFetch(ttpServer,fileRequest));
					serviceClient.start();
				}
				System.out.println("FTP Server listening");
			}
		} catch (SocketException e) 
		{
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}

 class fileFetch implements Runnable {
	
	private TTPServer ttpServer;	//File data is fetched and sent to the TTP Server
	private byte[] request;			//Request containing file name and client details	
	public fileFetch (TTPServer ttp_server, byte[] request) {
		
		super();
		this.ttpServer = ttp_server;
		this.request = request;

	}

	@Override

	public void run()  {

		try {
			byte[] file_data;		
			byte[] f_name;			
			byte[] clientDetails;	//client details like source ip and source port
			byte[] data;
			byte[] md5Hash;			//to store the MD5 hash of the file data
			byte[] hashData;			//to store client details and md5 hash of the file data
			FileInputStream fileStream;	
			File file;
			
			//Get the bytes corresponding to the requested file name
			
			f_name = new byte[request.length - 6];					
			System.arraycopy(request, 6, f_name, 0, request.length-6);
			
			//Get file name in file_name
			
			String file_name = new String(f_name,"US-ASCII");
			System.out.println("File: " + file_name);
			
			//Fetch source port and ip into clientDetails
			
			clientDetails = new byte[6];			
			System.arraycopy(request, 0, clientDetails, 0, 6);
			
			//Read data from requested file into file_data
			
			file = new File(file_name.toString());			
			fileStream = new FileInputStream(file);			
			file_data = new byte[(int)file.length()];						
			fileStream.read(file_data, 0, (int)file.length());
			fileStream.close();
						
			//MD5 hash of the file data
			MessageDigest msgDigest = MessageDigest.getInstance("MD5");
			md5Hash = msgDigest.digest(file_data);
			
			byte[] msg_type = "MD5".getBytes();	//to indicate that the message is an MD5 hash (not file data)
			hashData = new byte[md5Hash.length + 6 + msg_type.length];
			
			//build the file data message
			
			data = new byte[(int)file.length() + 6];
			System.arraycopy(clientDetails, 0, data, 0, 6);
			System.arraycopy(file_data, 0, data, 6, (int)file.length());
			
			//build the md5 hash message
			
			System.arraycopy(clientDetails, 0, hashData, 0, 6);			
			System.arraycopy(msg_type, 0,hashData, 6, msg_type.length);
			System.arraycopy(md5Hash, 0, hashData, 6 + msg_type.length, md5Hash.length);
			
			//Send MD5 hash message
			ttpServer.send(hashData);
			//Send the file data message after a delay
			Thread.sleep(500);		
			ttpServer.send(data);

			System.out.println("File sent to TTP server");
		
		} catch (FileNotFoundException e) {
		
			System.out.println("The requested file does not exist!");
		
		}
		catch (IOException e) {
		
			e.printStackTrace();
		
		} catch (NoSuchAlgorithmException e) {
		
			e.printStackTrace();
		
		} catch (InterruptedException e) {
		
			e.printStackTrace();
		
		} 
	}

}