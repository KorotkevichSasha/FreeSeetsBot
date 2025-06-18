package by.freeseatsbybot;

public class UserSession {
    private BotState state = BotState.MAIN_MENU;
    private String city;
    private String date;
    private String trainNumber;

    public BotState getState() {
        return state;
    }
    public void setState(BotState state) {
        this.state = state;
    }
    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
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