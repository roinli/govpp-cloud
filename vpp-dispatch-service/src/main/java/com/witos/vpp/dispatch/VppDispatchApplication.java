package com.witos.vpp.dispatch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.witos.common.security.annotation.EnableCustomConfig;
import com.witos.common.security.annotation.EnableWitOSFeignClients;
import com.witos.common.swagger.annotation.EnableCustomSwagger2;

/**
 * 虚拟电厂-调度服务（分配 / 指令 / 执行 / 评估 / 结算）
 *
 * @author witos
 */
@Slf4j
@EnableCustomConfig
@EnableCustomSwagger2
@EnableWitOSFeignClients
@EnableScheduling
@SpringBootApplication
public class VppDispatchApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(VppDispatchApplication.class, args);
        log.info("(^_^) VPP 虚拟电厂-调度服务启动成功 :39320 (^_^)");
    }
}
