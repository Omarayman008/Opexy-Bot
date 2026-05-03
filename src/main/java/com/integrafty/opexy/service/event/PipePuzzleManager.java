package com.integrafty.opexy.service.event;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class PipePuzzleManager extends ListenerAdapter {

    private final Map<Long, String[][]> activeGrids = new HashMap<>();
    private final Random random = new Random();
    private final String[] pieces = {"═", "║", "╔", "╗", "╚", "╝"};

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (!event.getComponentId().startsWith("pipe_")) return;

        long userId = event.getUser().getIdLong();
        String[][] grid = activeGrids.get(userId);

        if (grid == null) {
            event.reply("❌ لم يتم العثور على لغز نشط لك.").setEphemeral(true).queue();
            return;
        }

        String id = event.getComponentId();
        if (id.startsWith("pipe_rot_")) {
            String[] parts = id.split("_");
            int r = Integer.parseInt(parts[2]);
            int c = Integer.parseInt(parts[3]);
            
            grid[r][c] = rotatePiece(grid[r][c]);
            
            String renderedGrid = renderGrid(grid);
            
            event.editMessage(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                    .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("ENGINEERING", "لغز الأنابيب — قيد الحل", "قم بتوصيل الأنابيب للوصول إلى المخرج!\n\n" + renderedGrid, com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN,
                            net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                                    net.dv8tion.jda.api.components.buttons.Button.secondary("pipe_rot_1_1", "تدوير (1,1) 🔄"),
                                    net.dv8tion.jda.api.components.buttons.Button.secondary("pipe_rot_1_2", "تدوير (1,2) 🔄"),
                                    net.dv8tion.jda.api.components.buttons.Button.success("pipe_submit", "تحقق من الحل ✅")
                            )))
                    .useComponentsV2(true).build())
                    .useComponentsV2(true).queue();
        }
    }

    public String startNewGame(long userId, int size) {
        String[][] grid = new String[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid[i][j] = pieces[random.nextInt(pieces.length)];
            }
        }
        activeGrids.put(userId, grid);
        return renderGrid(grid);
    }

    private String rotatePiece(String piece) {
        return switch (piece) {
            case "═" -> "║";
            case "║" -> "═";
            case "╔" -> "╗";
            case "╗" -> "╝";
            case "╝" -> "╚";
            case "╚" -> "╔";
            default -> piece;
        };
    }

    public String renderGrid(String[][] grid) {
        StringBuilder sb = new StringBuilder("```\n");
        for (String[] row : grid) {
            for (String cell : row) {
                sb.append(cell).append(" ");
            }
            sb.append("\n");
        }
        sb.append("```");
        return sb.toString();
    }
}
