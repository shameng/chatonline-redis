package com.meng.chatonline.security.cache.session;

import com.meng.chatonline.security.cache.RedisManager;
import com.meng.chatonline.utils.SerializeUtil;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Collection;

/**
 * session操作
 *
 * @author xindemeng
 */
public class RedisShiroSessionRepository implements ShiroSessionRepository {

    private static Logger logger = LoggerFactory.getLogger(RedisShiroSessionRepository.class);

    private static final String DEFAULT_ACTIVE_SESSION_CACHE_NAME = "shiro-activeSessionCache";
    private static final int DEFAULT_DB_INDEX = 0;
    /**
     *  默认session缓存时间为30分钟，与session默认过期时间一致
     */
    private static final int DEFAULT_EXPIRE_TIME = 30 * 60;

    private int dbIndex = DEFAULT_DB_INDEX;
    private String activeSessionsCacheName = DEFAULT_ACTIVE_SESSION_CACHE_NAME;
    private int expireTime = DEFAULT_EXPIRE_TIME;

    private RedisManager redisManager;

    @Override
    public void saveSession(Session session) {
        if (session == null || session.getId() == null) {
            throw new NullPointerException("sessionId is empty");
        }
        try {
            byte[] key = SerializeUtil.serialize(buildRedisSessionKey(session.getId()));
            byte[] value = SerializeUtil.serialize(session);

            //设置session缓存过期时间
            int expireTime = getExpireTime();
            if (expireTime < session.getTimeout() / 1000) {
                expireTime = (int) (session.getTimeout() / 1000);
            }
            redisManager.saveValueByKey(dbIndex, key, value, expireTime);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("save session error, id:" + session.getId(), e);
            }
            e.printStackTrace();
        }
    }

    @Override
    public void deleteSession(Serializable sessionId) {
        if (sessionId == null) {
            throw new NullPointerException("sessionId is empty");
        }
        try {
            byte[] key = SerializeUtil.serialize(buildRedisSessionKey(sessionId));
            redisManager.deleteByKey(dbIndex, key);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("删除session出现异常", e);
            }
            e.printStackTrace();
        }
    }

    @Override
    public Session getSession(Serializable sessionId) {
        if (sessionId == null) {
            throw new NullPointerException("sessionId is empty");
        }

        Session session = null;
        try {
            byte[] key = SerializeUtil.serialize(buildRedisSessionKey(sessionId));
            byte[] value = redisManager.getValueByKey(dbIndex, key);
            session = SerializeUtil.deserialize(value, Session.class);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("获取session异常，id:" + sessionId, e);
            }
            e.printStackTrace();
        }
        return session;
    }

    @Override
    public void updateSession(Session session) throws UnknownSessionException {
        if (session == null || session.getId() == null) {
            throw new NullPointerException("sessionId is empty");
        }

        //如果不存在该session，则抛UnknownSessionException异常
        if (this.getSession(session.getId()) == null) {
            throw new UnknownSessionException("未知session，id:" + session.getId());
        }

        this.saveSession(session);
    }

    @Override
    public void updateSessionExpireTime(Session session) {
        if (session == null || session.getId() == null) {
            throw new NullPointerException("sessionId is empty");
        }
        int expireTime = getExpireTime();
        if (expireTime < session.getTimeout() / 1000) {
            expireTime = (int) (session.getTimeout() / 1000);
        }
        try {
            byte[] key = SerializeUtil.serialize(buildRedisSessionKey(session.getId()));
            redisManager.setExpireTime(key, expireTime);
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("更新缓存" + buildRedisSessionKey(session.getId()) + "过期时间时出现异常", e);
            }
            e.printStackTrace();
        }
    }

    @Override
    public Collection<Session> getAllSessions() {
        Collection<Session> sessions = null;
        try {
            sessions = redisManager.values(dbIndex, buildSessionCacheNamePattern().getBytes());
        } catch (Exception e) {
            if (logger.isErrorEnabled()) {
                logger.error("获取全部session异常", e);
            }
        }
        return sessions;
    }

    /**
     *
     * @return
     */
    private String buildSessionCacheNamePattern() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("*")
                .append(getActiveSessionsCacheName())
                .append("*");
        return buffer.toString();
    }

    /**
     *
     * @param sessionId
     * @return session在redis里的key
     */
    private String buildRedisSessionKey(Serializable sessionId) {
        return getActiveSessionsCacheName() + sessionId;
    }

    public String getActiveSessionsCacheName() {
        return activeSessionsCacheName;
    }

    public void setActiveSessionsCacheName(String activeSessionsCacheName) {
        this.activeSessionsCacheName = activeSessionsCacheName;
    }

    public RedisManager getRedisManager() {
        return redisManager;
    }

    public void setRedisManager(RedisManager redisManager) {
        this.redisManager = redisManager;
    }

    public int getDbIndex() {
        return dbIndex;
    }

    public void setDbIndex(int dbIndex) {
        this.dbIndex = dbIndex;
    }

    public int getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(int expireTime) {
        this.expireTime = expireTime;
    }
}
