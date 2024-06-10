package dev.rickcloudy.restapi.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Map;

@Component
public class GlobalErrorAttribute extends DefaultErrorAttributes {

    private static final Logger log = LogManager.getLogger(GlobalErrorAttribute.class);


    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request,
                                                  ErrorAttributeOptions options) {
        Map<String, Object> map = super.getErrorAttributes(request, options);
        Throwable error = getError(request);

        if (error instanceof HttpException httpError) {

            map.remove("path");
            map.remove("timestamp");
            map.remove("requestId");
            map.put("status", httpError.getHttpStatus().value());
            map.put("error", httpError.getHttpStatus());
            map.put("message", httpError.getMessage());
            map.put("success", false);
        } else {
            // handle other exceptions
            map.put("message", error.getMessage());
            map.put("success", false);
        }

        return map;
    }
}
