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
  private IvParameterSpec iv;
  private Crypto crypto = new Crypto();
  private FileOperations fo = new FileOperations();

  private Unlock unlock;

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

    File f3 = new File(".PublicKeys.txt");
    if(!f3.exists()){
      try{
        f3.createNewFile();
      }
      catch(Exception e){
        e.printStackTrace();
      }
    }
  }

  public void init() throws Exception{
    Console console = System.console();
    System.out.println("Welcome to our filesystem!");
    System.out.println("There are 3 commands:\nRegister\nLogin\nExit");
    String command = console.readLine("Enter your command: ");
    if(command.matches("[Ll][Oo][Gg][Ii][Nn]") || command.matches("[Ll]")){
        if(login()){
          unlock();
          display();
        }
        else{
            init();
        }
    }
    else if(command.matches("[Rr][Ee][Gg][Ii][Ss][Tt][Ee][Rr]") || command.matches("[Rr]")){
        boolean registed = registration();
        while(!registed)
            registed = registration();
        init();
    }
    /*else if(command.matches("[Pp][Aa][Ii][Rr][Ii][Nn][Gg]") || command.matches("[Pp]")){
      if(instaPairing()){
        //call pairing function with username? (return username)
      }
      else
        init();
    }*/
    else if(command.matches("[Ee][Xx][Ii][Tt]") || command.matches("[Ee]")){
      logout();
      exit();
    }
    else{
      System.out.println("Unknown Command. Try again\n");
      init();
    }
  }

  public void unlock() throws Exception{
    File dir = new File("Files/" + curUser.getUsername() + "/");
    if(dir.exists())
      for(File f : dir.listFiles())
        crypto.decryptFile(f, key, iv);
  }

  public void lock() throws Exception{
    File dir = new File("Files/" + curUser.getUsername() + "/");
    if(dir.exists())
      for(File f : dir.listFiles())
        crypto.encryptFile(f, key, iv); 
  }

  /*public boolean instaPairing(){
    Console console = System.console();
    String username = console.readLine("What is your username?\n");
    try{
      BufferedReader br = new BufferedReader(new FileReader(".PublicKeys.txt"));
      String line = br.readLine();

      while(line != null){
        String[] parts = line.split(" ");
        if(parts[0].equals(username)){
          br.close();
          return true;
        }
        line = br.readLine();
      }
      br.close();
      System.out.println("That user didn't pair yet");
    }
    catch(Exception e){
      e.printStackTrace();
    }
    return false;
  }*/

  public void display() throws Exception{
    Console console = System.console();
    System.out.println("There are 3 commands: \nPairing\nLogout\nExit");  //\nCreate\nRead\nWrite\n
    String command = console.readLine("Enter your command: ");
    /*if(command.matches("[Cc][Rr][Ee][Aa][Tt][Ee]") || command.matches("[Cc]")){
      fo.createFile(curUser);
      display();
    }
    else if(command.matches("[Rr][Ee][Aa][Dd]") || command.matches("[Rr]")){
      fo.readFile(curUser);
      display();
    }
    else if(command.matches("[Ww][Rr][Ii][Tt][Ee]") || command.matches("[Ww]")){
      fo.writeFile(curUser);
      display();
    }*/
    if(command.matches("[Pp][Aa][Ii][Rr][Ii][Nn][Gg]") || command.matches("[Pp]"))
      pairing();
    else if(command.matches("[Ll][Oo][Gg][Oo][Uu][Tt]") || command.matches("[Ll]")){
      logout();
      init();
    }
    else if(command.matches("[Ee][Xx][Ii][Tt]") || command.matches("[Ee]")){
      logout();
      exit();
    }
    else{
      System.out.println("Unknown Command. Try again\n");
      display();
    }
  }

  public void logout() throws Exception{
    if(curUser != null){
      lock();
    }
    curUser = null;
  }

  public boolean registration (){
    System.out.println("New User Registration");
    Console console = System.console();
    String username = console.readLine("Enter your username: ");
    char passwordArray[] = console.readPassword("Enter your password: ");
    String password = new String(passwordArray);

    if(username.trim().isEmpty()){
      System.out.println("Username needs characters\n");
      return false;
    }

    if(password.trim().isEmpty()){
      System.out.println("Password needs characters\n");
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

    System.out.println("Pinging smartphone...");
    System.out.println("This allows the phone to unlock the files");

    unlock = new Unlock(6100, pair.getSessionKey(), false);
    unlock.start();
    unlock.join();

    if(unlock.getLockVar() == true){
      lock();
    }

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