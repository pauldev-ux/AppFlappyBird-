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

    // Posicion horizontal fija del pajaro en NDC.
    private static final float BIRD_X = -0.45f;
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
    private Bird bird;
    private float timerSpawn;

    // Animacion de ala.
    private float wingAnimTime;
    private float wingAngle;
    private float wingFlapTimer;

    private boolean started;
    private boolean gameOver;
    private boolean prevSpace;
    private boolean prevR;

    // Lista de obstaculos activos.
    private final List<Tuberia> tuberias = new ArrayList<>();
    // RNG para variar la posicion del gap.
    private final Random random = new Random();

    /**
     * Modelo de una tuberia:
     * x: posicion horizontal comun para parte superior/inferior,
     * gapCentroY: centro vertical del hueco,
     * puntuada: evita sumar dos veces la misma tuberia.
     */
    private static class Tuberia {
        float x;
        float gapCentroY;
        boolean puntuada;

        Tuberia(float x, float gapCentroY) {
            this.x = x;
            this.gapCentroY = gapCentroY;
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
        if (bird == null) {
            bird = new Bird(BIRD_X, 0.0f, 0.98f, 0.85f, 0.20f);
        } else {
            bird.reset(0.0f);
        }
        timerSpawn = 0.0f;
        wingAnimTime = 0.0f;
        wingAngle = 0.0f;
        wingFlapTimer = 0.0f;
        started = false;
        gameOver = false;
        tuberias.clear();
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
            bird.jump();
            // Iniciar animacion de aleteo al saltar.
            wingFlapTimer = WING_FLAP_DURATION;
        }
        prevSpace = spaceAhora;

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

        // Actualizar fisica del pajaro.
        bird.update(dt);

        // Colision contra techo/suelo NDC.
        float birdTop = bird.getY() + (BIRD_ALTO * 0.5f);
        float birdBottom = bird.getY() - (BIRD_ALTO * 0.5f);
        if (birdTop >= 1.0f || birdBottom <= -1.0f) {
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

        Iterator<Tuberia> it = tuberias.iterator();
        while (it.hasNext()) {
            Tuberia t = it.next();
            // Avance horizontal de obstaculos (derecha -> izquierda).
            t.x -= VELOCIDAD_TUBERIAS * dt;

            // Puntuar cuando la tuberia ya quedo atras del pajaro.
            if (t.x + (TUBERIA_ANCHO * 0.5f) < bird.getX() && !t.puntuada) {
                t.puntuada = true;
                bird.addScore();
                actualizarTitulo();
            }

            if (colisionaConTuberia(t)) {
                gameOver = true;
                bird.kill();
                actualizarTitulo();
                return;
            }

            // Remover tuberias fuera de pantalla para no acumular memoria.
            if (t.x + (TUBERIA_ANCHO * 0.5f) < -1.3f) {
                it.remove();
            }
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
    private boolean colisionaConTuberia(Tuberia t) {
        float birdLeft = BIRD_X - (BIRD_ANCHO * 0.5f);
        float birdRight = BIRD_X + (BIRD_ANCHO * 0.5f);
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
     * - fondo,
     * - tuberias,
     * - pajaro,
     * - franja central en game over.
     */
    private void render() {
        // Cielo.
        GL11.glClearColor(0.52f, 0.80f, 0.92f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        // Activar pipeline y malla base.
        GL20.glUseProgram(programa);
        GL30.glBindVertexArray(vao);

        for (Tuberia t : tuberias) {
            // Calcular limites verticales del hueco.
            float gapTop = t.gapCentroY + (GAP_ALTO * 0.5f);
            float gapBottom = t.gapCentroY - (GAP_ALTO * 0.5f);

            // Tramo superior de tuberia.
            float altoSuperior = 1.0f - gapTop;
            if (altoSuperior > 0.0f) {
                float yCentroSup = gapTop + (altoSuperior * 0.5f);
                drawRect(t.x, yCentroSup, TUBERIA_ANCHO, altoSuperior, 0.0f, 0.18f, 0.70f, 0.25f);
            }

            // Tramo inferior de tuberia.
            float altoInferior = gapBottom + 1.0f;
            if (altoInferior > 0.0f) {
                float yCentroInf = -1.0f + (altoInferior * 0.5f);
                drawRect(t.x, yCentroInf, TUBERIA_ANCHO, altoInferior, 0.0f, 0.18f, 0.70f, 0.25f);
            }
        }

        //. Dibujar pajaro.
        //. La posición X queda fija y la posición Y cambia según la gravedad y el salto.
        drawBird(bird);

        // Overlay simple de game over (sin texto en framebuffer).
        if (gameOver) {
            drawRect(0.0f, 0.0f, 2.0f, 0.22f, 0.0f, 0.15f, 0.18f, 0.22f);
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
        float birdRotation = calculateBirdRotation();
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

    private float calculateBirdRotation() {
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
        float targetAngle;
        if (wingFlapTimer > 0.0f) {
            targetAngle = WING_JUMP_ANGLE;
        } else if (bird.getVelocityY() >= 0.0f) {
            targetAngle = WING_RISE_ANGLE;
        } else {
            targetAngle = WING_FALL_ANGLE;
        }
        wingAngle += (targetAngle - wingAngle) * Math.min(1.0f, dt * 10.0f);
    }

    private float toRadians(float degrees) {
        return degrees * ((float) Math.PI / 180.0f);
    }

    // Actualiza feedback visual en barra de titulo.
    private void actualizarTitulo() {
        String tituloBase = "Flappy Bird OpenGL | Puntos: " + bird.getScore();
        if (!started) {
            GLFW.glfwSetWindowTitle(window, tituloBase + " | SPACE para empezar");
        } else if (gameOver) {
            GLFW.glfwSetWindowTitle(window, tituloBase + " | GAME OVER - SPACE o R para reiniciar");
        } else {
            GLFW.glfwSetWindowTitle(window, tituloBase);
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
