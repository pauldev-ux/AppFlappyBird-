package com.graphics;

public class GameConfig {

    public static final int WIN_SCORE = 5;

    // Tamaño de la ventana del juego.
    // Para cambiar la resolución inicial, modifica WIDTH y HEIGHT.
    public static final int WIDTH = 1200;
    public static final int HEIGHT = 900;

    // Posición horizontal inicial de cada jugador.
    // Más negativo = más a la izquierda, más positivo = más a la derecha.
    public static final float BIRD_X_PLAYER1 = -0.45f;
    public static final float BIRD_X_PLAYER2 = -0.15f;
    public static final float BIRD_X_PLAYER3 = 0.15f;

    // Tamaño lógico del pájaro usado principalmente para colisiones.
    // Si lo aumentas, también cambia la zona donde choca.
    public static final float BIRD_WIDTH = 0.10f;
    public static final float BIRD_HEIGHT = 0.10f;

    // Física del pájaro.
    // Para que caiga más rápido, haz GRAVITY más negativo, por ejemplo -2.3f.
    public static final float GRAVITY = -1.9f;

    // Fuerza del salto.
    // Para saltar más alto, aumenta este valor, por ejemplo 1.0f.
    public static final float JUMP_IMPULSE = 4.85f;

    // Límite máximo de caída.
    // Evita que el pájaro caiga demasiado rápido.
    public static final float MAX_FALL_SPEED = -1.8f;

    // Duración del aleteo después de saltar.
    // Para que el ala tarde más en volver, aumenta este valor.
    public static final float WING_FLAP_DURATION = 0.18f;

    // Ángulo del ala al saltar.
    // Para un aleteo más notorio, aumenta este valor.
    public static final float WING_JUMP_ANGLE = 0.35f;

    // Ángulo del ala cuando el pájaro sube.
    public static final float WING_RISE_ANGLE = 0.12f;

    // Ángulo del ala cuando el pájaro cae.
    public static final float WING_FALL_ANGLE = -0.18f;

    // Velocidad de oscilación del ala.
    // Para que el ala se mueva más rápido, aumenta este valor.
    public static final float WING_OSCILLATION_SPEED = 18.0f;

    // Amplitud del movimiento del ala.
    // Para que el ala se mueva más exagerado, aumenta este valor.
    public static final float WING_OSCILLATION_AMPLITUDE = 0.04f;

    // Ancho de las tuberías.
    // Para hacerlas más gruesas, aumenta este valor.
    public static final float PIPE_WIDTH = 0.18f;

    // Altura del espacio entre tubería superior e inferior.
    // Más grande = más fácil, más pequeño = más difícil.
    public static final float GAP_HEIGHT = 0.48f;

    // Velocidad base de las tuberías.
    // Para que el juego inicie más rápido, aumenta este valor.

    public static final float PIPE_SPEED = 0.62f;

    // Tiempo entre aparición de tuberías.
    // Menor valor = aparecen más seguido.
    public static final float PIPE_SPAWN_INTERVAL = 1.5f;

    // Límites verticales donde puede aparecer el centro del hueco.
    // Sirve para que el hueco no salga demasiado arriba o abajo.
    public static final float GAP_MIN_CENTER = -0.45f;
    public static final float GAP_MAX_CENTER = 0.45f;

    // Nivel máximo de dificultad.
    // Si aumentas esto, también debes ajustar el HUD de dificultad.
    public static final int LEVEL_MAX = 5;

    // Cada cuántos puntos sube la dificultad.
    // Por ejemplo, 5 haría que suba cada 5 tuberías pasadas.
    public static final int POINTS_PER_LEVEL = 2;

    // Posiciones horizontales de las montañas del fondo.
    // Para agregar más montañas, añade más valores aquí y en MOUNTAINS_HEIGHTS.
    public static final float[] MOUNTAINS_X = { -0.8f, -0.2f, 0.4f, 1.0f };

    // Alturas de las montañas.
    // Debe tener la misma cantidad de valores que MOUNTAINS_X.
    public static final float[] MOUNTAINS_HEIGHTS = { 0.3f, 0.25f, 0.35f, 0.2f };

    // Color RGB del jugador 1.
    // Valores van de 0.0f a 1.0f. Ejemplo rojo: 1.0f, 0.1f, 0.1f.
    public static final float BIRD1_COLOR_R = 0.98f;
    public static final float BIRD1_COLOR_G = 0.85f;
    public static final float BIRD1_COLOR_B = 0.20f;

    // Color RGB del jugador 2.
    // Ejemplo morado: 0.6f, 0.2f, 0.9f.
    public static final float BIRD2_COLOR_R = 0.35f;
    public static final float BIRD2_COLOR_G = 0.70f;
    public static final float BIRD2_COLOR_B = 0.98f;

    //jugador 3.
    public static final float BIRD3_COLOR_R = 0.70f;
    public static final float BIRD3_COLOR_G = 0.38f;
    public static final float BIRD3_COLOR_B = 0.95f;

    // Constructor privado para evitar crear objetos de configuración.
    private GameConfig() {
        // Utility class.
    }
}