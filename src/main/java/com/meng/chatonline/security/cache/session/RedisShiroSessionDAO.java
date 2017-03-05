package com.meng.chatonline.security.cache.session;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;

import java.io.Serializable;
import java.util.Collection;

/**
 * 自定义SessionDAO，由redis来管理session缓存
 *
 * @author xindemeng
 */
public class RedisShiroSessionDAO extends AbstractSessionDAO {

    private ShiroSessionRepository shiroSessionRepository;

    @Override
    protected Serializable doCreate(Session session) {
        Serializable sessionId = this.generateSessionId(session);
        this.assignSessionId(session, sessionId);

        shiroSessionRepository.saveSession(session);
        return sessionId;
    }

    @Override
    protected Session doReadSession(Serializable sessionId) {
        Session session = shiroSessionRepository.getSession(sessionId);
        //更新session缓存过期时间
        shiroSessionRepository.updateSessionExpireTime(session);
        return session;
    }

    @Override
    public void update(Session session) throws UnknownSessionException {
        shiroSessionRepository.updateSession(session);
        //更新session缓存过期时间
        shiroSessionRepository.updateSessionExpireTime(session);
    }

    @Override
    public void delete(Session session) {
        if (session == null || session.getId() == null) {
            throw new NullPointerException("session id is empty");
        }
        shiroSessionRepository.deleteSession(session.getId());
    }

    @Override
    public Collection<Session> getActiveSessions() {
        return shiroSessionRepository.getAllSessions();
    }

    public ShiroSessionRepository getShiroSessionRepository() {
        return shiroSessionRepository;
    }

    public void setShiroSessionRepository(ShiroSessionRepository shiroSessionRepository) {
        this.shiroSessionRepository = shiroSessionRepository;
    }
}
