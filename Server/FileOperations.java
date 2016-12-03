import java.io.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class FileOperations
{

	public FileOperations(){}

	public void createFile(User curUser) throws Exception{
	    Console console = System.console();
	    String fileName = console.readLine("Enter filename: ");
	    String[] parts = fileName.split("\\.");
	    File file = new File("Files/" + curUser.getUsername() + "/" + parts[0] + ".txt");
	    file.createNewFile();
	    curUser.addFile(file.getName());
	    System.out.println("File created");
	}

	public boolean listFiles(User curUser) throws Exception{
		File dir = new File("Files/" + curUser.getUsername() + "/");
		if(dir.list().length==0){
		  System.out.println("You don't have files created");
		  return false;
		}

		else{
			System.out.println("You have these files:");
			for(String file : dir.list())
			  System.out.println(file);
			return true;
		}
	}

	public void writeFile(User curUser) throws Exception{
		Console console = System.console();
		if(!listFiles(curUser))
			return;
		String fileName = console.readLine("What file do you want to write to?\n");
		String[] parts = fileName.split("\\.");
		fileName = parts[0]+".txt";
		File file = new File("Files/" + curUser.getUsername() + "/" + fileName);
		if(!file.exists()){
		  System.out.println("File doesn't exist. Try again:");
		  writeFile(curUser);
		}
		else{
		  boolean append = false;
		  boolean set = false;

		  while(!set){
		    String string = console.readLine("Do you want to append? Write Yes or No\n");
		    if(string.matches("[Yy][Ee][Ss]") || string.matches("[Yy]")){
		      append = true;
		      set = true;
		    }
		    else if(string.matches("[Nn][Oo]") || string.matches("[Nn]")){
		      append = false;
		      set = true;
		    }
		  }

		  String content = console.readLine("What do you want to write?\n");
		  FileWriter fw = new FileWriter(file, append);
		  BufferedWriter bw = new BufferedWriter(fw);
		  bw.write(content);
		  bw.close();
		  System.out.println("File written");
		}
	}

	public void readFile(User curUser) throws Exception{
		Console console = System.console();

		if(!listFiles(curUser))
			return;

		String fileName = console.readLine("What file do you want to read?\n");
		String[] parts = fileName.split("\\.");
		fileName = parts[0]+".txt";
		File file = new File("Files/" + curUser.getUsername() + "/" + fileName);
		if(!file.exists()){
		  System.out.println("File doesn't exist. Try again:");
		  readFile(curUser);
		}
		else{
		  BufferedReader br = new BufferedReader(new FileReader(file));

		  String line = br.readLine();
		  while(line != null){
		    System.out.println(line);
		    line = br.readLine();
		  }
		  br.close();
		}
	}

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