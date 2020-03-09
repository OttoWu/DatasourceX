package com.dtstack.dtcenter.common.loader.rdbms.db2;

import com.dtstack.dtcenter.common.enums.DataSourceType;
import com.dtstack.dtcenter.common.exception.DBErrorCode;
import com.dtstack.dtcenter.common.exception.DtCenterDefException;
import com.dtstack.dtcenter.common.loader.rdbms.common.AbsRdbmsClient;
import com.dtstack.dtcenter.common.loader.rdbms.common.ConnFactory;
import com.dtstack.dtcenter.loader.dto.SourceDTO;
import com.dtstack.dtcenter.loader.dto.SqlQueryDTO;
import com.dtstack.dtcenter.loader.utils.DBUtil;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * @company: www.dtstack.com
 * @Author ：Nanqi
 * @Date ：Created in 16:09 2020/1/7
 * @Description：Db2 客户端
 */
public class Db2Client extends AbsRdbmsClient {
    private static final String TABLE_QUERY = "select tabname from syscat.tables where tabschema = '%s'";

    @Override
    protected ConnFactory getConnFactory() {
        return new Db2ConnFactory();
    }

    @Override
    protected DataSourceType getSourceType() {
        return DataSourceType.DB2;
    }

    @Override
    public List<String> getTableList(SourceDTO source, SqlQueryDTO queryDTO) throws Exception {
        Boolean closeQuery = beforeQuery(source, queryDTO, false);

        Statement statement = null;
        ResultSet rs = null;
        List<String> tableList = new ArrayList<>();
        try {
            String sql = String.format(TABLE_QUERY, source.getUsername().toUpperCase());
            statement = source.getConnection().createStatement();
            rs = statement.executeQuery(sql);
            int columnSize = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                tableList.add(rs.getString(1));
            }
        } catch (Exception e) {
            throw new DtCenterDefException("获取表异常", e);
        } finally {
            DBUtil.closeDBResources(rs, statement, closeQuery ? source.getConnection() : null);
        }
        return tableList;
    }

    @Override
    public String getTableMetaComment(SourceDTO source, SqlQueryDTO queryDTO) throws Exception {
        Boolean closeQuery = beforeColumnQuery(source, queryDTO);

        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = source.getConnection().createStatement();
            resultSet = statement.executeQuery(String.format("select remarks from syscat.tables where tabname = '%s'"
                    , queryDTO.getTableName()));
            while (resultSet.next()) {
                return resultSet.getString("REMARKS");
            }
        } catch (Exception e) {
            throw new DtCenterDefException(String.format("获取表:%s 的信息时失败. 请联系 DBA 核查该库、表信息.",
                    queryDTO.getTableName()),
                    DBErrorCode.GET_COLUMN_INFO_FAILED, e);
        } finally {
            DBUtil.closeDBResources(resultSet, statement, closeQuery ? source.getConnection() : null);
        }
        return null;
    }
}