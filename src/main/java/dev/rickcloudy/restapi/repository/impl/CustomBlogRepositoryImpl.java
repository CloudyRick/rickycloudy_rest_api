package dev.rickcloudy.restapi.repository.impl;

import dev.rickcloudy.restapi.entity.BlogPosts;
import dev.rickcloudy.restapi.exception.HttpException;
import dev.rickcloudy.restapi.repository.CustomBlogRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomBlogRepositoryImpl implements CustomBlogRepository {
    private static Logger log = LogManager.getLogger(CustomBlogRepositoryImpl.class);

    private final R2dbcEntityTemplate template;
    @Override
    public Flux<BlogPosts> findByParams(Map<String, String> params) {
        Criteria criteria = Criteria.empty();
        // Get the field names of the BlogPosts class
        List<String> fieldNames = Arrays.stream(BlogPosts.class.getDeclaredFields())
                .map(Field::getName)
                .toList();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            // Check if the key is a valid field name
            if (!fieldNames.contains(entry.getKey())) {
                return Flux.error(new HttpException(HttpStatus.BAD_REQUEST, "Invalid parameter name: " + entry.getKey()));
            }
            criteria = criteria.and(camelCaseToSnakeCase(entry.getKey())).like(entry.getValue() + "%");
            log.info("criteria: " + criteria.toString());
        }

        return template.select(BlogPosts.class)
                .from("blog_posts")
                .matching(Query.query(criteria))
                .all();
    }
    private String camelCaseToSnakeCase(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
}
