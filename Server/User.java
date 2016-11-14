import java.io.*;
import java.util.*;

public class User
{
  private String username;
  private List<String> files = new ArrayList<String>();
  public User(String username){
    this.username = username;
    File f = new File("Files\\" + username);
    if(f.exists()){
      try{
        for(File fileEntry : f.listFiles())
          files.add(fileEntry.getName());
      }
      catch(Exception e){
        e.printStackTrace();
      }
    }
  }
  public String getUsername(){
    return username;
  }
  public void addFile(String filepath){
    files.add(filepath);
  }

}