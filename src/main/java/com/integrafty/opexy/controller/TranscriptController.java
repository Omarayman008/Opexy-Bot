package com.integrafty.opexy.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.integrafty.opexy.entity.TicketEntity;
import com.integrafty.opexy.entity.TicketMessageEntity;
import com.integrafty.opexy.repository.TicketRepository;
import com.integrafty.opexy.repository.TicketMessageRepository;
import com.integrafty.opexy.service.TranscriptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/view/transcript")
@RequiredArgsConstructor
public class TranscriptController {

    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;

    @GetMapping(value = "/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> viewTranscript(@PathVariable String id) {
        Optional<TicketEntity> ticketOpt = ticketRepository.findById(Long.parseLong(id));
        
        List<TicketMessageEntity> messages = ticketMessageRepository.findAllByTicketIdOrderByCreatedAtAsc(id);
        
        if (ticketOpt.isEmpty() || messages.isEmpty()) {
            return ResponseEntity.ok("<body style='background:#0d0e10;color:#e3e5e8;font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh'><h1>Transcript Not Found or Empty</h1></body>");
        }

        TicketEntity ticket = ticketOpt.get();
        JsonArray msgArray = new JsonArray();
        for (TicketMessageEntity msg : messages) {
            JsonObject m = new JsonObject();
            m.addProperty("user_id", msg.getUserId());
            m.addProperty("user_name", msg.getUserName());
            m.addProperty("content", msg.getContent());
            m.addProperty("created_at", msg.getCreatedAt().toInstant(ZoneOffset.UTC).toString());
            msgArray.add(m);
        }

        byte[] html = TranscriptService.buildHtml(
            id, 
            "case-" + id, 
            "SUPPORT", 
            ticket.getStatus(), 
            ticket.getCreatedAt().toInstant(ZoneOffset.UTC).toString(), 
            "User-" + ticket.getUserId(), 
            ticket.getStaffId() != null ? ticket.getStaffId() : "Not Handled", 
            ticket.getStatus().equals("CLOSED") ? "Staff" : "N/A", 
            msgArray
        );

        return ResponseEntity.ok(new String(html, java.nio.charset.StandardCharsets.UTF_8));
    }
}
