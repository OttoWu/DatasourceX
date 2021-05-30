package com.dtstack.dtcenter.common.loader.libra;

import com.dtstack.dtcenter.common.loader.common.utils.DBUtil;
import com.dtstack.dtcenter.common.loader.rdbms.AbsRdbmsClient;
import com.dtstack.dtcenter.common.loader.rdbms.ConnFactory;
import com.dtstack.dtcenter.loader.dto.SqlQueryDTO;
import com.dtstack.dtcenter.loader.dto.source.ISourceDTO;
import com.dtstack.dtcenter.loader.dto.source.LibraSourceDTO;
import com.dtstack.dtcenter.loader.exception.DtLoaderException;
import com.dtstack.dtcenter.loader.source.DataSourceType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * @company: www.dtstack.com
 * @Author ：Nanqi
 * @Date ：Created in 11:54 2020/2/29
 * @Description：Libra 客户端
 */
public class LibraClient extends AbsRdbmsClient {

    // 获取正在使用数据库
    private static final String CURRENT_DB = "select current_database()";

    // 获取所有schema
    private static final String DATABASE_QUERY = "select nspname from pg_namespace";

    // 创建schema
    private static final String CREATE_SCHEMA_SQL_TMPL = "create schema if not exists %s ";

    // 判断schema是否存在
    private static final String DATABASE_IS_EXISTS = "select nspname from pg_namespace where nspname = '%s'";

    // 判断schema是否在
    private static final String TABLES_IS_IN_SCHEMA = "select table_name from information_schema.tables WHERE table_schema = '%s' and table_name = '%s'";

    @Override
    protected ConnFactory getConnFactory() {
        return new LibraConnFactory();
    }

    @Override
    protected DataSourceType getSourceType() {
        return DataSourceType.LIBRA;
    }

    @Override
    public List<String> getTableList(ISourceDTO iSource, SqlQueryDTO queryDTO) {
        LibraSourceDTO libraSourceDTO = (LibraSourceDTO) iSource;
        String currentDatabase = getCurrentDatabase(libraSourceDTO);
        Integer clearStatus = beforeQuery(libraSourceDTO, queryDTO, false);
        if ((queryDTO == null || StringUtils.isBlank(libraSourceDTO.getSchema())) && StringUtils.isBlank(currentDatabase)) {
            return super.getTableList(libraSourceDTO, queryDTO);
        }

        Statement statement = null;
        ResultSet rs = null;
        try {
            statement = libraSourceDTO.getConnection().createStatement();
            //大小写区分
            rs = statement.executeQuery(String.format("select table_name from information_schema.tables WHERE " +
                    "table_schema in ( '%s' )", StringUtils.isBlank(libraSourceDTO.getSchema()) ? currentDatabase : libraSourceDTO.getSchema()));
            List<String> tableList = new ArrayList<>();
            while (rs.next()) {
                tableList.add(rs.getString(1));
            }
            return tableList;
        } catch (Exception e) {
            throw new DtLoaderException(String.format("get table exception,%s", e.getMessage()), e);
        } finally {
            DBUtil.closeDBResources(rs, statement, DBUtil.clearAfterGetConnection(libraSourceDTO, clearStatus));
        }
    }

    @Override
    public String getCurrentDatabase(ISourceDTO iSource) {
        LibraSourceDTO libraSourceDTO = (LibraSourceDTO) iSource;
        if (iSource == null || StringUtils.isBlank(libraSourceDTO.getUrl())) {
            return StringUtils.EMPTY;
        }

        // 从 URL 中获取 Schema 信息
        String currentSchema = LibraConnFactory.getDriverPropertyInfo(libraSourceDTO.getUrl(), null, "currentSchema");
        if (StringUtils.isNotBlank(currentSchema)) {
            return currentSchema;
        }

        // 如果不存在则从数据库中获取
        return super.getCurrentDatabase(libraSourceDTO);
    }

    @Override
    protected String getCurrentDbSql() {
        return CURRENT_DB;
    }

    @Override
    protected String getCreateDatabaseSql(String dbName, String comment) {
        return String.format(CREATE_SCHEMA_SQL_TMPL, dbName);
    }

    @Override
    public String getShowDbSql() {
        return DATABASE_QUERY;
    }

    /**
     * 此处方法为判断schema是否存在
     *
     * @param source 数据源信息
     * @param dbName schema 名称
     * @return 是否存在结果
     */
    @Override
    public Boolean isDatabaseExists(ISourceDTO source, String dbName) {
        if (StringUtils.isBlank(dbName)) {
            throw new DtLoaderException("schema is not empty");
        }
        return CollectionUtils.isNotEmpty(executeQuery(source, SqlQueryDTO.builder().sql(String.format(DATABASE_IS_EXISTS, dbName)).build()));
    }

    /**
     * 此处方法为判断指定schema 是否有该表
     *
     * @param source 数据源信息
     * @param tableName 表名
     * @param dbName schema名
     * @return 判断结果
     */
    @Override
    public Boolean isTableExistsInDatabase(ISourceDTO source, String tableName, String dbName) {
        if (StringUtils.isBlank(dbName)) {
            throw new DtLoaderException("schema is not empty");
        }
        return CollectionUtils.isNotEmpty(executeQuery(source, SqlQueryDTO.builder().sql(String.format(TABLES_IS_IN_SCHEMA, dbName, tableName)).build()));
    }
}
