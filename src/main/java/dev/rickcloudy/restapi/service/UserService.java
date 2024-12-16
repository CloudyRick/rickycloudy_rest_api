package dev.rickcloudy.restapi.service;

import dev.rickcloudy.restapi.dto.ResponseDTO;
import dev.rickcloudy.restapi.enums.UserStatus;
import dev.rickcloudy.restapi.exception.HttpException;
import dev.rickcloudy.restapi.exception.custom.EmailAlreadyExistsException;
import dev.rickcloudy.restapi.exception.custom.UsernameAlreadyExistsException;
import dev.rickcloudy.restapi.mapper.UserMapper;
import dev.rickcloudy.restapi.repository.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import dev.rickcloudy.restapi.dto.UserDTO;
import dev.rickcloudy.restapi.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
	private final Logger log = LogManager.getLogger(UserService.class);
	private final UserRepository userRepository;
	private final UserMapper mapper;
	private final Validator validator;
	private final PasswordEncoder passwordEncoder;

	public Mono<UserDTO> save(Users user) {
	// Set up the Errors object
		Errors errors = new BeanPropertyBindingResult(user, "user");

		// Validate the entity
		validator.validate(user, errors);

		// Check if there are any validation errors
		if (errors.hasErrors()) {
			StringBuilder errorMessage = new StringBuilder("Validation failed: ");
			errors.getAllErrors().forEach(error -> errorMessage.append(error.getDefaultMessage()).append(" "));
			return Mono.error(new HttpException(HttpStatus.BAD_REQUEST, errorMessage.toString()));
		}

		user.setPassword(passwordEncoder.encode(user.getPassword()));

		return userRepository.existsByEmail(user.getEmail())
				.flatMap(exists -> {
					if(exists) {
						return Mono.error(new EmailAlreadyExistsException(HttpStatus.BAD_REQUEST, "User with email " + user.getEmail() + " already exists"));
					}
					return userRepository.existsByUsername(user.getUsername());
				})
				.flatMap(exists -> {
					if (exists) {
						return Mono.error(new UsernameAlreadyExistsException(HttpStatus.BAD_REQUEST, "User with username " + user.getUsername() + " already exists"));
					}
					return userRepository.save(user);
				})
				.flatMap(r -> Mono.just(mapper.userToDto(r)));
	}

	public Flux<ResponseDTO<UserDTO>> saveAll(Flux<Users> users) {
		return users.flatMap(user -> {
			Errors errors = new BeanPropertyBindingResult(user, "user");
			validator.validate(user, errors);
//			Validate the input from the entity class
//			Like Password needs to be 8 char bla bla bla, Maximum x char bla bla bla
			if (errors.hasErrors()) {
				String errorMessage = errors.getAllErrors()
						.stream()
						.map(DefaultMessageSourceResolvable::getDefaultMessage)
						.collect(Collectors.joining(", "));
				return Mono.just(ResponseDTO.fail(mapper.userToDto(user), errorMessage));
			}
			return this.save(user)
					.doOnNext(res -> log.info("save on next"))
					.flatMap(res -> {
						log.info("User saved successfully {}", res);
						return Mono.just(ResponseDTO.success(mapper.userToDto(user), "User saved successfully"));
					})
					.doOnError(err -> {
						log.error("Failed to save user {} due to error: {}", user, err.getMessage());
					})
					.onErrorResume(EmailAlreadyExistsException.class, err -> {
						log.warn("Email already exists for user {}: {}", user, err.getMessage());
						return Mono.just(ResponseDTO.fail(mapper.userToDto(user), err.getMessage()));
					})
					.onErrorResume(UsernameAlreadyExistsException.class, err -> {
						log.warn("Username already exists for user {}: {}", user, err.getMessage());
						return Mono.just(ResponseDTO.fail(mapper.userToDto(user), err.getMessage()));
					})
					// Fallback for other errors
					.onErrorResume(err -> {
						log.error("Unexpected error occurred for user {}: {}", user, err.getMessage());
						return Mono.just(ResponseDTO.fail(mapper.userToDto(user), "An unexpected error occurred"));
					});
		});
	}

	public Mono<UserDTO> findById(Long id) {
		return userRepository.findById(id)
				.flatMap(res -> Mono.just(mapper.userToDto(res)));
	}

	public Mono<UserDTO> findByEmail(String email) {
		return userRepository.findByEmail(email)
				.flatMap(r -> Mono.just(mapper.userToDto(r)));
	}

	public Flux<UserDTO> findAll() {
		return userRepository.findAll()
				.flatMap(r -> Flux.just(mapper.userToDto(r)));
	}

	public Mono<UserDTO> update(Users user) {
		return userRepository.findById(user.getId())
				.flatMap(r -> userRepository.existsByEmail(user.getEmail())
                        .flatMap(exists -> {
                            if(exists && !r.getEmail().equals(user.getEmail())) {
                                return Mono.error(new EmailAlreadyExistsException(HttpStatus.BAD_REQUEST, "User with email " + user.getEmail() + " already exists"));
                            }
                            return userRepository.existsByUsername(user.getUsername());
                        })
                        .flatMap(exists -> {
                            if(exists && !r.getUsername().equals(user.getUsername())) {
                                return Mono.error(new UsernameAlreadyExistsException(HttpStatus.BAD_REQUEST, "User with username " + user.getUsername() + " already exists"));
                            }
                        return Mono.just(r);
                    }))
				.flatMap(r -> userRepository.update(user))
				.flatMap(r -> Mono.just(mapper.userToDto(r)));
	}

	public Mono<Void> delete(Long id) {
		return userRepository.findById(id)
				.flatMap(r -> {
					r.setStatus(UserStatus.DELETED);
					return userRepository.update(r);
				}).then();
	}
	public Flux<UserDTO> findByParams(Map<String, String> params) {
		return userRepository.findByParams(params)
				.flatMap(r -> Flux.just(mapper.userToDto(r)));
	}
}
