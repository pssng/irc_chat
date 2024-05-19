import redis
import json
from fastapi import FastAPI, WebSocket
import base64
import telnetlib
from fastapi import FastAPI, WebSocket
from typing import Set
import websockets
app = FastAPI()


with open('env.json', 'r') as file:
    env = json.load(file)

host_telnet=env["telnet"]["hostname"]
port=env["telnet"]["port"]
r = redis.Redis(host=env["redis"]["hostname"], port=env["redis"]["port"], db=0)


# Set di clients già connessi
connected_clients: Set[WebSocket] = set()

'''
Endpoint REST di tipo GET per il recupero della chat tramite CODICE FISCALE UTENTE
Path Variables -> chat_key = chiave dei dati in cache: coincide con il codice fiscale del cliente.
(Gli admin non possiedono cronologia in cache, la cronologia è visualizzabile unicamente dai clienti per fini di privacy)
'''
@app.get("/chat/{chat_key}")
async def get_chat_content(chat_key: str):
    # Parsing in JSON del contenuto sottoforma di JSON String in Base64.
    return json.loads(base64.b64decode(str(r.get(chat_key)).split("'")[1]))


'''
Endpoint WEBSOCKET per il collegamento al sistema PSSNG (Progressive Solutions For Next Gen) CHAT tramite telnet.
In questo modo è stato semplificato il collegamento con il servizio da parte di un eventuale frontend realizzato con tecnologie
quali JavaScript e simili.
'''
@app.websocket("/chat")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    
    try:
        tn = telnetlib.Telnet(host_telnet, port) #Creazione dell'istanza di collegamento verso il server Telnet
        while True:
            message = await websocket.receive_text() #Ricezione del testo in input dal client WebSocket

            '''
               Scrittura del messaggio verso telnet sottoforma di bytes con codifica UTF-8.
               Tale codifica, a differenza di ASCII, consente di inviare messaggi contenenti caratteri speciali e/o accentati.
            '''
            tn.write(message.encode('utf-8') + b"\n") 

            '''
                A causa della logica del microservizio, non è necessario fornire all'utente la response tramite WebSocket, bensì tramite
                una chiamata periodica verso la cache. Tale scelta, sebbene sia discutibile in termini di performance, ci assicura che i
                messaggi siano presentati a front-end secondo il consueto ordine.
            '''

            '''
                La connessione viene chiusa in modo corretto, senza causare il lancio di eccezioni lato Java, quando viene
                lanciato il comando /exit
            '''
            if message == "/exit":
                tn.close()

    except Exception as e:
        print(f"Errore durante la comunicazione Telnet: {e}")


