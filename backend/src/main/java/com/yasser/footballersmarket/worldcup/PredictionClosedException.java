package com.yasser.footballersmarket.worldcup;

// thrown when a prediction is attempted after the fixture has kicked off
public class PredictionClosedException extends RuntimeException {
    public PredictionClosedException(String message) {
        super(message);
    }
}
