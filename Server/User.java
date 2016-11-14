import java.io.*;
import java.util.*;

public class User
{
  private String username;
  private List<String> files = new ArrayList<String>();
  public User(String username){
    this.username = username;
  }
  public String getUsername(){
    return username;
  }
  public void addFile(String filepath){
    files.add(filepath);
  }

}