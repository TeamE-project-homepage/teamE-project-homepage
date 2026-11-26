package com.teame.bingo.domain.recognition.dto;

public record RecognitionPrediction(String model, String label, Double score) {
}
