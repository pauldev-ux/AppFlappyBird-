package com.graphics;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * AppFlappyBird:
 * Mini-juego estilo Flappy Bird con OpenGL 2D (NDC directo, sin texturas).
 *
 * Estructura del juego:
 * - Jugador (pajaro) representado por un rectangulo.
 * - Obstaculos (tuberias) como rectangulos superior/inferior.
 * - Fisica basica: gravedad + impulso al saltar.
 * - Colision AABB simplificada.
 * - Puntuacion por cada tuberia superada.
 *
 * Nota didactica:
 * Para simplificar la clase, se usa un solo "quad base" (2 triangulos)
 * y se dibuja cualquier rectangulo con uniforms de offset/scale/color.
 */
public class AppFlappyBird {

    // Tamano inicial de ventana.
    private static final int ANCHO = 900;
    private static final int ALTO = 700;

    // Posiciones horizontales fijas de los dos jugadores en NDC.
    private static final float BIRD_X_PLAYER1 = -0.45f;
    private static final float BIRD_X_PLAYER2 = -0.15f;
    // Tamano del pajaro.
    private static final float BIRD_ANCHO = 0.10f;
    private static final float BIRD_ALTO = 0.10f;
    // Fisica vertical.
    private static final float GRAVEDAD = -1.9f;
    private static final float IMPULSO_SALTO = 0.85f;
    private static final float VELOCIDAD_MAX_CAIDA = -1.8f;

    // Animacion del ala.
    private static final float WING_FLAP_DURATION = 0.18f;
    private static final float WING_JUMP_ANGLE = 0.35f; // radianes
    private static final float WING_RISE_ANGLE = 0.12f; // radianes
    private static final float WING_FALL_ANGLE = -0.18f; // radianes
    private static final float WING_OSCILLATION_SPEED = 18.0f;
    private static final float WING_OSCILLATION_AMPLITUDE = 0.04f;

    // Parametros de tuberias.
    private static final float TUBERIA_ANCHO = 0.18f;
    private static final float GAP_ALTO = 0.48f;
    private static final float VELOCIDAD_TUBERIAS = 0.62f;
    private static final float TIEMPO_ENTRE_TUBERIAS = 1.5f;
    private static final float GAP_MIN_CENTRO = -0.45f;
    private static final float GAP_MAX_CENTRO = 0.45f;

    // Recursos OpenGL basicos.
    private long window;
    private int programa;
    private int vao;
    private int vbo;
    // Uniforms de transformacion, rotacion y color.
    private int uOffsetLocation;
    private int uScaleLocation;
    private int uRotationLocation;
    private int uColorLocation;

    // Recursos adicionales para figuras compuestas.
    private int vaoTriangle;
    private int vboTriangle;
    private int vaoCircle;
    private int vboCircle;

    // Estado del jugador/juego.
    private Bird player1;
    private Bird player2;
    private float timerSpawn;

    // Animacion de ala.
    private float wingAnimTime;
    private float wingAngle;
    private float wingFlapTimer;

    private boolean prevSpace;
    private boolean prevJump2;
    private boolean prevR;

    private boolean started;
    private boolean gameOver;

    // Lista de obstaculos activos.
    private final List<Tuberia> tuberias = new ArrayList<>();
    // RNG para variar la posicion del gap.
    private final Random random = new Random();

    // Sistema de dificultad progresiva.
    private static final int NIVEL_MAXIMO = 5;
    private static final int PUNTOS_POR_NIVEL = 3;
    private float velocidadBaseTuberias;
    private float velocidadActualTuberias;
    private int nivelDificultad;

    // Elementos decorativos para mejorar la interfaz visual.
    // Nubes: posiciones x de varias nubes que se mueven lentamente.
    private final List<Float> nubesX = new ArrayList<>();
    // Montañas: posiciones fijas para triángulos grandes en el fondo.
    private static final float[] MONTANAS_X = { -0.8f, -0.2f, 0.4f, 1.0f };
    private static final float[] MONTANAS_ALTURAS = { 0.3f, 0.25f, 0.35f, 0.2f };
    // Suelo: posiciones para césped decorativo.
    private final List<Float> cespedX = new ArrayList<>();
    // Offsets para efecto parallax.
    private float offsetNubes;
    private float offsetMontanas;
    private float offsetSuelo;

    /**
     * Modelo de una tuberia:
     * x: posicion horizontal comun para parte superior/inferior,
     * gapCentroY: centro vertical del hueco,
     * puntuada: evita sumar dos veces la misma tuberia.
     */
    private static class Tuberia {
        float x;
        float gapCentroY;
        boolean puntuadaP1;
        boolean puntuadaP2;

        Tuberia(float x, float gapCentroY) {
            this.x = x;
            this.gapCentroY = gapCentroY;
            this.puntuadaP1 = false;
            this.puntuadaP2 = false;
        }
    }

    // Flujo principal de la aplicacion.
    public void run() {
        init();
        // Estado inicial listo para jugar.
        resetGame();
        loop();
        cleanup();
    }

    // Inicializa GLFW/OpenGL + shaders + geometria base.
    private void init() {
        // Arranque de GLFW.
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("No se pudo iniciar GLFW");
        }

        // Config de ventana/contexto.
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);

        // Crear ventana.
        window = GLFW.glfwCreateWindow(ANCHO, ALTO, "Flappy Bird OpenGL", 0, 0);
        if (window == 0) {
            throw new RuntimeException("No se pudo crear la ventana");
        }

        // Contexto + VSync + mostrar.
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);
        // Cargar funciones OpenGL.
        GL.createCapabilities();

        // Crear pipeline y quad unitario reutilizable.
        //. Se crean los recursos gráficos base: shaders, rectángulo, triángulo y círculo.
        //. El triángulo y el círculo se usarán después para formar visualmente el pájaro.
        crearShaders();
        crearQuadBase();
        crearTriangle();
        crearCircleApprox();
    }

    /**
     * Crea shaders 2D:
     * - Vertex: transforma quad base con escala y offset.
     * - Fragment: color uniforme.
     */
    private void crearShaders() {
        String vertexSrc = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            uniform vec2 uOffset;
            uniform vec2 uScale;
            uniform float uRotation;
            void main() {
                float cosR = cos(uRotation);
                float sinR = sin(uRotation);
                vec2 scaledPos = aPos.xy * uScale;
                vec2 rotatedPos = vec2(
                    scaledPos.x * cosR - scaledPos.y * sinR,
                    scaledPos.x * sinR + scaledPos.y * cosR
                );
                vec2 finalPos = rotatedPos + uOffset;
                gl_Position = vec4(finalPos, aPos.z, 1.0);
            }
            """;

        // Color solido por objeto.
        String fragmentSrc = """
            #version 330 core
            uniform vec3 uColor;
            out vec4 fragColor;
            void main() {
                fragColor = vec4(uColor, 1.0);
            }
            """;

        // Compilar vertex shader.
        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexSrc);
        GL20.glCompileShader(vertexShader);
        comprobarShader(vertexShader, "Vertex");

        // Compilar fragment shader.
        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSrc);
        GL20.glCompileShader(fragmentShader);
        comprobarShader(fragmentShader, "Fragment");

        // Link de programa.
        programa = GL20.glCreateProgram();
        GL20.glAttachShader(programa, vertexShader);
        GL20.glAttachShader(programa, fragmentShader);
        GL20.glLinkProgram(programa);

        if (GL20.glGetProgrami(programa, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Error al enlazar programa: " + GL20.glGetProgramInfoLog(programa));
        }

        // Resolver uniforms.
        uOffsetLocation = GL20.glGetUniformLocation(programa, "uOffset");
        uScaleLocation = GL20.glGetUniformLocation(programa, "uScale");
        uRotationLocation = GL20.glGetUniformLocation(programa, "uRotation");
        uColorLocation = GL20.glGetUniformLocation(programa, "uColor");
        if (uOffsetLocation == -1 || uScaleLocation == -1 || uRotationLocation == -1 || uColorLocation == -1) {
            throw new RuntimeException("No se pudieron obtener uniforms del shader");
        }

        // Limpiar objetos shader temporales.
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
    }

    // Verificacion de compilacion GLSL.
    private void comprobarShader(int shader, String tipo) {
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(tipo + " shader: " + GL20.glGetShaderInfoLog(shader));
        }
    }

    /**
     * Crea un rectangulo unitario centrado en origen:
     * - Rango x,y de -0.5 a +0.5.
     * - 2 triangulos (6 vertices).
     * Cualquier objeto 2D se dibuja escalando y moviendo este quad.
     */
    private void crearQuadBase() {
        float[] vertices = {
            -0.5f, -0.5f, 0.0f,
             0.5f, -0.5f, 0.0f,
             0.5f,  0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,
             0.5f,  0.5f, 0.0f,
            -0.5f,  0.5f, 0.0f
        };

        // VAO.
        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        // VBO.
        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        // Subida de vertices.
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        // Atributo posicion.
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        // Desbind.
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    /**
     * Crea un triangulo equilatero centrado en origen.
     * Vertices en sentido antihorario.
     */
    //. Crea la figura base de un triángulo.
    //. Esta figura se reutiliza para dibujar partes del pájaro como el pico, el ala y la cola.
    private void crearTriangle() {
        float[] vertices = {
            0.0f,  0.5f, 0.0f,   // cima
           -0.433f, -0.25f, 0.0f, // izquierda
            0.433f, -0.25f, 0.0f  // derecha
        };

        // VAO.
        vaoTriangle = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoTriangle);

        // VBO.
        vboTriangle = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboTriangle);

        // Subida de vertices.
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        // Atributo posicion.
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        // Desbind.
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    /**
     * Crea una aproximacion de circulo usando triangle fan (12 triangulos).
     * Centro + 12 puntos en circunferencia.
     */
    //. Crea una aproximación de círculo usando varios vértices.
    //. Esta figura se reutiliza para dibujar el cuerpo, el ojo y la pupila del pájaro.
    private void crearCircleApprox() {
        int numSegments = 12;
        float[] vertices = new float[(numSegments + 2) * 3]; // centro + numSegments + cierre

        // Centro
        vertices[0] = 0.0f;
        vertices[1] = 0.0f;
        vertices[2] = 0.0f;

        // Puntos en circunferencia
        for (int i = 0; i <= numSegments; i++) {
            double angle = 2.0 * Math.PI * i / numSegments;
            float x = (float) Math.cos(angle) * 0.5f;
            float y = (float) Math.sin(angle) * 0.5f;
            vertices[(i + 1) * 3] = x;
            vertices[(i + 1) * 3 + 1] = y;
            vertices[(i + 1) * 3 + 2] = 0.0f;
        }

        // VAO.
        vaoCircle = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoCircle);

        // VBO.
        vboCircle = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboCircle);

        // Subida de vertices.
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        // Atributo posicion.
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        // Desbind.
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    /**
     * Reinicia estado de partida.
     * Se usa al iniciar app y al reiniciar tras game over.
     */
    private void resetGame() {
        if (player1 == null) {
            player1 = new Bird(BIRD_X_PLAYER1, 0.0f, 0.98f, 0.85f, 0.20f);
        } else {
            player1.reset(0.0f);
        }
        if (player2 == null) {
            player2 = new Bird(BIRD_X_PLAYER2, 0.0f, 0.35f, 0.70f, 0.98f);
        } else {
            player2.reset(0.0f);
        }
        timerSpawn = 0.0f;
        wingAnimTime = 0.0f;
        wingAngle = 0.0f;
        wingFlapTimer = 0.0f;
        started = false;
        gameOver = false;
        prevSpace = false;
        prevJump2 = false;
        tuberias.clear();
        // Reiniciar dificultad.
        velocidadBaseTuberias = VELOCIDAD_TUBERIAS;
        velocidadActualTuberias = velocidadBaseTuberias;
        nivelDificultad = 1;
        // Reiniciar elementos decorativos.
        nubesX.clear();
        nubesX.add(1.2f);
        nubesX.add(1.8f);
        nubesX.add(2.4f);
        cespedX.clear();
        for (int i = 0; i < 20; i++) {
            cespedX.add(-1.0f + i * 0.1f);
        }
        offsetNubes = 0.0f;
        offsetMontanas = 0.0f;
        offsetSuelo = 0.0f;
        actualizarTitulo();
    }

    /**
     * Input del jugador:
     * - ESC: salir.
     * - SPACE: empezar/saltar.
     * - R: reset manual (solo en game over).
     *
     * Se usa deteccion de flanco (prevSpace/prevR) para no disparar
     * multiples acciones mientras tecla permanece presionada.
     */
    private void procesarInput() {
        if (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS) {
            GLFW.glfwSetWindowShouldClose(window, true);
        }

        boolean spaceAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        if (spaceAhora && !prevSpace) {
            if (gameOver) {
                resetGame();
            }
            started = true;
            if (player1.isAlive()) {
                player1.jump();
            }
            wingFlapTimer = WING_FLAP_DURATION;
        }
        prevSpace = spaceAhora;

        boolean wAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
        boolean upAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS;
        boolean jump2Ahora = wAhora || upAhora;
        if (jump2Ahora && !prevJump2) {
            if (gameOver) {
                resetGame();
            }
            started = true;
            if (player2.isAlive()) {
                player2.jump();
            }
            wingFlapTimer = WING_FLAP_DURATION;
        }
        prevJump2 = jump2Ahora;

        boolean rAhora = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        if (rAhora && !prevR && gameOver) {
            resetGame();
        }
        prevR = rAhora;
    }

    /**
     * Actualizacion de logica por frame (dt en segundos):
     * - fisica vertical,
     * - spawn y movimiento de tuberias,
     * - puntaje y colisiones.
     */
    private void actualizar(float dt) {
        // Si aun no inicio o ya termino, no avanza simulacion.
        if (!started || gameOver) {
            return;
        }

        // Actualizar fisica de cada jugador activo.
        player1.update(dt);
        player2.update(dt);

        // Colision contra techo/suelo NDC para cada jugador.
        checkBoundsCollision(player1);
        checkBoundsCollision(player2);

        // La partida termina solo cuando los dos jugadores estan muertos.
        if (!player1.isAlive() && !player2.isAlive()) {
            gameOver = true;
            actualizarTitulo();
            return;
        }

        // Temporizador para generar nuevas tuberias.
        timerSpawn += dt;
        if (timerSpawn >= TIEMPO_ENTRE_TUBERIAS) {
            timerSpawn = 0.0f;
            spawnTuberia();
        }

        // Actualizar tiempo de animacion del ala.
        wingAnimTime += dt;
        if (wingFlapTimer > 0.0f) {
            wingFlapTimer -= dt;
        }
        updateWingAngle(dt);

        // Actualizar elementos decorativos para efecto parallax.
        actualizarElementosDecorativos(dt);

        // Actualizar dificultad según puntaje máximo.
        actualizarDificultad();

        Iterator<Tuberia> it = tuberias.iterator();
        while (it.hasNext()) {
            Tuberia t = it.next();
            // Avance horizontal de obstaculos (derecha -> izquierda) con velocidad ajustada por dificultad.
            t.x -= velocidadActualTuberias * dt;

                boolean scored = false;
            if (player1.isAlive() && t.x + (TUBERIA_ANCHO * 0.5f) < player1.getX() && !t.puntuadaP1) {
                player1.addScore();
                t.puntuadaP1 = true;
                scored = true;
            }
            if (player2.isAlive() && t.x + (TUBERIA_ANCHO * 0.5f) < player2.getX() && !t.puntuadaP2) {
                player2.addScore();
                t.puntuadaP2 = true;
                scored = true;
            }
            if (scored) {
                actualizarTitulo();
            }

            boolean collided1 = player1.isAlive() && colisionaConTuberia(t, player1);
            boolean collided2 = player2.isAlive() && colisionaConTuberia(t, player2);
            if (collided1) {
                player1.kill();
            }
            if (collided2) {
                player2.kill();
            }
            if ((!player1.isAlive() && !player2.isAlive())) {
                gameOver = true;
                actualizarTitulo();
                return;
            }

            // Remover tuberias fuera de pantalla para no acumular memoria.
            if (t.x + (TUBERIA_ANCHO * 0.5f) < -1.3f) {
                it.remove();
            }
        }
    }

    private void checkBoundsCollision(Bird player) {
        if (!player.isAlive()) {
            return;
        }
        float birdTop = player.getY() + (BIRD_ALTO * 0.5f);
        float birdBottom = player.getY() - (BIRD_ALTO * 0.5f);
        if (birdTop >= 1.0f || birdBottom <= -1.0f) {
            player.kill();
        }
    }

    // Crea tuberia nueva en borde derecho con gap vertical aleatorio.
    private void spawnTuberia() {
        float gapCentro = GAP_MIN_CENTRO + random.nextFloat() * (GAP_MAX_CENTRO - GAP_MIN_CENTRO);
        tuberias.add(new Tuberia(1.2f, gapCentro));
    }

    /**
     * Colision AABB simplificada:
     * 1) Si no hay overlap horizontal, no colisiona.
     * 2) Si hay overlap horizontal, colisiona si el pajaro esta fuera del gap.
     */
    private boolean colisionaConTuberia(Tuberia t, Bird bird) {
        float birdLeft = bird.getX() - (BIRD_ANCHO * 0.5f);
        float birdRight = bird.getX() + (BIRD_ANCHO * 0.5f);
        float birdBottom = bird.getY() - (BIRD_ALTO * 0.5f);
        float birdTop = bird.getY() + (BIRD_ALTO * 0.5f);

        float pipeLeft = t.x - (TUBERIA_ANCHO * 0.5f);
        float pipeRight = t.x + (TUBERIA_ANCHO * 0.5f);
        boolean overlapX = birdRight > pipeLeft && birdLeft < pipeRight;
        if (!overlapX) {
            return false;
        }

        float gapTop = t.gapCentroY + (GAP_ALTO * 0.5f);
        float gapBottom = t.gapCentroY - (GAP_ALTO * 0.5f);
        return birdTop > gapTop || birdBottom < gapBottom;
    }

    /**
     * Render del frame:
     * - fondo con degradado,
     * - montañas,
     * - nubes,
     * - tuberias decoradas,
     * - suelo,
     * - pajaros,
     * - indicador de dificultad,
     * - franja central en game over.
     */
    private void render() {
        // Cielo base (será sobreescrito por el degradado).
        GL11.glClearColor(0.52f, 0.80f, 0.92f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        // Activar pipeline y malla base.
        GL20.glUseProgram(programa);
        GL30.glBindVertexArray(vao);

        // Dibujar elementos decorativos en orden de profundidad.
        dibujarFondo();
        dibujarMontanas();
        dibujarNubes();

        // Dibujar tuberías decoradas.
        for (Tuberia t : tuberias) {
            dibujarTuberiaDecorada(t);
        }

        dibujarSuelo();

        //. Dibujar pajaro.
        //. La posición X queda fija y la posición Y cambia según la gravedad y el salto.
        drawBird(player1);
        drawBird(player2);

        // Dibujar indicador visual de dificultad (5 cuadros en esquina superior derecha).
        dibujarIndicadorDificultad();

        if (gameOver) {
            // Fondo oscuro para separar el panel del escenario.
            drawRect(0.0f, 0.0f, 2.0f, 2.0f, 0.0f, 0.04f, 0.04f, 0.06f);

            // Panel central más grande y oscuro.
            drawRect(0.0f, 0.0f, 0.70f, 0.36f, 0.0f, 0.02f, 0.02f, 0.04f);
            drawRect(0.0f, 0.0f, 0.66f, 0.32f, 0.0f, 0.10f, 0.10f, 0.18f);
            drawRect(0.0f, 0.0f, 0.63f, 0.28f, 0.0f, 0.16f, 0.16f, 0.24f);

            // Texto con sombra y tamaño ligeramente reducido para mejor proporción.
            drawCenteredTextWithShadow("GAME OVER", 0.0f, 0.08f, 1.08f, 1.0f, 0.25f, 0.25f, 0.025f, 0.025f);
            drawCenteredTextWithShadow("PRESIONE R PARA REINICIAR", 0.0f, -0.04f, 0.47f, 1.0f, 0.92f, 0.6f, 0.02f, 0.02f);

            // Ícono de reinicio debajo del subtítulo.
            dibujarIconoReinicio(0.0f, -0.16f, 0.10f);
        }
    }

    // Helper de dibujo parametrico de rectangulos.
    private void drawRect(float x, float y, float ancho, float alto, float rotation, float r, float g, float b) {
        // Asegurar que usamos el VAO base del quad.
        GL30.glBindVertexArray(vao);
        // Traslacion del quad.
        GL20.glUniform2f(uOffsetLocation, x, y);
        // Escala del quad.
        GL20.glUniform2f(uScaleLocation, ancho, alto);
        // Rotacion del quad.
        GL20.glUniform1f(uRotationLocation, rotation);
        // Color.
        GL20.glUniform3f(uColorLocation, r, g, b);
        // Dibujar 2 triangulos.
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    // Dibuja un triangulo usando el VAO de triangulo.
    //. Dibuja un triángulo con posición, tamaño, rotación y color.
    //. Se usa para construir partes angulares del pájaro: pico, ala y cola.
    private void drawTriangle(float x, float y, float scale, float rotation, float r, float g, float b) {
        GL30.glBindVertexArray(vaoTriangle);
        GL20.glUniform2f(uOffsetLocation, x, y);
        GL20.glUniform2f(uScaleLocation, scale, scale);
        GL20.glUniform1f(uRotationLocation, rotation);
        GL20.glUniform3f(uColorLocation, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
    }

    // Dibuja una aproximacion de circulo usando triangle fan.
    //. Dibuja un círculo aproximado con posición, radio, rotación y color.
    //. Se usa para representar partes redondas del pájaro: cuerpo, ojo y pupila.
    private void drawCircleApprox(float x, float y, float radius, float rotation, float r, float g, float b) {
        GL30.glBindVertexArray(vaoCircle);
        GL20.glUniform2f(uOffsetLocation, x, y);
        GL20.glUniform2f(uScaleLocation, radius * 2, radius * 2); // escala para radio
        GL20.glUniform1f(uRotationLocation, rotation);
        GL20.glUniform3f(uColorLocation, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 14); // centro + 12 puntos + cierre
    }

    // Dibuja un pajaro compuesto usando figuras geometricas.
    //. Construye visualmente el pájaro usando figuras geométricas.
    //. No usa imágenes ni texturas: combina círculos y triángulos con distintos tamaños,
    //. posiciones y colores para formar el cuerpo, pico, ala, cola, ojo y pupila.
    private void drawBird(Bird bird) {
        float birdRotation = calculateBirdRotation(bird);
        float x = bird.getX();
        float y = bird.getY();
        float bodyScale = 1.5f;

        // Cuerpo principal: circulo amarillo-anaranjado, representa ~65% del tamano total
        float bodyRadius = BIRD_ANCHO * 0.35f * bodyScale;
        drawCircleApprox(x, y, bodyRadius, birdRotation, bird.getColorR(), bird.getColorG(), bird.getColorB());

        // Pico: triangulo naranja apuntando a la derecha
        float beakOffsetX = bodyRadius * 0.85f;
        float beakScale = BIRD_ANCHO * 0.15f * bodyScale;
        float[] beakOffset = rotateOffset(beakOffsetX, 0.0f, birdRotation);
        drawTriangle(x + beakOffset[0], y + beakOffset[1], beakScale, birdRotation - toRadians(90.0f), 0.9f, 0.6f, 0.1f);

        // Ala: triangulo mas oscuro en el costado izquierdo del cuerpo
        float wingOffsetX = -bodyRadius * 0.45f;
        float wingScale = BIRD_ANCHO * 0.25f * bodyScale;
        float[] wingOffset = rotateOffset(wingOffsetX, 0.0f, birdRotation);
        float wingOscillation = (float) Math.sin(wingAnimTime * WING_OSCILLATION_SPEED) * WING_OSCILLATION_AMPLITUDE;
        drawTriangle(x + wingOffset[0], y + wingOffset[1], wingScale,
            birdRotation + toRadians(90.0f) + wingAngle + wingOscillation,
            0.8f, 0.7f, 0.15f);

        // Cola: dos triangulos pequenos atras del cuerpo
        float tailOffsetX = -bodyRadius * 1.05f;
        float tailY = bodyRadius * 0.3f;
        float tailScale = BIRD_ANCHO * 0.12f * bodyScale;
        float[] tailOffsetUp = rotateOffset(tailOffsetX, tailY, birdRotation);
        float[] tailOffsetDown = rotateOffset(tailOffsetX, -tailY, birdRotation);
        drawTriangle(x + tailOffsetUp[0], y + tailOffsetUp[1], tailScale, birdRotation + toRadians(90.0f), 0.98f, 0.85f, 0.20f);
        drawTriangle(x + tailOffsetDown[0], y + tailOffsetDown[1], tailScale, birdRotation + toRadians(90.0f), 0.98f, 0.85f, 0.20f);

        // Ojo: circulo blanco en la parte frontal superior
        float eyeOffsetX = bodyRadius * 0.4f;
        float eyeOffsetY = bodyRadius * 0.45f;
        float eyeRadius = bodyRadius * 0.18f;
        float[] eyeOffset = rotateOffset(eyeOffsetX, eyeOffsetY, birdRotation);
        drawCircleApprox(x + eyeOffset[0], y + eyeOffset[1], eyeRadius, birdRotation, 1.0f, 1.0f, 1.0f);

        // Pupila: circulo negro pequeno dentro del ojo
        float pupilOffsetX = eyeOffsetX + eyeRadius * 0.3f;
        float pupilRadius = eyeRadius * 0.4f;
        float[] pupilOffset = rotateOffset(pupilOffsetX, eyeOffsetY, birdRotation);
        drawCircleApprox(x + pupilOffset[0], y + pupilOffset[1], pupilRadius, birdRotation, 0.0f, 0.0f, 0.0f);
    }

    private float[] rotateOffset(float offsetX, float offsetY, float rotation) {
        float cos = (float) Math.cos(rotation);
        float sin = (float) Math.sin(rotation);
        return new float[] {
            offsetX * cos - offsetY * sin,
            offsetX * sin + offsetY * cos
        };
    }

    private float calculateBirdRotation(Bird bird) {
        float maxUp = 25.0f;
        float maxDown = -45.0f;
        float rotationDeg;
        if (bird.getVelocityY() >= 0.0f) {
            rotationDeg = (bird.getVelocityY() / IMPULSO_SALTO) * maxUp;
        } else {
            rotationDeg = (bird.getVelocityY() / VELOCIDAD_MAX_CAIDA) * maxDown;
        }
        rotationDeg = Math.max(Math.min(rotationDeg, maxUp), maxDown);
        return toRadians(rotationDeg);
    }

    private void updateWingAngle(float dt) {
        Bird referenceBird = player1.isAlive() ? player1 : player2;
        float targetAngle;
        if (wingFlapTimer > 0.0f) {
            targetAngle = WING_JUMP_ANGLE;
        } else if (referenceBird.getVelocityY() >= 0.0f) {
            targetAngle = WING_RISE_ANGLE;
        } else {
            targetAngle = WING_FALL_ANGLE;
        }
        wingAngle += (targetAngle - wingAngle) * Math.min(1.0f, dt * 10.0f);
    }

    private float toRadians(float degrees) {
        return degrees * ((float) Math.PI / 180.0f);
    }

    /**
     * Calcula el ancho total de un texto usando ancho fijo por carácter.
     */
    private float calcularAnchoTexto(String texto, float escala) {
        float anchoCaracter = 0.14f * escala;
        float espacioEntreCaracteres = 0.04f * escala;
        float anchoEspacio = 0.08f * escala;
        float anchoTotal = 0.0f;

        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            anchoTotal += (c == ' ') ? anchoEspacio : anchoCaracter;
            if (i < texto.length() - 1) {
                anchoTotal += espacioEntreCaracteres;
            }
        }
        return anchoTotal;
    }

    /**
     * Dibuja texto centrado con sombra para mejor legibilidad.
     */
    private void drawCenteredTextWithShadow(String text, float centerX, float y, float scale,
            float r, float g, float b, float shadowOffsetX, float shadowOffsetY) {
        drawCenteredText(text, centerX + shadowOffsetX, y + shadowOffsetY, scale, 0.0f, 0.0f, 0.0f);
        drawCenteredText(text, centerX, y, scale, r, g, b);
    }

    /**
     * Dibuja un texto centrado horizontalmente usando letras geométricas.
     */
    private void drawCenteredText(String text, float centerX, float y, float scale,
            float r, float g, float b) {
        float totalWidth = calcularAnchoTexto(text, scale);
        float startX = centerX - totalWidth / 2.0f;
        float charWidth = 0.14f * scale;
        float charSpacing = 0.04f * scale;
        float spaceWidth = 0.08f * scale;

        for (int i = 0; i < text.length(); i++) {
            char c = Character.toUpperCase(text.charAt(i));
            if (c == ' ') {
                startX += spaceWidth + charSpacing;
                continue;
            }
            drawCharacter(c, startX, y, scale, r, g, b);
            startX += charWidth + charSpacing;
        }
    }

    /**
     * Dibuja un carácter en un grid de bloques 5x4.
     */
    private void drawCharacter(char letra, float x, float y, float scale,
            float r, float g, float b) {
        boolean[][] pattern = getBlockPattern(letra);
        if (pattern == null) {
            return;
        }

        float width = 0.14f * scale;
        float height = 0.18f * scale;
        float paddingX = 0.01f * scale;
        float paddingY = 0.01f * scale;
        float blockWidth = (width - paddingX * 3) / 4.0f;
        float blockHeight = (height - paddingY * 4) / 5.0f;

        for (int row = 0; row < pattern.length; row++) {
            for (int col = 0; col < pattern[row].length; col++) {
                if (!pattern[row][col]) {
                    continue;
                }
                float blockX = x + col * (blockWidth + paddingX) + blockWidth / 2.0f;
                float blockY = y + (4 - row) * (blockHeight + paddingY) + blockHeight / 2.0f;
                drawRect(blockX, blockY, blockWidth, blockHeight, 0.0f, r, g, b);
            }
        }
    }

    private boolean[][] getBlockPattern(char c) {
        switch (c) {
            case 'G': return new boolean[][] {
                { true, true, true, true },
                { true, false, false, false },
                { true, false, true, true },
                { true, false, false, true },
                { true, true, true, true }
            };
            case 'A': return new boolean[][] {
                { false, true, true, false },
                { true, false, false, true },
                { true, true, true, true },
                { true, false, false, true },
                { true, false, false, true }
            };
            case 'M': return new boolean[][] {
                { true, false, false, true },
                { true, true, true, true },
                { true, false, false, true },
                { true, false, false, true },
                { true, false, false, true }
            };
            case 'E': return new boolean[][] {
                { true, true, true, true },
                { true, false, false, false },
                { true, true, true, false },
                { true, false, false, false },
                { true, true, true, true }
            };
            case 'O': return new boolean[][] {
                { true, true, true, true },
                { true, false, false, true },
                { true, false, false, true },
                { true, false, false, true },
                { true, true, true, true }
            };
            case 'V': return new boolean[][] {
                { true, false, false, true },
                { true, false, false, true },
                { true, false, false, true },
                { false, true, true, false },
                { false, true, true, false }
            };
            case 'R': return new boolean[][] {
                { true, true, true, false },
                { true, false, false, true },
                { true, true, true, false },
                { true, false, true, false },
                { true, false, false, true }
            };
            case 'P': return new boolean[][] {
                { true, true, true, false },
                { true, false, false, true },
                { true, true, true, false },
                { true, false, false, false },
                { true, false, false, false }
            };
            case 'S': return new boolean[][] {
                { true, true, true, true },
                { true, false, false, false },
                { true, true, true, true },
                { false, false, false, true },
                { true, true, true, true }
            };
            case 'I': return new boolean[][] {
                { true, true, true, true },
                { false, true, false, false },
                { false, true, false, false },
                { false, true, false, false },
                { true, true, true, true }
            };
            case 'N': return new boolean[][] {
                { true, false, false, true },
                { true, true, false, true },
                { true, false, true, true },
                { true, false, false, true },
                { true, false, false, true }
            };
            case 'T': return new boolean[][] {
                { true, true, true, true },
                { false, true, false, false },
                { false, true, false, false },
                { false, true, false, false },
                { false, true, false, false }
            };
            case 'U': return new boolean[][] {
                { true, false, false, true },
                { true, false, false, true },
                { true, false, false, true },
                { true, false, false, true },
                { true, true, true, true }
            };
            case 'C': return new boolean[][] {
                { true, true, true, true },
                { true, false, false, false },
                { true, false, false, false },
                { true, false, false, false },
                { true, true, true, true }
            };
            default:
                return null;
        }
    }

    /**
     * Dibuja un ícono de reinicio: flecha circular aproximada.
     */
    private void dibujarIconoReinicio(float x, float y, float size) {
        // Círculo exterior
        drawCircleApprox(x, y, size * 0.5f, 0.0f, 0.0f, 0.8f, 0.0f);
        // Flecha: triángulo apuntando a la derecha
        drawTriangle(x + size * 0.2f, y, size * 0.3f, 0.0f, 0.0f, 0.8f, 0.0f);
        // Cola de la flecha
        drawRect(x - size * 0.1f, y, size * 0.3f, size * 0.05f, 0.0f, 0.0f, 0.8f, 0.0f);
    }

    // Actualiza feedback visual en barra de titulo.
    private void actualizarTitulo() {
        String tituloBase = String.format("Flappy Bird OpenGL | P1: %d  P2: %d", player1.getScore(), player2.getScore());
        if (!started) {
            GLFW.glfwSetWindowTitle(window, tituloBase + " | SPACE o W/ARRIBA para empezar");
        } else if (gameOver) {
            GLFW.glfwSetWindowTitle(window, tituloBase + " | GAME OVER - SPACE o W/ARRIBA para reiniciar");
        } else {
            GLFW.glfwSetWindowTitle(window, tituloBase);
        }
    }

    /**
     * Calcula el nivel de dificultad basado en el puntaje máximo de los dos jugadores.
     * Fórmula: (scoreMaximo / PUNTOS_POR_NIVEL) + 1, limitado a NIVEL_MAXIMO
     * Ejemplo:
     * - 0-2 puntos: nivel 1
     * - 3-5 puntos: nivel 2
     * - 6-8 puntos: nivel 3
     * - 9-11 puntos: nivel 4
     * - 12+ puntos: nivel 5
     */
    private int calcularNivelDificultad() {
        int scoreMaximo = Math.max(player1.getScore(), player2.getScore());
        int nivel = (scoreMaximo / PUNTOS_POR_NIVEL) + 1;
        return Math.min(nivel, NIVEL_MAXIMO);
    }

    /**
     * Calcula la velocidad de las tuberías según el nivel de dificultad.
     * Velocidad base en nivel 1, se multiplica por factor progresivo:
     * - Nivel 1: velocidad base × 1.00
     * - Nivel 2: velocidad base × 1.15
     * - Nivel 3: velocidad base × 1.30
     * - Nivel 4: velocidad base × 1.45
     * - Nivel 5: velocidad base × 1.60
     */
    private float calcularVelocidadPorNivel(int nivel) {
        float[] multiplicadores = { 1.0f, 1.15f, 1.30f, 1.45f, 1.60f };
        if (nivel < 1 || nivel > NIVEL_MAXIMO) {
            return velocidadBaseTuberias;
        }
        return velocidadBaseTuberias * multiplicadores[nivel - 1];
    }

    /**
     * Actualiza la dificultad actual según el puntaje máximo.
     * Se llama cada frame en actualizar() para permitir cambio dinámico de dificultad.
     */
    private void actualizarDificultad() {
        int nivelAnterior = nivelDificultad;
        nivelDificultad = calcularNivelDificultad();
        velocidadActualTuberias = calcularVelocidadPorNivel(nivelDificultad);
    }

    /**
     * Dibuja el indicador visual de dificultad: 5 cuadros en la esquina superior derecha.
     * - Cuadros verdes según el nivel actual.
     * - Cuadros blancos para los niveles no alcanzados.
     * Se dibuja en la esquina superior derecha de la pantalla sin interferir con otros elementos.
     */
    private void dibujarIndicadorDificultad() {
        // Parámetros del indicador.
        final float CUADRO_TAMAÑO = 0.05f;
        final float ESPACIADO = 0.06f;
        final float MARGEN_DERECHA = 0.05f;
        final float MARGEN_ARRIBA = 0.05f;
        
        // Posición inicial (esquina superior derecha).
        float posInicialX = 0.9f - MARGEN_DERECHA;
        float posY = 0.95f - MARGEN_ARRIBA;
        
        // Activar pipeline y malla base.
        GL20.glUseProgram(programa);
        GL30.glBindVertexArray(vao);
        
        // Dibujar 5 cuadros.
        for (int i = 0; i < NIVEL_MAXIMO; i++) {
            // Calcular posición del cuadro (de derecha a izquierda).
            float posX = posInicialX - (i * ESPACIADO);
            
            // Color según si el cuadro está activo (verde) o no (blanco).
            float r, g, b;
            if (i < nivelDificultad) {
                // Verde para cuadros activos.
                r = 0.2f;
                g = 0.9f;
                b = 0.3f;
            } else {
                // Blanco para cuadros inactivos.
                r = 1.0f;
                g = 1.0f;
                b = 1.0f;
            }
            
            // Dibujar cuadro.
            drawRect(posX, posY, CUADRO_TAMAÑO, CUADRO_TAMAÑO, 0.0f, r, g, b);
        }
    }

    /**
     * Actualiza elementos decorativos para efecto parallax.
     * Los elementos se mueven a diferentes velocidades para crear profundidad visual.
     * No afecta la física ni la colisión del juego.
     */
    private void actualizarElementosDecorativos(float dt) {
        // Nubes se mueven lentamente (parallax).
        offsetNubes -= 0.1f * dt; // Muy lento.
        // Montañas se mueven aún más lento.
        offsetMontanas -= 0.05f * dt; // Más lento que nubes.
        // Suelo se mueve un poco más rápido para sensación de velocidad.
        offsetSuelo -= 0.3f * dt; // Más rápido que tuberías.

        // Actualizar posiciones de nubes.
        for (int i = 0; i < nubesX.size(); i++) {
            nubesX.set(i, nubesX.get(i) - 0.1f * dt);
            if (nubesX.get(i) < -1.5f) {
                nubesX.set(i, 1.5f); // Reiniciar nube.
            }
        }

        // Actualizar posiciones de césped.
        for (int i = 0; i < cespedX.size(); i++) {
            cespedX.set(i, cespedX.get(i) - 0.3f * dt);
            if (cespedX.get(i) < -1.2f) {
                cespedX.set(i, 1.2f); // Reiniciar césped.
            }
        }
    }

    /**
     * Dibuja el fondo con degradado visual usando franjas horizontales.
     * Simula un cielo con colores que van de azul claro arriba a celeste abajo.
     */
    private void dibujarFondo() {
        // Franjas horizontales para degradado.
        float[] franjasY = { 0.8f, 0.6f, 0.4f, 0.2f, 0.0f, -0.2f, -0.4f, -0.6f };
        float[] colores = {
            0.4f, 0.7f, 0.95f, // Azul claro arriba
            0.5f, 0.75f, 0.95f,
            0.6f, 0.8f, 0.95f,
            0.65f, 0.85f, 0.95f,
            0.7f, 0.9f, 0.95f,
            0.75f, 0.92f, 0.95f,
            0.8f, 0.94f, 0.95f,
            0.85f, 0.96f, 0.95f  // Más claro abajo
        };

        for (int i = 0; i < franjasY.length; i++) {
            float y = franjasY[i];
            float r = colores[i * 3];
            float g = colores[i * 3 + 1];
            float b = colores[i * 3 + 2];
            drawRect(0.0f, y, 2.0f, 0.4f, 0.0f, r, g, b);
        }
    }

    /**
     * Dibuja montañas o colinas en el fondo usando triángulos grandes.
     * Están detrás de todo y se mueven lentamente para efecto parallax.
     */
    private void dibujarMontanas() {
        GL30.glBindVertexArray(vaoTriangle);
        for (int i = 0; i < MONTANAS_X.length; i++) {
            float x = MONTANAS_X[i] + offsetMontanas;
            float altura = MONTANAS_ALTURAS[i];
            // Triángulo grande para montaña.
            drawTriangle(x, -0.5f + altura * 0.5f, altura, 0.0f, 0.3f, 0.6f, 0.3f); // Verde oscuro.
        }
        GL30.glBindVertexArray(vao); // Volver al quad.
    }

    /**
     * Dibuja nubes decorativas usando círculos o rectángulos.
     * Se mueven lentamente para efecto parallax.
     */
    private void dibujarNubes() {
        for (float nubeX : nubesX) {
            float x = nubeX + offsetNubes;
            // Nube compuesta por varios círculos.
            drawCircleApprox(x - 0.05f, 0.6f, 0.08f, 0.0f, 1.0f, 1.0f, 1.0f);
            drawCircleApprox(x, 0.6f, 0.1f, 0.0f, 1.0f, 1.0f, 1.0f);
            drawCircleApprox(x + 0.05f, 0.6f, 0.08f, 0.0f, 1.0f, 1.0f, 1.0f);
            drawCircleApprox(x - 0.02f, 0.65f, 0.06f, 0.0f, 1.0f, 1.0f, 1.0f);
            drawCircleApprox(x + 0.02f, 0.65f, 0.06f, 0.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    /**
     * Dibuja el suelo inferior con franja verde y césped decorativo.
     * El suelo se mueve para sensación de velocidad.
     */
    private void dibujarSuelo() {
        // Franja de suelo verde/marrón.
        drawRect(0.0f + offsetSuelo * 0.1f, -0.9f, 4.0f, 0.2f, 0.0f, 0.2f, 0.5f, 0.1f);

        // Césped decorativo encima del suelo.
        for (float cesped : cespedX) {
            float x = cesped + offsetSuelo;
            drawTriangle(x, -0.8f, 0.02f, 0.0f, 0.1f, 0.4f, 0.1f); // Triángulos verdes pequeños.
        }
    }

    /**
     * Dibuja una tubería decorada con borde, sombra y tapas.
     * Mejora visual sin afectar la colisión.
     */
    private void dibujarTuberiaDecorada(Tuberia t) {
        // Calcular limites verticales del hueco.
        float gapTop = t.gapCentroY + (GAP_ALTO * 0.5f);
        float gapBottom = t.gapCentroY - (GAP_ALTO * 0.5f);

        // Tramo superior de tuberia.
        float altoSuperior = 1.0f - gapTop;
        if (altoSuperior > 0.0f) {
            float yCentroSup = gapTop + (altoSuperior * 0.5f);
            // Sombra lateral.
            drawRect(t.x + 0.01f, yCentroSup, TUBERIA_ANCHO, altoSuperior, 0.0f, 0.1f, 0.5f, 0.15f);
            // Cuerpo principal.
            drawRect(t.x, yCentroSup, TUBERIA_ANCHO, altoSuperior, 0.0f, 0.18f, 0.70f, 0.25f);
            // Borde.
            drawRect(t.x, yCentroSup, TUBERIA_ANCHO + 0.01f, altoSuperior + 0.01f, 0.0f, 0.1f, 0.6f, 0.2f);
            // Tapa superior.
            drawRect(t.x, gapTop + 0.02f, TUBERIA_ANCHO + 0.04f, 0.04f, 0.0f, 0.15f, 0.65f, 0.22f);
        }

        // Tramo inferior de tuberia.
        float altoInferior = gapBottom + 1.0f;
        if (altoInferior > 0.0f) {
            float yCentroInf = -1.0f + (altoInferior * 0.5f);
            // Sombra lateral.
            drawRect(t.x + 0.01f, yCentroInf, TUBERIA_ANCHO, altoInferior, 0.0f, 0.1f, 0.5f, 0.15f);
            // Cuerpo principal.
            drawRect(t.x, yCentroInf, TUBERIA_ANCHO, altoInferior, 0.0f, 0.18f, 0.70f, 0.25f);
            // Borde.
            drawRect(t.x, yCentroInf, TUBERIA_ANCHO + 0.01f, altoInferior + 0.01f, 0.0f, 0.1f, 0.6f, 0.2f);
            // Tapa inferior.
            drawRect(t.x, gapBottom - 0.02f, TUBERIA_ANCHO + 0.04f, 0.04f, 0.0f, 0.15f, 0.65f, 0.22f);
        }
    }

    /**
     * Bucle principal:
     * - calcula dt,
     * - procesa input,
     * - actualiza logica,
     * - renderiza,
     * - swap/poll.
     */
    private void loop() {
        float ultimoTiempo = (float) GLFW.glfwGetTime();
        while (!GLFW.glfwWindowShouldClose(window)) {
            float ahora = (float) GLFW.glfwGetTime();
            float dt = ahora - ultimoTiempo;
            ultimoTiempo = ahora;
            // Limite de dt para evitar "saltos" grandes si el frame se congela.
            if (dt > 0.033f) {
                dt = 0.033f;
            }

            procesarInput();
            actualizar(dt);
            render();

            // Presentar frame y leer eventos.
            GLFW.glfwSwapBuffers(window);
            GLFW.glfwPollEvents();
        }
    }

    // Liberacion de recursos.
    private void cleanup() {
        GL30.glDeleteVertexArrays(vao);
        GL15.glDeleteBuffers(vbo);
        GL30.glDeleteVertexArrays(vaoTriangle);
        GL15.glDeleteBuffers(vboTriangle);
        GL30.glDeleteVertexArrays(vaoCircle);
        GL15.glDeleteBuffers(vboCircle);
        GL20.glDeleteProgram(programa);
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }

    // Entry point.
    public static void main(String[] args) {
        new AppFlappyBird().run();
    }
}
