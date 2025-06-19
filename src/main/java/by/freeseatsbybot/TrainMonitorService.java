package by.freeseatsbybot;

import org.springframework.stereotype.Component;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

@Component
public class TrainMonitorService {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final TrainParser parser;
    private BiConsumer<Long, String> sendTextConsumer;

    public TrainMonitorService(TrainParser parser) {
        this.parser = parser;
    }

    public void setSendTextConsumer(BiConsumer<Long, String> sendTextConsumer) {
        this.sendTextConsumer = sendTextConsumer;
    }

    public void startMonitoring(Long chatId, String fromCity, String toCity, String trainNumber, String date) {
        System.out.println("[Monitor] Запуск мониторинга: " + fromCity + " → " + toCity + ", поезд " + trainNumber + ", дата " + date);
        scheduler.scheduleAtFixedRate(() -> {
            if (parser.hasFreeSeats(fromCity, toCity, trainNumber, date)) {
                System.out.println("[Monitor] Места появились! Оповещаем пользователя " + chatId);
                if (sendTextConsumer != null) {
                    sendTextConsumer.accept(chatId, "Появились свободные места на поезд " + trainNumber + " (" + fromCity + " → " + toCity + ") на дату " + date + "!");
                }
            } else {
                System.out.println("[Monitor] Мест нет для " + trainNumber + " (" + fromCity + " → " + toCity + ") на " + date);
            }
        }, 0, 60, TimeUnit.SECONDS); // Проверять раз в минуту
    }
} 
