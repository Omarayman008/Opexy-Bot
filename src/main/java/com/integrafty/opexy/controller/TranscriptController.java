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
        long ticketId;
        try {
            // Clean ID of any trailing junk like )**
            String cleanId = id.replaceAll("[^0-9]", "");
            ticketId = Long.parseLong(cleanId);
        } catch (NumberFormatException e) {
            return ResponseEntity.ok("<body style='background:#0d0e10;color:#e3e5e8;font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh'><h1>Invalid Transcript ID</h1></body>");
        }

        Optional<TicketEntity> ticketOpt = ticketRepository.findById(ticketId);
        
        List<TicketMessageEntity> messages = ticketMessageRepository.findAllByTicketIdOrderByCreatedAtAsc(String.valueOf(ticketId));
        
        if (ticketOpt.isEmpty() || messages.isEmpty()) {
            return ResponseEntity.ok("<body style='background:#0d0e10;color:#e3e5e8;font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh'><h1>Transcript Not Found or Empty</h1></body>");
        }

        TicketEntity ticket = ticketOpt.get();
        String openerName = "Unknown User";
        
        JsonArray msgArray = new JsonArray();
        for (TicketMessageEntity msg : messages) {
            if (msg.getUserId().equals(ticket.getUserId()) && openerName.equals("Unknown User")) {
                openerName = msg.getUserName();
            }
            JsonObject m = new JsonObject();
            m.addProperty("user_id", msg.getUserId());
            m.addProperty("user_name", msg.getUserName());
            m.addProperty("content", msg.getContent());
            m.addProperty("created_at", msg.getCreatedAt().toInstant(ZoneOffset.UTC).toString());
            msgArray.add(m);
        }
        
        // Fallback to first message sender if still unknown
        if (openerName.equals("Unknown User") && !messages.isEmpty()) {
            openerName = messages.get(0).getUserName();
        }

        byte[] html = TranscriptService.buildHtml(
            id, 
            "case-" + id, 
            ticket.getCategory().toUpperCase(), 
            ticket.getStatus(), 
            ticket.getCreatedAt().toInstant(ZoneOffset.UTC).toString(), 
            openerName, 
            ticket.getStaffId() != null ? ticket.getStaffId() : "Not Handled", 
            ticket.getStatus().equals("CLOSED") ? "Staff" : "N/A", 
            msgArray
        );

        return ResponseEntity.ok(new String(html, java.nio.charset.StandardCharsets.UTF_8));
    }
}
