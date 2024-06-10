package dev.rickcloudy.restapi.dto;

import lombok.*;

@Getter
@Setter
public class UploadResult {
    private String url;
    private String key;

    public UploadResult(String url, String key) {
        this.url = url;
        this.key = key;
    }
}
