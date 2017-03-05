package com.meng.chatonline.test;

import com.meng.chatonline.dao.impl.UserDaoImpl;
import com.meng.chatonline.model.security.Authority;
import com.meng.chatonline.security.cache.RedisManager;
import com.meng.chatonline.security.cache.ShiroRedisCacheManager;
import com.meng.chatonline.service.AuthorityService;
import com.meng.chatonline.utils.ReflectionUtils;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.session.Session;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author xindemeng
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring-beans.xml","classpath:spring-shiro.xml","classpath:spring-redis.xml"})
public class CacheTest {

    @Resource
    private AuthorityService authorityService;
    @Resource
    private ShiroRedisCacheManager shiroRedisCacheManager;
    @Resource
    private RedisManager redisManager;

    @Test
    public void testGetAllAuthorities() {
        List<Authority> authorityList = authorityService.findAllAuthorities();
        System.out.println(authorityService.findAllAuthorities());
    }

    @Test
    public void testKeys() {
        Cache<String, Session> cache = shiroRedisCacheManager.getCache("userSessionsCache");
        Set<String> keys = cache.keys();
        System.out.println(keys);
//        cache.remove("29240bcf-e8e2-4aae-92a9-6a738ea2fbec");
    }

//    @Test
//    public void testGetAllSessions() {
//        Collection<Session> sessions = redisManager.getAllSession(0, "*chatonline-shiro-cache-shiro-activeSessionCache*");
//        System.out.println(sessions);
//    }

    @Test
    public void testClear() {
        Cache<String, Session> cache = shiroRedisCacheManager.getCache("userSessionsCache");
        cache.clear();
    }

    @Test
    public void testValues() {
        Cache<String, Object> cache = shiroRedisCacheManager.getCache("authorizationCache");
        Collection values = cache.values();
        System.out.println(values);
    }

    @Test
    public void test() {
        Class clazz = ReflectionUtils.getSuperGenericType(UserDaoImpl.class);
        System.out.println(clazz);
    }
}
