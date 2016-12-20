import java.io.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class FileOperations
{

	public FileOperations(){}


	public void unlock(User curUser, SecretKey key, IvParameterSpec iv) throws Exception{
		Crypto crypto = new Crypto();
	    File dir = new File("Files/" + curUser.getUsername() + "/");
	    if(dir.exists())
	      for(File f : dir.listFiles()){
	        crypto.decryptFile(f,key,iv);
	      }
	    Manager.isLocked = false;
  	}

  	public void lock(User curUser, SecretKey key, IvParameterSpec iv) throws Exception{
  		Crypto crypto = new Crypto();
	    File dir = new File("Files/" + curUser.getUsername() + "/");
	    if(dir.exists())
	      for(File f : dir.listFiles()){
	        crypto.encryptFile(f,key,iv);
	      }
	    Manager.isLocked = true;
  	}

}