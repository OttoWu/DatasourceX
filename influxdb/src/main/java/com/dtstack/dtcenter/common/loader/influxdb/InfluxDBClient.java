package com.dtstack.dtcenter.common.loader.influxdb;

import com.dtstack.dtcenter.common.loader.common.nosql.AbsNoSqlClient;
import com.dtstack.dtcenter.common.loader.common.utils.SearchUtil;
import com.dtstack.dtcenter.loader.dto.ColumnMetaDTO;
import com.dtstack.dtcenter.loader.dto.SqlQueryDTO;
import com.dtstack.dtcenter.loader.dto.source.ISourceDTO;
import com.dtstack.dtcenter.loader.exception.DtLoaderException;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.influxdb.InfluxDB;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * influxDB 客户端
 *
 * @author ：wangchuan
 * date：Created in 上午10:33 2021/6/7
 * company: www.dtstack.com
 */
public class InfluxDBClient<T> extends AbsNoSqlClient<T> {

    // 获取所有的数据库
    private static final String SHOW_DATABASE = "SHOW databases";

    // 获取当前数据库下面的所有表
    private static final String SHOW_TABLE = "SHOW measurements";

    // 数据预览并限制条数 SQL
    private static final String PREVIEW_LIMIT = "SELECT * FROM %s LIMIT %s";

    // 获取表字段信息 SQL
    private static final String SHOW_FIELD = "SHOW field keys from %s";

    @Override
    public Boolean testCon(ISourceDTO source) {
        return InfluxDBConnFactory.testCon(source);
    }

    @Override
    public List<String> getAllDatabases(ISourceDTO source, SqlQueryDTO queryDTO) {
        InfluxDB influxDB = InfluxDBConnFactory.getClient(source);
        List<String> dbList = InfluxDBUtil.queryWithOneWay(influxDB, SHOW_DATABASE, true);
        return SearchUtil.handleSearchAndLimit(dbList, queryDTO);
    }

    @Override
    public List<String> getTableList(ISourceDTO source, SqlQueryDTO queryDTO) {
        return getTableListBySchema(source, queryDTO);
    }

    @Override
    public List<String> getTableListBySchema(ISourceDTO source, SqlQueryDTO queryDTO) {
        InfluxDB influxDB = InfluxDBConnFactory.getClient(InfluxDBUtil.dealDb(source, queryDTO));
        List<String> tableList = InfluxDBUtil.queryWithOneWay(influxDB, SHOW_TABLE, true);
        return SearchUtil.handleSearchAndLimit(tableList, queryDTO);
    }

    @Override
    public List<List<Object>> getPreview(ISourceDTO source, SqlQueryDTO queryDTO) {
        InfluxDB influxDB = InfluxDBConnFactory.getClient(InfluxDBUtil.dealDb(source, queryDTO));
        if (StringUtils.isBlank(queryDTO.getTableName())) {
            throw new DtLoaderException("table name cannot be empty.");
        }
        return InfluxDBUtil.queryWithList(influxDB, String.format(PREVIEW_LIMIT, queryDTO.getTableName(), queryDTO.getPreviewNum()), true);
    }

    @Override
    public List<String> getRootDatabases(ISourceDTO source, SqlQueryDTO queryDTO) {
        return getAllDatabases(source, queryDTO);
    }

    @Override
    public List<Map<String, Object>> executeQuery(ISourceDTO source, SqlQueryDTO queryDTO) {
        InfluxDB influxDB = InfluxDBConnFactory.getClient(InfluxDBUtil.dealDb(source, queryDTO));
        return InfluxDBUtil.queryWithMap(influxDB, queryDTO.getSql(), true);
    }

    @Override
    public Boolean createDatabase(ISourceDTO source, String dbName, String comment) {
        InfluxDB influxDB = InfluxDBConnFactory.getClient(source);
        influxDB.createDatabase(dbName);
        return true;
    }

    @Override
    public List<ColumnMetaDTO> getColumnMetaData(ISourceDTO source, SqlQueryDTO queryDTO) {
        if (StringUtils.isBlank(queryDTO.getTableName())) {
            throw new DtLoaderException("table name cannot be empty.");
        }
        InfluxDB influxDB = InfluxDBConnFactory.getClient(InfluxDBUtil.dealDb(source, queryDTO));
        List<List<Object>> result = InfluxDBUtil.queryWithList(influxDB, String.format(SHOW_FIELD, queryDTO.getTableName()), true);
        List<ColumnMetaDTO> columnMetas = Lists.newArrayList();
        if (CollectionUtils.isEmpty(result) || result.size() < 2) {
            return columnMetas;
        }
        for (int i = 1; i < result.size(); i++) {
            List<Object> row = result.get(i);
            if (CollectionUtils.isNotEmpty(row) && row.size() == 2) {
                ColumnMetaDTO columnMetaDTO = new ColumnMetaDTO();
                columnMetaDTO.setKey(Objects.nonNull(row.get(0)) ? row.get(0).toString() : "");
                columnMetaDTO.setType(Objects.nonNull(row.get(1)) ? row.get(1).toString() : "");
                columnMetas.add(columnMetaDTO);
            }
        }
        return columnMetas;
    }

}