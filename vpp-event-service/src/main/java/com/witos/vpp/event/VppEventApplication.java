package com.witos.vpp.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.witos.common.security.annotation.EnableCustomConfig;
import com.witos.common.security.annotation.EnableWitOSFeignClients;
import com.witos.common.swagger.annotation.EnableCustomSwagger2;

/**
 * VPP 事件服务：资源台账 / 需求响应事件 / 申报
 *
 * @author witos
 */
@Slf4j
@EnableCustomConfig
@EnableCustomSwagger2
@EnableWitOSFeignClients
@EnableScheduling
@SpringBootApplication
public class VppEventApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(VppEventApplication.class, args);
        log.info("VPP 事件服务启动成功（资源台账 / 需求响应事件 / 申报）");
    }
}