package com.meng.chatonline.security.cache;

import com.meng.chatonline.utils.SerializeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author xindemeng
 */
public class RedisManager {

    private static final Logger log = LoggerFactory.getLogger(RedisManager.class);

    private JedisPool jedisPool;

    public Jedis getJedis() {
        Jedis jedis = null;
        try {
            jedis = getJedisPool().getResource();
        } catch (JedisConnectionException e) {
            String message = e.getMessage() == null ? "" : e.getMessage().trim();
            if("Could not get a resource from the pool".equalsIgnoreCase(message)){
                System.out.println("++++++++++请检查你的redis服务++++++++");
                System.out.println("|①.请检查是否安装redis服务，如果没安装，Windos 请参考Blog：http://www.sojson.com/blog/110.html|");
                System.out.println("|②.请检查redis 服务是否启动。启动口诀[安装目录中的redis-server.exe，双击即可，如果有错误，请用CMD方式启动，怎么启动百度，或者加QQ群。]|");
                System.out.println("|③.请检查redis启动是否带配置文件启动，也就是是否有密码，是否端口有变化（默认6379）。解决方案，参考第二点。如果需要配置密码和改变端口，请修改spring-shiro.xml配置。|");
                System.out.println("|④.QQ群：259217951，目前需要付费，免费的方案请参考链接：http://www.sojson.com/shiro");

                System.out.println("|PS.如果对Redis表示排斥，请使用Ehcache版本：http://www.sojson.com/jc_shiro_ssm_ehcache.html");
                System.out.println("项目退出中....生产环境中，请删除这些东西。我来自。JedisManage.java line:53");
//                System.exit(0);//停止项目
            }
            throw new JedisConnectionException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return jedis;
    }

    private void returnResource(Jedis jedis) {
        if (jedis == null)
            return;
        jedis.close();
    }

    public byte[] getValueByKey(int dbIndex, byte[] byteKey) {
        Jedis jedis = null;
        byte[] value = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            value = jedis.get(byteKey);
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
        return value;
    }

    public void saveValueByKey(int dbIndex, byte[] key, byte[] value, int expireTime) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            jedis.set(key, value);
            if (expireTime > 0) {
                jedis.expire(key, expireTime);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public void deleteByKey(int dbIndex, byte[] key) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            Long result = jedis.del(key);
            if (log.isDebugEnabled()) {
                log.debug("删除了该key下的" + result + "个元素");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public Set<byte[]> keys(int dbIndex, byte[] pattern) {
        Jedis jedis = null;
        Set<byte[]> byteKeys = new HashSet<>();
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            byteKeys = jedis.keys(pattern);
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
        return byteKeys;
    }

    public void clear(int dbIndex, byte[] pattern) {
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);
            Set<byte[]> byteKeys = jedis.keys(pattern);
            for (byte[] byteKey : byteKeys) {
                jedis.del(byteKey);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public <V> Collection<V> values(int dbIndex, byte[] pattern) {
        Set<V> values = null;
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.select(dbIndex);

            Set<byte[]> byteKeys = jedis.keys(pattern);
            if (!CollectionUtils.isEmpty(byteKeys)) {
                values = new HashSet<>(byteKeys.size());
                for (byte[] bs : byteKeys) {
                    V value = (V) SerializeUtil.deserialize(jedis.get(bs));
                    values.add(value);
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
        return values;
    }

    public void setExpireTime(byte[] key, int expireTime) {
        if (expireTime <= 0) {
            return;
        }
        Jedis jedis = null;
        try {
            jedis = getJedis();
            jedis.expire(key, expireTime);
        } catch (Exception e) {
            throw e;
        } finally {
            returnResource(jedis);
        }
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public void setJedisPool(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }
}
