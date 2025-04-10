package com.dodonov.MagaTariffBot;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class TelegramBotConfiguration {

    @Bean
    public TelegramBot telegramBot(
            @Value("${bot.token}") String botToken,
            @Value("${bot.username}") String botUsername,
            TelegramBotsApi telegramBotsApi
    ) throws TelegramApiException {
        DefaultBotOptions botOptions = new DefaultBotOptions();
        TelegramBot bot = new TelegramBot(botOptions, botToken, botUsername);
        telegramBotsApi.registerBot(bot);
        return bot;
    }

    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        return new TelegramBotsApi(DefaultBotSession.class);
    }
}
