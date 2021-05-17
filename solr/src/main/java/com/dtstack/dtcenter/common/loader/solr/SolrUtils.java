package com.dtstack.dtcenter.common.loader.solr;

import com.dtstack.dtcenter.loader.exception.DtLoaderException;
import com.dtstack.dtcenter.loader.kerberos.HadoopConfTool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import sun.security.krb5.Config;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Slf4j
public class SolrUtils {

    public synchronized static void initKerberosConfig(Map<String, Object> kerberosConfig) {
        String solrLoginConf = writeSolrJaas(kerberosConfig);
        // 刷新kerberos认证信息，在设置完java.security.krb5.conf后进行，否则会使用上次的krb5文件进行 refresh 导致认证失败
        try {
            Config.refresh();
            javax.security.auth.login.Configuration.setConfiguration(null);
        } catch (Exception e) {
            log.error("Kafka kerberos authentication information refresh failed!");
        }
        System.setProperty("solr.kerberos.jaas.appname", "SolrJClient");
        System.setProperty("java.security.auth.login.config", solrLoginConf);


    }

    /**
     * solr jaas文件，同时处理 krb5.conf
     *
     * @param kerberosConfig
     * @return jaas文件绝对路径
     */
    private static String writeSolrJaas(Map<String, Object> kerberosConfig) {
        log.info("Initialize solr JAAS file, kerberosConfig : {}", kerberosConfig);
        if (MapUtils.isEmpty(kerberosConfig)) {
            return null;
        }

        // 处理 krb5.conf
        if (kerberosConfig.containsKey(HadoopConfTool.KEY_JAVA_SECURITY_KRB5_CONF)) {
            System.setProperty(HadoopConfTool.KEY_JAVA_SECURITY_KRB5_CONF, MapUtils.getString(kerberosConfig, HadoopConfTool.KEY_JAVA_SECURITY_KRB5_CONF));
        }

        String keytabConf = MapUtils.getString(kerberosConfig, HadoopConfTool.PRINCIPAL_FILE);
        try {
            File file = new File(keytabConf);
            File jaas = new File(file.getParent() + File.separator + "solr_jaas.conf");
            if (jaas.exists()) {
                jaas.delete();
            }

            String principal = MapUtils.getString(kerberosConfig, HadoopConfTool.PRINCIPAL);
            FileUtils.write(jaas, String.format(SolrConsistent.SOLR_JAAS_CONTENT, keytabConf, principal));
            String solrLoginConf = jaas.getAbsolutePath();
            log.info("Init solr Kerberos:login-conf:{}\n --sasl.kerberos.service.name:{}", keytabConf, principal);
            return solrLoginConf;
        } catch (IOException e) {
            throw new DtLoaderException(String.format("Writing to solr configuration file exception,%s", e.getMessage()), e);
        }
    }
}
