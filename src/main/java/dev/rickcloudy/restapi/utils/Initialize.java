package dev.rickcloudy.restapi.utils;

import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Initialize implements ApplicationListener<ApplicationStartedEvent> {
    private final Logger log = LogManager.getLogger(Initialize.class);

    @Override
    public void onApplicationEvent(ApplicationStartedEvent event) {
        log.debug("============= Initialize Data =============");

        log.debug("============= Initialization Successfull =============");
    }
}
