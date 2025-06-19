package by.freeseatsbybot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class Main {

    @Autowired
    private ApplicationContext context;

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public CommandLineRunner runBot() {
        return args -> {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            FreeSeatsBYBot bot = context.getBean(FreeSeatsBYBot.class);
            botsApi.registerBot(bot);
            System.out.println("FreeSeatsBYBot запущен!");
        };
    }
}
