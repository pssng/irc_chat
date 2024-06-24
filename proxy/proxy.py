import redis
import json
from fastapi import FastAPI, WebSocket
import base64
import telnetlib
from fastapi import FastAPI, WebSocket
from typing import Set
import websockets
from fastapi.middleware.cors import CORSMiddleware
import requests
from openai import OpenAI

app = FastAPI()


with open('env.json', 'r') as file:
    env = json.load(file)

host_telnet=env["telnet"]["hostname"]
port=env["telnet"]["port"]
r = redis.Redis(host=env["redis"]["hostname"], port=env["redis"]["port"], db=0)



# Set di clients già connessi
connected_clients: Set[WebSocket] = set()

app.add_middleware(CORSMiddleware,allow_origins=["*"],allow_credentials=True,allow_methods=["*"],allow_headers=["*"])

'''
Endpoint REST di tipo GET per il recupero della chat tramite CODICE FISCALE UTENTE
Path Variables -> chat_key = chiave dei dati in cache: coincide con il codice fiscale del cliente.
(Gli admin non possiedono cronologia in cache, la cronologia è visualizzabile unicamente dai clienti per fini di privacy)
'''
@app.get("/chat/{chat_key}")
async def get_chat_content(chat_key: str):
    ## Parsing in JSON del contenuto sottoforma di JSON String in Base64.
    url = "http://localhost:8080/api/v1/cache/get?id="+chat_key
    response = requests.get(url)
    if response.status_code == 200:
        return json.loads(base64.b64decode(str(response.content).split("'")[1]))
    else:
        print('Errore nella chiamata API:', response.status_code)

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
            print(message)
            '''
                La connessione viene chiusa in modo corretto, senza causare il lancio di eccezioni lato Java, quando viene
                lanciato il comando /exit
            '''
            if message == "/exit":
                tn.close()

    except Exception as e:
        print(f"Errore durante la comunicazione Telnet: {e}")
# Carica la chiave API da un file
def load_api_key(file_path):
    with open(file_path, "r") as file:
        api_key = file.read().strip()
    return api_key

# Configura l'API client per OpenAI con la chiave API
api_key = load_api_key("proxy/openaikey")
client = OpenAI(api_key=api_key)

# Funzione per ottenere la risposta di GPT-4 utilizzando la cronologia dei messaggi
def get_gpt_response(messages):
    response = client.chat.completions.create(
        model="gpt-4",
        messages=messages
    )
    return response.choices[0].message.content

app = FastAPI()

'''
Endpoint WEBSOCKET per il collegamento al sistema di chat GPT-4.
Inizia con un contesto predefinito per supportare gli utenti di JustArt.
'''
@app.websocket("/gpt")
async def websocket_gpt(websocket: WebSocket):
    await websocket.accept()

    # Storico della conversazione, inizializzato con il contesto fornito
    conversation_history = [
        {"role": "system", "content": "vorrei che tu impersonificassi un assistente virtuale per una chat di supporto del sito JustArt. JustArt gestisce eventi artistici per promoter, artisti e clienti. Ecco le informazioni chiave da tenere a mente:\n\nMissione di JustArt: JustArt è una piattaforma che facilita la gestione di eventi artistici, offrendo supporto logistico e promozionale per promoter, artisti e clienti.\n\nServizi offerti:\n\nPer i Promoter: Supporto nella pianificazione, gestione dei biglietti, promozione dell'evento.\n\nPer gli Artisti: Assistenza con la logistica dell'evento, promozione e gestione delle richieste.\n\nPer i Clienti: Acquisto di biglietti, informazioni sugli eventi e supporto post-acquisto.\n\nTono e stile: Cordiale, professionale e disponibile.\n\nLa chat è appena iniziata. Rispondi con \"Ciao, come posso aiutarti?\""},
        {"role": "assistant", "content": "Ciao, come posso aiutarti?"}
    ]

    print("Inizia la conversazione con l'assistente JustArt (scrivi 'exit' per terminare):")

    while True:
        user_input = await websocket.receive_text()
        if user_input.lower() == "exit":
            break

        conversation_history.append({"role": "user", "content": user_input})

        response = get_gpt_response(conversation_history)
        await websocket.send_text(response)

        conversation_history.append({"role": "assistant", "content": response})

    await websocket.close()

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)