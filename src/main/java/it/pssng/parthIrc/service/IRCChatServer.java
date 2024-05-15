package it.pssng.parthIrc.service;


import it.pssng.parthIrc.model.UserRole;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import it.pssng.parthIrc.model.UserDetails;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


public class IRCChatServer {
    private static final int PORT = 8887;
    private static final Map<Channel, List<PrintWriter>> channels = new ConcurrentHashMap<>();

    private static final Logger log = LogManager.getLogger("IRCChatServer");

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        log.info("Service started at port {}",PORT);


        while (true) {
            Socket clientSocket = serverSocket.accept();
            log.info("New client connected: " + clientSocket);
            new ClientHandler(clientSocket).start();
        }
    }


    private static class ClientHandler extends Thread {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private Channel channel = new Channel();

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @SuppressWarnings("unused")
        private void flushBuffer(BufferedReader buffer) throws IOException {
            while (buffer.ready()) {
                buffer.read();
            }
        }


        @SneakyThrows
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

                flushBuffer(in);

                out.println("BENVENUTO NEL SERVIZIO DI SUPPORTO DI 'JustArt'! ");
                out.println("Se desideri recuperare una vecchia chat digita [1]");
                out.println("Se desideri iniziare una nuova chat digita [2]");

                String scelta = in.readLine();
                log.info("SCELTA: {}",scelta);

                switch(scelta){
                    case "1":
                        out.print("Inserisci il codice della chat: ");
                        String codice = in.readLine();
                        break;
                    case "2":

//                        out.println("Inserire Jwt Token: ");
//                        String jwt = in.readLine();


                        UserDetails admin = new UserDetails("Mario Rossi","ASDFFG", UserRole.ADMIN);
                        UserDetails user = new UserDetails("Armando Cobucci","FGFG",UserRole.USER);
                        out.println("MOCKED USERS:\n1 - "+admin+"\n2 - "+user);

                        String sceltaMock = in.readLine();

                        UserDetails userDetails = sceltaMock.equals("1") ? admin : user;

                        switch (userDetails.getRole()){
                            case ADMIN:

                                username = userDetails.getFiscalCode();

                                out.println("Benvenuto " + username);

                                out.println("Scegli un canale esistente:");

                                Channel assignedChannel = new Channel();
                                List<PrintWriter> assignedPrintWriter = new ArrayList<>();

                                synchronized (channels) {
                                    Map.Entry<Channel, List<PrintWriter>> mapFirst = channels.entrySet().iterator().next();
                                    channels.remove(mapFirst.getKey());
                                    assignedChannel = mapFirst.getKey();
                                    assignedPrintWriter = mapFirst.getValue();

                                }

                                channel = assignedChannel;

                                out.println("Ti sei connesso al canale: " + assignedChannel.getChannel());
                                broadcastMessage("Operatore connesso alla chat", assignedChannel.getChannel());
                                break; //break dello switch
                            case USER:
                                Channel userChannel = new Channel("channel_" + userDetails.getFiscalCode(),false);

                                synchronized (channels) {
                                    if (!channels.containsKey(userChannel)) {
                                        channels.put(userChannel, new ArrayList<>());
                                    }
                                    List<PrintWriter> channelUsers = channels.get(userChannel);
                                    if (channelUsers.size() >= 2) {
                                        out.println("Il canale è pieno. Disconnessione...");
                                        System.out.println("Il canale è pieno. Disconnessione...");
                                        return;
                                    }
                                    channelUsers.add(out);
                                }


                                channel.setChannel(userChannel.getChannel());

                                log.info("Ciao " + userDetails.getGenerals() + ", benvenuto nel tuo canale: " + userChannel);
                                out.println("Ciao " + userDetails.getGenerals() + ", benvenuto nella chat di supporto, a breve un nostro operatore si collegherà per aiutarti!.\nSalva l'ID della chat per recuperarla in futuro: " + UUID.randomUUID());


                                break;
                         }

                        break;
                    default:
                        log.error("Choice {} illegal.", scelta);
                        out.println("Scelta non valida");
                        return;
                }

                String message;
                while ((message = in.readLine()) != null) {
                    broadcastMessage(username, message, channel.getChannel());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void broadcastMessage(String username, String message, String userChannel) {
            synchronized (channels) {
                List<PrintWriter> channelUsers = channels.get(userChannel);
                if (channelUsers != null) {
                    for (PrintWriter writer : channelUsers) {
                        writer.println(username + "@" + userChannel + ": " + message);
                    }
                }
            }
        }

        private void broadcastMessage(String message, String userChannel) {
            synchronized (channels) {
                List<PrintWriter> channelUsers = channels.get(userChannel);
                if (channelUsers != null) {
                    for (PrintWriter writer : channelUsers) {
                        writer.println(message);
                    }
                }
            }
        }

    }
}