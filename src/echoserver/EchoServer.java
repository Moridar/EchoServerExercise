package echoserver;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import shared.ProtocolStrings;
import utils.Utils;

public class EchoServer {

    private static boolean keepRunning = true;
    private static ServerSocket serverSocket;
    private static final Properties properties = Utils.initProperties("server.properties");
    private ArrayList<ClientHandler> clients = new ArrayList<>();

    public static void stopServer() {
        keepRunning = false;
    }

    private class ClientHandler extends Thread {

        private Socket socket;
        private Scanner input;
        private PrintWriter writer;
        private EchoServer ES;

        ClientHandler(Socket socket, EchoServer ES) {
            this.socket = socket;
            this.ES = ES;           
        }

        public void send(String message) {
            writer.println(message.toUpperCase());
            Logger.getLogger(EchoServer.class.getName()).log(Level.INFO, String.format("Received the message: %1$S ", message.toUpperCase()));
        }

        @Override
        public void run() {

            try {
                input = new Scanner(socket.getInputStream());
                writer = new PrintWriter(socket.getOutputStream(), true);
                String message = input.nextLine(); //IMPORTANT blocking call
                Logger.getLogger(EchoServer.class.getName()).log(Level.INFO, String.format("Received the message: %1$S ", message));
                while (!message.equals(ProtocolStrings.STOP)) {
                    ES.send(message);
                    try {
                        message = input.nextLine(); //IMPORTANT blocking call
                    } catch (NoSuchElementException e) {
                        break;
                    }
                }
                writer.println(ProtocolStrings.STOP);//Echo the stop message back to the client for a nice closedown
                socket.close();
                ES.removeHandler(this);
                Logger.getLogger(EchoServer.class.getName()).log(Level.INFO, "Closed a Connection");
            } catch (IOException ex) {
                Logger.getLogger(EchoServer.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

    }

    private void runServer() {
        int port = Integer.parseInt(properties.getProperty("port"));
        String ip = properties.getProperty("serverIp");

        Logger.getLogger(EchoServer.class.getName()).log(Level.INFO, "Sever started. Listening on: " + port + ", bound to: " + ip);
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(ip, port));
            do {
                Socket socket = serverSocket.accept(); //Important Blocking call
                Logger.getLogger(EchoServer.class.getName()).log(Level.INFO, "Connected to a client");
                ClientHandler CH = new ClientHandler(socket,this);
                CH.start();
                clients.add(CH);
                System.out.println("Added new client");

            } while (keepRunning);
        } catch (IOException ex) {
            Logger.getLogger(EchoServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void removeHandler(ClientHandler ch) {
        clients.remove(ch);
        System.out.println("Removed a client");
    }

    public void send(String msg) {
        for (ClientHandler ch : clients) {
            ch.send(msg);
        }
    }

    public static void main(String[] args) {
        String logFile = properties.getProperty("logFile");
        Utils.setLogFile(logFile, EchoServer.class.getName());

        new EchoServer().runServer();
        Utils.closeLogger(EchoServer.class.getName());
    }
}
