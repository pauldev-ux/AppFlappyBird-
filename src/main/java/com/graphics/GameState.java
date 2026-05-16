package com.graphics;

// Estados principales del juego.
// Sirve para saber si el juego está en inicio, jugando o game over.
public enum GameState {
    // Pantalla inicial antes de empezar.
    START,

    // Estado donde los jugadores pueden saltar, avanzar y sumar puntos.
    PLAYING,

    // Estado final cuando ambos jugadores pierden.
    GAME_OVER
}