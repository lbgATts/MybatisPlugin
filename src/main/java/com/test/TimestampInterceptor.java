package com.test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

import com.test.commons.logging.RequestContext;
import com.test.commons.logging.RequestInfo;

@Intercepts({ @Signature(type = Executor.class, method = "update", args = { MappedStatement.class, Object.class }) })
public class TimestampInterceptor implements Interceptor {

    public Object intercept(Invocation invocation) throws Throwable {

        Object parameterObj = invocation.getArgs()[1];
        SqlCommandType type = ((MappedStatement) invocation.getArgs()[0]).getSqlCommandType();
        if (parameterObj instanceof BaseEntity) {
            populateDate(parameterObj, type);
        }

        if (parameterObj instanceof Map) {
            Map mapObj = (Map) parameterObj;
            if (mapObj.containsKey("collection")) {
                if (mapObj.get("collection") instanceof List) {
                    List<Object> objectList = (List) mapObj.get("collection");
                    for (Object obj : objectList) {
                        populateDate(obj, type);
                    }
                }
            }

            if (mapObj.containsKey("list")) {
                if (mapObj.get("list") instanceof List) {
                    List<Object> objectList = (List) mapObj.get("list");
                    for (Object obj : objectList) {
                        populateDate(obj, type);
                    }
                }
            }
        }

        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }

    private void populateDate(Object obj, SqlCommandType commandType) {

        if (!(obj instanceof BaseEntity)) {
            return;
        }

        BaseEntity entity = (BaseEntity) obj;
        RequestInfo context = RequestContext.get();
        UUID userId = context == null
                ? null
                : context.getUserContext().getUserId().asUUID();
        switch (commandType) {
            case INSERT:
                entity.setCreatedBy(userId);
                entity.setCreatedTime(Instant.now());
                break;
            case UPDATE:
                entity.setUpdatedBy(userId);
                entity.setUpdatedTime(Instant.now());
                break;
        }
    }
}
