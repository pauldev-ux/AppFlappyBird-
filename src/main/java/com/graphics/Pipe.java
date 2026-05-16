package com.graphics;

public class Pipe {
    // Posición horizontal de la tubería.
    // Disminuye con el tiempo para que la tubería se mueva hacia la izquierda.
    public float x;

    // Centro vertical del hueco entre la tubería superior e inferior.
    // Para hacer huecos más arriba o abajo, se cambia este valor al crear la tubería.
    public float gapCentroY;

    // Indica si esta tubería ya dio punto al jugador 1.
    // Evita sumar puntos repetidos por la misma tubería.
    public boolean puntuadaP1;

    // Indica si esta tubería ya dio punto al jugador 2.
    // Permite que cada jugador tenga su puntaje independiente.
    public boolean puntuadaP2;

    // Crea una tubería con posición X y centro del hueco.
    // Ejemplo: new Pipe(1.2f, 0.0f) crea una tubería a la derecha con hueco centrado.
    public Pipe(float x, float gapCentroY) {
        this.x = x;
        this.gapCentroY = gapCentroY;
        this.puntuadaP1 = false;
        this.puntuadaP2 = false;
    }
}