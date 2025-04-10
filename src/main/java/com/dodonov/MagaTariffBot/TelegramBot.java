package com.dodonov.MagaTariffBot;

import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    HistoryJsonChatParser historyParser;
    private Long botId;
    private final String botUsername;
    private boolean awaitingJsonFile = false;
    private final Map<Long, UserStats> statsMap = new ConcurrentHashMap<>();

    public TelegramBot(DefaultBotOptions options,
                       String botToken,
                       String botUsername
    ) {
        super(options, botToken);
        this.botUsername = botUsername;
        initBotId();
    }

    private void initBotId() {
        try {
            User botUser = execute(new GetMe());
            this.botId = botUser.getId();
        } catch (TelegramApiException e) {
            throw new RuntimeException("Failed to get bot ID", e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage()) {
            return;
        }

        Message message = update.getMessage();
        User user = message.getFrom();
        Chat chat = message.getChat();
        String text = message.getText();
        Voice voice = message.getVoice();
        Document document = message.getDocument();

        String username = (message.getFrom().getUserName() != null)
                ? "@" + message.getFrom().getUserName()
                : message.getFrom().getFirstName();

        if (!chat.isGroupChat() && !chat.isSuperGroupChat()) {
            return;
        }

        if (user.getIsBot()) {
            return;
        }

        if (awaitingJsonFile && document != null) {
            awaitingJsonFile = false;
            String fileName = message.getDocument().getFileName();
            if (fileName.endsWith(".json")) {
                try {
                    File file = downloadFile(execute(new GetFile(document.getFileId())));
                    Map<Long, UserStats> parsedStats = historyParser.parseJsonFile(file);
                    mergeStats(parsedStats);
                    sendText(message.getChatId(), "MAGA-данные из MAGA-файла успешно добавлены!");
                } catch (Exception e) {
                    sendText(message.getChatId(), "Ошибка при обработке MAGA файла: " + e.getMessage());
                }
            } else {
                sendText(message.getChatId(), "Что чёрт возьми ты мне скинул? Мне нужен файл формата MAGA.json. Попробуй снова, у тебя обязательно получится!");
            }
        }

        if (text != null){
            if (text.equalsIgnoreCase("!maga-счётчик")) {
                sendStats(message.getChatId());
            } else if (text.equalsIgnoreCase("!maga-справедливость")) {
                sendTariffStatistic(chat.getId(), user.getId());
            } else if (text.equalsIgnoreCase("!maga-актуализация")) {
                awaitingJsonFile = true;
                sendText(chat.getId(), "Нужен MAGA.json для продолжения");
            } else if (text.equalsIgnoreCase("!maga-интро")) {
                introduce(chat.getId());
            } else {
                validateTextMessages(user, username, text);
            }
        } else if (voice != null) {
            validateVoiceMessages(user, username, voice);
        }
    }

    private void introduce(Long chatId) {
        String introText = "👺 ВНИМАНИЕ, ДРУЗЬЯ! Этот чат ПОГРУЗИЛСЯ в ПУЧИНУ НЕСПРАВЕДЛИВОСТИ!!!\n\n"
                + "Наша ВЕЛИКАЯ ЧАТОЭКОНОМИКА СТРАДАЕТ из-за НЕЧЕСТНЫХ ИГРОКОВ!!!\n\n"
                + "КИТАЙСКИЕ БОТЫ, ЕВРОПЕЙСКИЕ ТРОЛЛИ и ФЕЙКОВЫЕ СМИ ЗАХВАТИЛИ наш чат!!!\n\n"
                + "🚨 СЕГОДНЯ МЫ ИСПРАВИМ ВЕСЬ ЭТОТ УЖАС!!!\n\n"
                + "💪 КАЖДЫЙ, кто ОБКРАДЫВАЕТ наш ВЕЛИКИЙ ЧАТ, кто НЕ СОБЛЮДАЕТ ЭКСПОРТНО-ИМПОРТНЫЙ БАЛАНС, БУДЕТ НАКАЗАН и ЗАПЛАТИТ БОЛЬШИЕ, МОЩНЫЕ ПОШЛИНЫ!!!!!\n\n"
                + "MAGA!!!!!!! СДЕЛАЕМ ЧАТ СНОВА ВЕЛИКИМ!!!!!!! 💥";
        sendText(chatId, introText);
    }

    private String makeBold(String text) {
        return "<b>" + text.replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;") + "</b>";
    }

    private void mergeStats(Map<Long, UserStats> parsedStats) {
        parsedStats.forEach((name, newStats) -> {
            Long key = (long) name.hashCode();
            statsMap.compute(key, (k, existingStat) -> {
                if (existingStat == null) {
                    return newStats;
                } else {
                    for (int i = 0; i < newStats.getMessageCount(); i++) {
                        existingStat.incrementMessageCount();
                    }
                    existingStat.addCharacters(newStats.getCharacterCount());
                    return existingStat;
                }
            });
        });
    }

    private void sendTariffStatistic(Long chatId, Long initiatorId) {
        UserStats initiatorStats = statsMap.get(initiatorId);
        if (initiatorStats == null) {
            sendText(chatId, "MAGA-статистика для вас отсутствует! Сударь, начните общаться!");
            return;
        }

        int initiatorExports = initiatorStats.getCharacterCount();
        List<UserStats> otherUsers = statsMap.values().stream()
                .filter(stats ->
                        !stats.getUserId().equals(initiatorId) &&
                                !stats.getUserId().equals(botId)
                )
                .collect(Collectors.toList());

        List<UserStats> tariffTargets = new ArrayList<>();
        List<UserStats> nonTariffTargets = new ArrayList<>();

        for (UserStats user : otherUsers) {
            int userExport = user.getCharacterCount();
            int tradeDeficit = userExport - initiatorExports;

            if (tradeDeficit < 0) {
                tariffTargets.add(user);
            } else {
                nonTariffTargets.add(user);
            }
        }

        tariffTargets.sort((u1, u2) -> {
            double tariff1 = calculateTariff(u1, initiatorExports);
            double tariff2 = calculateTariff(u2, initiatorExports);
            return Double.compare(tariff2, tariff1);
        });

        StringBuilder response = new StringBuilder();
        response.append("🔥 TRUMP-STYLE СПРАВЕДЛИВОСТЬ ДЛЯ ")
                .append(initiatorStats.getFirstName())
                .append(" 🔥\n\n");

        response.append("🚨 ВРАГИ ЧАТА (ЗАВАЛИТЬ ПОШЛИНАМИ):\n");
        if (tariffTargets.isEmpty()) {
            response.append("Все патриоты! Врагов нет!\n");
        } else {
            tariffTargets.forEach(user -> {
                int userExport = user.getCharacterCount();
                int deficit = initiatorExports - userExport;
                double tariff = calculateTariff(user, initiatorExports);

                String username = user.getUsername() != null ?
                        "@" + user.getUsername() : "Аноним";

                response.append(String.format(
                        "▫️ %s (%s) — отстаёт на %d → 🦅 +%.1f%%\n",
                        user.getFirstName(),
                        username,
                        deficit,
                        tariff
                ));
            });
        }

        response.append("\n🕊️ НАШИ СЛОНЯРЫ (БЕЗ ПОШЛИН):\n");
        if (nonTariffTargets.isEmpty()) {
            response.append("Слабые конкуренты! Все враги!\n");
        } else {
            nonTariffTargets.forEach(user -> {
                String username = user.getUsername() != null ?
                        "@" + user.getUsername() : "Аноним";

                response.append(String.format(
                        "▫️ %s (%s) — превосходство %d 🇺🇸\n",
                        user.getFirstName(),
                        username,
                        user.getCharacterCount() - initiatorExports
                ));
            });
        }

        sendText(chatId, response.toString());
    }

    private double calculateTariff(UserStats user, int initiatorExports) {
        int userExport = user.getCharacterCount();
        int deficit = initiatorExports - userExport;
        if (userExport == 0) {
            return 1000.0;
        }
        return ((double) Math.abs(deficit) / userExport) * 100;
    }

    private void sendText(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(makeBold(text));
        sendMessage.setParseMode(ParseMode.HTML);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendStats(Long chatId) {
        StringBuilder statsMessage = new StringBuilder("Статистика MAGA-участников:\n");
        statsMap.forEach((key, userStats) -> {
            statsMessage.append(String.format("%s (%s): сообщений: %d, суммарный MAGA-экспорт: %d\n",
                    userStats.getFirstName(), userStats.getUsername(), userStats.getMessageCount(), userStats.getCharacterCount()));
        });
        sendText(chatId, statsMessage.toString());
    }

    private void validateTextMessages(User user, String username, String text) {
        int charsToAdd = text.replaceAll("\\s", "").length();
        updateUserStats(user, username, charsToAdd);
    }

    private void validateVoiceMessages(User user, String username, Voice voice) {
        int duration = voice.getDuration();
        int charsToAdd = (int) (duration * 7.5);
        updateUserStats(user, username, charsToAdd);
    }

    private void updateUserStats(User user, String username, int charsToAdd) {
        statsMap.compute(user.getId(), (id, userStats) -> {
            if (userStats == null) {
                userStats = new UserStats(user.getId(), username, user.getFirstName(), 0, 0);
            }
            if (username != null && (userStats.getUsername() == null || !userStats.getUsername().equals(username))) {
                userStats.setUsername(username);
            }
            if (user.getFirstName() != null && !user.getFirstName().equals(userStats.getFirstName())) {
                userStats.setFirstName(user.getFirstName());
            }
            userStats.incrementMessageCount();
            userStats.addCharacters(charsToAdd);
            return userStats;
        });
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}