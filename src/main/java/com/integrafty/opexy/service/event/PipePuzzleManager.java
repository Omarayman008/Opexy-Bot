package com.integrafty.opexy.service.event;

import com.integrafty.opexy.service.EconomyService;
import com.integrafty.opexy.service.LogManager;
import com.integrafty.opexy.utils.EmbedUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.util.*;

@Service
public class PipePuzzleManager extends ListenerAdapter {

    private final Map<Long, char[][]> games = new HashMap<>();
    private final Map<Long, int[]> cursors = new HashMap<>();
    private final Map<Long, Integer> gameSizes = new HashMap<>();
    private final AchievementService achievementService;
    private final EconomyService economyService;
    private final LogManager logManager;
    private final Random random = new Random();

    public PipePuzzleManager(AchievementService achievementService, EconomyService economyService, LogManager logManager) {
        this.achievementService = achievementService;
        this.economyService = economyService;
        this.logManager = logManager;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();
        if (id == null || !id.startsWith("pipe_"))
            return;

        long userId = event.getUser().getIdLong();
        if (!games.containsKey(userId)) {
            event.reply("❌ انتهت صلاحية هذه اللعبة أو تم إعادة تشغيل البوت. يرجى البدء بلعبة جديدة.").setEphemeral(true).queue();
            return;
        }

        char[][] grid = games.get(userId);
        int[] cursor = cursors.get(userId);
        
        if (grid == null || cursor == null) return;

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
            handleSubmission(event, userId, grid);
        }
    }

    private void handleSubmission(ButtonInteractionEvent event, long userId, char[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;

        if (!canConnect(grid[0][0], 0)) {
            event.reply("❌ البداية غير موصلة! تأكد من أن الأنبوب الأول متصل بـ 🏁").setEphemeral(true).queue();
            return;
        }
        if (!canConnect(grid[rows - 1][cols - 1], 2)) {
            event.reply("❌ النهاية غير موصلة! تأكد من أن الأنبوب الأخير متصل بـ 🚩").setEphemeral(true).queue();
            return;
        }

        boolean[][] visited = new boolean[rows][cols];
        if (hasPath(grid, 0, 0, rows - 1, cols - 1, visited)) {
            int size = gameSizes.getOrDefault(userId, 3);
            long reward = size == 3 ? 35 : size == 4 ? 50 : 80;
            
            games.remove(userId);
            cursors.remove(userId);
            gameSizes.remove(userId);
            
            String guildId = event.getGuild() != null ? event.getGuild().getId() : "global";
            economyService.addBalance(String.valueOf(userId), guildId, reward);

            event.getHook().editOriginal(new net.dv8tion.jda.api.utils.messages.MessageEditBuilder()
                    .setContent(event.getUser().getAsMention())
                    .setEmbeds(com.integrafty.opexy.utils.EmbedUtil.successEmbed("تم الحل", "🎉 مبروك! لقد قمت بحل اللغز بنجاح!\n💰 الجائزة: **" + reward + "** Opex"))
                    .setComponents(Collections.emptyList())
                    .build()).queue();
            
            if (event.getGuild() != null) {
                achievementService.updateStats(userId, event.getGuild(), s -> s.setPipeWins(s.getPipeWins() + 1));
            }

            // LOGGING
            String diffStr = size == 3 ? "سهل" : size == 4 ? "متوسط" : "صعب";
            String logDetails = String.format("### 🎮 فعالية الأنابيب: فوز\n▫️ **اللاعب:** <@%d>\n▫️ **الصعوبة:** %s\n▫️ **الجائزة:** %d opex", 
                    userId, diffStr, reward);
            logManager.logEmbed(event.getGuild(), LogManager.LOG_GAMES, 
                    EmbedUtil.createOldLogEmbed("pipes", logDetails, null, net.dv8tion.jda.api.entities.UserSnowflake.fromId(userId), null, EmbedUtil.SUCCESS));
        } else {
            event.reply("❌ المسار غير مكتمل! تأكد من أن جميع الأنابيب متصلة ببعضها البعض.").setEphemeral(true).queue();
        }
    }

    public String startNewGame(long userId, int size, net.dv8tion.jda.api.entities.Guild guild) {
        char[][] grid = generateGrid(size);
        games.put(userId, grid);
        cursors.put(userId, new int[] { 1, 1 });
        gameSizes.put(userId, size);

        // LOGGING
        String diffStr = size == 3 ? "سهل" : size == 4 ? "متوسط" : "صعب";
        String logDetails = String.format("### 🎮 فعالية الأنابيب: بدء لعبة\n▫️ **اللاعب:** <@%d>\n▫️ **الصعوبة:** %s", userId, diffStr);
        logManager.logEmbed(guild, LogManager.LOG_GAMES, 
                EmbedUtil.createOldLogEmbed("pipes", logDetails, null, net.dv8tion.jda.api.entities.UserSnowflake.fromId(userId), null, EmbedUtil.INFO));

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
        
        // 1. Fill everything with empty spaces for a clean look
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid[i][j] = ' ';
            }
        }

        // 2. Generate a random path from (0,0) to (size-1, size-1)
        List<int[]> path = new ArrayList<>();
        int currR = 0, currC = 0;
        path.add(new int[]{currR, currC});
        
        while (currR < size - 1 || currC < size - 1) {
            if (random.nextBoolean() && currR < size - 1) currR++;
            else if (currC < size - 1) currC++;
            else currR++;
            path.add(new int[]{currR, currC});
        }

        // 3. Place pipes ONLY along the path
        for (int k = 0; k < path.size(); k++) {
            int[] curr = path.get(k);
            int prevR = (k == 0) ? -1 : path.get(k-1)[0];
            int prevC = (k == 0) ? 0 : path.get(k-1)[1];
            int nextR = (k == path.size() - 1) ? curr[0] + 1 : path.get(k+1)[0];
            int nextC = (k == path.size() - 1) ? curr[1] : path.get(k+1)[1];
            grid[curr[0]][curr[1]] = getPieceConnecting(prevR - curr[0], prevC - curr[1], nextR - curr[0], nextC - curr[1]);
        }
        
        // 4. Scramble ONLY the path pieces (others remain empty)
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (grid[i][j] == ' ') continue;
                int rotations = random.nextInt(4);
                for (int r = 0; r < rotations; r++) {
                    grid[i][j] = rotatePiece(grid[i][j]);
                }
            }
        }
        return grid;
    }

    private char getPieceConnecting(int dr1, int dc1, int dr2, int dc2) {
        // dr, dc relative to center: -1,0=Top, 0,1=Right, 1,0=Bottom, 0,-1=Left
        boolean t = (dr1 == -1 || dr2 == -1);
        boolean r = (dc1 == 1 || dc2 == 1);
        boolean b = (dr1 == 1 || dr2 == 1);
        boolean l = (dc1 == -1 || dc2 == -1);
        
        if (t && b) return '║';
        if (r && l) return '═';
        if (r && b) return '╔';
        if (l && b) return '╗';
        if (l && t) return '╝';
        if (r && t) return '╚';
        return '║'; // Fallback
    }

    private char rotatePiece(char piece) {
        return switch (piece) {
            case '═' -> '║';
            case '║' -> '═';
            case '╔' -> '╗';
            case '╗' -> '╝';
            case '╝' -> '╚';
            case '╚' -> '╔';
            default -> piece;
        };
    }

    public String renderGrid(char[][] grid, int cursorR, int cursorC) {
        StringBuilder sb = new StringBuilder("```ansi\n");
        sb.append("🏁\n");
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                if (i == cursorR && j == cursorC) {
                    // Red background for cursor
                    sb.append("\u001B[41m").append(grid[i][j] == ' ' ? '·' : grid[i][j]).append("\u001B[0m");
                } else {
                    if (grid[i][j] == ' ') {
                        // Light gray dots for empty spaces (ANSI 2;37m is dim white/gray)
                        sb.append("\u001B[2;37m·\u001B[0m");
                    } else {
                        sb.append(grid[i][j]);
                    }
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
        if (!canConnect(grid[0][0], 0)) return false;
        if (!canConnect(grid[rows-1][cols-1], 2)) return false;
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
            case '═' -> port == 1 || port == 3;
            case '║' -> port == 0 || port == 2;
            case '╔' -> port == 1 || port == 2;
            case '╗' -> port == 2 || port == 3;
            case '╝' -> port == 0 || port == 3;
            case '╚' -> port == 0 || port == 1;
            default -> false;
        };
    }
}
