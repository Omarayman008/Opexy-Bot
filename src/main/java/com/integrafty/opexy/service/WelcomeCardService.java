package com.integrafty.opexy.service;

import net.dv8tion.jda.api.entities.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URL;

@Service
public class WelcomeCardService {
    private static final Logger log = LoggerFactory.getLogger(WelcomeCardService.class);

    public static byte[] generateWelcomeCard(Member member) throws Exception {
        BufferedImage background = null;
        try {
            log.info("Loading background from classpath resources...");
            java.io.InputStream is = WelcomeCardService.class.getResourceAsStream("/welcome.png");
            
            if (is != null) {
                background = ImageIO.read(is);
            }
            
            if (background == null) {
                // Fallback URL from Highcore if local resource fails
                String urlStr = "https://i.imgur.com/Lzun3rb.png";
                log.warn("Resource missing. Attempting emergency remote fetch: [{}]", urlStr);
                
                java.net.URL url = new java.net.URL(urlStr);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(8000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36");
                
                background = ImageIO.read(connection.getInputStream());
            }
        } catch (Exception e) {
            log.error("Resource pipeline failure: {}", e.getMessage());
            throw new Exception("Branding pipeline failure: " + e.getMessage());
        }

        if (background == null) {
            throw new Exception("Background image is null after read attempt.");
        }

        int width = background.getWidth();
        int height = background.getHeight();

        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        // 1. Draw Template
        g.drawImage(background, 0, 0, width, height, null);

        // 2. Avatar
        String avatarUrl = member.getUser().getEffectiveAvatarUrl() + "?size=256";
        BufferedImage avatar = null;
        try {
            java.net.URL url = new java.net.URL(avatarUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            avatar = ImageIO.read(conn.getInputStream());
        } catch (Exception e) {
            log.warn("Failed to load user avatar: {}. Using generic fallback.", e.getMessage());
            avatar = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gAv = avatar.createGraphics();
            gAv.setColor(new Color(212, 175, 55));
            gAv.fillOval(0, 0, 256, 256);
            gAv.dispose();
        }

        // --- THE DESIGNER'S BLUEPRINT ---
        int avatarSize = 642; 
        int avatarX = 409; 
        int avatarY = 198;

        g.setClip(new Ellipse2D.Float(avatarX, avatarY, avatarSize, avatarSize));
        g.drawImage(avatar, avatarX, avatarY, avatarSize, avatarSize, null);
        g.setClip(null);

        // 2. Member Identity Engine
        String name = member.getEffectiveName().toUpperCase(); 
        if (name.length() > 25) name = name.substring(0, 23) + "..";

        int fontSize = 60; 
        g.setFont(new Font("SansSerif", Font.BOLD, fontSize));
        
        java.util.Map<java.awt.font.TextAttribute, Object> attributes = new java.util.HashMap<>();
        attributes.put(java.awt.font.TextAttribute.TRACKING, 0.1);
        g.setFont(g.getFont().deriveFont(attributes));

        FontMetrics metrics = g.getFontMetrics();
        int nameWidth = metrics.stringWidth(name);
        int nameBoxWidth = 2045 - 1204;
        int nameX = 1204 + (nameBoxWidth - nameWidth) / 2; 
        int nameY = 652 + ((725 - 652) / 2) + (metrics.getAscent() / 2) - 5;

        // A. Drop Shadow
        g.setColor(new Color(0, 0, 0, 180));
        g.drawString(name, nameX + 3, nameY + 3);

        // B. Master Golden Gradient
        GradientPaint gp = new GradientPaint(
            nameX, nameY - fontSize, new Color(197, 160, 89), 
            nameX, nameY, new Color(142, 115, 65) 
        );
        g.setPaint(gp);
        g.drawString(name, nameX, nameY);

        // C. Specular Highlight
        g.setColor(new Color(255, 255, 255, 60));
        g.drawString(name, nameX, nameY - 1);

        // D. Subtle Highlight
        g.setColor(new Color(240, 230, 140, 100));
        g.drawString(name, nameX, nameY - 1); 

        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(combined, "png", baos);
        return baos.toByteArray();
    }
}
