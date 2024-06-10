package dev.rickcloudy.restapi.entity;

import dev.rickcloudy.restapi.enums.ImageType;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.ZonedDateTime;
import java.util.Objects;

@Table(name = "blog_images")
@Data
@Builder
public class BlogImages {
    @Id
    private Long id;
    private Long blogPostId;
    private String imageUrl;
    private String alt;
    private String caption;
    private String credit;
    private ImageType type;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlogImages that = (BlogImages) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(blogPostId, that.blogPostId) &&
                Objects.equals(imageUrl, that.imageUrl) &&
                Objects.equals(alt, that.alt) &&
                Objects.equals(caption, that.caption) &&
                Objects.equals(credit, that.credit) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, blogPostId, imageUrl, alt, caption, credit, type);
    }
}
