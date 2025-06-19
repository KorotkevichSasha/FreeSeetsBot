package by.freeseatsbybot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import jakarta.annotation.PostConstruct;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class FreeSeatsBYBot extends TelegramLongPollingBot {
    @Value("${bot.username}")
    private String botUsername;
    @Value("${bot.token}")
    private String botToken;
    private final Map<Long, UserSession> userSessions = new HashMap<>();
    private final TrainMonitorService monitorService;
    private final TrainParser trainParser;

    public FreeSeatsBYBot(TrainMonitorService monitorService, TrainParser trainParser) {
        this.monitorService = monitorService;
        this.trainParser = trainParser;
    }

    @PostConstruct
    public void init() {
        monitorService.setSendTextConsumer(this::sendText);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery().getFrom().getId(), update.getCallbackQuery().getData(), update.getCallbackQuery().getMessage().getChatId());
        }
    }

    private void handleMessage(Message message) {
        long chatId = message.getChatId();
        String text = message.getText();
        UserSession session = userSessions.computeIfAbsent(chatId, k -> new UserSession());

        if ("/start".equals(text)) {
            sendMainMenu(chatId);
            session.setState(BotState.MAIN_MENU);
        } else if (session.getState() == BotState.AWAITING_TRAIN_NUMBER) {
            session.setTrainNumber(text.trim());
            sendDateSelection(chatId, true);
            session.setState(BotState.AWAITING_DATE_AFTER_NUMBER);
        } else {
            sendMainMenu(chatId);
            session.setState(BotState.MAIN_MENU);
        }
    }

    private void handleCallback(Long userId, String data, Long chatId) {
        UserSession session = userSessions.computeIfAbsent(chatId, k -> new UserSession());
        if (data.startsWith("main_menu")) {
            sendMainMenu(chatId);
            session.setState(BotState.MAIN_MENU);
        } else if (data.equals("choose_train")) {
            sendFromCitySelection(chatId);
            session.setState(BotState.AWAITING_FROM_CITY);
        } else if (data.startsWith("from_city_")) {
            String fromCity = data.substring(10);
            session.setFromCity(fromCity);
            sendToCitySelection(chatId, fromCity);
            session.setState(BotState.AWAITING_TO_CITY);
        } else if (data.startsWith("to_city_")) {
            String toCity = data.substring(8);
            session.setToCity(toCity);
            sendDateSelection(chatId, false);
            session.setState(BotState.AWAITING_DATE);
        } else if (data.equals("enter_train_number")) {
            sendText(chatId, "Введите номер поезда (например, 748Б):");
            session.setState(BotState.AWAITING_TRAIN_NUMBER);
        } else if (data.startsWith("city_")) {
            // legacy, для совместимости
            String city = data.substring(5);
            session.setFromCity(city);
            session.setToCity("Гомель");
            sendDateSelection(chatId, false);
            session.setState(BotState.AWAITING_DATE);
        } else if (data.startsWith("date_")) {
            String date = data.substring(5);
            session.setDate(date);
            if (session.getState() == BotState.AWAITING_DATE) {
                sendTrainList(chatId, session.getFromCity(), session.getToCity(), date);
                session.setState(BotState.AWAITING_TRAIN_CHOICE);
            } else if (session.getState() == BotState.AWAITING_DATE_AFTER_NUMBER) {
                sendText(chatId, "Мониторинг поезда " + session.getTrainNumber() + " на дату " + date + ". Уведомим, если появятся места!");
                monitorService.startMonitoring(chatId, session.getFromCity(), session.getToCity(), session.getTrainNumber(), date);
                session.setState(BotState.MONITORING);
            }
        } else if (data.startsWith("train_")) {
            String trainNumber = data.substring(6);
            session.setTrainNumber(trainNumber);
            sendText(chatId, "Мониторинг поезда " + trainNumber + " на дату " + session.getDate() + ". Уведомим, если появятся места!");
            monitorService.startMonitoring(chatId, session.getFromCity(), session.getToCity(), trainNumber, session.getDate());
            session.setState(BotState.MONITORING);
        }
    }

    private void sendMainMenu(Long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(InlineKeyboardButton.builder().text("Выбрать поезд").callbackData("choose_train").build());
        row1.add(InlineKeyboardButton.builder().text("Ввести номер поезда").callbackData("enter_train_number").build());
        rows.add(row1);
        markup.setKeyboard(rows);
        sendTextWithKeyboard(chatId, "Привет! Выберите действие:", markup);
    }

    private void sendFromCitySelection(Long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("Минск-Пассажирский").callbackData("from_city_Минск-Пассажирский").build()));
        rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("Гомель").callbackData("from_city_Гомель").build()));
        rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("Брест").callbackData("from_city_Брест").build()));
        rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("Витебск").callbackData("from_city_Витебск").build()));
        rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("Гродно").callbackData("from_city_Гродно").build()));
        rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("Могилев").callbackData("from_city_Могилев").build()));
        markup.setKeyboard(rows);
        sendTextWithKeyboard(chatId, "Выберите город отправления:", markup);
        System.out.println("[Bot] Выбор города отправления");
    }

    private void sendToCitySelection(Long chatId, String fromCity) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        if (!"Минск-Пассажирский".equals(fromCity)) rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("Минск-Пассажирский").callbackData("to_city_Минск-Пассажирский").build()));
        if (!"Гомель".equals(fromCity)) rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("Гомель").callbackData("to_city_Гомель").build()));
        if (!"Брест".equals(fromCity)) rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("Брест").callbackData("to_city_Брест").build()));
        if (!"Витебск".equals(fromCity)) rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("Витебск").callbackData("to_city_Витебск").build()));
        if (!"Гродно".equals(fromCity)) rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("Гродно").callbackData("to_city_Гродно").build()));
        if (!"Могилев".equals(fromCity)) rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("Могилев").callbackData("to_city_Могилев").build()));
        markup.setKeyboard(rows);
        sendTextWithKeyboard(chatId, "Выберите город прибытия:", markup);
        System.out.println("[Bot] Выбор города прибытия для " + fromCity);
    }

    private void sendDateSelection(Long chatId, boolean afterNumber) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        // Сегодня и завтра
        String today = java.time.LocalDate.now().toString();
        String tomorrow = java.time.LocalDate.now().plusDays(1).toString();
        rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("Сегодня").callbackData("date_" + today).build()));
        rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("Завтра").callbackData("date_" + tomorrow).build()));
        // Остальные даты (еще 3 дня)
        for (int i = 2; i <= 4; i++) {
            String date = java.time.LocalDate.now().plusDays(i).toString();
            rows.add(Collections.singletonList(InlineKeyboardButton.builder().text(date).callbackData("date_" + date).build()));
        }
        markup.setKeyboard(rows);
        sendTextWithKeyboard(chatId, afterNumber ? "Выберите дату для мониторинга:" : "Выберите дату отправления:", markup);
    }

    private void sendTrainList(Long chatId, String fromCity, String toCity, String date) {
        System.out.println("[Bot] Получение списка поездов " + fromCity + " → " + toCity + " на " + date);
        java.util.List<String> trains = trainParser.findTrains(fromCity, toCity, date);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        java.util.List<java.util.List<InlineKeyboardButton>> rows = new java.util.ArrayList<>();
        if (trains.isEmpty()) {
            sendText(chatId, "Поездов на выбранную дату не найдено. Попробуйте выбрать другую дату или направление.");
            return;
        }
        for (String train : trains) {
            rows.add(java.util.Collections.singletonList(
                InlineKeyboardButton.builder().text(train).callbackData("train_" + train).build()
            ));
        }
        rows.add(java.util.Collections.singletonList(
            InlineKeyboardButton.builder().text("Другой поезд").callbackData("train_other").build()
        ));
        markup.setKeyboard(rows);
        sendTextWithKeyboard(chatId, "Выберите поезд:", markup);
    }

    public void sendText(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendTextWithKeyboard(Long chatId, String text, InlineKeyboardMarkup markup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setReplyMarkup(markup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
} 
