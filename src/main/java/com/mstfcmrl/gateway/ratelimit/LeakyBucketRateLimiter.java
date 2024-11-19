package com.mstfcmrl.gateway.ratelimit;

import org.springframework.cloud.gateway.filter.ratelimit.AbstractRateLimiter;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Primary
public class LeakyBucketRateLimiter extends AbstractRateLimiter<LeakyBucketRateLimiter.Config> {

    @Value("${ratelimiter.leakybucket.request-limit:5}")
    private int requestLimit; // Max requests allowed in the bucket
    @Value("${ratelimiter.leakybucket.leak-rate-in-milliseconds:1000}")
    private long leakRateInMilliseconds; // Leak rate in milliseconds (1 request per second)

    private final Map<String, LeakyBucket> clientsBucketMap = new ConcurrentHashMap<>();

    @Autowired
    public LeakyBucketRateLimiter(ConfigurationService configurationService) {
        super(Config.class, "leaky-bucket-rate-limiter", configurationService);
    }

    @Override
    public Mono<Response> isAllowed(String routeId, String ipAddress) {
        LeakyBucket leakyBucket = clientsBucketMap.computeIfAbsent(ipAddress, key -> new LeakyBucket(requestLimit, leakRateInMilliseconds));

        synchronized (leakyBucket) {
            leakyBucket.leak();

            if (leakyBucket.hasCapacity()) {
                leakyBucket.addRequest();
                return Mono.just(new Response(true, Collections.emptyMap()));
            } else {
                return Mono.just(new Response(false, Collections.emptyMap()));
            }
        }
    }

    public static class Config {
        // Configuration properties for the rate limiter can be added here if needed
    }

    static class LeakyBucket {
        private final int capacity;
        private final long leakRateInMillis;
        private AtomicInteger currentRequestCount;
        private long lastLeakTimestamp;

        public LeakyBucket(int capacity, long leakRateInMillis) {
            this.capacity = capacity;
            this.leakRateInMillis = leakRateInMillis;
            this.currentRequestCount = new AtomicInteger(0);
            this.lastLeakTimestamp = Instant.now().toEpochMilli();
        }

        public void leak() {
            long currentTime = Instant.now().toEpochMilli();
            long elapsed = currentTime - lastLeakTimestamp;

            if (elapsed >= leakRateInMillis) {
                int leaks = Math.min((int) (elapsed / leakRateInMillis), currentRequestCount.get());
                currentRequestCount.addAndGet(-leaks);
                lastLeakTimestamp = currentTime;
            }
        }

        public boolean hasCapacity() {
            return currentRequestCount.get() < capacity;
        }

        public void addRequest() {
            currentRequestCount.incrementAndGet();
        }
    }
}
