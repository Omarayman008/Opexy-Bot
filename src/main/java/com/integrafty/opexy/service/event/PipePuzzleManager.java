package com.integrafty.opexy.service.event;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class PipePuzzleManager extends ListenerAdapter {

    private final Map<Long, char[][]> games = new HashMap<>();
    private final Map<Long, int[]> cursors = new HashMap<>();
    private final AchievementService achievementService;
    private final Random random = new Random();

    public PipePuzzleManager(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (id == null || !id.startsWith("pipe_"))
            return;

        long userId = event.getUser().getIdLong();
        if (!games.containsKey(userId))
            return;

        char[][] grid = games.get(userId);
        int[] cursor = cursors.get(userId);

        if (id.startsWith("pipe_move_")) {
            String dir = id.replace("pipe_move_", "");
            if (dir.equals("up") && cursor[0] > 0)
                cursor[0]--;
            else if (dir.equals("down") && cursor[0] < grid.length - 1)
                cursor[0]++;
            else if (dir.equals("left") && cursor[1] > 0)
                cursor[1]--;
            else if (dir.equals("right") && cursor[1] < grid[0].length - 1)
                cursor[1]++;
            updateGame(event, grid, cursor);
        } else if (id.equals("pipe_rotate")) {
            grid[cursor[0]][cursor[1]] = rotatePiece(grid[cursor[0]][cursor[1]]);
            updateGame(event, grid, cursor);
        } else if (id.equals("pipe_submit")) {
            if (isSolved(grid)) {
                games.remove(userId);
                cursors.remove(userId);
                event.editMessage("🎉 مبروك! لقد قمت بحل اللغز بنجاح!")
                        .setComponents(Collections.emptyList())
                        .queue();
                achievementService.updateStats(userId, event.getGuild(), s -> s.setPipeWins(s.getPipeWins() + 1));
            } else {
                event.reply("❌ اللغز لم يحل بعد، حاول مرة أخرى!").setEphemeral(true).queue();
            }
        }
    }

    public String startNewGame(long userId, int size) {
        char[][] grid = generateGrid(size);
        games.put(userId, grid);
        cursors.put(userId, new int[] { 1, 1 });
        return renderGrid(grid, 1, 1);
    }

    private void updateGame(ButtonInteractionEvent event, char[][] grid, int[] cursor) {
        String renderedGrid = renderGrid(grid, cursor[0], cursor[1]);

        event.editMessage(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                .setComponents(com.integrafty.opexy.utils.EmbedUtil.containerBranded("ENGINEERING",
                        "لغز الأنابيب — قيد الحل",
                        "استخدم الأسهم للتحرك والزر الدائري لتدوير الأنبوب!\n\n" + renderedGrid,
                        com.integrafty.opexy.utils.EmbedUtil.BANNER_MAIN,
                        net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                                net.dv8tion.jda.api.components.buttons.Button.secondary("pipe_move_up", "🔼"),
                                net.dv8tion.jda.api.components.buttons.Button.secondary("pipe_move_down", "🔽"),
                                net.dv8tion.jda.api.components.buttons.Button.secondary("pipe_move_left", "◀️"),
                                net.dv8tion.jda.api.components.buttons.Button.secondary("pipe_move_right", "▶️"),
                                net.dv8tion.jda.api.components.buttons.Button.primary("pipe_rotate", "🔄")),
                        net.dv8tion.jda.api.components.actionrow.ActionRow.of(
                                net.dv8tion.jda.api.components.buttons.Button.success("pipe_submit",
                                        "تحقق من الحل ✅"))))
                .useComponentsV2(true).build())
                .useComponentsV2(true).queue();
    }

    private char[][] generateGrid(int size) {
        char[][] grid = new char[size][size];
        char[] allPieces = {'─', '│', '┌', '┐', '┘', '└'};
        
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid[i][j] = allPieces[random.nextInt(allPieces.length)];
            }
        }

        // Guaranteed path (L-shape for simplicity but reliable)
        for (int j = 0; j < size; j++) grid[0][j] = '─';
        for (int i = 0; i < size; i++) grid[i][size-1] = '│';
        grid[0][size-1] = '┐';
        
        return grid;
    }

    private char rotatePiece(char piece) {
        return switch (piece) {
            case '─' -> '│';
            case '│' -> '─';
            case '┌' -> '┐';
            case '┐' -> '┘';
            case '┘' -> '└';
            case '└' -> '┌';
            default -> piece;
        };
    }

    public String renderGrid(char[][] grid, int cursorR, int cursorC) {
        StringBuilder sb = new StringBuilder("```ansi\n");
        sb.append("🏁\n");
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                if (i == cursorR && j == cursorC) {
                    sb.append("\u001B[7m").append(grid[i][j]).append("\u001B[0m");
                } else {
                    sb.append(grid[i][j]);
                }
            }
            sb.append("\n");
        }
        for (int j = 0; j < grid[0].length - 1; j++) sb.append(" ");
        sb.append("🚩\n");
        sb.append("```");
        return sb.toString();
    }

    private boolean isSolved(char[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;
        boolean[][] visited = new boolean[rows][cols];
        return hasPath(grid, 0, 0, rows - 1, cols - 1, visited);
    }

    private boolean hasPath(char[][] grid, int r, int c, int targetR, int targetC, boolean[][] visited) {
        if (r == targetR && c == targetC) return true;
        visited[r][c] = true;

        int[][] dirs = {{-1, 0, 0, 2}, {0, 1, 1, 3}, {1, 0, 2, 0}, {0, -1, 3, 1}};

        for (int[] d : dirs) {
            int nr = r + d[0];
            int nc = c + d[1];

            if (nr >= 0 && nr < grid.length && nc >= 0 && nc < grid[0].length && !visited[nr][nc]) {
                if (canConnect(grid[r][c], d[2]) && canConnect(grid[nr][nc], d[3])) {
                    if (hasPath(grid, nr, nc, targetR, targetC, visited)) return true;
                }
            }
        }
        return false;
    }

    private boolean canConnect(char piece, int port) {
        // Ports: 0:Top, 1:Right, 2:Bottom, 3:Left
        return switch (piece) {
            case '─' -> port == 1 || port == 3;
            case '│' -> port == 0 || port == 2;
            case '┌' -> port == 1 || port == 2;
            case '┐' -> port == 2 || port == 3;
            case '┘' -> port == 0 || port == 3;
            case '└' -> port == 0 || port == 1;
            default -> false;
        };
    }
}
