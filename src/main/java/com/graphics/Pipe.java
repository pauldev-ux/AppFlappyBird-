package com.graphics;

public class Pipe {
    public float x;
    public float gapCentroY;
    public boolean puntuadaP1;
    public boolean puntuadaP2;

    public Pipe(float x, float gapCentroY) {
        this.x = x;
        this.gapCentroY = gapCentroY;
        this.puntuadaP1 = false;
        this.puntuadaP2 = false;
    }
}
