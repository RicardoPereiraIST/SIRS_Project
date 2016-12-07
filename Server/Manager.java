import java.io.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.file.Files;
import java.math.BigInteger;
import java.security.SecureRandom;

public class Manager
{
  private List<User> users = new ArrayList<User>();
  private User curUser;
  private SecretKey key;
  private SecretKey sessionKey;
  private IvParameterSpec iv;
  private Crypto crypto = new Crypto();
  private FileOperations fo = new FileOperations();

  private Unlock unlock;
  public static Boolean isLocked;

  public Manager(){
    File dir = new File("Files");
    if(!dir.exists())
      dir.mkdir();
    File f = new File(".Users.txt");
    if(f.exists()){
      try{
        BufferedReader br = new BufferedReader(new FileReader(".Users.txt"));
        String line = br.readLine();
        while(line != null){
          String[] parts = line.split(" ");
          User u = new User(parts[0]);
          users.add(u);
          line = br.readLine();
        }
        br.close(); 
      }
      catch(Exception e){
        e.printStackTrace();
      }
    }
    else{
      try{
        f.createNewFile();
      }
      catch(Exception e){
        e.printStackTrace();
      }
    }

    File f2 = new File(".Iv.txt");
    if(!f2.exists()){
      SecureRandom random = new SecureRandom();
      byte[] ivBytes = new byte[16];
      random.nextBytes(ivBytes);
      iv = new IvParameterSpec(ivBytes);
      try{
        Files.write(f2.toPath(), ivBytes);
      }
      catch(Exception e){
        e.printStackTrace();
      }
    }
    else{
      try{
        byte[] ivBytes = Files.readAllBytes(f2.toPath());
        iv = new IvParameterSpec(ivBytes);
      }
      catch(Exception e){
        e.printStackTrace();
      }
    }
  }

  public void init() throws Exception{
    Console console = System.console();
    System.out.println("Welcome to our filesystem!");
    System.out.println("There are 3 commands:\n1-Register\n2-Login\n3-Exit");
    String command = console.readLine("Enter your command: ");
    if(command.matches("1")){
      boolean registed = registration();
      while(!registed)
          registed = registration();
      init();
    }
    else if(command.matches("2")){
        if(login()){
          fo.unlock(curUser, key, iv);
          display();
        }
        else{
            init();
        }
    }

    else if(command.matches("3")){
      logout();
      exit();
    }
    else{
      System.out.println("Unknown Command. Try again\n");
      init();
    }
  }


  public void display() throws Exception{
    Console console = System.console();
    System.out.println("There are 5 commands: \n1-Lock\n2-Pairing\n3-Unlock files with Phone\n4-Logout\n5-Exit");  //\nCreate\nRead\nWrite\n
    String command = console.readLine("Enter your command: ");

    if(command.matches("1")){
      if(isLocked == false){
        fo.lock(curUser, key, iv);
        System.out.println("Files locked\n");
      }
      else
        System.out.println("Files already locked\n");
      display();
    }
  /*  else if(command.matches("2")){
      if(isLocked){
        fo.unlock(curUser, key, iv);
        System.out.println("Files unlocked\n");
        isLocked = false;
      }
      else
        System.out.println("Files already unlocked\n");
      display();
    }*/
    else if(command.matches("2"))
      pairing();
    else if(command.matches("3"))
      listenToUnlockRequest();
    else if(command.matches("4")){
      logout();
      init();
    }
    else if(command.matches("5")){
      logout();
      exit();
    }
    else{
      System.out.println("Unknown Command. Try again\n");
      display();
    }
  }

  public void logout() throws Exception{
    if(curUser != null && isLocked == false){
      fo.lock(curUser, key, iv);
    }
    curUser = null;
  }

  public boolean registration (){
    System.out.println("New User Registration");
    Console console = System.console();
    String username = console.readLine("Enter your username: ");
    char passwordArray[] = console.readPassword("Enter your password (has to be 8 characters long and have letters and numbers): ");
    String password = new String(passwordArray);

    if(username.trim().isEmpty()){
      System.out.println("Username needs characters\n");
      return false;
    }

    if(password.trim().isEmpty()){
      System.out.println("Password needs characters\n");
      return false;
    }

    if(password.length() < 8){
      System.out.println("Password has to be 8 characters long\n");
      return false;
    }

    if(!password.matches("^(?=.*[a-zA-Z])(?=.*[0-9])[a-zA-Z0-9]+$")){
      System.out.println("Please add numbers and letters to your password\n");
      return false;
    }

    try{
      for(int i = 0; i<users.size(); i++){
        if(users.get(i).getUsername().equals(username)){
          System.out.println("Username already exists.\nChoose another.\n");
          return false;
        }
      }
      User u = new User(username);
      users.add(u);
      File f = new File("Files/"+username);
      if(!f.exists())
        f.mkdir();

      FileWriter fw = new FileWriter(".Users.txt", true);
      BufferedWriter bw = new BufferedWriter(fw);
      String salt = crypto.generateSalt();
      password = crypto.encryptPassword(password+salt);
      bw.write(username + " " + salt + " " + password);
      bw.newLine();
      bw.close();
    }
    catch(Exception e){
      e.printStackTrace();
    }
    System.out.println("Registration successful\n");
    return true;
  }


  public boolean login() throws Exception{
    System.out.println("Enter your credentials.");
    Console console = System.console();
    String username = console.readLine("Enter your username: ");
    char passwordArray[] = console.readPassword("Enter your password: ");
    String password = new String(passwordArray);

    if(checkCredentials(username, password)){
      for(int i = 0; i<users.size(); i++){
        if(users.get(i).getUsername().equals(username)){
          curUser = users.get(i);
        }
      }
      return true;
    }

    else{
      System.out.println("Try again\n");
      return false;
    }    
  }

  public void pairing() throws Exception{
    System.out.println("Pairing with your smartphone");

    SecureRandom r = new SecureRandom();
    String token = new BigInteger(32, r).toString(32);
    System.out.println("Write this token in your app: " + token);

    Pair pair = new Pair(6000, token);
    pair.start();
    pair.join();

    sessionKey = pair.getSessionKey();

    if(sessionKey == null){
      display();
      return;
    }

    String option = "";
    Console console = System.console();
    while(!option.equals("yes") && !option.equals("no")){
      option = console.readLine("Do you want to pair the phone right now? (yes/no) ");
    }

    if(option.equals("yes")){
      listenToUnlockRequest();
    }else{
      System.out.println("Very well.");
      display();
    }


  }

  public void listenToUnlockRequest() throws Exception{

    if (sessionKey == null){
      System.out.println("The phone is not yet paired!");
      display();
      return;
    }

    System.out.println("Waiting for smartphone...");
    System.out.println("This allows the phone to unlock the files");

    unlock = new Unlock(6100, sessionKey, curUser, iv, key);
    unlock.start();
    unlock.join();

    display();
    System.out.println();
  }

  public void exit(){
    System.exit(0);
  }
 

  public boolean checkCredentials(String username, String password) throws Exception{
    try{
      BufferedReader br = new BufferedReader(new FileReader(".Users.txt"));
      String line = br.readLine();

      while(line != null){
        String[] parts = line.split(" ");
        if(parts[0].equals(username) && parts[2].equals(crypto.encryptPassword(password+parts[1]))){   
        	byte[] salt = parts[1].getBytes();
        	key = crypto.generateEncryptionKey(password, salt);
        	System.out.println("You are logged in!\n");
        	br.close();
        	return true;
        }
        line = br.readLine();
      }
      br.close();
      System.out.println("Wrong Credentials!\n");
    }
    catch(Exception e){
      e.printStackTrace();
    }
    return false;
  }

}