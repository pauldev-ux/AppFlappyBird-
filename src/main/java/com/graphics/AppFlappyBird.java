package com.graphics;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

public class AppFlappyBird {

    // Ventana principal de GLFW.
    private long window;

    // Renderer dibuja todo: fondo, pájaros, tuberías, HUD y Game Over.
    private Renderer renderer;

    // InputManager lee teclas como SPACE, W, UP, R y ESC.
    private InputManager inputManager;

    // Jugadores del juego.
    // Para cambiar colores o posición inicial, revisa resetGame() y GameConfig.
    private Bird player1;
    private Bird player2;

    // Lista de tuberías activas en pantalla.
    private final List<Pipe> pipes = new ArrayList<>();

    // Generador aleatorio para colocar el hueco de las tuberías.
    private final Random random = new Random();

    // Temporizador para saber cuándo crear una nueva tubería.
    private float timerSpawn;

    // Variables para animar el ala del pájaro.
    private float wingAnimTime;
    private float wingAngle;
    private float wingFlapTimer;

    // Estado actual del juego: START, PLAYING o GAME_OVER.
    private GameState gameState;

    // Velocidad base y actual de tuberías.
    // currentPipeSpeed aumenta según el nivel.
    private float basePipeSpeed;
    private float currentPipeSpeed;

    // Nivel actual de dificultad.
    private int difficultyLevel;

    // Posiciones de decoración: nubes y césped.
    private final List<Float> cloudsX = new ArrayList<>();
    private final List<Float> grassX = new ArrayList<>();

    // Offsets para efecto de movimiento/parallax.
    private float offsetClouds;
    private float offsetMountains;
    private float offsetGround;

    // Punto de arranque del juego.
    // Inicializa, reinicia, entra al loop y limpia recursos al cerrar.
    public void run() {
        init();
        resetGame();
        loop();
        cleanup();
    }

    // Inicializa GLFW, ventana, OpenGL, renderer e input.
    // Si la ventana no abre, revisa configuración de GLFW o dependencias LWJGL.
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

        // Crea la ventana con tamaño definido en GameConfig.
        window = GLFW.glfwCreateWindow(GameConfig.WIDTH, GameConfig.HEIGHT, "Flappy Bird OpenGL", 0, 0);
        if (window == 0) {
            throw new RuntimeException("No se pudo crear la ventana");
        }

        GLFW.glfwMakeContextCurrent(window);

        // Sincroniza FPS con el monitor.
        GLFW.glfwSwapInterval(1);

        GLFW.glfwShowWindow(window);

        // Habilita llamadas OpenGL en LWJGL.
        GL.createCapabilities();

        renderer = new Renderer();
        renderer.init();

        inputManager = new InputManager();
    }

    // Reinicia todo el juego a valores iniciales.
    // Se usa al iniciar y al reiniciar después de Game Over.
    private void resetGame() {
        // Crea o reinicia jugador 1.
        // Para cambiar color, modifica GameConfig.BIRD1_COLOR_*.
        if (player1 == null) {
            player1 = new Bird(GameConfig.BIRD_X_PLAYER1, 0.0f,
                    GameConfig.BIRD1_COLOR_R, GameConfig.BIRD1_COLOR_G, GameConfig.BIRD1_COLOR_B);
        } else {
            player1.reset(0.0f);
        }

        // Crea o reinicia jugador 2.
        // Para cambiar color, modifica GameConfig.BIRD2_COLOR_*.
        if (player2 == null) {
            player2 = new Bird(GameConfig.BIRD_X_PLAYER2, 0.0f,
                    GameConfig.BIRD2_COLOR_R, GameConfig.BIRD2_COLOR_G, GameConfig.BIRD2_COLOR_B);
        } else {
            player2.reset(0.0f);
        }

        // Reinicia animación y timers.
        timerSpawn = 0.0f;
        wingAnimTime = 0.0f;
        wingAngle = 0.0f;
        wingFlapTimer = 0.0f;

        // El juego empieza en pantalla inicial.
        gameState = GameState.START;

        // Limpia tuberías anteriores.
        pipes.clear();

        // Reinicia dificultad.
        basePipeSpeed = GameConfig.PIPE_SPEED;
        currentPipeSpeed = basePipeSpeed;
        difficultyLevel = 1;

        // Posiciones iniciales de nubes.
        // Para agregar más nubes, añade más valores aquí.
        cloudsX.clear();
        cloudsX.add(1.2f);
        cloudsX.add(1.8f);
        cloudsX.add(2.4f);

        // Posiciones iniciales del césped.
        grassX.clear();
        for (int i = 0; i < 20; i++) {
            grassX.add(-1.0f + i * 0.1f);
        }

        // Reinicia desplazamientos de fondo.
        offsetClouds = 0.0f;
        offsetMountains = 0.0f;
        offsetGround = 0.0f;

        updateTitle();
    }

    // Procesa las teclas leídas por InputManager.
    // Para cambiar controles, modifica InputManager.java.
    private void processInput() {
        inputManager.update(window);

        // ESC cierra la ventana.
        if (inputManager.isExitRequested()) {
            GLFW.glfwSetWindowShouldClose(window, true);
        }

        // Salto jugador 1.
        if (inputManager.isJumpPlayer1Pressed()) {
            handleJump(player1);
        }

        // Salto jugador 2.
        if (inputManager.isJumpPlayer2Pressed()) {
            handleJump(player2);
        }

        // R reinicia solo desde Game Over.
        if (inputManager.isResetPressed() && gameState == GameState.GAME_OVER) {
            resetGame();
        }
    }

    // Maneja el salto y también inicia/reinicia la partida.
    // Si quieres que SPACE no reinicie en Game Over, cambia esta lógica.
    private void handleJump(Bird player) {
        if (gameState == GameState.GAME_OVER) {
            resetGame();
        }

        gameState = GameState.PLAYING;

        // Solo salta si el jugador está vivo.
        if (player.isAlive()) {
            player.jump();
        }

        // Activa animación de ala.
        wingFlapTimer = GameConfig.WING_FLAP_DURATION;
    }

    // Actualiza la lógica del juego en cada frame.
    // Aquí se mueven jugadores, tuberías, dificultad y colisiones.
    private void update(float dt) {
        if (gameState != GameState.PLAYING) {
            return;
        }

        // Actualiza física de ambos jugadores.
        player1.update(dt);
        player2.update(dt);

        // Revisa choque con techo o piso.
        checkBoundsCollision(player1);
        checkBoundsCollision(player2);

        // Si ambos murieron, termina el juego.
        if (!player1.isAlive() && !player2.isAlive()) {
            gameState = GameState.GAME_OVER;
            updateTitle();
            return;
        }

        // Genera tuberías según el intervalo configurado.
        // Para que salgan más seguido, baja PIPE_SPAWN_INTERVAL en GameConfig.
        timerSpawn += dt;
        if (timerSpawn >= GameConfig.PIPE_SPAWN_INTERVAL) {
            timerSpawn = 0.0f;
            spawnPipe();
        }

        // Actualiza animación del ala y decoración.
        wingAnimTime += dt;

        if (wingFlapTimer > 0.0f) {
            wingFlapTimer -= dt;
        }

        updateWingAngle(dt);
        updateDecorativeElements(dt);

        // Actualiza velocidad según puntaje.
        updateDifficulty();

        // Mueve, puntúa, colisiona y elimina tuberías.
        Iterator<Pipe> it = pipes.iterator();
        while (it.hasNext()) {
            Pipe pipe = it.next();

            // Movimiento de tuberías.
            // Para aumentar velocidad base, cambia PIPE_SPEED en GameConfig.
            pipe.x -= currentPipeSpeed * dt;

            boolean scored = false;

            // Puntaje jugador 1 cuando supera una tubería.
            if (player1.isAlive() && pipe.x + (GameConfig.PIPE_WIDTH * 0.5f) < player1.getX() && !pipe.puntuadaP1) {
                player1.addScore();
                pipe.puntuadaP1 = true;
                scored = true;
            }

            // Puntaje jugador 2 cuando supera una tubería.
            if (player2.isAlive() && pipe.x + (GameConfig.PIPE_WIDTH * 0.5f) < player2.getX() && !pipe.puntuadaP2) {
                player2.addScore();
                pipe.puntuadaP2 = true;
                scored = true;
            }

            if (scored) {
                updateTitle();
            }

            // Revisa colisiones con tubería.
            boolean collided1 = player1.isAlive() && collidesWithPipe(pipe, player1);
            boolean collided2 = player2.isAlive() && collidesWithPipe(pipe, player2);

            if (collided1) {
                player1.kill();
            }

            if (collided2) {
                player2.kill();
            }

            // Game Over solo cuando ambos jugadores pierden.
            if (!player1.isAlive() && !player2.isAlive()) {
                gameState = GameState.GAME_OVER;
                updateTitle();
                return;
            }

            // Elimina tuberías que ya salieron de pantalla.
            if (pipe.x + (GameConfig.PIPE_WIDTH * 0.5f) < -1.3f) {
                it.remove();
            }
        }
    }

    // Revisa choque con techo o piso.
    // Para cambiar tamaño de colisión, modifica BIRD_WIDTH/BIRD_HEIGHT.
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

    // Crea una nueva tubería a la derecha.
    // Para cambiar dónde aparecen los huecos, usa GAP_MIN_CENTER y GAP_MAX_CENTER.
    private void spawnPipe() {
        float gapCenter = GameConfig.GAP_MIN_CENTER
                + random.nextFloat() * (GameConfig.GAP_MAX_CENTER - GameConfig.GAP_MIN_CENTER);

        pipes.add(new Pipe(1.2f, gapCenter));
    }

    // Detecta colisión entre un pájaro y una tubería.
    // Usa una caja AABB simple para el pájaro.
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

        // Si el pájaro está fuera del hueco, colisiona.
        return birdTop > gapTop || birdBottom < gapBottom;
    }

    // Calcula el ángulo del ala según salto/subida/caída.
    // Para cambiar movimiento del ala, modifica constantes WING_* en GameConfig.
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

    // Actualiza dificultad según el puntaje mayor.
    // Cada POINTS_PER_LEVEL sube un nivel hasta LEVEL_MAX.
    private void updateDifficulty() {
        int previousLevel = difficultyLevel;

        difficultyLevel = calculateDifficultyLevel();
        currentPipeSpeed = calculateSpeedByLevel(difficultyLevel);

        if (difficultyLevel != previousLevel) {
            // Aquí podrías agregar sonido o efecto cuando sube el nivel.
        }
    }

    // Calcula nivel con base en el puntaje más alto entre ambos jugadores.
    // Ejemplo: si POINTS_PER_LEVEL = 3, sube nivel cada 3 puntos.
    private int calculateDifficultyLevel() {
        int maxScore = Math.max(player1.getScore(), player2.getScore());
        int level = (maxScore / GameConfig.POINTS_PER_LEVEL) + 1;

        return Math.min(level, GameConfig.LEVEL_MAX);
    }

    // Calcula velocidad según nivel.
    // Para hacer más difícil el juego, sube los multiplicadores.
    private float calculateSpeedByLevel(int level) {
        float[] multipliers = { 1.0f, 1.15f, 1.30f, 1.45f, 1.60f };

        if (level < 1 || level > GameConfig.LEVEL_MAX) {
            return basePipeSpeed;
        }

        return basePipeSpeed * multipliers[level - 1];
    }

    // Mueve nubes, montañas y césped para dar efecto de movimiento.
    // Para parallax más rápido, aumenta 0.1f, 0.05f o 0.3f.
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

    // Actualiza el título de la ventana con puntajes y estado.
    // Para mostrar más datos, agrega texto al String titleBase.
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

    // Llama al renderer para dibujar la escena.
    private void render() {
        renderer.renderScene(gameState, player1, player2, pipes,
                cloudsX, grassX,
                offsetClouds, offsetMountains, offsetGround,
                difficultyLevel, wingAnimTime, wingAngle);
    }

    // Bucle principal del juego.
    // Calcula deltaTime, procesa input, actualiza lógica y renderiza.
    private void loop() {
        float lastTime = (float) GLFW.glfwGetTime();

        while (!GLFW.glfwWindowShouldClose(window)) {
            float now = (float) GLFW.glfwGetTime();
            float dt = now - lastTime;
            lastTime = now;

            // Limita dt para evitar saltos grandes si se congela un frame.
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

    // Libera renderer, destruye ventana y termina GLFW.
    private void cleanup() {
        renderer.cleanup();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    // Método principal.
    public static void main(String[] args) {
        new AppFlappyBird().run();
    }
}