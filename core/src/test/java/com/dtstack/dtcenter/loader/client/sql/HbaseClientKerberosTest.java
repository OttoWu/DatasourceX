package com.dtstack.dtcenter.loader.client.sql;

import com.dtstack.dtcenter.loader.client.ClientCache;
import com.dtstack.dtcenter.loader.client.IHbase;
import com.dtstack.dtcenter.loader.client.IKerberos;
import com.dtstack.dtcenter.loader.dto.source.HbaseSourceDTO;
import com.dtstack.dtcenter.loader.kerberos.HadoopConfTool;
import com.dtstack.dtcenter.loader.source.DataSourceType;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * hbase新客户端开启kerberos认证测试
 *
 * @author ：wangchuan
 * date：Created in 9:38 上午 2020/12/2
 * company: www.dtstack.com
 */
public class HbaseClientKerberosTest {

    private static final HbaseSourceDTO source = HbaseSourceDTO.builder()
            .url("eng-cdh1,eng-cdh2,eng-cdh3:2181")
            .path("/hbase")
            .build();

    /**
     * 数据准备
     */
    @BeforeClass
    public static void setUp () throws Exception {
        // 准备 Kerberos 参数
        Map<String, Object> kerberosConfig = new HashMap<>();
        kerberosConfig.put(HadoopConfTool.PRINCIPAL, "hbase/eng-cdh1@DTSTACK.COM");
        kerberosConfig.put(HadoopConfTool.HBASE_MASTER_PRINCIPAL, "hbase/_HOST@DTSTACK.COM");
        kerberosConfig.put(HadoopConfTool.HBASE_REGION_PRINCIPAL, "hbase/_HOST@DTSTACK.COM");
        kerberosConfig.put(HadoopConfTool.PRINCIPAL_FILE, "/hbase-master.keytab");
        kerberosConfig.put(HadoopConfTool.KEY_JAVA_SECURITY_KRB5_CONF, "/krb5.conf");
        source.setKerberosConfig(kerberosConfig);

        String localKerberosPath = HbaseKerberosTest.class.getResource("/eng-cdh").getPath();
        IKerberos kerberos = ClientCache.getKerberos(DataSourceType.HBASE.getVal());
        kerberos.prepareKerberosForConnect(kerberosConfig, localKerberosPath);

        IHbase hbaseClient = ClientCache.getHbase(DataSourceType.HBASE.getVal());
        try {
            hbaseClient.createHbaseTable(source, "wangchuan_test", new String[]{"info1", "info2"});
        } catch (Exception e) {
            // 目前插件化里没有方法支持判断表是否存在，异常不作处理
        }
        hbaseClient.putRow(source, "wangchuan_test", "1001", "info1", "name", "wangchuan");
        hbaseClient.putRow(source, "wangchuan_test", "1002", "info1", "name", "wangbin");
        hbaseClient.putRow(source, "wangchuan_test", "1003", "info2", "name", "wangchuan");
        hbaseClient.putRow(source, "wangchuan_test", "1003", "info2", "age", "18");
        hbaseClient.putRow(source, "wangchuan_test", "1004_loader", "info2", "addr", "beijing");
        hbaseClient.putRow(source, "wangchuan_test", "1005_loader", "info2", "addr", "shanghai");
        hbaseClient.putRow(source, "wangchuan_test", "1006_loader", "info2", "addr", "shenzhen");
        hbaseClient.putRow(source, "wangchuan_test", "1007_loader", "info2", "addr", "hangzhou");
    }

    /**
     * 测试已经存在的namespace
     */
    @Test
    public void dbExists() throws Exception {
        IHbase hbaseClient = ClientCache.getHbase(DataSourceType.HBASE.getVal());
        Boolean check = hbaseClient.isDbExists(source, "default");
        Assert.assertTrue(check);
    }

    /**
     * 测试不存在的namespace
     */
    @Test
    public void dbNotExists() throws Exception {
        IHbase hbaseClient = ClientCache.getHbase(DataSourceType.HBASE.getVal());
        Boolean check = hbaseClient.isDbExists(source, UUID.randomUUID().toString());
        Assert.assertFalse(check);
    }

    /**
     * 创建已经存在的表测试
     */
    @Test
    public void createHbaseTableExists() throws Exception {
        IHbase hbaseClient = ClientCache.getHbase(DataSourceType.HBASE.getVal());
        Boolean check = hbaseClient.createHbaseTable(source, "wangchuan_test", new String[]{"info1", "info2"});
        Assert.assertFalse(check);
    }

    /**
     * 已注try-catch
     * 创建已经存在的表测试，需要测试自己手动修改表名，目前暂时不支持hbase删除表
     */
    @Test
    public void createHbaseTableNotExists() throws Exception {
        IHbase hbaseClient = ClientCache.getHbase(DataSourceType.HBASE.getVal());
        try {
            Boolean check = hbaseClient.createHbaseTable(source, "_tableName", new String[]{"info1", "info2"});
            System.out.println(check);
        } catch (Exception e){
            // 不作处理
        }
    }

    /**
     * 根据rowKey正则获取对应的rowKey列表
     */
    @Test
    public void scanByRegex() throws Exception {
        IHbase hbaseClient = ClientCache.getHbase(DataSourceType.HBASE.getVal());
        List<String> list = hbaseClient.scanByRegex(source, "wangchuan_test", ".*_loader");
        Assert.assertTrue(CollectionUtils.isNotEmpty(list));
    }

    /**
     * 插入指定rowKey、列族、列名的数据
     */
    @Test
    public void putRow() throws Exception{
        IHbase hbaseClient = ClientCache.getHbase(DataSourceType.HBASE.getVal());
        Boolean check = hbaseClient.putRow(source, "wangchuan_test", "1002", "info1", "name", "wangchuan");
        Assert.assertTrue(check);
    }

    /**
     * 获取指定rowKey、列族、列名的数据
     */
    @Test
    public void getRow() throws Exception{
        IHbase hbaseClient = ClientCache.getHbase(DataSourceType.HBASE.getVal());
        String row = hbaseClient.getRow(source, "wangchuan_test", "1003", "info2", "name");
        Assert.assertTrue(org.apache.commons.lang3.StringUtils.isNotBlank(row));
    }

    /**
     * 删除指定rowKey、列族、列名的数据
     */
    @Test
    public void deleteByRowKey() throws Exception {
        IHbase hbaseClient = ClientCache.getHbase(DataSourceType.HBASE.getVal());
        Boolean check = hbaseClient.deleteByRowKey(source, "wangchuan_test", "info1", "name", Lists.newArrayList("1001", "1002"));
        Assert.assertTrue(check);
    }
}
