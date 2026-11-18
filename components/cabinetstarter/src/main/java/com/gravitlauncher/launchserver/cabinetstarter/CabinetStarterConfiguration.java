package com.gravitlauncher.launchserver.cabinetstarter;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CabinetStarterRunner.class)
public class CabinetStarterConfiguration {
}
