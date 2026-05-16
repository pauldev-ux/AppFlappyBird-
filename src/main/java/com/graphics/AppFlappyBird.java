package com.graphics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

public class AppFlappyBird {

    private long window;
    private Renderer renderer;
    private InputManager inputManager;

    private Bird player1;
    private Bird player2;
    private final List<Pipe> pipes = new ArrayList<>();
    private final Random random = new Random();
    private float timerSpawn;

    private float wingAnimTime;
    private float wingAngle;
    private float wingFlapTimer;

    private GameState gameState;

    private float basePipeSpeed;
    private float currentPipeSpeed;
    private int difficultyLevel;

    private final List<Float> cloudsX = new ArrayList<>();
    private final List<Float> grassX = new ArrayList<>();
    private float offsetClouds;
    private float offsetMountains;
    private float offsetGround;

    public void run() {
        init();
        resetGame();
        loop();
        cleanup();
    }

    private void init() {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("No se pudo iniciar GLFW");
        }

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        window = GLFW.glfwCreateWindow(GameConfig.WIDTH, GameConfig.HEIGHT, "Flappy Bird OpenGL", 0, 0);
        if (window == 0) {
            throw new RuntimeException("No se pudo crear la ventana");
        }

        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);
        GL.createCapabilities();

        renderer = new Renderer();
        renderer.init();
        inputManager = new InputManager();
    }

    private void resetGame() {
        if (player1 == null) {
            player1 = new Bird(GameConfig.BIRD_X_PLAYER1, 0.0f,
                    GameConfig.BIRD1_COLOR_R, GameConfig.BIRD1_COLOR_G, GameConfig.BIRD1_COLOR_B);
        } else {
            player1.reset(0.0f);
        }
        if (player2 == null) {
            player2 = new Bird(GameConfig.BIRD_X_PLAYER2, 0.0f,
                    GameConfig.BIRD2_COLOR_R, GameConfig.BIRD2_COLOR_G, GameConfig.BIRD2_COLOR_B);
        } else {
            player2.reset(0.0f);
        }

        timerSpawn = 0.0f;
        wingAnimTime = 0.0f;
        wingAngle = 0.0f;
        wingFlapTimer = 0.0f;

        gameState = GameState.START;
        pipes.clear();

        basePipeSpeed = GameConfig.PIPE_SPEED;
        currentPipeSpeed = basePipeSpeed;
        difficultyLevel = 1;

        cloudsX.clear();
        cloudsX.add(1.2f);
        cloudsX.add(1.8f);
        cloudsX.add(2.4f);

        grassX.clear();
        for (int i = 0; i < 20; i++) {
            grassX.add(-1.0f + i * 0.1f);
        }

        offsetClouds = 0.0f;
        offsetMountains = 0.0f;
        offsetGround = 0.0f;

        updateTitle();
    }

    private void processInput() {
        inputManager.update(window);

        if (inputManager.isExitRequested()) {
            GLFW.glfwSetWindowShouldClose(window, true);
        }

        if (inputManager.isJumpPlayer1Pressed()) {
            handleJump(player1);
        }
        if (inputManager.isJumpPlayer2Pressed()) {
            handleJump(player2);
        }

        if (inputManager.isResetPressed() && gameState == GameState.GAME_OVER) {
            resetGame();
        }
    }

    private void handleJump(Bird player) {
        if (gameState == GameState.GAME_OVER) {
            resetGame();
        }
        gameState = GameState.PLAYING;
        if (player.isAlive()) {
            player.jump();
        }
        wingFlapTimer = GameConfig.WING_FLAP_DURATION;
    }

    private void update(float dt) {
        if (gameState != GameState.PLAYING) {
            return;
        }

        player1.update(dt);
        player2.update(dt);

        checkBoundsCollision(player1);
        checkBoundsCollision(player2);

        if (!player1.isAlive() && !player2.isAlive()) {
            gameState = GameState.GAME_OVER;
            updateTitle();
            return;
        }

        timerSpawn += dt;
        if (timerSpawn >= GameConfig.PIPE_SPAWN_INTERVAL) {
            timerSpawn = 0.0f;
            spawnPipe();
        }

        wingAnimTime += dt;
        if (wingFlapTimer > 0.0f) {
            wingFlapTimer -= dt;
        }
        updateWingAngle(dt);
        updateDecorativeElements(dt);
        updateDifficulty();

        Iterator<Pipe> it = pipes.iterator();
        while (it.hasNext()) {
            Pipe pipe = it.next();
            pipe.x -= currentPipeSpeed * dt;

            boolean scored = false;
            if (player1.isAlive() && pipe.x + (GameConfig.PIPE_WIDTH * 0.5f) < player1.getX() && !pipe.puntuadaP1) {
                player1.addScore();
                pipe.puntuadaP1 = true;
                scored = true;
            }
            if (player2.isAlive() && pipe.x + (GameConfig.PIPE_WIDTH * 0.5f) < player2.getX() && !pipe.puntuadaP2) {
                player2.addScore();
                pipe.puntuadaP2 = true;
                scored = true;
            }
            if (scored) {
                updateTitle();
            }

            boolean collided1 = player1.isAlive() && collidesWithPipe(pipe, player1);
            boolean collided2 = player2.isAlive() && collidesWithPipe(pipe, player2);
            if (collided1) {
                player1.kill();
            }
            if (collided2) {
                player2.kill();
            }

            if (!player1.isAlive() && !player2.isAlive()) {
                gameState = GameState.GAME_OVER;
                updateTitle();
                return;
            }

            if (pipe.x + (GameConfig.PIPE_WIDTH * 0.5f) < -1.3f) {
                it.remove();
            }
        }
    }

    private void checkBoundsCollision(Bird player) {
        if (!player.isAlive()) {
            return;
        }
        float birdTop = player.getY() + (GameConfig.BIRD_HEIGHT * 0.5f);
        float birdBottom = player.getY() - (GameConfig.BIRD_HEIGHT * 0.5f);
        if (birdTop >= 1.0f || birdBottom <= -1.0f) {
            player.kill();
        }
    }

    private void spawnPipe() {
        float gapCenter = GameConfig.GAP_MIN_CENTER + random.nextFloat() * (GameConfig.GAP_MAX_CENTER - GameConfig.GAP_MIN_CENTER);
        pipes.add(new Pipe(1.2f, gapCenter));
    }

    private boolean collidesWithPipe(Pipe pipe, Bird bird) {
        float birdLeft = bird.getX() - (GameConfig.BIRD_WIDTH * 0.5f);
        float birdRight = bird.getX() + (GameConfig.BIRD_WIDTH * 0.5f);
        float birdBottom = bird.getY() - (GameConfig.BIRD_HEIGHT * 0.5f);
        float birdTop = bird.getY() + (GameConfig.BIRD_HEIGHT * 0.5f);

        float pipeLeft = pipe.x - (GameConfig.PIPE_WIDTH * 0.5f);
        float pipeRight = pipe.x + (GameConfig.PIPE_WIDTH * 0.5f);
        boolean overlapX = birdRight > pipeLeft && birdLeft < pipeRight;
        if (!overlapX) {
            return false;
        }

        float gapTop = pipe.gapCentroY + (GameConfig.GAP_HEIGHT * 0.5f);
        float gapBottom = pipe.gapCentroY - (GameConfig.GAP_HEIGHT * 0.5f);
        return birdTop > gapTop || birdBottom < gapBottom;
    }

    private void updateWingAngle(float dt) {
        Bird referenceBird = player1.isAlive() ? player1 : player2;
        float targetAngle;
        if (wingFlapTimer > 0.0f) {
            targetAngle = GameConfig.WING_JUMP_ANGLE;
        } else if (referenceBird.getVelocityY() >= 0.0f) {
            targetAngle = GameConfig.WING_RISE_ANGLE;
        } else {
            targetAngle = GameConfig.WING_FALL_ANGLE;
        }
        wingAngle += (targetAngle - wingAngle) * Math.min(1.0f, dt * 10.0f);
    }

    private void updateDifficulty() {
        int previousLevel = difficultyLevel;
        difficultyLevel = calculateDifficultyLevel();
        currentPipeSpeed = calculateSpeedByLevel(difficultyLevel);
        if (difficultyLevel != previousLevel) {
            // El título se actualiza cuando cambia el puntaje, pero mantenemos el mismo
            // comportamiento si el nivel cambia entre actualizaciones consecutivas.
        }
    }

    private int calculateDifficultyLevel() {
        int maxScore = Math.max(player1.getScore(), player2.getScore());
        int level = (maxScore / GameConfig.POINTS_PER_LEVEL) + 1;
        return Math.min(level, GameConfig.LEVEL_MAX);
    }

    private float calculateSpeedByLevel(int level) {
        float[] multipliers = { 1.0f, 1.15f, 1.30f, 1.45f, 1.60f };
        if (level < 1 || level > GameConfig.LEVEL_MAX) {
            return basePipeSpeed;
        }
        return basePipeSpeed * multipliers[level - 1];
    }

    private void updateDecorativeElements(float dt) {
        offsetClouds -= 0.1f * dt;
        offsetMountains -= 0.05f * dt;
        offsetGround -= 0.3f * dt;

        for (int i = 0; i < cloudsX.size(); i++) {
            cloudsX.set(i, cloudsX.get(i) - 0.1f * dt);
            if (cloudsX.get(i) < -1.5f) {
                cloudsX.set(i, 1.5f);
            }
        }

        for (int i = 0; i < grassX.size(); i++) {
            grassX.set(i, grassX.get(i) - 0.3f * dt);
            if (grassX.get(i) < -1.2f) {
                grassX.set(i, 1.2f);
            }
        }
    }

    private void updateTitle() {
        String titleBase = String.format("Flappy Bird OpenGL | P1: %d  P2: %d",
                player1.getScore(), player2.getScore());
        if (gameState == GameState.START) {
            GLFW.glfwSetWindowTitle(window, titleBase + " | SPACE o W/ARRIBA para empezar");
        } else if (gameState == GameState.GAME_OVER) {
            GLFW.glfwSetWindowTitle(window, titleBase + " | GAME OVER - SPACE o W/ARRIBA para reiniciar");
        } else {
            GLFW.glfwSetWindowTitle(window, titleBase);
        }
    }

    private void render() {
        renderer.renderScene(gameState, player1, player2, pipes,
                cloudsX, grassX,
                offsetClouds, offsetMountains, offsetGround,
                difficultyLevel, wingAnimTime, wingAngle);
    }

    private void loop() {
        float lastTime = (float) GLFW.glfwGetTime();
        while (!GLFW.glfwWindowShouldClose(window)) {
            float now = (float) GLFW.glfwGetTime();
            float dt = now - lastTime;
            lastTime = now;
            if (dt > 0.033f) {
                dt = 0.033f;
            }

            processInput();
            update(dt);
            render();

            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    private void cleanup() {
        renderer.cleanup();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    public static void main(String[] args) {
        new AppFlappyBird().run();
    }
}
