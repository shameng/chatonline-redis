package com.meng.chatonline.security.filter;

import com.meng.chatonline.model.ActiveUser;
import com.meng.chatonline.model.User;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.apache.shiro.web.util.WebUtils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.util.Deque;
import java.util.LinkedList;

/**
 * @author xindemeng
 *
 * 并发登录人数控制过滤器
 * 此处使用了Cache缓存用户名—会话id之间的关系；如果量比较大可以考虑如持久化到数据库/其他带持久化的Cache中；
 * 另外此处没有并发控制的同步实现，可以考虑根据用户名获取锁来控制，减少锁的粒度。
 */
public class KickoutSessionControlFilter extends AccessControlFilter
{
    //同一个帐号最大会话数，默认是1
    private int maxSession = 1;
    //踢出之前登陆的还是只有登陆的用户，默认是踢出之前的
    private boolean kickoutAfter = false;
    //提出后返回的地址
    private String kickoutUrl;

    private SessionManager sessionManager;
    private CacheManager cacheManager;
    private String cacheName;

    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception
    {
        //返回false就会到onAccessDenied方法上继续处理
        return false;
    }

    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws IOException {
        Subject subject = getSubject(request, response);
        //如果没有登录，直接进行之后的流程
        if (!subject.isAuthenticated() && !subject.isRemembered())
            return true;

        Session session = subject.getSession(true);
        ActiveUser user = (ActiveUser) subject.getPrincipal();
        Serializable sessionId = session.getId();

        //是否已经被踢出登陆
        boolean isKickout = session.getAttribute("kickout") != null;
        //如果被踢出了，直接退出，重定向到踢出后的地址
        if (isKickout)
        {
            kickout(subject, request, response);
            //不执行后面的拦截器了
            return false;
        }

        Cache<String, Deque<Serializable>> cache = getCache();
        if (cache != null)
        {
            Deque<Serializable> deque = cache.get(buildCacheKey(user));
            if (deque == null)
            {
                deque = new LinkedList<Serializable>();
            }

            //如果还是本用户，例如进行页面刷新操作，则直接返回true
            if (deque.contains(sessionId))
                return true;
            //如果队列里没有此sessionId，则放入队列
            else
                deque.push(sessionId);

            //如果队列里的sessionId书超出最大会话数，则开始踢人
            while (deque.size() > maxSession)
            {
                Serializable kickoutSessionId = null;
                //如果踢出后者
                if (kickoutAfter)
                    kickoutSessionId = deque.removeFirst();
                //如果踢出前者
                else
                    kickoutSessionId = deque.removeLast();

                Session kickoutSession = sessionManager.getSession(new DefaultSessionKey(kickoutSessionId));
                if (kickoutSession != null)
                {
                    //设置会话的kickout属性表示踢出了
                    kickoutSession.setAttribute("kickout", true);
                    System.out.println(user + "被踢出登陆，需重新登陆");
                }
            }

            cache.put(buildCacheKey(user), deque);
        }

        //继续执行后面的拦截器
        return true;
    }

    /**
     * 踢出登陆
     * @param subject
     * @param request
     * @param response
     * @throws IOException
     */
    private void kickout(Subject subject, ServletRequest request, ServletResponse response) throws IOException {
        //会话被踢出
        subject.logout();

        //保存当前地址并重定向到登录界面
        saveRequest(request);
        WebUtils.issueRedirect(request, response, kickoutUrl);
    }

    private String buildCacheKey(User user) {
        return "user_id(" + user.getId() + ")";
    }

    //获得缓存区
    public Cache<String, Deque<Serializable>> getCache()
    {
        return cacheManager.getCache(getCacheName());
    }

    public void setCacheManager(CacheManager cacheManager)
    {
        this.cacheManager = cacheManager;
    }

    public int getMaxSession()
    {
        return maxSession;
    }

    public void setMaxSession(int maxSession)
    {
        this.maxSession = maxSession;
    }

    public boolean isKickoutAfter()
    {
        return kickoutAfter;
    }

    public void setKickoutAfter(boolean kickoutAfter)
    {
        this.kickoutAfter = kickoutAfter;
    }

    public String getKickoutUrl()
    {
        return kickoutUrl;
    }

    public void setKickoutUrl(String kickoutUrl)
    {
        this.kickoutUrl = kickoutUrl;
    }

    public SessionManager getSessionManager()
    {
        return sessionManager;
    }

    public void setSessionManager(SessionManager sessionManager)
    {
        this.sessionManager = sessionManager;
    }

    public String getCacheName()
    {
        return cacheName;
    }

    public void setCacheName(String cacheName)
    {
        this.cacheName = cacheName;
    }
}
