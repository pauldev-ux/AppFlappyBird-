package com.graphics;

import org.lwjgl.glfw.GLFW;

public class InputManager {
    private boolean prevSpace;
    private boolean prevJump2;
    private boolean prevR;

    private boolean exitRequested;
    private boolean jumpPlayer1Pressed;
    private boolean jumpPlayer2Pressed;
    private boolean resetPressed;

    public void update(long window) {
        exitRequested = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;

        boolean spaceNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        jumpPlayer1Pressed = spaceNow && !prevSpace;
        prevSpace = spaceNow;

        boolean wNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
        boolean upNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_UP) == GLFW.GLFW_PRESS;
        boolean jump2Now = wNow || upNow;
        jumpPlayer2Pressed = jump2Now && !prevJump2;
        prevJump2 = jump2Now;

        boolean rNow = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_R) == GLFW.GLFW_PRESS;
        resetPressed = rNow && !prevR;
        prevR = rNow;
    }

    public boolean isExitRequested() {
        return exitRequested;
    }

    public boolean isJumpPlayer1Pressed() {
        return jumpPlayer1Pressed;
    }

    public boolean isJumpPlayer2Pressed() {
        return jumpPlayer2Pressed;
    }

    public boolean isResetPressed() {
        return resetPressed;
    }
}
