package com.integrafty.opexy.utils;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class TranscriptService {

    public static byte[] generateSimpleTranscript(TextChannel channel, List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- TRANSCRIPT FOR CHANNEL: ").append(channel.getName()).append(" ---\n");
        sb.append("Generated on: ").append(java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");

        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);
            sb.append("[").append(msg.getTimeCreated().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("] ");
            sb.append(msg.getAuthor().getEffectiveName()).append(": ");
            sb.append(msg.getContentDisplay());
            if (!msg.getAttachments().isEmpty()) {
                sb.append(" [Attachments: ").append(msg.getAttachments().size()).append("]");
            }
            sb.append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static String generateHtmlTranscript(String tid, String channelName, String type, String status, String opener, List<Message> messages) {
        // Simple HTML structure for now, can be expanded to match Highcore's luxury look
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><title>Transcript #").append(tid).append("</title>");
        html.append("<style>");
        html.append("body { background-color: #0f0f12; color: #e1e1e6; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 40px; }");
        html.append(".header { border-bottom: 2px solid #1c1c21; padding-bottom: 20px; margin-bottom: 30px; }");
        html.append(".message { margin-bottom: 15px; padding: 10px; border-radius: 8px; background-color: #16161a; }");
        html.append(".author { color: #d1d1d6; font-weight: bold; margin-right: 10px; }");
        html.append(".time { color: #52525e; font-size: 0.85em; }");
        html.append(".content { margin-top: 5px; line-height: 1.5; }");
        html.append("</style></head><body>");
        
        html.append("<div class='header'>");
        html.append("<h1>Ticket Transcript: #").append(tid).append("</h1>");
        html.append("<p>Channel: ").append(channelName).append(" | Category: ").append(type).append("</p>");
        html.append("<p>Opener: ").append(opener).append(" | Status: ").append(status).append("</p>");
        html.append("</div>");

        for (int i = messages.size() - 1; i >= 0; i--) {
            Message m = messages.get(i);
            html.append("<div class='message'>");
            html.append("<span class='author'>").append(m.getAuthor().getEffectiveName()).append("</span>");
            html.append("<span class='time'>").append(m.getTimeCreated().format(DateTimeFormatter.ofPattern("HH:mm:ss"))).append("</span>");
            html.append("<div class='content'>").append(m.getContentDisplay()).append("</div>");
            html.append("</div>");
        }

        html.append("</body></html>");
        return html.toString();
    }
}
