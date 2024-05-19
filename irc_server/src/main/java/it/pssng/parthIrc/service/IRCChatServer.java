package it.pssng.parthIrc.service;

import it.pssng.parthIrc.model.UserRole;
import it.pssng.parthIrc.utils.BannerUtil;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import it.pssng.parthIrc.json.JsonMessage;
import it.pssng.parthIrc.json.JsonRedis;
import it.pssng.parthIrc.model.SessionData;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

@Log4j2
public class IRCChatServer {
    private static final int PORT = 8887;
    private static final Map<String, List<PrintWriter>> channels = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {

        BannerUtil.printBanner();

        log.info("Testing Redis..");
        CacheService cache = new CacheService();

        JsonRedis jsonRedis = new JsonRedis("TEST_REDIS");
        jsonRedis.loadMessage(new JsonMessage("TEST_REDIS", "HELLO FROM PSSNG IRC"));
        cache.save("TEST_FISCAL_CODE", jsonRedis.toString());

        ServerSocket serverSocket = new ServerSocket(PORT);
        log.info("Service started at port {}", PORT);

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
        private String currentChannel;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @SneakyThrows
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

                out.println("");
                SessionData userDetails = new SessionData(in.readLine());                

                if (userDetails.getRole() == UserRole.ADMIN) {
                    username = userDetails.getFiscalCode();
                    out.println("Benvenuto " + username);

                    synchronized (channels) {
                        Map.Entry<String, List<PrintWriter>> mapFirst = channels.entrySet().iterator().next();
                        currentChannel = mapFirst.getKey();
                        out.println("Ti sei connesso al canale: " + currentChannel);
                        mapFirst.getValue().add(out);
                    }

                    broadcastMessage("Operatore connesso alla chat", currentChannel);
                } else {
                    currentChannel = "channel_" + userDetails.getFiscalCode();
                    synchronized (channels) {
                        channels.putIfAbsent(currentChannel, new ArrayList<>());
                        List<PrintWriter> channelUsers = channels.get(currentChannel);
                        if (channelUsers.size() >= 2) {
                            out.println("Il canale è pieno. Disconnessione...");
                            return;
                        }
                        channelUsers.add(out);
                    }
                    username = userDetails.getFiscalCode();
                    log.info("Ciao " + username + ", benvenuto nel tuo canale: "
                            + currentChannel);
                    out.println("Ciao " + username
                            + ", benvenuto nella chat di supporto, a breve un nostro operatore si collegherà per aiutarti!.\nSalva l'ID della chat per recuperarla in futuro: "
                            + UUID.randomUUID());

                }

                String message;
                while ((message = in.readLine()) != null) {
                    broadcastMessage(username, message, currentChannel);
                    CacheService cache = new CacheService();

                    String redisKey = userDetails.getRole() == UserRole.ADMIN ? currentChannel.split("_")[1] : username;

                    if (cache.exists(redisKey)) {
                        JsonRedis cacheHistory = cache.retrieve(redisKey);
                        log.info(cacheHistory);
                        cacheHistory.loadMessage(new JsonMessage(username, message));
                        cache.save(redisKey, cacheHistory.toString());
                        cache.close();
                    } else {

                        JsonRedis jsonRedis = new JsonRedis(redisKey);
                        jsonRedis.loadMessage(new JsonMessage(username, message));
                        cache.save(redisKey, jsonRedis.toString());
                    }

                }
            } catch (

            IOException e) {
                log.error("Error handling client: ", e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.error("Error closing socket: ", e);
                }
            }
        }

        private void broadcastMessage(String username, String message, String userChannel) {
            List<PrintWriter> channelUsers = channels.get(userChannel);
            if (channelUsers != null) {
                synchronized (channelUsers) {
                    for (PrintWriter writer : channelUsers) {
                        writer.println(username + "@" + userChannel + ": " + message);
                    }
                }
            }
        }

        private void broadcastMessage(String message, String userChannel) {
            List<PrintWriter> channelUsers = channels.get(userChannel);
            if (channelUsers != null) {
                synchronized (channelUsers) {
                    for (PrintWriter writer : channelUsers) {
                        writer.println(message);
                    }
                }
            }
        }
    }
}
