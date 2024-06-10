package dev.rickcloudy.restapi.repository;


import dev.rickcloudy.restapi.config.UnitTestingContainerized;
import dev.rickcloudy.restapi.entity.Users;
import dev.rickcloudy.restapi.exception.UserNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import dev.rickcloudy.restapi.enums.UserStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;


@UnitTestingContainerized
public class UserRepositoryTest {

	private static final Logger log = LogManager.getLogger(UserRepositoryTest.class);
	@Autowired
	UserRepository userRepository;
	@BeforeEach
	void setUp() {
		userRepository.deleteAll().block();
	}
	@Test
	public void save_given_NewUser_when_SaveUser_then_ReturnUser() {
		Users user = Users.builder()
						.id(102931111100L)
						.firstName("Rickya")
						.lastName("Cahyadi")
						.email("rickycahyadi23@gmail.com1s")
						.username("rckychydii1da")
						.status(UserStatus.ACTIVE) 
						.password("anjing123")
						.createdAt(ZonedDateTime.now())
						.build();
		Mono<Users> create = userRepository.save(user)
											.flatMap(r -> userRepository.findById(r.getId()));
		
		StepVerifier.create(create)
					.expectNext(user)
					.verifyComplete();
	}

	@Test
	void findByEmail_given_twoUser_when_findByEmail_then_returnUserWithThatEmail() {
//		Given
		Users user = Users.builder()
				.id(1L)
				.firstName("Ricky")
				.lastName("Cahyadi")
				.email("rickycahyadi23@gmail.com")
				.username("rckychydii1da")
				.status(UserStatus.ACTIVE)
				.password("anjing123")
				.createdAt(ZonedDateTime.now())
				.build();
		userRepository.save(user)
				.flatMap(r -> userRepository.findById(r.getId()))
				.block();
//		When
		Mono<Users> findByEmail = userRepository.findByEmail(user.getEmail())
				.log();
//		Expect
		StepVerifier.create(findByEmail)
				.consumeNextWith(res -> {
					log.debug("User {}", user.getCreatedAt());
					log.debug("Res {}", res.getCreatedAt());
					Assertions.assertEquals(user.getId(), res.getId());
					Assertions.assertEquals(user.getFirstName(), res.getFirstName());
					Assertions.assertEquals(user.getLastName(), res.getLastName());
					Assertions.assertEquals(user.getUsername(), res.getUsername());
					Assertions.assertEquals(user.getPassword(), res.getPassword());
					Assertions.assertEquals(user.getStatus(), res.getStatus());
				})
				.verifyComplete();
	}
	@Test
	void findByEmail_given_twoUser_when_emailNotExists_then_returnError() {
//		Given
		Users user = Users.builder()
				.id(1L)
				.firstName("Ricky")
				.lastName("Cahyadi")
				.email("rickycahyadi23@gmail.com")
				.username("rckychydii1da")
				.status(UserStatus.ACTIVE)
				.password("anjing123")
				.createdAt(ZonedDateTime.now())
				.build();
		userRepository.save(user)
				.flatMap(r -> userRepository.findById(r.getId()))
				.block();
//		When
		Mono<Users> findByEmail = userRepository.findByEmail("randomemail@gmai.com")
				.log();
//		Expect
		StepVerifier.create(findByEmail)
				.expectError(UserNotFoundException.class)
				.verify();
	}

	@Test
	void update_Given_oneUserAndTheUpdatedValue_When_update_Expect_SuccessAndUpdatedValueInserted() {
		Users user = Users.builder()
				.id(102931111100L)
				.firstName("Rickya")
				.lastName("Cahyadi")
				.email("rickycahyadi23@gmail.com1s")
				.username("rckychydii1da")
				.status(UserStatus.ACTIVE)
				.password("anjing123")
				.createdAt(ZonedDateTime.now())
				.build();
		userRepository.save(user)
				.flatMap(res -> userRepository.findById(res.getId()))
				.block();
		user.setFirstName("Syahna");
		Mono<Users> updateTest = userRepository.update(user).flatMap(res -> userRepository.findById(res.getId())).log();

		StepVerifier.create(updateTest)
				.assertNext(updatedUser -> {
					Assertions.assertEquals("Syahna", updatedUser.getFirstName());
				})
				.verifyComplete();
	}
	@Test
	void update_Given_oneUser_When_update_Expect_httpExceptionUserNotFound() {
		Users user = Users.builder()
				.id(102931111100L)
				.firstName("Rickya")
				.lastName("Cahyadi")
				.email("rickycahyadi23@gmail.com1s")
				.username("rckychydii1da")
				.status(UserStatus.ACTIVE)
				.password("anjing123")
				.createdAt(ZonedDateTime.now())
				.build();
		userRepository.save(user)
				.flatMap(res -> userRepository.findById(res.getId()))
				.block();
		user.setFirstName("Syahna");
		Mono<Users> updateTest = userRepository.update(user).flatMap(res -> userRepository.findById(res.getId())).log();

		StepVerifier.create(updateTest)
				.assertNext(updatedUser -> {
					Assertions.assertEquals("Syahna", updatedUser.getFirstName());
				})
				.verifyComplete();
	}
	/*
//	Controller Layer
	@Test
	void update_Given_oneUserAndTheUpdatedValueWithDifferentUserId_When_update_Expect_httpExceptionForbidden() {
		User user = User.builder()
				.id(1L)
				.firstName("Rickya")
				.lastName("Cahyadi")
				.email("rickycahyadi23@gmail.com1s")
				.username("rckychydii1da")
				.status(UserStatus.ACTIVE)
				.password("anjing123")
				.createdAt(ZonedDateTime.now())
				.build();
		User updatedValue = User.builder()
				.id(2L)
				.firstName("Syahna")
				.lastName("Cahyadi")
				.email("rickycahyadi23@gmail.com1s")
				.username("rckychydii1da")
				.status(UserStatus.ACTIVE)
				.password("anjing123")
				.build();
		userRepository.save(user)
				.flatMap(res -> userRepository.findById(res.getId()))
				.block();
		Mono<User> updateTest = userRepository.update(updatedValue).flatMap(res -> userRepository.findById(res.getId())).log();

		StepVerifier.create(updateTest)
				.assertNext(updatedUser -> {
					Assertions.assertEquals("Syahna", updatedUser.getFirstName());
				})
				.verifyComplete();
	}*/

	@Test
	void delete_Given_oneUserExistingActiveUser_When_deletingThatUser_Expect_statusToDeletedAndRecordStillExists() {
//		Given
		Users user = Users.builder()
				.id(12222L)
				.firstName("Rickya")
				.lastName("Cahyadi")
				.email("rickycahyadi23@gmail.com1s")
				.username("rckychydii1da")
				.status(UserStatus.ACTIVE)
				.password("anjing123")
				.createdAt(ZonedDateTime.now())
				.build();
		Users user2 = Users.builder()
				.id(13333L)
				.firstName("Syahna")
				.lastName("Indira")
				.email("syahna@gmail.com")
				.username("syahnaaa")
				.status(UserStatus.ACTIVE)
				.password("anjing123")
				.createdAt(ZonedDateTime.now(ZoneId.of("UTC")))
				.updatedAt(ZonedDateTime.now(ZoneId.of("UTC")))
				.build();
		userRepository.save(user)
				.block();
		userRepository.save(user2)
				.block();
//		When
		userRepository.delete(user.getId())
				.as(StepVerifier::create)
				.expectNext(1L) // Assuming one row is affected by the delete operation
				.verifyComplete();

		Mono<Users> user2Exists = userRepository.findById(user2.getId());
		StepVerifier.create(userRepository.findById(user.getId()))
						.expectError() // Expect an error after deletion
								.verify();
		StepVerifier.create(user2Exists)
				.expectNext(user2) // Verify that the other user still exists
				.verifyComplete();

	}

	@Test
	void changePassword_Given_OneUser_When_changeUser_Expected_successAndNewPasswordInserted() {
		//		Given
		Users user = Users.builder()
				.id(12222L)
				.firstName("Rickya")
				.lastName("Cahyadi")
				.email("rickycahyadi23@gmail.com1s")
				.username("rckychydii1da")
				.status(UserStatus.ACTIVE)
				.password("anjing123")
				.createdAt(ZonedDateTime.now())
				.build();
		userRepository.save(user)
				.block();
		String newPass = "Assoooyy Password Baruu";
		Mono<Long> changePasswordPublisher = userRepository.changePassword(user.getId(), newPass);
		Mono<Users> userWithNewPassword = userRepository.findById(user.getId());

		StepVerifier.create(changePasswordPublisher)
				.expectNext(1L) // Number of rows affected
				.verifyComplete();
		StepVerifier.create(userWithNewPassword)
				.consumeNextWith(res -> {
					Assertions.assertEquals(newPass, res.getPassword());
				})
				.verifyComplete();
	}

	@Test
	void existsByEmail_Given_oneUser_When_emailExists_Expect_returnTrue() {
		Users user = Users.builder()
				.id(102931111100L)
				.firstName("Rickya")
				.lastName("Cahyadi")
				.email("rickycahyadi23@gmail.com1s")
				.username("rckychydii1da")
				.status(UserStatus.ACTIVE)
				.password("anjing123")
				.createdAt(ZonedDateTime.now())
				.build();
		userRepository.save(user)
				.flatMap(res -> userRepository.findById(res.getId()))
				.block();
		Mono<Boolean> existsByEmail = userRepository.existsByEmail(user.getEmail());
		StepVerifier.create(existsByEmail)
				.assertNext(Assertions::assertTrue).verifyComplete();
	}

	@Test
	void existsByUsername_Given_oneUser_When_usernameExists_returnTrue() {
		Users user = Users.builder()
				.id(102931111100L)
				.firstName("Rickya")
				.lastName("Cahyadi")
				.email("rickycahyadi23@gmail.com1s")
				.username("rckychydii1da")
				.status(UserStatus.ACTIVE)
				.password("anjing123")
				.createdAt(ZonedDateTime.now())
				.build();
		userRepository.save(user)
				.flatMap(res -> userRepository.findById(res.getId()))
				.block();
		Mono<Boolean> existsByUsername = userRepository.existsByUsername(user.getUsername());
		StepVerifier.create(existsByUsername)
				.assertNext(Assertions::assertTrue).verifyComplete();
	}

	@Test
	void deleteAll() {
		Users user = Users.builder()
				.id(102931111100L)
				.firstName("Rickya")
				.lastName("Cahyadi")
				.email("rickycahyadi23@gmail.com1s")
				.username("rckychydii1da")
				.status(UserStatus.ACTIVE)
				.password("anjing123")
				.createdAt(ZonedDateTime.now())
				.build();
		userRepository.save(user)
				.flatMap(res -> userRepository.findById(res.getId()))
				.block();
		userRepository.deleteAll().as(StepVerifier::create).verifyComplete();
	}

	@Test
	void findById_Given_twoUser_When_findById_Expect_returnTheExactRecords() {
//		Given
		Users user = Users.builder()
				.id(1L)
				.firstName("Ricky")
				.lastName("Cahyadi")
				.email("rickycahyadi23@gmail.com")
				.username("rckychydii1da")
				.status(UserStatus.ACTIVE)
				.password("anjing123")
				.createdAt(ZonedDateTime.now())
				.build();
		Users user2 = Users.builder()
				.id(2L)
				.firstName("Syahna")
				.lastName("Indira")
				.email("syahnaindira@gmail.com1")
				.username("syhnaa")
				.status(UserStatus.ACTIVE)
				.password("anjing123")
				.createdAt(ZonedDateTime.now())
				.build();
		userRepository.save(user)
				.flatMap(r -> userRepository.findById(r.getId()))
				.doOnSuccess(r -> log.info("Create {}", r))
				.block();
		userRepository.save(user2)
				.flatMap(r -> userRepository.findById(r.getId()))
				.doOnSuccess(r -> log.info("Create {}", r))
				.block();
//		When
		Mono<Users> findById1 = userRepository.findByEmail(user.getEmail())
				.log();
		Mono<Users> findById2 = userRepository.findByEmail(user2.getEmail())
				.log();
//		Expect
		StepVerifier.create(findById1)
				.expectNext(user)
				.verifyComplete();
		StepVerifier.create(findById2)
				.expectNext(user2)
				.verifyComplete();
	}
	@Test
	void findById_Given_usersInDB_When_findByIdWithNotExistingId_Expect_errorThrow() {
//		Given
		Users user = Users.builder()
				.id(1L)
				.firstName("Ricky")
				.lastName("Cahyadi")
				.email("rickycahyadi23@gmail.com")
				.username("rckychydii1da")
				.status(UserStatus.ACTIVE)
				.password("anjing123")
				.createdAt(ZonedDateTime.now())
				.build();
		userRepository.save(user)
				.block();
//		When
		Mono<Users> findById1 = userRepository.findById(11212L) // Random ID That's not exists
				.log();
//		Expect
		StepVerifier.create(findById1)
				.expectError()
				.verify();
	}

	@Test
	public void findByParams_Given_UsersAndParams_When_findByParams_Expect_ReturnCorrectUsers() {
		// Given
		Users user1 = Users.builder()
				.id(1L)
				.firstName("John")
				.lastName("Doe")
				.email("john.doe@gmail.com")
				.username("johndoe")
				.status(UserStatus.ACTIVE)
				.password("password123")
				.createdAt(ZonedDateTime.now())
				.build();

		Users user2 = Users.builder()
				.id(2L)
				.firstName("Jone")
				.lastName("Doe")
				.email("jane.doe@gmail.com")
				.username("janedoe")
				.status(UserStatus.ACTIVE)
				.password("password123")
				.createdAt(ZonedDateTime.now())
				.build();

		userRepository.save(user1).block();
		userRepository.save(user2).block();

		Map<String, String> params = new HashMap<>();
		params.put("firstName", "Jo");
		params.put("lastName", "Do");

		// When
		Flux<Users> users = userRepository.findByParams(params).log();

		// Expect
		StepVerifier.create(users)
				.assertNext(user -> {
					Assertions.assertTrue(user.getFirstName().startsWith("Jo"));
					Assertions.assertTrue(user.getLastName().startsWith("Do"));
					System.out.println(user);
				}).assertNext(user -> {
					Assertions.assertTrue(user.getFirstName().startsWith("Jo"));
					Assertions.assertTrue(user.getLastName().startsWith("Do"));
					System.out.println(user);
				})
				.verifyComplete();
	}

}
