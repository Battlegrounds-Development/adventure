package me.remag501.adventure.model;

public record ItemLimitViolation(String itemKey, int maxAllowed, int currentAmount) {}
