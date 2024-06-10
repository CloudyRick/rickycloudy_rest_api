package dev.rickcloudy.restapi.entity;

import dev.rickcloudy.restapi.enums.BlogStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

@Table(name = "blog_posts")
@Data
@Builder
public class BlogPosts {
    @Id
    private Long id;
    @NotBlank(message = "Title must not be blank")
    private String title;
    @NotBlank(message = "Content must not be blank")
    private String content;
    @NotBlank(message = "Author must not be blank")
    private Long authorId;
    private BlogStatus status;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlogPosts blogPosts = (BlogPosts) o;
        return Objects.equals(id, blogPosts.id) &&
                Objects.equals(title, blogPosts.title) &&
                Objects.equals(content, blogPosts.content) &&
                Objects.equals(authorId, blogPosts.authorId) &&
                Objects.equals(status, blogPosts.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, content, authorId, status);
    }
}
