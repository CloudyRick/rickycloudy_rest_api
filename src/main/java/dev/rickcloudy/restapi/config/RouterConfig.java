package dev.rickcloudy.restapi.config;

import dev.rickcloudy.restapi.controller.BlogPostHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import dev.rickcloudy.restapi.controller.UserHandler;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterConfig {
	@Bean
	public RouterFunction<ServerResponse> userRoutes(UserHandler handler) {
		return route()
				.POST("/users", handler::save)
				.POST("/users/all", handler::saveAll)
				.GET("/users/{id}", handler::findById)
				.PUT("/users/{id}", handler::update)
				.DELETE("/users/{id}", handler::delete)
				.GET("/users", handler::findAll)
				.GET("/users/", handler::findByParams)
				.build();
	}
	@Bean
	public RouterFunction<ServerResponse> blogRoutes(BlogPostHandler handler) {
		return route().POST("/blogs", handler::save)
//				.GET() // Get By Id
//				.PUT()
//				.DELETE()
//				.GET() // Get All
//				.GET() // Search
				.build();
	}
	/*@Bean
	public RouterFunction<ServerResponse> fileRoute(FileHandler handler) {
		return route().POST()
				.GET() // Get By Id
				.PUT()
				.DELETE()
				.GET() // Get All
				.GET() // Search
				.build();
	}*/
	
}
