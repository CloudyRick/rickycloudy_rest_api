package dev.rickcloudy.restapi.controller;

import dev.rickcloudy.restapi.entity.Users;
import dev.rickcloudy.restapi.exception.HttpException;
import dev.rickcloudy.restapi.mapper.UserMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import dev.rickcloudy.restapi.dto.ResponseDTO;
import dev.rickcloudy.restapi.service.UserService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserHandler implements Handler {
	private final UserService userService;
	private final UserMapper mapper;
	private final Validator validator;
	private final Logger LOG = LogManager.getLogger(UserHandler.class);


	public Mono<ServerResponse> save(ServerRequest req) {
		return req.bodyToMono(Users.class)
				.flatMap(user -> {
					Errors errors = new BeanPropertyBindingResult(user, "user");
					validator.validate(user, errors);
					if (errors.hasErrors()) {
						String errorMessage = errors.getAllErrors()
								.stream()
								.map(DefaultMessageSourceResolvable::getDefaultMessage)
								.collect(Collectors.joining(", "));
						return Mono.just(ResponseDTO.fail(null, errorMessage));
					}
					return userService.save(user)
							.map(savedUser -> ResponseDTO.success(savedUser, "User created successfully"));
				})
				.flatMap(responseDTO -> ServerResponse.status(responseDTO.isSuccess() ? HttpStatus.CREATED : HttpStatus.BAD_REQUEST)
						.body(Mono.just(responseDTO), ResponseDTO.class)
				);
	}

	@Override
	public Mono<ServerResponse> saveAll(ServerRequest request) {
		return userService.saveAll(request.bodyToFlux(Users.class))
				.collectList()
				.flatMap(res -> {
					boolean hasFailure = res.stream().anyMatch(responseDTO -> !responseDTO.isSuccess());
					HttpStatus status = hasFailure ? HttpStatus.BAD_REQUEST : HttpStatus.CREATED;
					return ServerResponse.status(status)
							.body(Mono.just(res), ResponseDTO.class);
				});
	}

	public Mono<ServerResponse> findById(ServerRequest req) {
		return userService.findById(Long.valueOf(req.pathVariable("id")))
				.flatMap(res -> ServerResponse.ok()
						.body(Mono.just(ResponseDTO.success(res, "User found")), ResponseDTO.class)
				);
	}

	@Override
	public Mono<ServerResponse> update(ServerRequest request) {
		var existed = userService.findById(Long.valueOf(request.pathVariable("id"))).map(mapper::dtoToUser);
		return Mono.zip(
						(data) -> {
							Users u = (Users) data[0];
							Users u2 = (Users) data[1];

							if (!Long.valueOf(request.pathVariable("id")).equals(u2.getId())) {
								throw new HttpException(HttpStatus.FORBIDDEN, "Action not allowed");
							}
							// Validation: Validate the updated user data
							Errors errors = new BeanPropertyBindingResult(u2, "user");
							validator.validate(u2, errors);
							if (errors.hasErrors()) {
								String errorMessage = errors.getAllErrors()
										.stream()
										.map(DefaultMessageSourceResolvable::getDefaultMessage)
										.collect(Collectors.joining(", "));
								throw new HttpException(HttpStatus.BAD_REQUEST, errorMessage);
							}

                            u.setId(u2.getId());
                            if(StringUtils.hasText(u2.getFirstName())) {
								u.setFirstName(u2.getFirstName());
							}
							if(StringUtils.hasText(u2.getLastName())) {
								u.setLastName(u2.getLastName());
							}
							if(StringUtils.hasText(u2.getEmail())) {
								u.setEmail(u2.getEmail());
							}
							return u;
						},
						existed,
						request.bodyToMono(Users.class)
				)
				.cast(Users.class)
				.flatMap(userService::update)
				.flatMap(user -> ServerResponse.status(HttpStatus.OK)
						.body(Mono.just(ResponseDTO.success(user, "User has been updated")), ResponseDTO.class));
	}

	@Override
	public Mono<ServerResponse> delete(ServerRequest request) {
		return userService.delete(Long.valueOf(request.pathVariable("id")))
				.then(ServerResponse.ok()
						.body(Mono.just(ResponseDTO.success(null, "User deleted")), ResponseDTO.class)
				);
	}

	@Override
	public Mono<ServerResponse> findAll(ServerRequest request) {
		return userService.findAll()
				.collectList()
				.flatMap(res -> ServerResponse.ok()
						.body(Mono.just(ResponseDTO.success(res, "All users retrieved")), ResponseDTO.class)
				);
	}

	@Override
	public Mono<ServerResponse> findByParams(ServerRequest request) {
		// Extract query parameters into a map
		Map<String, String> params = new HashMap<>();
		request.queryParams().forEach((key, value) -> params.put(key, value.get(0)));

		// Call the service method
		return userService.findByParams(params)
				.collectList()
				.flatMap(users -> ServerResponse.ok()
						.body(Mono.just(ResponseDTO.success(users, "Users retrieved by parameters")), ResponseDTO.class)
				);
	}
}
