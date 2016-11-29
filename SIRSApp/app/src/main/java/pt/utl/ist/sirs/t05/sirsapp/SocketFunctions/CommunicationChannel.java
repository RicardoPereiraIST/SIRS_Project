package pt.utl.ist.sirs.t05.sirsapp.SocketFunctions;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by Diogo on 11/19/2016.
 */

public class CommunicationChannel {

    private Socket client;

    public CommunicationChannel(Socket client){
        this.client = client;
    }

    public void writeToServer(String message) throws IOException{
        OutputStream outToServer = client.getOutputStream();
        DataOutputStream out = new DataOutputStream(outToServer);

        out.writeUTF(message);
    }

    public String readFromServer() throws IOException{
        InputStream inFromServer = client.getInputStream();
        DataInputStream in = new DataInputStream(inFromServer);

        return in.readUTF();
    }
}
