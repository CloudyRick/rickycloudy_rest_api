package dev.rickcloudy.restapi.config;

import dev.rickcloudy.restapi.controller.AuthHandler;
import dev.rickcloudy.restapi.controller.BlogPostHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import dev.rickcloudy.restapi.controller.UserHandler;

import java.util.Arrays;
import java.util.Collections;

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
				.POST("/blogs/images", handler::uploadBlogImage)
//				.GET() // Get By Id
//				.PUT()
//				.DELETE()
//				.GET() // Get All
//				.GET() // Search
				.build();
	}

	@Bean
	@CrossOrigin(origins = "http://localhost:5173")
	public RouterFunction<ServerResponse> authRoutes(AuthHandler handler) {
		return route()
				.POST("/auth/login", handler::login)
				.POST("/auth/refresh-token", handler::refreshToken)
				.build();
	}

	@Bean
	public CorsWebFilter corsWebFilter() {
		CorsConfiguration corsConfig = new CorsConfiguration();
		corsConfig.addAllowedOrigin("http://localhost:5173"); // Add your frontend's URL
		corsConfig.addAllowedMethod("*"); // Allow all HTTP methods
		corsConfig.addAllowedHeader("*"); // Allow all headers
		corsConfig.setAllowCredentials(true); // Allow cookies if needed

		// Explicitly add headers if using custom ones
		corsConfig.addExposedHeader("Authorization");

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", corsConfig);

		return new CorsWebFilter(source);
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
