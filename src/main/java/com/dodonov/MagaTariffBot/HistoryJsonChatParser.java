package com.dodonov.MagaTariffBot;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Service
public class HistoryJsonChatParser {
    public Map<Long, UserStats> parseJsonFile(File file) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(file);
        JsonNode messages = root.get("messages");
        Map<Long, UserStats> parsedStats = new HashMap<>();

        if (messages == null || !messages.isArray()) {
            throw new Exception("MAGA-Файл не содержит MAGA-сообщений или имеет не MAGA.json формат.");
        }

        for (JsonNode msg : messages) {
            if (!msg.has("type") || !msg.get("type").asText().equals("message")) {
                continue;
            }
            if (!msg.has("from_id")) {
                continue;
            }
            String rawId = msg.get("from_id").asText();
            if (rawId.equals("user7853204051")) {
                continue;
            }
            String idStr = rawId.startsWith("user") ? rawId.substring(4) : rawId;
            Long userId = Long.valueOf(idStr);
            String firstName = msg.has("from") ? msg.get("from").asText() : "Unknown";

            int symbols = 0;
            if (msg.has("text")) {
                String text;
                if (msg.get("text").isArray()) {
                    text = concatTextArray(msg.get("text"));
                } else {
                    text = msg.get("text").asText();
                }
                if (text.startsWith("!maga") || text.startsWith("!MAGA")) {
                    continue;
                }
                symbols = text.replaceAll("\\s", "").length();
            }
            else if (msg.has("media_type")
                    && msg.get("media_type").asText().equals("voice")
                    && msg.has("duration_seconds")) {
                int duration = msg.get("duration_seconds").asInt();
                symbols = (int) (duration * 7.5);
            }
            if (symbols == 0) {
                continue;
            }
            final int finalSymbols = symbols;
            parsedStats.compute(userId, (k, stat) -> {
                if (stat == null) {
                    stat = new UserStats(userId, null, firstName, 0, 0);
                }
                stat.incrementMessageCount();
                stat.addCharacters(finalSymbols);
                return stat;
            });
        }
        return parsedStats;
    }

    private String concatTextArray(JsonNode textNode) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode part : textNode) {
            if (part.isTextual()) {
                sb.append(part.asText());
            } else if (part.has("text")) {
                sb.append(part.get("text").asText());
            }
        }
        return sb.toString();
    }
}