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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

@Log4j2
public class IRCChatServer {
    private static final int PORT = 8887;
    private static final Map<String, List<PrintWriter>> channels = new ConcurrentHashMap<>();
    private static CacheService cacheService;

    public static void main(String[] args) throws IOException {

        BannerUtil.printBanner();

        log.info("Testing Cache..");
        cacheService = new CacheService();

        JsonRedis jsonRedis = new JsonRedis("TEST_CACHE");
        jsonRedis.loadMessage(new JsonMessage("TEST_CACHE", "HELLO FROM PSSNG IRC"));
        cacheService.save("TEST_FISCAL_CODE", jsonRedis.toString());

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
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

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
                    log.info("Ciao " + username + ", benvenuto nel tuo canale: " + currentChannel);
                    out.println("Ciao " + username
                            + ", benvenuto nella chat di supporto, a breve un nostro operatore si collegherà per aiutarti!.\nSalva l'ID della chat per recuperarla in futuro: "
                            + UUID.randomUUID());

                }

                String message;
                while ((message = in.readLine()) != null) {
                    broadcastMessage(username, message, currentChannel);

                    // Aggiornamento della cache
                    handleCacheUpdate(userDetails, currentChannel, username, message);
                }
            } catch (SocketException e) {
                log.error("SocketException: Connection reset by peer");
            } catch (IOException e) {
                log.error("IOException while handling client", e);
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.error("Error closing socket", e);
                }
            }
        }

        private void handleCacheUpdate(SessionData userDetails, String currentChannel, String username,
                String message) {
            String redisKey = userDetails.getRole() == UserRole.ADMIN ? currentChannel.split("_")[1] : username;

            if (cacheService.exists(redisKey)) {
                JsonRedis cacheHistory = cacheService.retrieve(redisKey);
                log.info("Retrieved from cache: {}", cacheHistory);

                // Aggiungi il nuovo messaggio alla lista dei messaggi
                cacheHistory.loadMessage(new JsonMessage(username, message));
                cacheService.save(redisKey, cacheHistory.toString());
            } else {
                // Crea una nuova istanza di JsonRedis e salva il primo messaggio
                JsonRedis jsonRedis = new JsonRedis(redisKey);
                jsonRedis.loadMessage(new JsonMessage(username, message));
                cacheService.save(redisKey, jsonRedis.toString());
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
