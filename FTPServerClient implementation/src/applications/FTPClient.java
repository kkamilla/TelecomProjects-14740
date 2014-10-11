/*
 * FTP Client
 */
package applications;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Scanner;import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import services.TTPGoBackN;

public class FTPClient {
	public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
		
		System.out.println("File name:");
		String file_name;
		
		Scanner input = new Scanner(System.in);
		file_name = input.nextLine();
		String path = System.getProperty("user.dir") + "/ReceivedFilesFromServer/";

		//Set source and destination IP and ports
		int dest_port = 3015;
		int src_port = 2216;
		String src_ip = "127.0.0.1";
		String dest_ip = "127.0.0.1";
		
		byte[] msg_type = "MD5".getBytes();
		byte[] start = new byte[msg_type.length];		
		byte[] receivedHash = new byte[16];
		
		//Intialize client side TTP Go back N protocol
		TTPGoBackN ttpGBNClient = new TTPGoBackN(Integer.parseInt(args[0]),Integer.parseInt(args[1]));

		
			//Open connection to server
			ttpGBNClient.open(src_ip, dest_ip, (short)src_port, (short)dest_port, 10);
			
			System.out.println("\nConnection established to FTP server - " + dest_ip + ":" + dest_port);

			ttpGBNClient.sendD(file_name.getBytes());

			while (true) {
				
				byte[] data;		
				
				//Receive data from server
				data = ttpGBNClient.receiveD();

				if (data!=null) {			//If some data has been received from the server
					
					//Copy the starting bytes of data into start
					System.arraycopy(data, 0, start, 0, msg_type.length);	
					
					if (Arrays.equals(start, msg_type)) { //The message is an MD5 hash
						
						System.arraycopy(data, start.length, receivedHash, 0, 16);
						
					} else {							  //The message contains file data	
												
						byte[] md5Hash;
						
						//Compute the MD5 hash to verify that file data is not corrupted
						MessageDigest msgDigest = MessageDigest.getInstance("MD5");
						md5Hash = msgDigest.digest(data);
						System.out.println("\nFile received from server");
						
						//Compare received hash with computed hash
						if (Arrays.equals(md5Hash,receivedHash)) {
							
							System.out.println("\nMD5 hash verified: file has been received correctly.");
							File file;							
							file = new File(path + file_name);
							file.createNewFile();
							FileOutputStream fileStream = new FileOutputStream(file);
							BufferedOutputStream bufferStream = new BufferedOutputStream(fileStream);
							//write data to buffer output stream
							bufferStream.write(data);
							bufferStream.close();
							bufferStream = null;
					
						} else {
							
							System.out.println("\nMD5 did not match. File has been received in error.");
						}
						
						ttpGBNClient.close();
					}				
				}
			}			
	}
}