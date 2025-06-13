package it.mathsanalysis.load.spi.cache.impl.java.entry;

import java.time.Duration;
import java.time.Instant;

 /**
 * Cache entry with expiration support
 */
public class CacheEntry<V> {

     public final V value;
     public final Instant expireTime;
     public volatile Instant lastAccessTime;
     public volatile long accessCount;
     public final Instant createTime;

     public CacheEntry(V value, Duration ttl) {
         this.value = value;
         this.createTime = Instant.now();
         this.lastAccessTime = createTime;
         this.accessCount = 0;
         this.expireTime = ttl != null ? createTime.plus(ttl) : null;
     }

     public boolean isExpired() {
         return isExpired(Instant.now());
     }

     public boolean isExpired(Instant now) {
         return expireTime != null && now.isAfter(expireTime);
     }

     public void updateAccessTime() {
         this.lastAccessTime = Instant.now();
         this.accessCount++;
     }
 }