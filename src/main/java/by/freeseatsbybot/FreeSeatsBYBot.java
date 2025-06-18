package by.freeseatsbybot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

public class FreeSeatsBYBot extends TelegramLongPollingBot {
    private static final String BOT_USERNAME = System.getenv("BOT_USERNAME");
    private static final String BOT_TOKEN = System.getenv("BOT_TOKEN");

    private final Map<Long, UserSession> userSessions = new HashMap<>();

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
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
            sendCitySelection(chatId);
            session.setState(BotState.AWAITING_CITY);
        } else if (data.equals("enter_train_number")) {
            sendText(chatId, "Введите номер поезда (например, 748Б):");
            session.setState(BotState.AWAITING_TRAIN_NUMBER);
        } else if (data.startsWith("city_")) {
            String city = data.substring(5);
            session.setCity(city);
            sendDateSelection(chatId, false);
            session.setState(BotState.AWAITING_DATE);
        } else if (data.startsWith("date_")) {
            String date = data.substring(5);
            session.setDate(date);
            if (session.getState() == BotState.AWAITING_DATE) {
                // После выбора города и даты — показать список поездов (заглушка)
                sendTrainList(chatId, session.getCity(), date);
                session.setState(BotState.AWAITING_TRAIN_CHOICE);
            } else if (session.getState() == BotState.AWAITING_DATE_AFTER_NUMBER) {
                // После ввода номера поезда и даты — запуск мониторинга (заглушка)
                sendText(chatId, "Мониторинг поезда " + session.getTrainNumber() + " на дату " + date + ". Уведомим, если появятся места!");
                // TODO: Запустить мониторинг
                session.setState(BotState.MONITORING);
            }
        } else if (data.startsWith("train_")) {
            String trainNumber = data.substring(6);
            session.setTrainNumber(trainNumber);
            sendText(chatId, "Мониторинг поезда " + trainNumber + " на дату " + session.getDate() + ". Уведомим, если появятся места!");
            // TODO: Запустить мониторинг
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

    private void sendCitySelection(Long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("Минск").callbackData("city_Minsk").build()));
        rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("Гомель").callbackData("city_Gomel").build()));
        rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("Другой город").callbackData("city_Other").build()));
        markup.setKeyboard(rows);
        sendTextWithKeyboard(chatId, "Выберите город отправления:", markup);
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

    private void sendTrainList(Long chatId, String city, String date) {
        // Заглушка: список поездов (реализовать парсер позже)
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("748Б").callbackData("train_748Б").build()));
        rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("482Б").callbackData("train_482Б").build()));
        rows.add(Collections.singletonList(InlineKeyboardButton.builder().text("Другой поезд").callbackData("train_other").build()));
        markup.setKeyboard(rows);
        sendTextWithKeyboard(chatId, "Выберите поезд:", markup);
    }

    private void sendText(Long chatId, String text) {
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
