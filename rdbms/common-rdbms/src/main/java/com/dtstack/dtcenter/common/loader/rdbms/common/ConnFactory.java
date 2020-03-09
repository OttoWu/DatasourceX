package com.dtstack.dtcenter.common.loader.rdbms.common;

import com.dtstack.dtcenter.common.exception.DtCenterDefException;
import com.dtstack.dtcenter.loader.dto.SourceDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @company: www.dtstack.com
 * @Author ：Nanqi
 * @Date ：Created in 11:22 2020/1/13
 * @Description：连接工厂
 */
@Slf4j
public class ConnFactory {
    protected String driverName = null;

    protected String testSql;

    private AtomicBoolean isFirstLoaded = new AtomicBoolean(true);

    protected void init() throws ClassNotFoundException {
        // 减少加锁开销
        if (!isFirstLoaded.get()) {
            return;
        }

        synchronized (ConnFactory.class) {
            if (isFirstLoaded.get()) {
                Class.forName(driverName);
                isFirstLoaded.set(false);
            }
        }
    }

    public Connection getConn(SourceDTO source) throws Exception {
        if (source == null) {
            throw new DtCenterDefException("数据源信息为 NULL");
        }

        init();
        if (StringUtils.isBlank(source.getUsername())) {
            return DriverManager.getConnection(source.getUrl());
        }

        return DriverManager.getConnection(source.getUrl(), source.getUsername(), source.getPassword());
    }

    public Boolean testConn(SourceDTO source) {
        boolean isConnected = false;
        Connection conn = null;
        try {
            conn = getConn(source);
            if (StringUtils.isBlank(testSql)) {
                conn.isValid(5);
            } else {
                conn.createStatement().execute((testSql));
            }

            isConnected = true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return isConnected;
    }
}