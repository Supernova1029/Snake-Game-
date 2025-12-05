package ta.tugasakhir;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class SnakeGame extends Application {

    private static final int CELL_SIZE = 28;
    private static final int GAME_WIDTH = 20;
    private static final int GAME_HEIGHT = 15;
    private static final int WINDOW_WIDTH = 900;
    private static final int WINDOW_HEIGHT = 650;
    private static final int INITIAL_SPEED = 150;
    private static final int ROCK_SPAWN_INTERVAL = 10;

    private Canvas gameCanvas;
    private GraphicsContext gc;
    private Label scoreLabel;
    private Label highScoreLabel;
    private Button pauseButton;
    private Button restartButton;
    private Button exitButton;
    private StackPane gameOverPane;
    private VBox statsPanel;
    private BorderPane root;

    private LinkedList<Point> snake = new LinkedList<>();
    private List<Point> foods = new ArrayList<>();
    private List<Point> rocks = new ArrayList<>();
    private Direction currentDirection = Direction.RIGHT;
    private Direction nextDirection = Direction.RIGHT;
    private boolean gameRunning = false;
    private boolean gamePaused = false;
    private boolean gameOver = false;
    private int score = 0;
    private int highScore = 0;
    private int speed = INITIAL_SPEED;
    private int foodsEaten = 0;
    private Timeline gameLoop;
    private Timeline rockSpawnTimer;
    private Random random = new Random();

    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    private static class Point {
        int x, y;
        Color color;
        boolean special;
        int points;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
            this.color = Color.RED;
            this.special = false;
            this.points = 10;
        }

        Point(int x, int y, Color color, boolean special, int points) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.special = special;
            this.points = points;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        root = new BorderPane();
        root.getStyleClass().add("root");

        root.setOnKeyPressed(event -> {
            handleKeyPress(event.getCode());
        });

        setupHeader();
        setupGameArea();
        setupStatsPanel();
        setupFooter();

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

        primaryStage.setTitle("Google Snake Game");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.show();

        setupGame();
        startGame();
        startRockSpawner();
    }

    private void handleKeyPress(KeyCode code) {
        if (gameOver) {
            if (code == KeyCode.R || code == KeyCode.SPACE || code == KeyCode.ENTER) {
                resetGame();
            }
            return;
        }

        switch (code) {
            case UP:
            case W:
                if (currentDirection != Direction.DOWN) nextDirection = Direction.UP;
                break;
            case DOWN:
            case S:
                if (currentDirection != Direction.UP) nextDirection = Direction.DOWN;
                break;
            case LEFT:
            case A:
                if (currentDirection != Direction.RIGHT) nextDirection = Direction.LEFT;
                break;
            case RIGHT:
            case D:
                if (currentDirection != Direction.LEFT) nextDirection = Direction.RIGHT;
                break;
            case SPACE:
                togglePause();
                break;
            case R:
                resetGame();
                break;
            case ESCAPE:
                System.exit(0);
                break;
        }
    }

    private void setupHeader() {
        HBox header = new HBox();
        header.getStyleClass().add("header");
        HBox.setHgrow(header, Priority.ALWAYS);

        HBox leftSection = new HBox();
        leftSection.setAlignment(Pos.CENTER_LEFT);
        leftSection.setPadding(new Insets(0, 0, 0, 20));

        Label title = new Label("GOOGLE SNAKE");
        title.getStyleClass().add("title-label");
        leftSection.getChildren().add(title);

        HBox rightSection = new HBox(10);
        rightSection.setAlignment(Pos.CENTER_RIGHT);
        rightSection.setPadding(new Insets(0, 20, 0, 0));

        pauseButton = new Button("PAUSE");
        pauseButton.getStyleClass().add("control-button");
        pauseButton.setOnAction(e -> togglePause());

        restartButton = new Button("RESTART");
        restartButton.getStyleClass().add("control-button");
        restartButton.setOnAction(e -> resetGame());

        exitButton = new Button("EXIT");
        exitButton.getStyleClass().add("control-button");
        exitButton.setOnAction(e -> System.exit(0));

        rightSection.getChildren().addAll(pauseButton, restartButton, exitButton);

        header.getChildren().addAll(leftSection, rightSection);
        root.setTop(header);
    }

    private void setupGameArea() {
        StackPane gameAreaContainer = new StackPane();
        gameAreaContainer.getStyleClass().add("game-container");
        gameAreaContainer.setPadding(new Insets(10));

        Rectangle border = new Rectangle(GAME_WIDTH * CELL_SIZE + 4, GAME_HEIGHT * CELL_SIZE + 4);
        border.setFill(Color.TRANSPARENT);
        border.setStroke(LinearGradient.valueOf("linear-gradient(from 0% 0% to 100% 100%, #34a853 0%, #4285f4 50%, #ea4335 100%)"));
        border.setStrokeWidth(4);
        border.setArcWidth(15);
        border.setArcHeight(15);

        gameCanvas = new Canvas(GAME_WIDTH * CELL_SIZE, GAME_HEIGHT * CELL_SIZE);
        gc = gameCanvas.getGraphicsContext2D();

        gameOverPane = new StackPane();
        gameOverPane.setVisible(false);
        gameOverPane.setPrefSize(GAME_WIDTH * CELL_SIZE, GAME_HEIGHT * CELL_SIZE);
        gameOverPane.getStyleClass().add("game-over-pane");

        VBox gameOverContent = new VBox(15);
        gameOverContent.setAlignment(Pos.CENTER);
        gameOverContent.setPadding(new Insets(20));

        Label gameOverLabel = new Label("GAME OVER");
        gameOverLabel.getStyleClass().add("game-over-label");

        Label finalScore = new Label("SCORE: 0");
        finalScore.getStyleClass().add("final-score-label");
        finalScore.setId("finalScore");

        Label restartHint = new Label("Press SPACE or R to restart");
        restartHint.getStyleClass().add("hint-label");

        Button restartGameButton = new Button("PLAY AGAIN");
        restartGameButton.getStyleClass().add("restart-button");
        restartGameButton.setOnAction(e -> resetGame());

        gameOverContent.getChildren().addAll(gameOverLabel, finalScore, restartHint, restartGameButton);
        gameOverPane.getChildren().add(gameOverContent);

        gameAreaContainer.getChildren().addAll(border, gameCanvas, gameOverPane);

        HBox center = new HBox(30);
        center.getStyleClass().add("center-container");
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(20));
        center.getChildren().add(gameAreaContainer);

        root.setCenter(center);
    }

    private void setupStatsPanel() {
        statsPanel = new VBox(12);
        statsPanel.getStyleClass().add("stats-panel");
        statsPanel.setPadding(new Insets(20));
        statsPanel.setPrefWidth(250);

        Label statsTitle = new Label("GAME STATS");
        statsTitle.getStyleClass().add("stats-title");

        scoreLabel = new Label("SCORE: 0");
        scoreLabel.getStyleClass().add("stat-label");

        highScoreLabel = new Label("HIGH SCORE: 0");
        highScoreLabel.getStyleClass().add("stat-label");

        VBox controlsBox = new VBox(8);
        controlsBox.getStyleClass().add("controls-box");
        controlsBox.setPadding(new Insets(15, 0, 0, 0));

        Label controlsTitle = new Label("CONTROLS");
        controlsTitle.getStyleClass().add("controls-title");

        Label upControl = new Label("↑ W   - MOVE UP");
        upControl.getStyleClass().add("control-item");

        Label downControl = new Label("↓ S   - MOVE DOWN");
        downControl.getStyleClass().add("control-item");

        Label leftControl = new Label("← A   - MOVE LEFT");
        leftControl.getStyleClass().add("control-item");

        Label rightControl = new Label("→ D   - MOVE RIGHT");
        rightControl.getStyleClass().add("control-item");

        Label pauseControl = new Label("SPACE - PAUSE/RESUME");
        pauseControl.getStyleClass().add("control-item");

        Label restartControl = new Label("R     - RESTART");
        restartControl.getStyleClass().add("control-item");

        Label exitControl = new Label("ESC   - EXIT GAME");
        exitControl.getStyleClass().add("control-item");

        controlsBox.getChildren().addAll(controlsTitle, upControl, downControl, leftControl, rightControl,
                pauseControl, restartControl, exitControl);

        statsPanel.getChildren().addAll(statsTitle, scoreLabel, highScoreLabel, controlsBox);

        HBox center = (HBox) root.getCenter();
        center.getChildren().add(statsPanel);
    }

    private void setupFooter() {
        HBox footer = new HBox();
        footer.getStyleClass().add("footer");
        footer.setAlignment(Pos.CENTER);

        Label footerLabel = new Label("Use arrow keys or WASD to move | Collect food to grow | Avoid walls and rocks");
        footerLabel.getStyleClass().add("footer-label");

        footer.getChildren().add(footerLabel);
        root.setBottom(footer);
    }

    private void setupGame() {
        snake.clear();
        foods.clear();
        rocks.clear();

        for (int i = 0; i < 3; i++) {
            snake.add(new Point(5 - i, 7));
        }

        spawnInitialRocks();
        spawnFood();
        spawnSpecialFood();

        gameRunning = true;
        gameOver = false;
        gamePaused = false;
        score = 0;
        speed = INITIAL_SPEED;
        foodsEaten = 0;
        currentDirection = Direction.RIGHT;
        nextDirection = Direction.RIGHT;

        updateStats();
        drawGame();
    }

    private void spawnInitialRocks() {
        for (int i = 0; i < 5; i++) {
            spawnRock();
        }
    }

    private void startGame() {
        if (gameLoop != null) {
            gameLoop.stop();
        }

        gameLoop = new Timeline(new KeyFrame(Duration.millis(speed), e -> {
            if (!gamePaused && !gameOver) {
                updateGame();
            }
        }));
        gameLoop.setCycleCount(Timeline.INDEFINITE);
        gameLoop.play();
    }

    private void startRockSpawner() {
        if (rockSpawnTimer != null) {
            rockSpawnTimer.stop();
        }

        rockSpawnTimer = new Timeline(new KeyFrame(Duration.seconds(ROCK_SPAWN_INTERVAL), e -> {
            if (!gamePaused && !gameOver && gameRunning) {
                spawnRock();
                drawGame();
            }
        }));
        rockSpawnTimer.setCycleCount(Timeline.INDEFINITE);
        rockSpawnTimer.play();
    }

    private void updateGame() {
        if (!gameRunning || gamePaused || gameOver) return;

        currentDirection = nextDirection;
        Point head = snake.getFirst();
        Point newHead = new Point(head.x, head.y);

        switch (currentDirection) {
            case UP: newHead.y--; break;
            case DOWN: newHead.y++; break;
            case LEFT: newHead.x--; break;
            case RIGHT: newHead.x++; break;
        }

        if (checkCollision(newHead)) {
            gameOver();
            return;
        }

        snake.addFirst(newHead);

        boolean ateFood = false;
        Point foodToRemove = null;

        for (Point food : foods) {
            if (newHead.x == food.x && newHead.y == food.y) {
                score += food.points;
                foodsEaten++;
                ateFood = true;
                foodToRemove = food;

                if (food.special) {
                    score += 50;
                    spawnSpecialFood();
                } else {
                    if (foods.size() < 3) {
                        spawnFood();
                    }
                }
                break;
            }
        }

        if (foodToRemove != null) {
            foods.remove(foodToRemove);
        }

        if (!ateFood) {
            snake.removeLast();
        }

        updateSpeed();
        updateStats();
        drawGame();
    }

    private void drawGame() {
        gc.clearRect(0, 0, gameCanvas.getWidth(), gameCanvas.getHeight());

        drawBackground();
        drawRocks();
        drawFoods();
        drawSnake();
    }

    private void drawBackground() {
        for (int x = 0; x < GAME_WIDTH; x++) {
            for (int y = 0; y < GAME_HEIGHT; y++) {
                Color color;
                if ((x + y) % 2 == 0) {
                    color = Color.rgb(162, 209, 73);
                } else {
                    color = Color.rgb(170, 215, 81);
                }

                gc.setFill(color);
                gc.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
    }

    private void drawSnake() {
        for (int i = 0; i < snake.size(); i++) {
            Point segment = snake.get(i);

            double x = segment.x * CELL_SIZE;
            double y = segment.y * CELL_SIZE;

            if (i == 0) {
                drawSnakeHead(x, y);
            } else if (i == snake.size() - 1) {
                drawSnakeTail(x, y, i);
            } else {
                drawSnakeBody(x, y, i);
            }
        }
    }

    private void drawSnakeHead(double x, double y) {
        double size = CELL_SIZE - 2;

        RadialGradient headGradient = new RadialGradient(
                0, 0, 0.3, 0.3, 0.7, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(76, 175, 80)),
                new Stop(0.6, Color.rgb(56, 142, 60)),
                new Stop(1, Color.rgb(27, 94, 32))
        );

        gc.setFill(headGradient);
        gc.fillRoundRect(x + 1, y + 1, size, size, 20, 20);

        gc.setStroke(Color.rgb(27, 94, 32));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x + 1, y + 1, size, size, 20, 20);

        drawSnakeFace(x, y, size);
    }

    private void drawSnakeBody(double x, double y, int index) {
        double size = CELL_SIZE - 2;

        Color bodyColor = Color.rgb(76, 175, 80);
        Color darkColor = Color.rgb(56, 142, 60);

        LinearGradient bodyGradient = new LinearGradient(
                0, 0, 1, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new Stop(0, bodyColor.brighter()),
                new Stop(0.5, bodyColor),
                new Stop(1, darkColor)
        );

        gc.setFill(bodyGradient);
        gc.fillRoundRect(x + 1, y + 1, size, size, 15, 15);

        gc.setStroke(darkColor);
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(x + 1, y + 1, size, size, 15, 15);

        if (index % 2 == 0) {
            gc.setFill(Color.rgb(255, 255, 255, 0.2));
            gc.fillOval(x + 4, y + 4, size - 8, 6);
        }

        gc.setFill(Color.rgb(129, 199, 132));
        gc.fillOval(x + 6, y + 6, size - 12, size - 12);
    }

    private void drawSnakeTail(double x, double y, int index) {
        double size = CELL_SIZE - 2;

        Color tailColor = Color.rgb(76, 175, 80);
        Color darkColor = Color.rgb(56, 142, 60);

        LinearGradient tailGradient = new LinearGradient(
                0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new Stop(0, tailColor),
                new Stop(1, darkColor)
        );

        gc.setFill(tailGradient);

        if (index % 2 == 0) {
            gc.fillRoundRect(x + 1, y + 1, size, size, 20, 20);
            gc.setStroke(darkColor);
            gc.setLineWidth(1.5);
            gc.strokeRoundRect(x + 1, y + 1, size, size, 20, 20);
        } else {
            gc.fillOval(x + 1, y + 1, size, size);
            gc.setStroke(darkColor);
            gc.setLineWidth(1.5);
            gc.strokeOval(x + 1, y + 1, size, size);
        }
    }

    private void drawSnakeFace(double x, double y, double size) {
        double eyeSize = size / 5;
        double pupilSize = eyeSize / 2.5;

        double leftEyeX, leftEyeY, rightEyeX, rightEyeY;

        switch (currentDirection) {
            case RIGHT:
                leftEyeX = x + size - eyeSize * 2.2;
                leftEyeY = y + size / 3;
                rightEyeX = x + size - eyeSize * 2.2;
                rightEyeY = y + size - size / 3 - eyeSize;
                break;
            case LEFT:
                leftEyeX = x + eyeSize * 1.2;
                leftEyeY = y + size / 3;
                rightEyeX = x + eyeSize * 1.2;
                rightEyeY = y + size - size / 3 - eyeSize;
                break;
            case UP:
                leftEyeX = x + size / 3;
                leftEyeY = y + eyeSize * 1.2;
                rightEyeX = x + size - size / 3 - eyeSize;
                rightEyeY = y + eyeSize * 1.2;
                break;
            default: // DOWN
                leftEyeX = x + size / 3;
                leftEyeY = y + size - eyeSize * 2.2;
                rightEyeX = x + size - size / 3 - eyeSize;
                rightEyeY = y + size - eyeSize * 2.2;
                break;
        }

        drawEye(leftEyeX, leftEyeY, eyeSize, pupilSize);
        drawEye(rightEyeX, rightEyeY, eyeSize, pupilSize);

        drawMouth(x, y, size);
    }

    private void drawEye(double x, double y, double eyeSize, double pupilSize) {
        gc.setFill(Color.WHITE);
        gc.fillOval(x, y, eyeSize, eyeSize);

        gc.setStroke(Color.rgb(100, 100, 100, 0.3));
        gc.setLineWidth(0.8);
        gc.strokeOval(x, y, eyeSize, eyeSize);

        gc.setFill(Color.rgb(30, 30, 30));
        gc.fillOval(x + eyeSize/2 - pupilSize/2, y + eyeSize/2 - pupilSize/2, pupilSize, pupilSize);

        gc.setFill(Color.WHITE);
        gc.fillOval(x + eyeSize/3, y + eyeSize/3, pupilSize/3, pupilSize/3);
    }

    private void drawMouth(double x, double y, double size) {
        double mouthWidth = size * 0.4;
        double mouthHeight = size * 0.15;
        double mouthY = y + size * 0.7;

        switch (currentDirection) {
            case RIGHT:
                gc.setStroke(Color.rgb(27, 94, 32));
                gc.setLineWidth(1.8);
                gc.strokeArc(x + size/2, mouthY, mouthWidth, mouthHeight, 0, -180, javafx.scene.shape.ArcType.OPEN);
                break;
            case LEFT:
                gc.setStroke(Color.rgb(27, 94, 32));
                gc.setLineWidth(1.8);
                gc.strokeArc(x + size/2 - mouthWidth, mouthY, mouthWidth, mouthHeight, 0, 180, javafx.scene.shape.ArcType.OPEN);
                break;
            case UP:
                gc.setStroke(Color.rgb(27, 94, 32));
                gc.setLineWidth(1.8);
                gc.strokeArc(x + size/2 - mouthWidth/2, mouthY - mouthHeight, mouthWidth, mouthHeight, 90, 180, javafx.scene.shape.ArcType.OPEN);
                break;
            default: // DOWN
                gc.setStroke(Color.rgb(27, 94, 32));
                gc.setLineWidth(1.8);
                gc.strokeArc(x + size/2 - mouthWidth/2, mouthY, mouthWidth, mouthHeight, -90, 180, javafx.scene.shape.ArcType.OPEN);
                break;
        }
    }

    private void drawFoods() {
        for (Point food : foods) {
            double x = food.x * CELL_SIZE;
            double y = food.y * CELL_SIZE;
            double size = CELL_SIZE - 6;

            if (food.special) {
                drawStarFood(x, y, size);
            } else {
                drawNormalFood(x, y, size);
            }
        }
    }

    private void drawNormalFood(double x, double y, double size) {
        RadialGradient foodGradient = new RadialGradient(
                0, 0, 0.4, 0.4, 0.8, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 255, 255)),
                new Stop(0.3, Color.rgb(255, 150, 150)),
                new Stop(0.7, Color.rgb(244, 67, 54)),
                new Stop(1, Color.rgb(211, 47, 47))
        );

        gc.setFill(foodGradient);
        gc.fillOval(x + 3, y + 3, size, size);

        gc.setStroke(Color.rgb(183, 28, 28));
        gc.setLineWidth(2);
        gc.strokeOval(x + 3, y + 3, size, size);

        gc.setFill(Color.rgb(255, 255, 255, 0.6));
        gc.fillOval(x + size/3, y + size/4, size/3, size/4);
    }

    private void drawStarFood(double x, double y, double size) {
        RadialGradient starGradient = new RadialGradient(
                0, 0, 0.4, 0.4, 0.8, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new Stop(0, Color.rgb(255, 255, 220)),
                new Stop(0.4, Color.rgb(255, 235, 59)),
                new Stop(0.7, Color.rgb(255, 193, 7)),
                new Stop(1, Color.rgb(245, 127, 23))
        );

        gc.setFill(starGradient);
        gc.fillOval(x + 3, y + 3, size, size);

        gc.setStroke(Color.rgb(245, 127, 23));
        gc.setLineWidth(2.5);
        gc.strokeOval(x + 3, y + 3, size, size);

        gc.setFill(Color.rgb(255, 255, 255, 0.9));
        drawStarShape(x + 3 + size/2, y + 3 + size/2, size/2.5);
    }

    private void drawStarShape(double centerX, double centerY, double radius) {
        int points = 5;
        double[] xPoints = new double[points * 2];
        double[] yPoints = new double[points * 2];

        for (int i = 0; i < points * 2; i++) {
            double angle = Math.PI / points * i;
            double r = (i % 2 == 0) ? radius : radius / 2;
            xPoints[i] = centerX + Math.cos(angle - Math.PI/2) * r;
            yPoints[i] = centerY + Math.sin(angle - Math.PI/2) * r;
        }

        gc.fillPolygon(xPoints, yPoints, points * 2);
    }

    private void drawRocks() {
        for (Point rock : rocks) {
            double x = rock.x * CELL_SIZE;
            double y = rock.y * CELL_SIZE;
            double size = CELL_SIZE;

            LinearGradient rockGradient = new LinearGradient(
                    0, 0, 1, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(158, 158, 158)),
                    new Stop(0.5, Color.rgb(117, 117, 117)),
                    new Stop(1, Color.rgb(97, 97, 97))
            );

            gc.setFill(rockGradient);
            gc.fillRect(x, y, size, size);

            gc.setStroke(Color.rgb(66, 66, 66));
            gc.setLineWidth(2);
            gc.strokeRect(x, y, size, size);

            gc.setFill(Color.rgb(189, 189, 189, 0.3));
            gc.fillRect(x + 2, y + 2, size - 4, 4);
            gc.fillRect(x + 2, y + 2, 4, size - 4);
        }
    }

    private void spawnFood() {
        while (true) {
            int x = random.nextInt(GAME_WIDTH);
            int y = random.nextInt(GAME_HEIGHT);

            if (isValidPosition(x, y)) {
                foods.add(new Point(x, y));
                break;
            }
        }
    }

    private void spawnSpecialFood() {
        if (foods.size() > 4) return;

        while (true) {
            int x = random.nextInt(GAME_WIDTH);
            int y = random.nextInt(GAME_HEIGHT);

            if (isValidPosition(x, y)) {
                foods.add(new Point(x, y, Color.ORANGE, true, 50));
                break;
            }
        }
    }

    private void spawnRock() {
        if (rocks.size() >= 15) return;

        int attempts = 0;
        while (attempts < 50) {
            int x = random.nextInt(GAME_WIDTH);
            int y = random.nextInt(GAME_HEIGHT);

            if (isValidRockPosition(x, y)) {
                rocks.add(new Point(x, y));
                break;
            }
            attempts++;
        }
    }

    private boolean isValidPosition(int x, int y) {
        for (Point segment : snake) {
            if (segment.x == x && segment.y == y) return false;
        }

        for (Point rock : rocks) {
            if (rock.x == x && rock.y == y) return false;
        }

        for (Point food : foods) {
            if (food.x == x && food.y == y) return false;
        }

        return true;
    }

    private boolean isValidRockPosition(int x, int y) {
        if (x < 2 && y < 2) return false;

        for (Point segment : snake) {
            if (segment.x == x && segment.y == y) return false;

            if (Math.abs(segment.x - x) <= 1 && Math.abs(segment.y - y) <= 1) {
                return false;
            }
        }

        for (Point rock : rocks) {
            if (rock.x == x && rock.y == y) return false;
        }

        for (Point food : foods) {
            if (food.x == x && food.y == y) return false;
        }

        return true;
    }

    private boolean checkCollision(Point point) {
        if (point.x < 0 || point.x >= GAME_WIDTH || point.y < 0 || point.y >= GAME_HEIGHT) {
            return true;
        }

        for (int i = 1; i < snake.size(); i++) {
            Point segment = snake.get(i);
            if (segment.x == point.x && segment.y == point.y) {
                return true;
            }
        }

        for (Point rock : rocks) {
            if (rock.x == point.x && rock.y == point.y) {
                return true;
            }
        }

        return false;
    }

    private void updateSpeed() {
        if (foodsEaten % 5 == 0 && foodsEaten > 0) {
            speed = Math.max(80, INITIAL_SPEED - (foodsEaten / 5) * 20);

            if (gameLoop != null) {
                gameLoop.stop();
                startGame();
            }
        }
    }

    private void updateStats() {
        scoreLabel.setText("SCORE: " + score);
        if (score > highScore) {
            highScore = score;
            highScoreLabel.setText("HIGH SCORE: " + highScore);
        }
    }

    private void gameOver() {
        gameRunning = false;
        gameOver = true;

        if (gameLoop != null) {
            gameLoop.stop();
        }

        if (rockSpawnTimer != null) {
            rockSpawnTimer.stop();
        }

        Label finalScore = (Label) gameOverPane.lookup("#finalScore");
        if (finalScore != null) {
            finalScore.setText("SCORE: " + score);
        }

        gameOverPane.setVisible(true);
    }

    private void togglePause() {
        gamePaused = !gamePaused;
        if (gamePaused) {
            pauseButton.setText("RESUME");
        } else {
            pauseButton.setText("PAUSE");
        }
    }

    private void resetGame() {
        gameOverPane.setVisible(false);

        if (rockSpawnTimer != null) {
            rockSpawnTimer.stop();
        }

        setupGame();
        startGame();
        startRockSpawner();

        root.requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}