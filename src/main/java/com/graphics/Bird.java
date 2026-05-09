package com.graphics;

/**
 * Representa el estado y comportamiento del jugador (pajaro) en Flappy Bird.
 * Esta clase guarda posicion, velocidad, color, puntuacion y estado de vida.
 */
public class Bird {

    private final float x;
    private float y;
    private float velocityY;
    private boolean alive;
    private int score;
    private float colorR;
    private float colorG;
    private float colorB;

    private static final float GRAVITY = -1.9f;
    private static final float JUMP_IMPULSE = 0.85f;
    private static final float MAX_FALL_SPEED = -1.8f;

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

    public void jump() {
        this.velocityY = JUMP_IMPULSE;
    }

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

    public void kill() {
        alive = false;
    }

    public void reset(float startY) {
        y = startY;
        velocityY = 0.0f;
        alive = true;
        score = 0;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getVelocityY() {
        return velocityY;
    }

    public boolean isAlive() {
        return alive;
    }

    public int getScore() {
        return score;
    }

    public float getColorR() {
        return colorR;
    }

    public float getColorG() {
        return colorG;
    }

    public float getColorB() {
        return colorB;
    }

    public void addScore() {
        score++;
    }
}
