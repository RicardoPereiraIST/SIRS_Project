import java.io.*;
import java.util.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.file.Files;

public class Manager
{
  private List<User> users = new ArrayList<User>();
  private User curUser;
  private SecretKey key;
  private IvParameterSpec iv;
  private Crypto crypto = new Crypto();
  private FileOperations fo = new FileOperations();

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
    System.out.println("There are 3 commands:\nRegister\nLogin\nExit");
    String command = console.readLine("Enter your command: ");
    if(command.matches("[Ll][Oo][Gg][Ii][Nn]") || command.matches("[Ll]")){
        if(login()){
          File dir = new File("Files/" + curUser.getUsername() + "/");
          if(dir.exists())
            for(File f : dir.listFiles())
              crypto.decryptFile(f, key, iv);
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
    else if(command.matches("[Ee][Xx][Ii][Tt]") || command.matches("[Ee]")){
      logout();
      exit();
    }
    else{
      System.out.println("Unknown Command. Try again");
      init();
    }
  }

  public void display() throws Exception{
    Console console = System.console();
    System.out.println("There are 6 commands:\nCreate\nRead\nWrite\nPairing\nLogout\nExit");
    String command = console.readLine("Enter your command: ");
    if(command.matches("[Cc][Rr][Ee][Aa][Tt][Ee]") || command.matches("[Cc]")){
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
    }
    else if(command.matches("[Pp][Aa][Ii][Rr][Ii][Nn][Gg]") || command.matches("[Pp]"))
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
      System.out.println("Unknown Command. Try again");
      display();
    }
  }

  public void logout() throws Exception{
    if(curUser != null){
      File dir = new File("Files/" + curUser.getUsername() + "/");
        for(File f : dir.listFiles())
          crypto.encryptFile(f, key, iv);
    }
    curUser = null;
  }

  public boolean registration (){
    System.out.println("New User Registration");
    Console console = System.console();
    String username = console.readLine("Enter your username: ");
    char passwordArray[] = console.readPassword("Enter your password: ");
    String password = new String(passwordArray);

    try{
      for(int i = 0; i<users.size(); i++){
        if(users.get(i).getUsername().equals(username)){
          System.out.println("Username already exists.\nChoose another.");
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
    System.out.println("Registration successful");
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
      System.out.println("Try again");
      return false;
    }    
  }

  public void pairing() throws Exception{
    System.out.println("Paired");
    // DEMO
    Thread thread = new ServerSocketFunctions(6000, "espargueteabolonhesa");
    thread.start();
    display();
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
        	System.out.println("You are logged in!");
        	br.close();
        	return true;
        }
        line = br.readLine();
      }
      br.close();
      System.out.println("Wrong Credentials!");
    }
    catch(Exception e){
      e.printStackTrace();
    }
    return false;
  }

}