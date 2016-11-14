import java.io.*;
import java.util.*;
import java.security.*;
import java.math.*;
import java.lang.*;

public class Manager
{
  private List<User> users = new ArrayList<User>();
  private User curUser;

  public Manager(){
    File f = new File("Users.txt");
    if(f.exists()){
      try{
        BufferedReader br = new BufferedReader(new FileReader("Users.txt"));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();
        while(line != null){
          String[] parts = line.split(" ");
          User u = new User(parts[0]);
          users.add(u);
          line = br.readLine();
        }
      }
      catch(Exception e){
        e.printStackTrace();
      }
    }
  }

  public boolean registration ()
  {
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

      FileWriter fw = new FileWriter("Users.txt", true);
      BufferedWriter bw = new BufferedWriter(fw);
      password = encrypt(password);
      bw.write(username + " " + password);
      bw.newLine();
      bw.close();
    }
    catch(Exception e){
      e.printStackTrace();
    }
    System.out.println("Registration successful");
    return true;
  }

  public String encrypt(String password) throws Exception{
    MessageDigest cript = MessageDigest.getInstance("SHA-1");
    cript.reset();
    cript.update(password.getBytes("utf8"));
    String hex = String.format("%040x", new BigInteger(1,cript.digest()));
    return hex;
  }

  public boolean login() throws Exception{
    System.out.println("Enter your credentials.");
    Console console = System.console();
    String username = console.readLine("Enter your username: ");
    char passwordArray[] = console.readPassword("Enter your password: ");
    String password = new String(passwordArray);
    password = encrypt(password);

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

  public void createFile() throws Exception{
    Console console = System.console();
    String fileName = console.readLine("Enter filename: ");
    String[] parts = fileName.split(".");
    System.out.println(curUser.getUsername());
    File file = new File("Files/" + curUser.getUsername() + "/" + parts[0] + ".txt");
    file.createNewFile();
    //FileWriter fw = new FileWriter(fileNameTxt);
  }

  public void exit(){
    System.exit(0);
  }

  public boolean checkCredentials(String username, String password){
    try{
      BufferedReader br = new BufferedReader(new FileReader("Users.txt"));
      StringBuilder sb = new StringBuilder();
      String line = br.readLine();
      while(line != null){
        String[] parts = line.split(" ");
        if(parts[0].equals(username) && parts[1].equals(password)){
          System.out.println("You are logged in!");
          return true;
        }
        line = br.readLine();
      }
      System.out.println("Wrong Credentials!");
    }
    catch(Exception e){
      e.printStackTrace();
    }
    return false;
  }

}