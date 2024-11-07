package dev.rickcloudy.restapi.dto;

import dev.rickcloudy.restapi.entity.BlogImages;
import dev.rickcloudy.restapi.enums.BlogStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlogPostsDTO {
    private Long id;
    private String title;
    private String content;
    private Long authorId;
    private BlogStatus status;
    private List<BlogImages> images;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
}
