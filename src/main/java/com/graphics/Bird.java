package com.graphics;

/**
 * Representa el estado y comportamiento del jugador/pajaro en Flappy Bird.
 * Guarda posicion, velocidad, color, puntuacion y estado de vida.
 */
public class Bird {

    // Posición horizontal fija del pájaro.
    // Para mover al jugador más a la izquierda/derecha, cambia su X al crear el Bird.
    private final float x;

    // Posición vertical del pájaro.
    // Cambia cuando salta o cae.
    private float y;

    // Velocidad vertical.
    // Positiva = sube, negativa = baja.
    private float velocityY;

    // Indica si el pájaro sigue jugando.
    private boolean alive;

    // Puntaje individual del jugador.
    private int score;

    // Color RGB del pájaro.
    // Para cambiar el color, modifica estos valores al crear el Bird.
    private float colorR;
    private float colorG;
    private float colorB;

    // Gravedad aplicada al pájaro.
    // Más negativo = cae más rápido.
    private static final float GRAVITY = -1.9f;

    // Fuerza del salto.
    // Mayor valor = salto más alto.
    private static final float JUMP_IMPULSE = 0.85f;

    // Límite de velocidad de caída.
    // Evita que el pájaro caiga demasiado rápido.
    private static final float MAX_FALL_SPEED = -1.8f;

    // Crea un pájaro con posición inicial y color.
    // Ejemplo color rojo: new Bird(x, y, 1.0f, 0.1f, 0.1f)
    public Bird(float x, float y, float colorR, float colorG, float colorB) {
        this.x = x;
        this.y = y;
        this.velocityY = 0.0f;
        this.alive = true;
        this.score = 0;
        this.colorR = colorR;
        this.colorG = colorG;
        this.colorB = colorB;
    }

    // Hace saltar al pájaro.
    // Para cambiar la fuerza, modifica JUMP_IMPULSE.
    public void jump() {
        this.velocityY = JUMP_IMPULSE;
    }

    // Actualiza la física del pájaro en cada frame.
    // Aplica gravedad, limita caída y cambia la posición vertical.
    public void update(float deltaTime) {
        if (!alive) {
            return;
        }

        velocityY += GRAVITY * deltaTime;

        if (velocityY < MAX_FALL_SPEED) {
            velocityY = MAX_FALL_SPEED;
        }

        y += velocityY * deltaTime;
    }

    // Marca al pájaro como muerto.
    // Se usa cuando choca con tubería, suelo o techo.
    public void kill() {
        alive = false;
    }

    // Reinicia el pájaro para una nueva partida.
    // También reinicia velocidad, vida y puntaje.
    public void reset(float startY) {
        y = startY;
        velocityY = 0.0f;
        alive = true;
        score = 0;
    }

    // Devuelve la posición horizontal del pájaro.
    public float getX() {
        return x;
    }

    // Devuelve la posición vertical del pájaro.
    public float getY() {
        return y;
    }

    // Devuelve la velocidad vertical.
    // Se usa, por ejemplo, para inclinar visualmente el pájaro.
    public float getVelocityY() {
        return velocityY;
    }

    // Indica si el pájaro sigue vivo.
    public boolean isAlive() {
        return alive;
    }

    // Devuelve el puntaje del jugador.
    public int getScore() {
        return score;
    }

    // Devuelve componente roja del color del pájaro.
    public float getColorR() {
        return colorR;
    }

    // Devuelve componente verde del color del pájaro.
    public float getColorG() {
        return colorG;
    }

    // Devuelve componente azul del color del pájaro.
    public float getColorB() {
        return colorB;
    }

    // Suma un punto al jugador.
    // Se llama cuando el pájaro pasa una tubería.
    public void addScore() {
        score +=2;
    }
}