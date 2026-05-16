package com.graphics;

public class GameConfig {
    public static final int WIDTH = 900;
    public static final int HEIGHT = 700;

    public static final float BIRD_X_PLAYER1 = -0.45f;
    public static final float BIRD_X_PLAYER2 = -0.15f;
    public static final float BIRD_WIDTH = 0.10f;
    public static final float BIRD_HEIGHT = 0.10f;
    public static final float GRAVITY = -1.9f;
    public static final float JUMP_IMPULSE = 0.85f;
    public static final float MAX_FALL_SPEED = -1.8f;

    public static final float WING_FLAP_DURATION = 0.18f;
    public static final float WING_JUMP_ANGLE = 0.35f;
    public static final float WING_RISE_ANGLE = 0.12f;
    public static final float WING_FALL_ANGLE = -0.18f;
    public static final float WING_OSCILLATION_SPEED = 18.0f;
    public static final float WING_OSCILLATION_AMPLITUDE = 0.04f;

    public static final float PIPE_WIDTH = 0.18f;
    public static final float GAP_HEIGHT = 0.48f;
    public static final float PIPE_SPEED = 0.62f;
    public static final float PIPE_SPAWN_INTERVAL = 1.5f;
    public static final float GAP_MIN_CENTER = -0.45f;
    public static final float GAP_MAX_CENTER = 0.45f;

    public static final int LEVEL_MAX = 5;
    public static final int POINTS_PER_LEVEL = 3;

    public static final float[] MOUNTAINS_X = { -0.8f, -0.2f, 0.4f, 1.0f };
    public static final float[] MOUNTAINS_HEIGHTS = { 0.3f, 0.25f, 0.35f, 0.2f };

    public static final float BIRD1_COLOR_R = 0.98f;
    public static final float BIRD1_COLOR_G = 0.85f;
    public static final float BIRD1_COLOR_B = 0.20f;
    public static final float BIRD2_COLOR_R = 0.35f;
    public static final float BIRD2_COLOR_G = 0.70f;
    public static final float BIRD2_COLOR_B = 0.98f;

    private GameConfig() {
        // Utility class.
    }
}
