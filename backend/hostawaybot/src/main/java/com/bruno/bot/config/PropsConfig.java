package com.bruno.bot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({HostawayProperties.class, DemoProperties.class})
public class PropsConfig {}
