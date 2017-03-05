package com.meng.chatonline.security.cache;

import com.meng.chatonline.constant.Constants;
import com.meng.chatonline.utils.SerializeUtil;
import org.apache.shiro.ShiroException;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.util.Initializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author xindemeng
 */
public class ShiroRedisCache<K, V> implements Cache<K, V>, Initializable {

    private static final Logger log = LoggerFactory.getLogger(ShiroRedisCache.class);

    /**
     * 为了不和其他的缓存混淆，采用追加前缀方式以作区分
     */
    private static final String DEFAULT_REDIS_SHIRO_CACHE_PREFIX = Constants.RESOURCE_SERVER_NAME + "-shiro-cache-";
    /**
     * Redis 分片(分区)，也可以在配置文件中配置
     */
    private static final int DEFAULT_DB_INDEX = 0;

    private RedisManager redisManager;
    /**
     * 缓存名
     */
    private String name;
    private String redisShiroCachePrefix = DEFAULT_REDIS_SHIRO_CACHE_PREFIX;
    private int dbIndex = DEFAULT_DB_INDEX;

    public ShiroRedisCache(RedisManager redisManager, String name) {
        this.redisManager = redisManager;
        this.name = name;
    }

    @Override
    public void init() throws ShiroException {
    }

    @Override
    public V get(K key) throws CacheException {
        byte[] byteKey = SerializeUtil.serialize(buildCacheKey(key));
        byte[] byteValue = new byte[0];
        try  {
            byteValue = redisManager.getValueByKey(dbIndex, byteKey);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("get value by cache throw exception", e);
            }
        }
        return (V) SerializeUtil.deserialize(byteValue);
    }

    @Override
    public V put(K key, V value) throws CacheException {
        V previousValue = this.get(key);
        try {
            redisManager.saveValueByKey(dbIndex, SerializeUtil.serialize(buildCacheKey(key)),
                    SerializeUtil.serialize(value), -1);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("put cache throw exception", e);
            }
        }
        return previousValue;
    }

    @Override
    public V remove(K key) throws CacheException {
        V value = get(key);
        try {
            redisManager.deleteByKey(dbIndex, SerializeUtil.serialize(buildCacheKey(key)));
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("remove cache throw exception", e);
            }
            e.printStackTrace();
        }
        return value;
    }

    @Override
    public void clear() throws CacheException {
        try {
            redisManager.clear(dbIndex, keyPattern().getBytes());
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("清空缓存" + getName() + "时出现异常", e);
            }
            e.printStackTrace();
        }
    }

    @Override
    public int size() {
        Set<byte[]> redisByteKeys = null;
        try {
            redisByteKeys = redisManager.keys(dbIndex, keyPattern().getBytes());
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("返回"+getName()+"缓存所有key时出现错误", e);
            }
            e.printStackTrace();
        }
        return redisByteKeys.size();
    }

    @Override
    public Set<K> keys() {
        Set<byte[]> redisByteKeys = null;
        try {
            redisByteKeys = redisManager.keys(dbIndex, keyPattern().getBytes());
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("返回"+getName()+"缓存所有key时出现错误", e);
            }
            e.printStackTrace();
        }
        Set<K> keys = new HashSet<K>(redisByteKeys.size());
        for (byte[] redisByteKey : redisByteKeys) {
            String keyStr = SerializeUtil.deserialize(redisByteKey, String.class);
            keyStr = removeRedisShiroCachePrefix(keyStr);
            keys.add((K) keyStr);
        }
//        return keys;
        //参照EhCache类
        return Collections.unmodifiableSet(keys);
    }

    @Override
    public Collection<V> values() {
        Collection<V> values = null;
        try {
            values = redisManager.values(dbIndex, keyPattern().getBytes());
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("返回"+getName()+"缓存所有value时出现错误", e);
            }
            e.printStackTrace();
        }
        return values;
    }

    /**
     * 除去redis里key的前缀
     * @param keyStr
     * @return
     */
    private String removeRedisShiroCachePrefix(String keyStr) {
        String prefix = buildRedisShiroCachePrefix();
        return keyStr.substring(prefix.length());
    }

    /**
     *
     * @return redis里key的前缀
     */
    private String buildRedisShiroCachePrefix() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(redisShiroCachePrefix)
                .append(getName())
                .append(":");
        return buffer.toString();
    }

    /**
     * @return 该缓存在redis的key匹配字符串
     */
    private String keyPattern() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("*")
                .append(redisShiroCachePrefix)
                .append(getName())
                .append("*");
        return buffer.toString();
    }

    /**
     * 组合成redis里的key值
     * @param key
     * @return
     */
    private String buildCacheKey(K key) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(this.buildRedisShiroCachePrefix())
                .append(key);
        return buffer.toString();
    }

    public RedisManager getRedisManager() {
        return redisManager;
    }

    public void setRedisManager(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    public String getName() {
        if (name == null)
            return "";
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRedisShiroCachePrefix() {
        return redisShiroCachePrefix;
    }

    public void setRedisShiroCachePrefix(String redisShiroCachePrefix) {
        this.redisShiroCachePrefix = redisShiroCachePrefix;
    }

    public int getDbIndex() {
        return dbIndex;
    }

    public void setDbIndex(int dbIndex) {
        this.dbIndex = dbIndex;
    }
}
