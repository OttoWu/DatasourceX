package com.dtstack.dtcenter.common.loader.kingbase;

import com.dtstack.dtcenter.common.loader.common.utils.DBUtil;
import com.dtstack.dtcenter.common.loader.rdbms.AbsRdbmsClient;
import com.dtstack.dtcenter.common.loader.rdbms.ConnFactory;
import com.dtstack.dtcenter.loader.IDownloader;
import com.dtstack.dtcenter.loader.dto.ColumnMetaDTO;
import com.dtstack.dtcenter.loader.dto.SqlQueryDTO;
import com.dtstack.dtcenter.loader.dto.source.ISourceDTO;
import com.dtstack.dtcenter.loader.dto.source.KingbaseSourceDTO;
import com.dtstack.dtcenter.loader.dto.source.RdbmsSourceDTO;
import com.dtstack.dtcenter.loader.exception.DtLoaderException;
import com.dtstack.dtcenter.loader.source.DataSourceType;
import org.apache.commons.lang3.StringUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * company: www.dtstack.com
 * @author ：忘川
 * Date ：Created in 17:18 2020/09/01
 * Description：kingbase 客户端
 */
public class KingbaseClient extends AbsRdbmsClient {

    /**
     * 获取所有schema，去除系统库
     */
    private static final String SCHEMA_SQL = "SELECT NSPNAME FROM SYS_CATALOG.SYS_NAMESPACE WHERE NSPNAME !~ 'sys' AND NSPNAME <> 'information_schema' ORDER BY NSPNAME ";

    /**
     * 获取某个schema下的所有表
     */
    private static final String SCHEMA_TABLE_SQL = "SELECT tablename FROM SYS_CATALOG.sys_tables WHERE schemaname = '%s' ";

    /**
     * 获取所有表名，表名前拼接schema，并对schema和tableName进行增加双引号处理
     */
    private static final String ALL_TABLE_SQL = "SELECT '\"'||schemaname||'\".\"'||tablename||'\"' AS schema_table FROM SYS_CATALOG.sys_tables order by schema_table ";

    /**
     * 获取某个表的表注释信息
     */
    private static final String TABLE_COMMENT_SQL = "SELECT COMMENTS FROM ALL_TAB_COMMENTS WHERE TABLE_NAME = '%s' ";

    /**
     * 获取某个表的字段注释信息
     */
    private static final String COL_COMMENT_SQL = "SELECT COLUMN_NAME,COMMENTS FROM ALL_COL_COMMENTS WHERE TABLE_NAME = '%s' ";

    // 获取正在使用数据库
    private static final String CURRENT_DB = "select current_database()";

    private static final String DONT_EXIST = "doesn't exist";

    @Override
    protected ConnFactory getConnFactory() {
        return new KingbaseConnFactory();
    }

    @Override
    protected DataSourceType getSourceType() {
        return DataSourceType.KINGBASE8;
    }

    @Override
    public List<String> getTableList(ISourceDTO source, SqlQueryDTO queryDTO) throws Exception {
        KingbaseSourceDTO kingbaseSourceDTO = (KingbaseSourceDTO) source;
        Integer clearStatus = beforeQuery(kingbaseSourceDTO, queryDTO, false);
        Statement statement = null;
        ResultSet rs = null;
        try {
            statement = kingbaseSourceDTO.getConnection().createStatement();
            //不区分大小写
            rs = statement.executeQuery(StringUtils.isNotBlank(kingbaseSourceDTO.getSchema()) ?
                    String.format(SCHEMA_TABLE_SQL, kingbaseSourceDTO.getSchema()) : ALL_TABLE_SQL);
            List<String> tableList = new ArrayList<>();
            while (rs.next()) {
                tableList.add(rs.getString(1));
            }
            return tableList;
        } catch (Exception e) {
            throw new DtLoaderException("获取表异常", e);
        } finally {
            DBUtil.closeDBResources(rs, statement, kingbaseSourceDTO.clearAfterGetConnection(clearStatus));
        }
    }

    /**
     * 获取表注释信息
     * @param source
     * @param queryDTO
     * @return
     * @throws Exception
     */
    @Override
    public String getTableMetaComment(ISourceDTO source, SqlQueryDTO queryDTO) throws Exception {
        KingbaseSourceDTO kingbaseSourceDTO = (KingbaseSourceDTO) source;
        Integer clearStatus = beforeColumnQuery(kingbaseSourceDTO, queryDTO);
        Statement statement = null;
        ResultSet resultSet = null;

        try {
            statement = kingbaseSourceDTO.getConnection().createStatement();
            resultSet = statement.executeQuery(String.format(TABLE_COMMENT_SQL, queryDTO.getTableName()));
            while (resultSet.next()) {
                return resultSet.getString(1);
            }
        } catch (Exception e) {
            throw new DtLoaderException(String.format("获取表:%s 的信息时失败. 请联系 DBA 核查该库、表信息.",
                    queryDTO.getTableName()), e);
        } finally {
            DBUtil.closeDBResources(resultSet, statement, kingbaseSourceDTO.clearAfterGetConnection(clearStatus));
        }
        return "";
    }

    /**
     * 处理kingbase schema和tableName，适配schema和tableName中有.的情况
     * @param schema
     * @param tableName
     * @return
     */
    @Override
    protected String transferSchemaAndTableName(String schema, String tableName) {
        if (!tableName.startsWith("\"") || !tableName.endsWith("\"")) {
            tableName = String.format("\"%s\"", tableName);
        }
        if (StringUtils.isBlank(schema)) {
            return tableName;
        }
        if (!schema.startsWith("\"") || !schema.endsWith("\"")){
            schema = String.format("\"%s\"", schema);
        }
        return String.format("%s.%s", schema, tableName);
    }

    /**
     * 获取所有 数据库/schema sql语句
     * @return
     */
    @Override
    protected String getShowDbSql(){
        return SCHEMA_SQL;
    }

    /**
     * 获取字段注释
     * @param sourceDTO
     * @param queryDTO
     * @return
     * @throws Exception
     */
    @Override
    protected Map<String, String> getColumnComments(RdbmsSourceDTO sourceDTO, SqlQueryDTO queryDTO) throws Exception {
        Integer clearStatus = beforeColumnQuery(sourceDTO, queryDTO);
        Statement statement = null;
        ResultSet rs = null;
        Map<String, String> columnComments = new HashMap<>();
        try {
            statement = sourceDTO.getConnection().createStatement();
            rs = statement.executeQuery(String.format(COL_COMMENT_SQL, queryDTO.getTableName()));
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME");
                String columnComment = rs.getString("COMMENTS");
                columnComments.put(columnName, columnComment);
            }

        } catch (Exception e) {
            throw new DtLoaderException(String.format("获取表:%s 的字段的注释信息时失败. 请联系 DBA 核查该库、表信息.",
                    queryDTO.getTableName()), e);
        }finally {
            DBUtil.closeDBResources(rs, statement, sourceDTO.clearAfterGetConnection(clearStatus));
        }
        return columnComments;
    }

    @Override
    public List<ColumnMetaDTO> getFlinkColumnMetaData(ISourceDTO source, SqlQueryDTO queryDTO) throws Exception {
        Integer clearStatus = beforeColumnQuery(source, queryDTO);
        KingbaseSourceDTO kingbaseSourceDTO = (KingbaseSourceDTO) source;
        Statement statement = null;
        ResultSet rs = null;
        List<ColumnMetaDTO> columns = new ArrayList<>();
        try {
            statement = kingbaseSourceDTO.getConnection().createStatement();
            String queryColumnSql = "select * from " + transferSchemaAndTableName(kingbaseSourceDTO.getSchema(), queryDTO.getTableName())
                    + " where 1=2";
            rs = statement.executeQuery(queryColumnSql);
            ResultSetMetaData rsMetaData = rs.getMetaData();
            for (int i = 0, len = rsMetaData.getColumnCount(); i < len; i++) {
                ColumnMetaDTO columnMetaDTO = new ColumnMetaDTO();
                columnMetaDTO.setKey(rsMetaData.getColumnName(i + 1));
                String type = rsMetaData.getColumnTypeName(i + 1);
                int columnType = rsMetaData.getColumnType(i + 1);
                int precision = rsMetaData.getPrecision(i + 1);
                int scale = rsMetaData.getScale(i + 1);
                //kingbase类型转换
                String flinkSqlType = KingbaseAdapter.mapColumnTypeJdbc2Java(columnType, precision, scale);
                if (StringUtils.isNotEmpty(flinkSqlType)) {
                    type = flinkSqlType;
                }
                columnMetaDTO.setType(type);
                // 获取字段精度
                if (columnMetaDTO.getType().equalsIgnoreCase("decimal")
                        || columnMetaDTO.getType().equalsIgnoreCase("float")
                        || columnMetaDTO.getType().equalsIgnoreCase("double")
                        || columnMetaDTO.getType().equalsIgnoreCase("numeric")) {
                    columnMetaDTO.setScale(rsMetaData.getScale(i + 1));
                    columnMetaDTO.setPrecision(rsMetaData.getPrecision(i + 1));
                }
                columns.add(columnMetaDTO);
            }
            return columns;

        } catch (SQLException e) {
            if (e.getMessage().contains(DONT_EXIST)) {
                throw new DtLoaderException(queryDTO.getTableName() + "表不存在", e);
            } else {
                throw new DtLoaderException(String.format("获取表:%s 的字段的元信息时失败. 请联系 DBA 核查该库、表信息.", queryDTO.getTableName()) , e);
            }
        } finally {
            DBUtil.closeDBResources(rs, statement, kingbaseSourceDTO.clearAfterGetConnection(clearStatus));
        }

    }

    /**
     * 处理kingbase数据预览sql
     * @param sourceDTO
     * @param sqlQueryDTO
     * @return
     */
    @Override
    protected String dealSql(ISourceDTO sourceDTO, SqlQueryDTO sqlQueryDTO){
        KingbaseSourceDTO kingbaseSourceDTO = (KingbaseSourceDTO) sourceDTO;
        return "select * from " + transferSchemaAndTableName(kingbaseSourceDTO.getSchema(), sqlQueryDTO.getTableName());
    }

    @Override
    public IDownloader getDownloader(ISourceDTO source, SqlQueryDTO queryDTO) throws Exception {
        throw new DtLoaderException("Not Support");
    }

    @Override
    public List<ColumnMetaDTO> getPartitionColumn(ISourceDTO source, SqlQueryDTO queryDTO) throws Exception {
        throw new DtLoaderException("Not Support");
    }

    @Override
    protected String getCurrentDbSql() {
        return CURRENT_DB;
    }
}
