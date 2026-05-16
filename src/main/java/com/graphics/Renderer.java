package com.graphics;

import java.nio.FloatBuffer;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class Renderer {
    private int program;
    private int vaoQuad;
    private int vboQuad;
    private int vaoTriangle;
    private int vboTriangle;
    private int vaoCircle;
    private int vboCircle;

    private int uOffsetLocation;
    private int uScaleLocation;
    private int uRotationLocation;
    private int uColorLocation;

    public void init() {
        crearShaders();
        crearQuadBase();
        crearTriangle();
        crearCircleApprox();
    }

    public void cleanup() {
        GL30.glDeleteVertexArrays(vaoQuad);
        GL15.glDeleteBuffers(vboQuad);
        GL30.glDeleteVertexArrays(vaoTriangle);
        GL15.glDeleteBuffers(vboTriangle);
        GL30.glDeleteVertexArrays(vaoCircle);
        GL15.glDeleteBuffers(vboCircle);
        GL20.glDeleteProgram(program);
    }

    public void renderScene(GameState gameState, Bird player1, Bird player2, List<Pipe> pipes,
            List<Float> cloudsX, List<Float> grassX,
            float offsetClouds, float offsetMountains, float offsetGround,
            int difficultyLevel, float wingAnimTime, float wingAngle) {
        GL11.glClearColor(0.52f, 0.80f, 0.92f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL20.glUseProgram(program);
        drawBackground(cloudsX, offsetClouds, offsetMountains, offsetGround, grassX);
        drawPipes(pipes);
        drawBird(player1, wingAnimTime, wingAngle);
        drawBird(player2, wingAnimTime, wingAngle);
        drawHUD(difficultyLevel);

        if (gameState == GameState.GAME_OVER) {
            drawGameOver();
        }
    }

    public void drawRect(float x, float y, float ancho, float alto, float rotation, float r, float g, float b) {
        GL30.glBindVertexArray(vaoQuad);
        GL20.glUniform2f(uOffsetLocation, x, y);
        GL20.glUniform2f(uScaleLocation, ancho, alto);
        GL20.glUniform1f(uRotationLocation, rotation);
        GL20.glUniform3f(uColorLocation, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
    }

    public void drawTriangle(float x, float y, float scale, float rotation, float r, float g, float b) {
        GL30.glBindVertexArray(vaoTriangle);
        GL20.glUniform2f(uOffsetLocation, x, y);
        GL20.glUniform2f(uScaleLocation, scale, scale);
        GL20.glUniform1f(uRotationLocation, rotation);
        GL20.glUniform3f(uColorLocation, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
    }

    public void drawCircleApprox(float x, float y, float radius, float rotation, float r, float g, float b) {
        GL30.glBindVertexArray(vaoCircle);
        GL20.glUniform2f(uOffsetLocation, x, y);
        GL20.glUniform2f(uScaleLocation, radius * 2, radius * 2);
        GL20.glUniform1f(uRotationLocation, rotation);
        GL20.glUniform3f(uColorLocation, r, g, b);
        GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN, 0, 14);
    }

    public void drawBird(Bird bird, float wingAnimTime, float wingAngle) {
        float birdRotation = calculateBirdRotation(bird);
        float x = bird.getX();
        float y = bird.getY();
        float bodyScale = 1.5f;
        float bodyRadius = GameConfig.BIRD_WIDTH * 0.35f * bodyScale;

        drawCircleApprox(x, y, bodyRadius, birdRotation, bird.getColorR(), bird.getColorG(), bird.getColorB());

        float beakOffsetX = bodyRadius * 0.85f;
        float beakScale = GameConfig.BIRD_WIDTH * 0.15f * bodyScale;
        float[] beakOffset = rotateOffset(beakOffsetX, 0.0f, birdRotation);
        drawTriangle(x + beakOffset[0], y + beakOffset[1], beakScale, birdRotation - toRadians(90.0f), 0.9f, 0.6f, 0.1f);

        float wingOffsetX = -bodyRadius * 0.45f;
        float wingScale = GameConfig.BIRD_WIDTH * 0.25f * bodyScale;
        float[] wingOffset = rotateOffset(wingOffsetX, 0.0f, birdRotation);
        float wingOscillation = (float) Math.sin(wingAnimTime * GameConfig.WING_OSCILLATION_SPEED) * GameConfig.WING_OSCILLATION_AMPLITUDE;
        drawTriangle(x + wingOffset[0], y + wingOffset[1], wingScale,
                birdRotation + toRadians(90.0f) + wingAngle + wingOscillation,
                0.8f, 0.7f, 0.15f);

        float tailOffsetX = -bodyRadius * 1.05f;
        float tailY = bodyRadius * 0.3f;
        float tailScale = GameConfig.BIRD_WIDTH * 0.12f * bodyScale;
        float[] tailOffsetUp = rotateOffset(tailOffsetX, tailY, birdRotation);
        float[] tailOffsetDown = rotateOffset(tailOffsetX, -tailY, birdRotation);
        drawTriangle(x + tailOffsetUp[0], y + tailOffsetUp[1], tailScale, birdRotation + toRadians(90.0f), 0.98f, 0.85f, 0.20f);
        drawTriangle(x + tailOffsetDown[0], y + tailOffsetDown[1], tailScale, birdRotation + toRadians(90.0f), 0.98f, 0.85f, 0.20f);

        float eyeOffsetX = bodyRadius * 0.4f;
        float eyeOffsetY = bodyRadius * 0.45f;
        float eyeRadius = bodyRadius * 0.18f;
        float[] eyeOffset = rotateOffset(eyeOffsetX, eyeOffsetY, birdRotation);
        drawCircleApprox(x + eyeOffset[0], y + eyeOffset[1], eyeRadius, birdRotation, 1.0f, 1.0f, 1.0f);

        float pupilOffsetX = eyeOffsetX + eyeRadius * 0.3f;
        float pupilRadius = eyeRadius * 0.4f;
        float[] pupilOffset = rotateOffset(pupilOffsetX, eyeOffsetY, birdRotation);
        drawCircleApprox(x + pupilOffset[0], y + pupilOffset[1], pupilRadius, birdRotation, 0.0f, 0.0f, 0.0f);
    }

    public void drawPipes(List<Pipe> pipes) {
        for (Pipe pipe : pipes) {
            drawDecoratedPipe(pipe);
        }
    }

    public void drawBackground(List<Float> cloudsX, float offsetClouds, float offsetMountains, float offsetGround, List<Float> grassX) {
        drawSkyGradient();
        drawMountains(offsetMountains);
        drawClouds(cloudsX, offsetClouds);
        drawGround(offsetGround, grassX);
    }

    public void drawHUD(int difficultyLevel) {
        drawDifficultyIndicator(difficultyLevel);
    }

    public void drawGameOver() {
        drawRect(0.0f, 0.0f, 2.0f, 2.0f, 0.0f, 0.0f, 0.04f, 0.04f);
        drawRect(0.0f, 0.0f, 0.70f, 0.36f, 0.0f, 0.0f, 0.02f, 0.02f);
        drawRect(0.0f, 0.0f, 0.66f, 0.32f, 0.0f, 0.10f, 0.10f, 0.18f);
        drawRect(0.0f, 0.0f, 0.63f, 0.28f, 0.0f, 0.16f, 0.16f, 0.24f);
        drawCenteredTextWithShadow("GAME OVER", 0.0f, 0.08f, 1.08f, 1.0f, 0.25f, 0.25f, 0.025f, 0.025f);
        drawCenteredTextWithShadow("PRESIONE R PARA REINICIAR", 0.0f, -0.04f, 0.47f, 1.0f, 0.92f, 0.6f, 0.02f, 0.02f);
        drawRestartIcon(0.0f, -0.16f, 0.10f);
    }

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

        String fragmentSrc = """
            #version 330 core
            uniform vec3 uColor;
            out vec4 fragColor;
            void main() {
                fragColor = vec4(uColor, 1.0);
            }
            """;

        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexSrc);
        GL20.glCompileShader(vertexShader);
        comprobarShader(vertexShader, "Vertex");

        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSrc);
        GL20.glCompileShader(fragmentShader);
        comprobarShader(fragmentShader, "Fragment");

        program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException("Error al enlazar programa: " + GL20.glGetProgramInfoLog(program));
        }

        uOffsetLocation = GL20.glGetUniformLocation(program, "uOffset");
        uScaleLocation = GL20.glGetUniformLocation(program, "uScale");
        uRotationLocation = GL20.glGetUniformLocation(program, "uRotation");
        uColorLocation = GL20.glGetUniformLocation(program, "uColor");
        if (uOffsetLocation == -1 || uScaleLocation == -1 || uRotationLocation == -1 || uColorLocation == -1) {
            throw new RuntimeException("No se pudieron obtener uniforms del shader");
        }

        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
    }

    private void comprobarShader(int shader, String tipo) {
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            throw new RuntimeException(tipo + " shader: " + GL20.glGetShaderInfoLog(shader));
        }
    }

    private void crearQuadBase() {
        float[] vertices = {
            -0.5f, -0.5f, 0.0f,
             0.5f, -0.5f, 0.0f,
             0.5f,  0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,
             0.5f,  0.5f, 0.0f,
            -0.5f,  0.5f, 0.0f
        };

        vaoQuad = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoQuad);
        vboQuad = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboQuad);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    private void crearTriangle() {
        float[] vertices = {
            0.0f,  0.5f, 0.0f,
           -0.433f, -0.25f, 0.0f,
            0.433f, -0.25f, 0.0f
        };

        vaoTriangle = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoTriangle);
        vboTriangle = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboTriangle);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    private void crearCircleApprox() {
        int numSegments = 12;
        float[] vertices = new float[(numSegments + 2) * 3];
        vertices[0] = 0.0f;
        vertices[1] = 0.0f;
        vertices[2] = 0.0f;

        for (int i = 0; i <= numSegments; i++) {
            double angle = 2.0 * Math.PI * i / numSegments;
            float x = (float) Math.cos(angle) * 0.5f;
            float y = (float) Math.sin(angle) * 0.5f;
            vertices[(i + 1) * 3] = x;
            vertices[(i + 1) * 3 + 1] = y;
            vertices[(i + 1) * 3 + 2] = 0.0f;
        }

        vaoCircle = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoCircle);
        vboCircle = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboCircle);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
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
            rotationDeg = (bird.getVelocityY() / GameConfig.JUMP_IMPULSE) * maxUp;
        } else {
            rotationDeg = (bird.getVelocityY() / GameConfig.MAX_FALL_SPEED) * maxDown;
        }
        rotationDeg = Math.max(Math.min(rotationDeg, maxUp), maxDown);
        return toRadians(rotationDeg);
    }

    private float toRadians(float degrees) {
        return degrees * ((float) Math.PI / 180.0f);
    }

    private void drawSkyGradient() {
        float[] stripesY = { 0.8f, 0.6f, 0.4f, 0.2f, 0.0f, -0.2f, -0.4f, -0.6f };
        float[] colors = {
            0.4f, 0.7f, 0.95f,
            0.5f, 0.75f, 0.95f,
            0.6f, 0.8f, 0.95f,
            0.65f, 0.85f, 0.95f,
            0.7f, 0.9f, 0.95f,
            0.75f, 0.92f, 0.95f,
            0.8f, 0.94f, 0.95f,
            0.85f, 0.96f, 0.95f
        };
        for (int i = 0; i < stripesY.length; i++) {
            float y = stripesY[i];
            float r = colors[i * 3];
            float g = colors[i * 3 + 1];
            float b = colors[i * 3 + 2];
            drawRect(0.0f, y, 2.0f, 0.4f, 0.0f, r, g, b);
        }
    }

    private void drawMountains(float offsetMountains) {
        for (int i = 0; i < GameConfig.MOUNTAINS_X.length; i++) {
            float x = GameConfig.MOUNTAINS_X[i] + offsetMountains;
            float height = GameConfig.MOUNTAINS_HEIGHTS[i];
            drawTriangle(x, -0.5f + height * 0.5f, height, 0.0f, 0.3f, 0.6f, 0.3f);
        }
    }

    private void drawClouds(List<Float> cloudsX, float offsetClouds) {
        for (float cloudX : cloudsX) {
            float x = cloudX + offsetClouds;
            drawCircleApprox(x - 0.05f, 0.6f, 0.08f, 0.0f, 1.0f, 1.0f, 1.0f);
            drawCircleApprox(x, 0.6f, 0.10f, 0.0f, 1.0f, 1.0f, 1.0f);
            drawCircleApprox(x + 0.05f, 0.6f, 0.08f, 0.0f, 1.0f, 1.0f, 1.0f);
            drawCircleApprox(x - 0.02f, 0.65f, 0.06f, 0.0f, 1.0f, 1.0f, 1.0f);
            drawCircleApprox(x + 0.02f, 0.65f, 0.06f, 0.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private void drawGround(float offsetGround, List<Float> grassX) {
        drawRect(0.0f + offsetGround * 0.1f, -0.9f, 4.0f, 0.2f, 0.0f, 0.2f, 0.5f, 0.1f);
        for (float grass : grassX) {
            float x = grass + offsetGround;
            drawTriangle(x, -0.8f, 0.02f, 0.0f, 0.1f, 0.4f, 0.1f);
        }
    }

    private void drawDifficultyIndicator(int difficulty) {
        final float SIZE = 0.05f;
        final float SPACING = 0.06f;
        final float MARGIN_RIGHT = 0.05f;
        final float MARGIN_TOP = 0.05f;
        float startX = 0.9f - MARGIN_RIGHT;
        float y = 0.95f - MARGIN_TOP;

        for (int i = 0; i < GameConfig.LEVEL_MAX; i++) {
            float x = startX - i * SPACING;
            if (i < difficulty) {
                drawRect(x, y, SIZE, SIZE, 0.0f, 0.2f, 0.9f, 0.3f);
            } else {
                drawRect(x, y, SIZE, SIZE, 0.0f, 1.0f, 1.0f, 1.0f);
            }
        }
    }

    private void drawDecoratedPipe(Pipe pipe) {
        float gapTop = pipe.gapCentroY + (GameConfig.GAP_HEIGHT * 0.5f);
        float gapBottom = pipe.gapCentroY - (GameConfig.GAP_HEIGHT * 0.5f);

        float topHeight = 1.0f - gapTop;
        if (topHeight > 0.0f) {
            float centerY = gapTop + topHeight * 0.5f;
            drawRect(pipe.x + 0.01f, centerY, GameConfig.PIPE_WIDTH, topHeight, 0.0f, 0.1f, 0.5f, 0.15f);
            drawRect(pipe.x, centerY, GameConfig.PIPE_WIDTH, topHeight, 0.0f, 0.18f, 0.70f, 0.25f);
            drawRect(pipe.x, centerY, GameConfig.PIPE_WIDTH + 0.01f, topHeight + 0.01f, 0.0f, 0.1f, 0.6f, 0.2f);
            drawRect(pipe.x, gapTop + 0.02f, GameConfig.PIPE_WIDTH + 0.04f, 0.04f, 0.0f, 0.15f, 0.65f, 0.22f);
        }

        float bottomHeight = gapBottom + 1.0f;
        if (bottomHeight > 0.0f) {
            float centerY = -1.0f + bottomHeight * 0.5f;
            drawRect(pipe.x + 0.01f, centerY, GameConfig.PIPE_WIDTH, bottomHeight, 0.0f, 0.1f, 0.5f, 0.15f);
            drawRect(pipe.x, centerY, GameConfig.PIPE_WIDTH, bottomHeight, 0.0f, 0.18f, 0.70f, 0.25f);
            drawRect(pipe.x, centerY, GameConfig.PIPE_WIDTH + 0.01f, bottomHeight + 0.01f, 0.0f, 0.1f, 0.6f, 0.2f);
            drawRect(pipe.x, gapBottom - 0.02f, GameConfig.PIPE_WIDTH + 0.04f, 0.04f, 0.0f, 0.15f, 0.65f, 0.22f);
        }
    }

    private void drawCenteredTextWithShadow(String text, float centerX, float y, float scale,
            float r, float g, float b, float shadowOffsetX, float shadowOffsetY) {
        drawCenteredText(text, centerX + shadowOffsetX, y + shadowOffsetY, scale, 0.0f, 0.0f, 0.0f);
        drawCenteredText(text, centerX, y, scale, r, g, b);
    }

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

    private float calcularAnchoTexto(String text, float scale) {
        float charWidth = 0.14f * scale;
        float spacing = 0.04f * scale;
        float spaceWidth = 0.08f * scale;
        float width = 0.0f;

        for (int i = 0; i < text.length(); i++) {
            char c = Character.toUpperCase(text.charAt(i));
            width += (c == ' ') ? spaceWidth : charWidth;
            if (i < text.length() - 1) {
                width += spacing;
            }
        }
        return width;
    }

    private void drawCharacter(char letter, float x, float y, float scale,
            float r, float g, float b) {
        boolean[][] pattern = getBlockPattern(letter);
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

    private void drawRestartIcon(float x, float y, float size) {
        drawCircleApprox(x, y, size * 0.5f, 0.0f, 0.0f, 0.8f, 0.0f);
        drawTriangle(x + size * 0.2f, y, size * 0.3f, 0.0f, 0.0f, 0.8f, 0.0f);
        drawRect(x - size * 0.1f, y, size * 0.3f, size * 0.05f, 0.0f, 0.0f, 0.8f, 0.0f);
    }
}
