package cz.johnslovakia.gameapi.levelSystem;

public record LevelProgress(int level, LevelRange levelRange, LevelEvolution levelEvolution, int xpOnCurrentLevel, int xpToNextLevel) {}