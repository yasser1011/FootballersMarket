package com.yasser.footballersmarket.matchpredictions;

// thrown when a prediction is attempted after the fixture has kicked off
public class PredictionClosedException extends RuntimeException {
    public PredictionClosedException(String message) {
        super(message);
    }
}
