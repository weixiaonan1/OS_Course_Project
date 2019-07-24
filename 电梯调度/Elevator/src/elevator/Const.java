package elevator;

public class Const {
    public static class Command{
        public static final int REST = 0;
        public static final int UP = 1;
        public static final int DOWN = -1;
    }

    public static class Icon{
        public static final String REST_ICON= "-";
        public static final String UP_ICON= "↑";
        public static final String DOWN_ICON= "↓";
    }

    public static class FXColor{
        public static final String GREY = "-fx-background-color: #DDDDDD";
        public static final String YELLOW = "-fx-background-color: #FF6600";
    }
}
