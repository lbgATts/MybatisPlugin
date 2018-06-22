package com.test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ibatis.executor.statement.RoutingStatementHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

import com.test.utils.context.UserContextUtils;
import com.test.commons.id.CompanyAccount;
import com.test.commons.id.ID;

import sun.plugin.javascript.ReflectUtil;

@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = { Connection.class }) })
public class DataSecurityInterceptor implements Interceptor {

    public static final String SECURITY_PATTERN = "\\/\\*SECURITY_DAS.*SECURITY_DAE\\*\\/";
    public static final String SECURITY_PATTERN_START = "\\/\\*SECURITY_DAS\\|";
    public static final String SECURITY_PATTERN_END = "\\|SECURITY_DAE\\*\\/";
    public static final String SECURITY_FIRST_LEVEL_SEPARATOR = "\\|";
    public static final String SECURITY_SECOND_LEVEL_SEPARATOR = ",";
    public static final String SECURITY_THIRD_LEVEL_SEPARATOR = ":";

    private static final String REFLECT_NAME = "sql";

    @Override
    public Object intercept(Invocation invocation) throws Throwable {

        RoutingStatementHandler handler = (RoutingStatementHandler) invocation.getTarget();
        StatementHandler realHandler = (StatementHandler) ReflectUtil.getFieldValue(handler, "delegate");
        String originSQL = realHandler.getBoundSql().getSql();

        ReflectUtil.setFieldValue(realHandler.getBoundSql(), REFLECT_NAME, generateSQLwithSecurity(originSQL));

        return invocation.proceed();
    }

    private String generateSQLwithSecurity(String originSQL) {

        Pattern pattern = Pattern.compile(SECURITY_PATTERN);
        Matcher matcher = pattern.matcher(originSQL);

        while (matcher.find()) {
            String realData = matcher.group(0).split(SECURITY_FIRST_LEVEL_SEPARATOR)[1];
            StringBuffer buffer = new StringBuffer();
            String[] keyColumns = realData.split(SECURITY_SECOND_LEVEL_SEPARATOR);

            for (String keyColumn : keyColumns) {
                String[] typeToColumn = keyColumn.split(SECURITY_THIRD_LEVEL_SEPARATOR);
                buffer.append(" " + typeToColumn[2] + generateTenantFilter());
            }
            originSQL = originSQL.replaceAll(
                    SECURITY_PATTERN_START + realData + SECURITY_PATTERN_END, buffer.toString());
            matcher = pattern.matcher(originSQL);
        }
        return originSQL;
    }

    private String generateTenantFilter() {
        ID<CompanyAccount> tenantId = UserContextUtils.getCompanyAccountId();
        List<UUID> tenantIdList = UserContextUtils.getBranchTenantIdList();

        StringBuffer buffer = new StringBuffer();
        if (tenantIdList != null) {
            buffer.append(" in ('" + tenantId.toString() + "'");
            for (UUID branch : tenantIdList) {
                buffer.append(",'" + branch + "'");
            }
            buffer.append(")");
        } else {
            buffer.append(" = '" + tenantId.toString() + "'");
        }
        return buffer.toString();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
