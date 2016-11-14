import java.io.*;

public class Scanner
{

  public static void registration ()
  {
    System.out.println("New User Registration");
    Console console = System.console();
    String username = console.readLine("Enter your username: ");
    char passwordArray[] = console.readPassword("Enter your password: ");
    System.out.println(username);
    String password = new String(passwordArray);
  }

  public static void login(){

  }

}