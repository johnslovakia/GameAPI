package cz.johnslovakia.gameapi.utils;

import lombok.Getter;

import java.time.Duration;

@Getter
public class BatchConfig {
    
    private final String name;
    private final int maxBatchSize;
    private final Duration flushInterval;
    private final boolean debugEnabled;
    private final boolean cacheEvictionEnabled;
    private final Duration cacheMaxAge;
    
    private BatchConfig(Builder builder) {
        this.name = builder.name;
        this.maxBatchSize = builder.maxBatchSize;
        this.flushInterval = builder.flushInterval;
        this.debugEnabled = builder.debugEnabled;
        this.cacheEvictionEnabled = builder.cacheEvictionEnabled;
        this.cacheMaxAge = builder.cacheMaxAge;
    }
    
    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        private final String name;
        private int maxBatchSize = 50;
        private Duration flushInterval = Duration.ofSeconds(30);
        private boolean cacheEvictionEnabled;
        private Duration cacheMaxAge;
        private boolean debugEnabled = false;

        private Builder(String name) {
            this.name = name;
        }

        public Builder maxBatchSize(int size) {
            this.maxBatchSize = size;
            return this;
        }

        public Builder flushInterval(Duration interval) {
            this.flushInterval = interval;
            return this;
        }

        public Builder flushIntervalSeconds(long seconds) {
            this.flushInterval = Duration.ofSeconds(seconds);
            return this;
        }

        public Builder debugEnabled(boolean enabled) {
            this.debugEnabled = enabled;
            return this;
        }

        public Builder setCacheEvictionEnabled(boolean cacheEvictionEnabled) {
            this.cacheEvictionEnabled = cacheEvictionEnabled;
            return this;
        }

        public Builder setCacheMaxAge(Duration cacheMaxAge) {
            this.cacheMaxAge = cacheMaxAge;
            return this;
        }

        public BatchConfig build() {
            return new BatchConfig(this);
        }
    }
}