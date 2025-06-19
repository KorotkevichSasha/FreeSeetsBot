package by.freeseatsbybot;

public class UserSession {
    private BotState state = BotState.MAIN_MENU;
    private String fromCity;
    private String toCity;
    private String date;
    private String trainNumber;

    public BotState getState() {
        return state;
    }
    public void setState(BotState state) {
        this.state = state;
    }
    public String getFromCity() {
        return fromCity;
    }
    public void setFromCity(String fromCity) {
        this.fromCity = fromCity;
    }
    public String getToCity() {
        return toCity;
    }
    public void setToCity(String toCity) {
        this.toCity = toCity;
    }
    public String getDate() {
        return date;
    }
    public void setDate(String date) {
        this.date = date;
    }
    public String getTrainNumber() {
        return trainNumber;
    }
    public void setTrainNumber(String trainNumber) {
        this.trainNumber = trainNumber;
    }
} 