package de.matsch.backend.websocket

import org.slf4j.Logger
import org.springframework.stereotype.Component
import org.springframework.web.socket.BinaryMessage
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.BinaryWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

@Component
class ProtobufWebSocketHandler(val logger: Logger) : BinaryWebSocketHandler() {

    val sessions: MutableMap<String, WebSocketSession> = ConcurrentHashMap()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        sessions[session.id] = session
        logger.info("Client connected: ${session.id}")
    }

    override fun handleBinaryMessage(session: WebSocketSession, message: BinaryMessage) {
        val content = String(message.payload.array(), Charsets.UTF_8)
        logger.info("Got message from {}, {}", session.id, content)

        val newMessage = BinaryMessage("$content addendum".toByteArray())
        session.sendMessage(newMessage)
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        sessions.remove(session.id)
        logger.warn("Transport error for session ${session.id}", exception)
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessions.remove(session.id)
        logger.info("Client disconnected: ${session.id}, status: $status")
    }
}