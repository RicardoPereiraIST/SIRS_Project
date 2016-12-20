import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class Timestamps {
	
	// ------ Timestamps -----------------
	   public long generateTimeStamp() throws Exception{

	        final CountDownLatch latch = new CountDownLatch(1);
	        final String[] timeStamp = new String[1];

	        new Thread()
	        {
	            public void run() {
	                try {
	                    URL obj = new URL("http://www.google.pt");
	                    URLConnection conn = obj.openConnection();

	                    Map<String, List<String>> map = conn.getHeaderFields();

	                    List<String> timeList = map.get("Date");
	                    String time = timeList.get(0).split(",")[1].replaceFirst(" ", "");

	                    timeStamp[0] = time;
	                    latch.countDown();
	                }catch (Exception e){
	                    e.printStackTrace();
	                }
	            }
	        }.start();
	        latch.await();

	        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy HH:mm:ss z");
	        Date date = sdf.parse(timeStamp[0]);

	        return date.getTime();
	   }


	      public boolean isWithinRange(long receivedDate) throws Exception{
	        long time = generateTimeStamp();

	        if(time >= receivedDate && time <= receivedDate+10000)
	           return true;

	        return false;
	      }

}
