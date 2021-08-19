package com.dtstack.dtcenter.loader.dto.tsdb;

import lombok.Builder;
import lombok.Data;

/**
 * 聚合器类型
 *
 * @author ：wangchuan
 * date：Created in 上午10:20 2021/6/24
 * company: www.dtstack.com
 */
@Data
@Builder
public class DeltaOptions {

    private Boolean counter;

    private Boolean dropResets;

    private Long counterMax;
}