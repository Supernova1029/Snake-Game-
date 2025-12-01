package ta.tugasakhir;

import javafx.animation.*;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.*;

public class SnakeGame extends Application {
    private static final int GRID_SIZE = 20;
    private static final int CELL_SIZE = 30;
    private static final int BASE_GAME_SPEED = 100; // Faster base speed
    private static final int MIN_GAME_SPEED = 50;   // Minimum speed (max FPS)
    private static final int SPEED_INCREASE_INTERVAL = 5; // Increase speed every 5 points

    private GridPane gameGrid;
    private Label scoreLabel;
    private Label gameOverLabel;
    private Label highScoreLabel;
    private Label fpsLabel;
    private StackPane root;
    private StackPane[][] gridCells;

    private List<Position> snake = new ArrayList<>();
    private Position food;
    private Direction currentDirection = Direction.RIGHT;
    private Direction nextDirection = Direction.RIGHT; // Buffer for smooth input
    private boolean gameRunning = false;
    private int score = 0;
    private int highScore = 0;
    private Random random = new Random();

    // FPS tracking
    private long frameCount = 0;
    private long lastFpsUpdate = 0;
    private double currentFps = 0;

    private enum Direction { UP, DOWN, LEFT, RIGHT }

    private static class Position {
        int x, y;
        Position(int x, int y) { this.x = x; this.y = y; }
        boolean equals(Position other) { return this.x == other.x && this.y == other.y; }
    }

    @Override
    public void start(Stage stage) {
        initializeUI();
        setupGame();

        int windowWidth = GRID_SIZE * CELL_SIZE + 100;
        int windowHeight = GRID_SIZE * CELL_SIZE + 200;

        Scene scene = new Scene(root, windowWidth, windowHeight);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        scene.setOnKeyPressed(e -> handleKeyPress(e.getCode()));

        stage.setTitle("🐍 Snake Game - High FPS");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        startGameLoop();
    }

    private void initializeUI() {
        // Top header with scores and FPS
        HBox topHeader = new HBox(20);
        topHeader.getStyleClass().add("header");

        scoreLabel = new Label("🎯 Score: 0");
        scoreLabel.getStyleClass().add("score-label");

        highScoreLabel = new Label("🏆 High Score: 0");
        highScoreLabel.getStyleClass().add("high-score-label");

        fpsLabel = new Label("FPS: 0");
        fpsLabel.getStyleClass().add("fps-label");

        topHeader.getChildren().addAll(scoreLabel, highScoreLabel, fpsLabel);
        topHeader.setAlignment(javafx.geometry.Pos.CENTER);

        // Game grid
        gameGrid = new GridPane();
        gameGrid.getStyleClass().add("game-grid");
        gameGrid.setPrefSize(GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE);
        gameGrid.setMaxSize(GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE);

        gridCells = new StackPane[GRID_SIZE][GRID_SIZE];

        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                StackPane cell = new StackPane();
                cell.getStyleClass().add("cell");
                cell.setPrefSize(CELL_SIZE, CELL_SIZE);
                gameGrid.add(cell, i, j);
                gridCells[i][j] = cell;
            }
        }

        // Center the game grid
        StackPane gameContainer = new StackPane();
        gameContainer.getStyleClass().add("game-container");
        gameContainer.getChildren().add(gameGrid);

        // Game over label
        gameOverLabel = new Label("🎮 GAME OVER 🎮\nPress SPACE to restart");
        gameOverLabel.getStyleClass().add("game-over-label");
        gameOverLabel.setVisible(false);

        // Instructions
        Label instructions = new Label("Controls: ↑ ↓ ← → Arrow Keys | SPACE to Restart");
        instructions.getStyleClass().add("instructions");

        // Footer
        HBox footer = new HBox();
        footer.getStyleClass().add("footer");
        footer.getChildren().add(instructions);
        footer.setAlignment(javafx.geometry.Pos.CENTER);

        // Main content layout
        VBox mainContent = new VBox(10);
        mainContent.getStyleClass().add("main-content");
        mainContent.getChildren().addAll(topHeader, gameContainer, footer);

        // Use StackPane as root for overlay
        root = new StackPane();
        root.getStyleClass().add("root");
        root.getChildren().addAll(mainContent, gameOverLabel);
    }

    private void setupGame() {
        snake.clear();
        clearGrid();

        // Initialize snake in the center
        snake.add(new Position(GRID_SIZE / 2, GRID_SIZE / 2));
        snake.add(new Position(GRID_SIZE / 2 - 1, GRID_SIZE / 2));
        snake.add(new Position(GRID_SIZE / 2 - 2, GRID_SIZE / 2));

        spawnFood();
        currentDirection = Direction.RIGHT;
        nextDirection = Direction.RIGHT;
        score = 0;
        gameRunning = true;
        gameOverLabel.setVisible(false);
        updateScore();
        renderGame();
    }

    private void clearGrid() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                StackPane cell = gridCells[i][j];
                cell.getStyleClass().removeAll("snake-head", "snake-body", "food");
                cell.getStyleClass().add("cell");
            }
        }
    }

    private void renderGame() {
        clearGrid();

        // Render snake body
        for (int i = 1; i < snake.size(); i++) {
            Position segment = snake.get(i);
            StackPane cell = gridCells[segment.x][segment.y];
            cell.getStyleClass().remove("cell");
            cell.getStyleClass().add("snake-body");
        }

        // Render snake head
        Position head = snake.get(0);
        StackPane headCell = gridCells[head.x][head.y];
        headCell.getStyleClass().remove("cell");
        headCell.getStyleClass().add("snake-head");

        // Render food
        StackPane foodCell = gridCells[food.x][food.y];
        foodCell.getStyleClass().remove("cell");
        foodCell.getStyleClass().add("food");
    }

    private void spawnFood() {
        while (true) {
            int x = random.nextInt(GRID_SIZE);
            int y = random.nextInt(GRID_SIZE);
            food = new Position(x, y);

            boolean onSnake = false;
            for (Position segment : snake) {
                if (segment.equals(food)) {
                    onSnake = true;
                    break;
                }
            }

            if (!onSnake) break;
        }
    }

    private void handleKeyPress(KeyCode code) {
        // Buffer input for smoother controls
        switch (code) {
            case UP:    if (currentDirection != Direction.DOWN) nextDirection = Direction.UP; break;
            case DOWN:  if (currentDirection != Direction.UP) nextDirection = Direction.DOWN; break;
            case LEFT:  if (currentDirection != Direction.RIGHT) nextDirection = Direction.LEFT; break;
            case RIGHT: if (currentDirection != Direction.LEFT) nextDirection = Direction.RIGHT; break;
            case SPACE: if (!gameRunning) setupGame(); break;
            case R:     if (!gameRunning) setupGame(); break;
        }
    }

    private int getCurrentGameSpeed() {
        // Game gets faster as score increases
        int speedReduction = (score / SPEED_INCREASE_INTERVAL) * 10;
        int currentSpeed = BASE_GAME_SPEED - speedReduction;
        return Math.max(currentSpeed, MIN_GAME_SPEED);
    }

    private void updateGame() {
        if (!gameRunning) return;

        // Apply buffered direction
        currentDirection = nextDirection;

        Position head = snake.get(0);
        Position newHead = new Position(head.x, head.y);

        switch (currentDirection) {
            case UP:    newHead.y--; break;
            case DOWN:  newHead.y++; break;
            case LEFT:  newHead.x--; break;
            case RIGHT: newHead.x++; break;
        }

        // Check wall collision
        if (newHead.x < 0 || newHead.x >= GRID_SIZE || newHead.y < 0 || newHead.y >= GRID_SIZE) {
            gameOver();
            return;
        }

        // Check self collision
        for (Position segment : snake) {
            if (segment.equals(newHead)) {
                gameOver();
                return;
            }
        }

        // Move snake
        snake.add(0, newHead);

        // Check food collision
        if (newHead.equals(food)) {
            score++;
            if (score > highScore) {
                highScore = score;
            }
            updateScore();
            spawnFood();
        } else {
            snake.remove(snake.size() - 1);
        }

        renderGame();
    }

    private void updateFPS(long now) {
        frameCount++;

        // Update FPS counter every second
        if (now - lastFpsUpdate >= 1_000_000_000) {
            currentFps = frameCount;
            frameCount = 0;
            lastFpsUpdate = now;
            fpsLabel.setText(String.format("FPS: %.0f", currentFps));
        }
    }

    private void gameOver() {
        gameRunning = false;
        gameOverLabel.setVisible(true);
    }

    private void updateScore() {
        scoreLabel.setText("🎯 Score: " + score);
        highScoreLabel.setText("🏆 High Score: " + highScore);
    }

    private void startGameLoop() {
        new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                // Update FPS counter
                updateFPS(now);

                // Get dynamic game speed based on score
                int currentGameSpeed = getCurrentGameSpeed();

                if (now - lastUpdate >= currentGameSpeed * 1_000_000L) {
                    updateGame();
                    lastUpdate = now;
                }
            }
        }.start();
    }
}