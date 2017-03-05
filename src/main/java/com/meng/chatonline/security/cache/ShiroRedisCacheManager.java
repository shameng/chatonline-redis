package com.meng.chatonline.security.cache;

import org.apache.shiro.ShiroException;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.util.Destroyable;
import org.apache.shiro.util.Initializable;

/**
 * shiro的redis缓存管理器
 *
 * @author xindemeng
 */
public class ShiroRedisCacheManager implements CacheManager, Initializable, Destroyable {

    private RedisManager redisManager;

    @Override
    public <K, V> Cache<K, V> getCache(String name) throws CacheException {
        return new ShiroRedisCache<K, V>(redisManager, name);
    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void init() throws ShiroException {

    }

    public RedisManager getRedisManager() {
        return redisManager;
    }

    public void setRedisManager(RedisManager redisManager) {
        this.redisManager = redisManager;
    }
}
