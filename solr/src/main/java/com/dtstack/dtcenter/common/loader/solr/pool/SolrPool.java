/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtstack.dtcenter.common.loader.solr.pool;

import com.dtstack.dtcenter.common.loader.common.Pool;
import org.apache.solr.client.solrj.impl.CloudSolrClient;

/**
 * @company: www.dtstack.com
 * @Author ：qianyi
 * @Date ：Created in 下午5:14 2021/5/7
 * @Description：
 */
public class SolrPool extends Pool<CloudSolrClient> {

    private SolrPoolConfig config;

    public SolrPool(SolrPoolConfig config) {
        super(config, new SolrPoolFactory(config));
        this.config = config;
    }

    public SolrPoolConfig getConfig() {
        return config;
    }

    public void setConfig(SolrPoolConfig config) {
        this.config = config;
    }

}
