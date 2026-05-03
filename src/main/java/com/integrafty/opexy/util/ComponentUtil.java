package com.integrafty.opexy.util;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import java.util.ArrayList;
import java.util.List;

public class ComponentUtil {
    public static List<ActionRow> splitToRows(List<Button> components) {
        List<ActionRow> rows = new ArrayList<>();
        for (int i = 0; i < components.size(); i += 5) {
            rows.add(ActionRow.of(components.subList(i, Math.min(i + 5, components.size()))));
        }
        return rows;
    }
}
