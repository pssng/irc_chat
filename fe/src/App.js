import React, { useState, useEffect } from "react";
import axios from "axios";
import "./Assets/Chat_me.css";

localStorage.setItem("id", "MTSSLO98R11Z777K");
localStorage.setItem(
  "userGenerals",
  `{"fiscalCode":"MTSSLO98R11Z777K","name":"Carm","surname":"flower","email":"ccc@cdc.com","city":"ccc","address":"ccc","birthDate":"Mon, 03 Ju"}`
);
localStorage.setItem("userRole", "ROLE_CUSTOMER");

const id = localStorage.getItem("id");
const generals = JSON.parse(localStorage.getItem("userGenerals"));
const role_ = localStorage.getItem("userRole");

function App() {
  const [chatKey, setChatKey] = useState("");
  const [messages, setMessages] = useState([]);
  const [message, setMessage] = useState("");
  const [connected, setConnected] = useState(false);
  const [ws, setWs] = useState(null);

  const role = role_ === "ROLE_ADMIN" ? "1" : "0";
  useEffect(() => {
    const newWs = new WebSocket("ws://localhost:8000/chat");
    setWs(newWs);

    newWs.onopen = () => {
      console.log("WebSocket aperto");
      retriveMessages();
      setTimeout(() => {
        newWs.send(`/${id}$$${role}`);
      }, 200);
    };

    newWs.onmessage = (event) => {
      const messageFromServer = event.data;
      console.log("Ricevuto messaggio:", messageFromServer);
      setMessages((prevMessages) => [...prevMessages, messageFromServer]);
    };

    newWs.onclose = () => {
      console.log("WebSocket chiuso");
    };
    return () => {
      newWs.close();
    };
  }, [connected]);

  async function retriveMessages() {
    try {
      await axios
        .get("http://127.0.0.1:8000/chat/" + id)
        .then(
          (resp) => (
            console.log(resp.data),
            setMessages(resp.data.list),
            console.log(messages)
          )
        );
      setConnected(true);
    } catch (error) {
      console.log(error);
    }
  }

  // Funzione che simula una chiamata a un servizio Web
  function callWebService(msg) {
    return new Promise((resolve) => {
      setTimeout(() => {
        ws.send(msg);
      }, 2000);
      setTimeout(() => {
        resolve("Dati trasferiti con successo");
      }, 2000);
    });
  }

  // Funzione asincrona che chiama il servizio Web e attende il completamento del trasferimento dei dati
  async function sendMsg() {
    try {
      console.log("Chiamata al servizio Web in corso...");
      const result = await callWebService(message);
      console.log("Dati ricevuti: ", result);
      console.log("Il trasferimento dei dati è completato.");
    } catch (error) {
      console.error(
        "Si è verificato un errore durante il trasferimento dei dati: ",
        error
      );
    }
  }

  const handleSend = async () => {
    await sendMsg();
    setMessage("");
  };

  const handleChange = (event) => {
    setMessage(event.target.value);
  };

  return id === null ? (
    <>
      <h2>
        {"Utente non trovato. Effettua il login per visualizzare questa pagina"}
      </h2>{" "}
    </>
  ) : (
    <div className="chatme">
      <div className="chatbox">
        <>
          <h3>Chat with Us!</h3>
          <ul>
            {messages &&
              messages.map((msg, index) => (
                <li key={index} align={msg.sender !== id ? "left" : "right"}>
                  <div
                    style={{
                      borderRadius: "20px",
                      padding: "1rem",
                      backgroundColor: msg.sender !== id ? "#fff" : "#dcf8c6",
                      marginLeft: msg.sender !== id ? "" : "auto",
                    }}
                  >
                    <h4>
                      <i>{msg.sender !== id ? "Chat Support" : "You"}</i>
                    </h4>
                    <p>{msg.message}</p>
                  </div>
                </li>
              ))}
          </ul>
          <input
            type="text"
            className="input"
            placeholder="Inserisci il messaggio"
            disabled={ws === null}
            value={message}
            onChange={handleChange}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                handleSend();
              }
            }}
          />
          <button
            disabled={ws === null}
            className="button"
            onClick={handleSend}
          >
            Invia
          </button>
        </>
      </div>
    </div>
  );
}

export default App;
