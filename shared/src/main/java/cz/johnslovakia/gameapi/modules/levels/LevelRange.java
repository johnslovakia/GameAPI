package cz.johnslovakia.gameapi.modules.levels;

import cz.johnslovakia.gameapi.modules.ModuleManager;
import cz.johnslovakia.gameapi.rewards.Reward;
import cz.johnslovakia.gameapi.users.PlayerIdentity;

public record LevelRange(
        int startLevel,
        int endLevel,
        int baseXP,
        XPScaling scaling,
        Reward reward
) {
    public int getLength() {
        return endLevel - startLevel + 1;
    }

    public int getXPForLevel(int level) {
        if (level < startLevel || level > endLevel) {
            throw new IllegalArgumentException("Level " + level + " is out of range (" + startLevel + "-" + endLevel + ")");
        }

        int levelInRange = level - startLevel;
        int rawXP = scaling.calculateXP(baseXP, levelInRange);

        return roundToNiceNumber(rawXP);
    }

    private int roundToNiceNumber(int xp) {
        if (xp < 1000) {
            return Math.round(xp / 50.0f) * 50;
        } else if (xp < 10000) {
            return Math.round(xp / 100.0f) * 100;
        } else if (xp < 50000) {
            return Math.round(xp / 500.0f) * 500;
        } else if (xp < 100000) {
            return Math.round(xp / 1000.0f) * 1000;
        } else {
            return Math.round(xp / 2500.0f) * 2500;
        }
    }

    public int getTotalXPInRange() {
        int total = 0;
        for (int level = startLevel; level <= endLevel; level++) {
            total += getXPForLevel(level);
        }
        return total;
    }

    public enum XPScaling {
        FLAT {
            @Override
            public int calculateXP(int baseXP, int levelIndex) {
                return baseXP;
            }
        },
        LINEAR {
            @Override
            public int calculateXP(int baseXP, int levelIndex) {
                return (int)(baseXP * (1 + levelIndex * 0.08));
            }
        },
        MILD_EXPONENTIAL {
            @Override
            public int calculateXP(int baseXP, int levelIndex) {
                return (int)(baseXP * Math.pow(1.08, levelIndex));
            }
        },
        EXPONENTIAL {
            @Override
            public int calculateXP(int baseXP, int levelIndex) {
                return (int)(baseXP * Math.pow(1.12, levelIndex));
            }
        },
        AGGRESSIVE_EXPONENTIAL {
            @Override
            public int calculateXP(int baseXP, int levelIndex) {
                return (int)(baseXP * Math.pow(1.18, levelIndex));
            }
        },
        QUADRATIC {
            @Override
            public int calculateXP(int baseXP, int levelIndex) {
                double factor = 1 + (levelIndex * levelIndex * 0.02);
                return (int)(baseXP * factor);
            }
        };

        public abstract int calculateXP(int baseXP, int levelIndex);
    }
}