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
    File dir = new File("Files");
    if(!dir.exists())
      dir.mkdir();
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

  public void init() throws Exception{
    Console console = System.console();
    System.out.println("Welcome to our filesystem!");
    System.out.println("There are 3 commands:\nRegister\nLogin\nExit");
    String command = console.readLine("Enter your command: ");
    if(command.matches("[Ll][Oo][Gg][Ii][Nn]")){
        if(login()){
            display();
        }
        else{
            init();
        }
    }
    else if(command.matches("[Rr][Ee][Gg][Ii][Ss][Tt][Ee][Rr]")){
        boolean registed = registration();
        while(!registed)
            registed = registration();
        init();
    }
    else if(command.matches("[Ee][Xx][Ii][Tt]"))
        exit();
  }

  public void display() throws Exception{
    Console console = System.console();
    System.out.println("There are 6 commands:\nCreate\nRead\nWrite\nPairing\nLogout\nExit");
    String command = console.readLine("Enter your command: ");
    if(command.matches("[Cc][Rr][Ee][Aa][Tt][Ee]")){
      createFile();
      display();
    }
    else if(command.matches("[Rr][Ee][Aa][Dd]")){
      readFile();
    }
    else if(command.matches("[Ww][Rr][Ii][Tt][Ee]")){
      writeFile();
    }
    else if(command.matches("[Pp][Aa][Ii][Rr][Ii][Nn][Gg]"))
      pairing();
    else if(command.matches("[Ll][Oo][Gg][Oo][Uu][Tt]"))
      init();
    else if(command.matches("[Ee][Xx][Ii][Tt]"))
      exit();
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
    String[] parts = fileName.split("\\.");
    File file = new File("Files/" + curUser.getUsername() + "/" + parts[0] + ".txt");
    file.createNewFile();
    System.out.println("File created");
  }

  public void writeFile() throws Exception{
    Console console = System.console();
    String fileName = console.readLine("What file do you want to write to?\n");
    String[] parts = fileName.split("\\.");
    fileName = parts[0]+".txt";
    File file = new File("Files/" + curUser.getUsername() + "/" + fileName);
    if(!file.exists()){
      System.out.println("File doesn't exist. Try again:");
      writeFile();
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
      bw.newLine();
      bw.close();
      System.out.println("File written");
      display();
    }
  }

  public void readFile() throws Exception{
    Console console = System.console();
    String fileName = console.readLine("What file do you want to read?\n");
    String[] parts = fileName.split("\\.");
    fileName = parts[0]+".txt";
    File file = new File("Files/" + curUser.getUsername() + "/" + fileName);
    if(!file.exists()){
      System.out.println("File doesn't exist. Try again:");
      readFile();
    }
    else{
      BufferedReader br = new BufferedReader(new FileReader(file));
      StringBuilder sb = new StringBuilder();
      String line = br.readLine();
      while(line != null){
        System.out.println(line);
        line = br.readLine();
      }
    }
    display();
  }

  public void pairing() throws Exception{
    System.out.println("Paired");
    display();
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