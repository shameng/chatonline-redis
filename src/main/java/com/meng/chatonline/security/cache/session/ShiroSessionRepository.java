package com.meng.chatonline.security.cache.session;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;

import java.io.Serializable;
import java.util.Collection;

/**
 * session操作
 *
 * @author xindemeng
 */
public interface ShiroSessionRepository {

    void saveSession(Session session);

    void deleteSession(Serializable session);

    Session getSession(Serializable sessionId);

    Collection<Session> getAllSessions();

    void updateSession(Session session) throws UnknownSessionException;

    void updateSessionExpireTime(Session session);
}
