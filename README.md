# 🤖 ChatBot Application  
*A full-stack Java + Spring Boot chatbot backend with Docker and MySQL*

---

## 🧠 Overview

This project is a **chatbot backend service** built with **Spring Boot 3**, **Java 17**, and **MySQL**, fully containerized using **Docker Compose**.  
It allows users to **upload and manage chatbot conversation flows** defined in a JSON file and interact with the bot through **REST** and **WebSocket** communication.

The chatbot logic reads structured flows from a JSON configuration stored in the database.  
When a user sends a message, the system detects the intent and replies with preconfigured responses defined inside the JSON flow.

---

## ⚙️ Technologies Used

| Category | Tools / Libraries |
|-----------|------------------|
| **Language** | Java 17 |
| **Framework** | Spring Boot 3.5.7 |
| **Build Tool** | Maven |
| **Database** | MySQL (with persistent Docker volume) |
| **WebSocket** | Spring WebSocket |
| **REST API** | Spring Web |
| **Persistence** | Spring Data JPA + Hibernate |
| **Validation** | Spring Boot Starter Validation |
| **Testing** | JUnit (Spring Boot Starter Test) |
| **Containerization** | Docker & Docker Compose |
| **API Testing** | Postman |

---

## 💬 How the Application Works

1. **Startup**  
   The application boots with Spring Boot and automatically connects to a MySQL container defined in `docker-compose.yml`.

2. **JSON Chat Flow**  
   The chatbot logic is driven by a **JSON file** that defines conversation flows (blocks, intents, and messages).  
   - This JSON is uploaded through a REST API.  
   - The active configuration is saved in the MySQL database.  
   - Every new configuration replaces the previous active one.

3. **REST Endpoints**

   | Method | Endpoint | Description |
   |---------|-----------|-------------|
   | `GET` | `/api/config` | Returns the currently active chatbot JSON configuration |
   | `POST` | `/api/config` | Uploads and activates a new chatbot configuration |

4. **WebSocket Chat**  
   The user connects through WebSocket (`/ws`) and can send messages to the bot.  
   - Messages from the user are received and processed by `ChatController`.  
   - The chatbot detects intents and replies according to the JSON-defined flow.  
   - All interactions happen in real time.

5. **Database Storage**  
   - The chatbot configurations are stored in the `files` table.  
   - Each row contains the JSON data, name, creation time, and a flag for the active configuration.  
   - Data persists even after container restart, thanks to Docker volume mapping.

---
