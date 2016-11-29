package pt.utl.ist.sirs.t05.sirsapp.SocketFunctions;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeStamps{
	
	public String generateTimeStamp() throws Exception{
      Calendar cal = Calendar.getInstance();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss");
      String time = sdf.format(cal.getTime());
      return time;
  	}

   	public boolean compareTimeStamp(String timestamp) throws Exception{
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss");
      Date receivedDate = sdf.parse(timestamp);
      return isWithinRange(receivedDate);
   	}

   	public boolean isWithinRange(Date receivedDate) throws Exception{
      String time = generateTimeStamp();
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss");
      Date date = sdf.parse(time);

      if(date.getTime() >= receivedDate.getTime() && date.getTime() <= receivedDate.getTime()+10000)
         return true;
      return false;
   	}
}