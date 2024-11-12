package dev.rickcloudy.restapi.repository;


import dev.rickcloudy.restapi.entity.Users;
import dev.rickcloudy.restapi.exception.custom.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import dev.rickcloudy.restapi.enums.UserStatus;
import dev.rickcloudy.restapi.exception.HttpException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.springframework.data.relational.core.query.Criteria.where;
import static org.springframework.data.relational.core.query.Query.query;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


@Repository
@RequiredArgsConstructor
public class UserRepository {
	private final R2dbcEntityTemplate template;
	private final Logger log = LogManager.getLogger(UserRepository.class);

	public Mono<Users> save(Users user) {
		return template.insert(Users.class)
				.into("users")
				.using(user)
				.doOnNext(res -> log.debug("New user created successfully with id {}", res))
				.doOnError(res -> {
					log.debug("Failed to create user");
				});
	}

	public Flux<Users> saveAll(List<Users> users) {
		return Flux.fromIterable(users)
				.flatMap(this::save)
				.doOnNext(res -> log.debug("User saved with id {}", res.getId()))
				.doOnError(error -> log.error("Error saving users batch", error));
	}

	public Mono<Users> findById(Long id) {
		return template.select(Users.class)
						.from("users")
						.matching(
								query(where("id").is(id))
						)
				.one()
				.flatMap(res -> {
					if (res.getStatus() == UserStatus.DELETED) return Mono.error(new UserNotFoundException(HttpStatus.BAD_REQUEST, "User with ID " + id + " does not exists"));
					return Mono.just(res);
				})
				.switchIfEmpty(Mono.error(new UserNotFoundException(HttpStatus.NOT_FOUND, "User with ID " + id + " does not exists")));
	}

	public Mono<Users> findByEmail(String email) {
		return template.select(Users.class)
				.from("users")
				.matching(
						query(where("email").is(email))
				)
				.one()
				.flatMap(res -> {
					if (res.getStatus() == UserStatus.DELETED) return Mono.error(new UserNotFoundException(HttpStatus.BAD_REQUEST, "User with email " + email + " does not exists"));
					return Mono.just(res);
				})
				.switchIfEmpty(Mono.error(new UserNotFoundException(HttpStatus.BAD_REQUEST, "User with email " + email + " does not exists")));
	}

	public Mono<Users> findByUsername(String username) {
		return template.select(Users.class)
				.from("users")
				.matching(
						query(where("username").is(username))
				)
				.one()
				.flatMap(res -> {
					if (res.getStatus() == UserStatus.DELETED) return Mono.error(new UserNotFoundException(HttpStatus.BAD_REQUEST, "User with username " + username + " does not exists"));
					return Mono.just(res);
				})
				.switchIfEmpty(Mono.error(new HttpException(HttpStatus.BAD_REQUEST, "User with username " + username + " does not exists")));
	}
	public Mono<Users> update(Users user) {
		return template.update(Users.class)
						.inTable("users")
						.matching(
								query(where("id").is(user.getId()))
						)
						.apply(
								Update.update("first_name", user.getFirstName())
								.set("last_name", user.getLastName())
								.set("email", user.getEmail())
								.set("username", user.getUsername())
								.set("status", user.getStatus())
						).then(Mono.just(user));
	}

	
	public Mono<Long> delete(Long id) {
		return template.delete(Users.class)
						.from("users")
						.matching(
								query(where("id").is(id))
								)
				.all();
	}

	public Mono<Long> changePassword(Long id, String newPassword) {
		return template.update(Users.class)
				.inTable("users")
				.matching(
						query(where("id").is(id))
				).apply(
						Update.update("password", newPassword)
				);
    }

	public Flux<Users> findAll() {
		return template.select(Users.class)
				.from("users")
						.all();
	}
	
	public Mono<Boolean> existsByEmail(String email) {
		return template.select(Users.class)
				.from("users")
						.matching(query(
									where("email").is(email)
								)
						).exists()
				.doOnNext(res -> log.info("Exists by email is invoked"));
	}
	
	public Mono<Boolean> existsByUsername(String username) {
		return template.select(Users.class)
				.from("users")
						.matching(query(
									where("username").is(username)
								)
						).exists();
	}
	public Flux<Users> findByParams(Map<String, String> params) {
		Criteria criteria = Criteria.empty();
		List<String> fieldNames = Arrays.stream(Users.class.getDeclaredFields())
				.map(Field::getName)
				.toList();

		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (!fieldNames.contains(entry.getKey())) {
				return Flux.error(new HttpException(HttpStatus.BAD_REQUEST, "Invalid parameter name: " + entry.getKey()));
			}
			criteria = criteria.and(camelCaseToSnakeCase(entry.getKey())).like(entry.getValue() + "%");
		}

		return template.select(Users.class)
				.from("users")
				.matching(Query.query(criteria))
				.all();
	}

	public Mono<Void> deleteAll() {
		return template.delete(Users.class)
				.from("users")
				.all().then();
	}
	public Mono<Long> count() {
		return template.count(Query.empty(), Users.class);
	}
	private String camelCaseToSnakeCase(String camelCase) {
		return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
	}
}
