import java.io.*;

public class Main {
	
    public static void main(String[] args) throws Exception{
        Console console = System.console();
        Manager manager = new Manager();
        System.out.println("Welcome to our filesystem!");
        System.out.println("There are 3 commands:\nRegister\nLogin\nExit");
        String command = console.readLine("Enter your command: ");
        if(command.matches("[Ll][Oo][Gg][Ii][Nn]")){
            if(manager.login()){
                display();
            }
            else{
                main(args);
            }
        }
        else if(command.matches("[Rr][Ee][Gg][Ii][Ss][Tt][Ee][Rr]")){
            boolean registed = manager.registration();
            while(!registed)
                registed = manager.registration();
            main(args);
        }
        else if(command.matches("[Ee][Xx][Ii][Tt]"))
            manager.exit();


    }

    public static void display(){
        Manager manager = new Manager();
        Console console = System.console();
        System.out.println("There are 4 commands:\nCreate\nRead\nWrite\nExit");
        String command = console.readLine("Enter your command: ");
        if(command.matches("[Cc][Rr][Ee][Aa][Tt][Ee]")){

        }
        else if(command.matches("[Rr][Ee][Aa][Dd]")){

        }
        else if(command.matches("[Ww][Rr][Ii][Tt][Ee]")){

        }
        else if(command.matches("[Ee][Xx][Ii][Tt]"))
            manager.exit();
    }
}