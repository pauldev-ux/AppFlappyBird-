package com.graphics;

import org.lwjgl.glfw.GLFW;

public class InputManager {
    // Guarda si ESPACIO estaba presionado en el frame anterior.
    // Sirve para detectar un solo salto por pulsación.
    private boolean prevSpace;

    // Guarda si W o Flecha Arriba estaban presionadas antes.
    // Sirve para evitar saltos repetidos al mantener la tecla.
    private boolean prevJump2;

    // tercer jugador con enter
    private boolean prevEnter;

    // Guarda si R estaba presionada antes.
    // Sirve para detectar un solo reinicio por pulsación.
    private boolean prevR;

    // Indica si el jugador pidió cerrar el juego con ESC.
    private boolean exitRequested;

    // Indica si jugador 1 presionó salto en este frame.
    private boolean jumpPlayer1Pressed;

    // Indica si jugador 2 presionó salto en este frame.
    private boolean jumpPlayer2Pressed;

    // Indica si jugador 3 presionó salto en este frame.
    private boolean jumpPlayer3Pressed;

    // Indica si se presionó R para reiniciar.
    private boolean resetPressed;

    // Lee el teclado en cada frame.
    // Para cambiar controles, modifica aquí las teclas GLFW_KEY_*.
    public void update(long window) {
        // ESC cierra la ventana.
        exitRequested = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;

        // ESPACIO controla el salto del jugador 1.
        // Para cambiarlo, reemplaza GLFW_KEY_SPACE por otra tecla.
        boolean spaceNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        jumpPlayer1Pressed = spaceNow && !prevSpace;
        prevSpace = spaceNow;

        // W o Flecha Arriba controlan el salto del jugador 2.
        // Para agregar otra tecla, únela con || igual que wNow y upNow.
        boolean wNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
        boolean upNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS;
        boolean jump2Now = wNow || upNow;
        jumpPlayer2Pressed = jump2Now && !prevJump2;
        prevJump2 = jump2Now;

        //jugador 3 tecla enter
        boolean enterNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS;
        jumpPlayer3Pressed = enterNow && !prevEnter;
        prevEnter = enterNow;

        // R sirve para reiniciar en Game Over.
        // Para cambiar reinicio a ENTER, usa GLFW_KEY_ENTER.
        boolean rNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        resetPressed = rNow && !prevR;
        prevR = rNow;
    }

    // Devuelve true si se presionó ESC.
    public boolean isExitRequested() {
        return exitRequested;
    }

    // Devuelve true solo cuando jugador 1 presiona ESPACIO una vez.
    public boolean isJumpPlayer1Pressed() {
        return jumpPlayer1Pressed;
    }

    // Devuelve true solo cuando jugador 2 presiona W o Flecha Arriba una vez.
    public boolean isJumpPlayer2Pressed() {
        return jumpPlayer2Pressed;
    }

    // jugador 3 con enter 
    public boolean isJumpPlayer3Pressed() {
        return jumpPlayer3Pressed;
    }

    // Devuelve true solo cuando se presiona R una vez.
    public boolean isResetPressed() {
        return resetPressed;
    }
}