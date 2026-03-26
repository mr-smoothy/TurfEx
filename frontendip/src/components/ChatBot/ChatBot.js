import React, { useState, useEffect, useRef } from "react";
import "./Chatbot.css";

function ChatBot({ onClose }) {
  const [message, setMessage] = useState("");
  const [chat, setChat] = useState([]);
  const [location, setLocation] = useState(null);
  const [locationFetched, setLocationFetched] = useState(false);
  const chatEndRef = useRef(null);

  // Example quick options
  const quickOptions = [
    "Book Turf",
    "Cancel",
    "Login",
    "Sign Up",
    "Best turf near me",
    "Cheap turf under 1000"
  ];

  // Scrolls to the bottom of the chat automatically
  const scrollToBottom = () => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };

  useEffect(() => {
    scrollToBottom();
  }, [chat]);

  const getCurrentLocation = () => {
    if (locationFetched) {
      return Promise.resolve(location);
    }

    return new Promise((resolve) => {
      if (!navigator.geolocation) {
        setLocationFetched(true);
        resolve(null);
        return;
      }

      navigator.geolocation.getCurrentPosition(
        (position) => {
          const userLocation = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
          };
          setLocation(userLocation);
          setLocationFetched(true);
          resolve(userLocation);
        },
        () => {
          // Permission denied or unavailable; continue chat without location.
          setLocationFetched(true);
          resolve(null);
        },
        {
          enableHighAccuracy: true,
          timeout: 6000,
        }
      );
    });
  };

  const sendMessage = async (msgText) => {
    const textToSend = typeof msgText === "string" ? msgText : message;
    if (!textToSend.trim()) return;

    // Optimistically add user message and temporary bot "..." message
    setChat((prev) => [...prev, { user: textToSend, bot: "..." }]);
    setMessage(""); // Clear input

    try {
      const userLocation = await getCurrentLocation();
      const payload = {
        message: textToSend,
      };

      if (userLocation?.latitude != null && userLocation?.longitude != null) {
        payload.latitude = userLocation.latitude;
        payload.longitude = userLocation.longitude;
      }

      const res = await fetch("http://localhost:8080/api/chat", {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
      });

      const data = await res.json();
      const reply = data.reply || "Sorry, no response from server.";

      // Replace the temporary "..." with actual server reply
      setChat((prev) => {
        const newChat = [...prev];
        newChat[newChat.length - 1].bot = reply;
        return newChat;
      });
    } catch (error) {
      setChat((prev) => {
        const newChat = [...prev];
        newChat[newChat.length - 1].bot = "Error connecting to server.";        
        return newChat;
      });
    }
  };

  // Handler for quick options to send instantly
  const handleQuickOption = (option) => {
    sendMessage(option);
  };

  return (
    <div className="chatbot-container">
      <div className="chatbot-header">
        Turf Explorer Assistant
        <span className="close-btn" onClick={onClose} style={{cursor: 'pointer'}}>X</span>
      </div>

      <div className="chatbot-messages">
        {chat.length === 0 && (
          <div className="chatbot-bot">
            <span>Hello! How can I help you today?</span>
          </div>
        )}
        {chat.map((c, i) => (
          <div key={i}>
            <div className="chatbot-user">
              <span>{c.user}</span>
            </div>
            <div className="chatbot-bot">
              <span>{c.bot}</span>
            </div>
          </div>
        ))}
        <div ref={chatEndRef} />
      </div>

      <div className="chatbot-quick-options">
        {quickOptions.map((opt, idx) => (
          <button key={idx} onClick={() => handleQuickOption(opt)}>
            {opt}
          </button>
        ))}
      </div>

      <div className="chatbot-input-area">
        <input
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && sendMessage(message)}      
          placeholder="Ask something..."
        />
        <button onClick={() => sendMessage(message)}>Send</button>
      </div>
    </div>
  );
}

export default ChatBot;

