package by.freeseatsbybot;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
public class TrainParser {
    private static final int MAX_DAYS_AHEAD = 60;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public boolean isDateWithin60Days(String dateStr) {
        try {
            LocalDate now = LocalDate.now();
            LocalDate date = LocalDate.parse(dateStr, FORMATTER);
            long daysBetween = ChronoUnit.DAYS.between(now, date);
            return daysBetween >= 0 && daysBetween <= MAX_DAYS_AHEAD;
        } catch (Exception e) {
            System.out.println("[TrainParser] Ошибка парсинга даты: " + dateStr);
            return false;
        }
    }

    public List<String> findTrains(String fromCity, String toCity, String date) {
        List<String> trains = new ArrayList<>();
        if (fromCity == null || toCity == null || date == null) {
            System.out.println("[TrainParser] Не заданы города или дата");
            return trains;
        }
        if (!isDateWithin60Days(date)) {
            System.out.println("[TrainParser] Дата вне диапазона 60 дней: " + date);
            return trains;
        }
        try {
            String url = String.format("https://pass.rw.by/ru/route/?from=%s&from_exp=&from_esr=&to=%s&to_exp=&to_esr=&front_date=сегодня&date=%s",
                    URLEncoder.encode(fromCity, StandardCharsets.UTF_8),
                    URLEncoder.encode(toCity, StandardCharsets.UTF_8),
                    date);
            System.out.println("[TrainParser] Поиск поездов: " + url);
            Document doc = Jsoup.connect(url).get();
            Elements trainRows = doc.select("div.sch-table__row");
            for (Element row : trainRows) {
                Element trainNumEl = row.selectFirst("div.sch-table__train-number");
                if (trainNumEl != null) {
                    String trainNum = trainNumEl.text().trim();
                    if (!trainNum.isEmpty() && !trains.contains(trainNum)) {
                        trains.add(trainNum);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[TrainParser] Ошибка поиска поездов: " + e.getMessage());
        }
        return trains;
    }

    // Новый метод для проверки мест по прямой ссылке на поезд
    public boolean hasFreeSeatsByTrainNumber(String trainNumber, String fromCity, String toCity, String date) {
        try {
            String url = String.format("https://pass.rw.by/ru/train/?train=%s&from_exp=&to_exp=&date=%s&from=%s&to=%s",
                    URLEncoder.encode(trainNumber, StandardCharsets.UTF_8),
                    date == null ? "" : date,
                    URLEncoder.encode(fromCity, StandardCharsets.UTF_8),
                    URLEncoder.encode(toCity, StandardCharsets.UTF_8));
            System.out.println("[TrainParser] Проверка мест по прямой ссылке: " + url);
            Document doc = Jsoup.connect(url).get();
            Elements places = doc.select("div.train-route__places");
            for (Element place : places) {
                if (!place.text().contains("нет мест")) {
                    System.out.println("[TrainParser] Места найдены для " + trainNumber);
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("[TrainParser] Ошибка проверки мест по номеру: " + e.getMessage());
        }
        System.out.println("[TrainParser] Мест нет для " + trainNumber);
        return false;
    }

    public boolean hasFreeSeats(String fromCity, String toCity, String trainNumber, String date) {
        if (fromCity == null || toCity == null || trainNumber == null || date == null) {
            System.out.println("[TrainParser] Не заданы параметры для проверки мест");
            return false;
        }
        if (!isDateWithin60Days(date)) {
            System.out.println("[TrainParser] Дата вне диапазона 60 дней: " + date);
            return false;
        }
        // Используем прямую ссылку для проверки поезда по номеру
        return hasFreeSeatsByTrainNumber(trainNumber, fromCity, toCity, date);
    }
} 