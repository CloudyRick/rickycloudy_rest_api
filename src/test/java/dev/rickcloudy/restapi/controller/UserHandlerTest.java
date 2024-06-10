package dev.rickcloudy.restapi.controller;

import dev.rickcloudy.restapi.config.TestContainerBeanConfiguration;
import dev.rickcloudy.restapi.dto.ResponseDTO;
import dev.rickcloudy.restapi.enums.UserStatus;
import dev.rickcloudy.restapi.entity.Users;
import dev.rickcloudy.restapi.repository.UserRepository;
import dev.rickcloudy.restapi.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestContainerBeanConfiguration.class)
@ActiveProfiles("test-container")
class UserHandlerTest {

    @LocalServerPort
    private int port;
    @Autowired
    WebTestClient client;
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserService userService;
    @BeforeEach
    void setUp() {
        userRepository.deleteAll().block();
        System.out.println("Database is cleared");
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void givenValidUser_whenSave_thenUserIsCreated() {
        // Given: a valid user
        Users user = Users.builder()
                .id(111L)
                .firstName("Ricky")
                .lastName("Cahyadi")
                .email("rickycahyadi23@gmail.com")
                .username("rckychydii1da")
                .status(UserStatus.ACTIVE)
                .password("Password123!")
                .createdAt(ZonedDateTime.now())
                .build();

        // When: save is called
        EntityExchangeResult<byte[]> result = client.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(user))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.data.email").isEqualTo(user.getEmail())
                .jsonPath("$.data.username").isEqualTo(user.getUsername())
                .returnResult();

        // Log the response body
        System.out.println("Response body: " + new String(result.getResponseBody()));
    }

    @Test
    void givenInvalidUser_whenSave_thenBadRequest() {
        // Given: an invalid user
        Users user = Users.builder()
                .firstName("") // firstName is empty
                .lastName("Cahyadi")
                .email("rickycahyadi23@gmail.com")
                .username("rckychydii1da")
                .password("Password123!")
                .build();

        // When: save is called
        EntityExchangeResult<byte[]> result = client.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(user))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").isEqualTo("First name cannot be blank")
                .returnResult();

        // Log the response body
        System.out.println("Response body: " + new String(result.getResponseBody()));
    }

    @Test
    void saveAll_Given_listOfValidUser_Expect_SuccessResponseDTOsReturned() {
        // Given: a list of valid users
        Users validUser1 = Users.builder()
                .id(444L)
                .firstName("Valid")
                .lastName("User1")
                .email("validuser1@gmail.com")
                .username("validuser1")
                .status(UserStatus.ACTIVE)
                .password("Password123!")
                .createdAt(ZonedDateTime.now())
                .build();

        Users validUser2 = Users.builder()
                .id(555L)
                .firstName("Valid")
                .lastName("User2")
                .email("validuser2@gmail.com")
                .username("validuser2")
                .status(UserStatus.ACTIVE)
                .password("Password123!")
                .createdAt(ZonedDateTime.now())
                .build();

        List<Users> users = Arrays.asList(validUser1, validUser2);

        // When: saveAll is called
        var result = client.post()
                .uri("/users/all")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(users))
                .exchange()
                .expectStatus().isCreated()
                .expectBodyList(ResponseDTO.class)
                .hasSize(2)
                .consumeWith(response -> {
                    List<ResponseDTO> results = response.getResponseBody();
                    // Then: Check that the list contains the expected results
                    assertTrue(results.stream().allMatch(responseDTO ->
                            responseDTO.isSuccess() &&
                                    responseDTO.getMessage().equals("User saved successfully")
                    ));
                }).returnResult();
        System.out.println("Response body: " + result.getResponseBody());
    }
    @Test
    void saveAll_Given_listOfUser_When_someEmailOrUsernameAlreadyExists_Expect_correctResponseDTOsReturned() {
        // Given: a list of users
        Users newUser = Users.builder()
                .id(444L)
                .firstName("New")
                .lastName("User")
                .email("newuser@gmail.com")
                .username("newuser")
                .status(UserStatus.ACTIVE)
                .password("Anjing123!")
                .createdAt(ZonedDateTime.now())
                .build();

        Users existingUser = Users.builder()
                .id(111L)
                .firstName("Ricky")
                .lastName("Cahyadi")
                .email("rickycahyadi23@gmail.com")
                .username("rckychydii1da")
                .status(UserStatus.ACTIVE)
                .password("Anjing123!")
                .createdAt(ZonedDateTime.now())
                .build();

        Users existingUser2 = Users.builder()
                .id(222L)
                .firstName("Syahna")
                .lastName("Indira")
                .email("syahnaindira@gmail.com1")
                .username("syhnaa")
                .status(UserStatus.ACTIVE)
                .password("Anjing123!")
                .createdAt(ZonedDateTime.now())
                .build();
        userService.saveAll(Flux.just(existingUser, existingUser2)).collectList().block();

        List<Users> users = Arrays.asList(newUser, existingUser, existingUser2);

        // When: saveAll is called
        var result = client.post()
                .uri("/users/all")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(users))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBodyList(ResponseDTO.class)
                .hasSize(3)
                .consumeWith(response -> {
                    List<ResponseDTO> results = response.getResponseBody();
                    // Then: Check that the list contains the expected results
                    assertTrue(results.stream().anyMatch(responseDTO ->
                            !responseDTO.isSuccess() &&
                                    responseDTO.getMessage().equals("User with email " + existingUser.getEmail() + " already exists")
                    ));
                    assertTrue(results.stream().anyMatch(responseDTO ->
                            responseDTO.isSuccess() &&
                                    responseDTO.getMessage().equals("User saved successfully")
                    ));
                    assertTrue(results.stream().anyMatch(responseDTO ->
                            !responseDTO.isSuccess() &&
                                    responseDTO.getMessage().equals("User with email " + existingUser2.getEmail() + " already exists")
                    ));
                }).returnResult();
        System.out.println("Response body: " + result.getResponseBody());
    }

    @Test
    void saveAll_Given_listOfUser_When_InvalidInputOrDuplicateEmail_Expect_correctResponseDTOsReturned() {
        // Given: a list of users
        Users invalidUser = Users.builder()
                .id(444L)
                .firstName("") // firstName is empty
                .lastName("User")
                .email("invaliduser@gmail.com")
                .username("invaliduser")
                .status(UserStatus.ACTIVE)
                .password("Anjing123!")
                .createdAt(ZonedDateTime.now())
                .build();

        Users duplicateEmailUser = Users.builder()
                .id(555L)
                .firstName("Duplicate")
                .lastName("User")
                .email("rickycahyadi23@gmail.com") // duplicate email
                .username("duplicateuser")
                .status(UserStatus.ACTIVE)
                .password("Anjing123!")
                .createdAt(ZonedDateTime.now())
                .build();
        userRepository.save(duplicateEmailUser).block();
        List<Users> users = Arrays.asList(invalidUser, duplicateEmailUser);

        // When: saveAll is called
        var result = client.post()
                .uri("/users/all")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(users))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBodyList(ResponseDTO.class)
                .hasSize(2)
                .consumeWith(response -> {
                    List<ResponseDTO> results = response.getResponseBody();
                    // Then: Check that the list contains the expected results
                    assertTrue(results.stream().anyMatch(responseDTO ->
                            !responseDTO.isSuccess() &&
                                    responseDTO.getMessage().equals("First name cannot be blank")
                    ));
                    assertTrue(results.stream().anyMatch(responseDTO ->
                            !responseDTO.isSuccess() &&
                                    responseDTO.getMessage().equals("User with email " + duplicateEmailUser.getEmail() + " already exists")
                    ));
                }).returnResult();
        System.out.println("Response body: " + result.getResponseBody());
    }

    @Test
    void update_Given_ValidUser_Expect_SuccessResponseDTOReturned() {
        // Given: a valid user
        Users validUser = Users.builder()
                .id(444L)
                .firstName("Updated")
                .lastName("User")
                .email("updateduser@gmail.com")
                .username("updateduser")
                .status(UserStatus.ACTIVE)
                .password("Password123!")
                .createdAt(ZonedDateTime.now())
                .build();
        userRepository.save(validUser).block();
        validUser.setFirstName("Asik Asik Jos");
        // When: update is called
        var result = client.put()
                .uri("/users/{id}", validUser.getId())
                .bodyValue(validUser)
                .exchange()
                // Then: Check that the response is as expected
                .expectStatus().isOk()
                .expectBody(ResponseDTO.class)
                .consumeWith(response -> {
                    assertTrue(response.getResponseBody().isSuccess());
                    assertEquals("User has been updated", response.getResponseBody().getMessage());
                }).returnResult();
        System.out.println("Response Body " + result.getResponseBody());
    }

    @Test
    void update_Given_NonExistentUser_Expect_NotFoundStatus() {
        // Given: a non-existent user
        Users nonExistentUser = Users.builder()
                .id(999L)
                .firstName("NonExistent")
                .lastName("User")
                .email("nonexistentuser@gmail.com")
                .username("nonexistentuser")
                .status(UserStatus.ACTIVE)
                .password("P!assword123")
                .createdAt(ZonedDateTime.now())
                .build();

        // When: update is called
        var result = client.put()
                .uri("/users/{id}", nonExistentUser.getId())
                .bodyValue(nonExistentUser)
                .exchange()
                // Then: Check that the response is as expected
                .expectStatus().isNotFound()
                .expectBody(String.class) // Expect the body to be a String
                .returnResult();
        // Log the response body
        System.out.println("Response body: " + new String(result.getResponseBody()));
    }

    @Test
    void update_Given_WrongIdInPathVariable_Expect_ForbiddenStatus() {
        // Given: a valid user
        Users validUser = Users.builder()
                .id(444L)
                .firstName("Updated")
                .lastName("User")
                .email("updateduser@gmail.com")
                .username("updateduser")
                .status(UserStatus.ACTIVE)
                .password("Password123!")
                .createdAt(ZonedDateTime.now())
                .build();
        userRepository.save(validUser).block();
        validUser.setId(555L); // Change the ID to a different value

        // When: update is called with a different ID in the path variable
        var result = client.put()
                .uri("/users/{id}", 444L) // Use the original ID in the path variable
                .bodyValue(validUser) // But the user object has a different ID
                .exchange()
                // Then: Check that the response is as expected
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Action not allowed")
                .returnResult();
        // Log the response body
        System.out.println("Response body: " + new String(result.getResponseBody()));
    }

    @Test
    void update_Given_InvalidUser_Expect_BadRequestStatus() {
        // Given: an invalid user
        Users invalidUser = Users.builder()
                .id(444L)
                .firstName("UserFirstName") // firstName is empty
                .lastName("User")
                .email("invaliduser@gmail.com")
                .username("invaliduser")
                .status(UserStatus.ACTIVE)
                .password("Password123!")
                .createdAt(ZonedDateTime.now())
                .build();
        userRepository.save(invalidUser).block();
        invalidUser.setFirstName(""); // Set the firstName to an invalid value

        // When: update is called
        var result = client.put()
                .uri("/users/{id}", invalidUser.getId())
                .bodyValue(invalidUser)
                .exchange()
                // Then: Check that the response is as expected
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("First name cannot be blank")
                .returnResult();
        // Log the response body
        System.out.println("Response body: " + new String(result.getResponseBody()));
    }

    @Test
    void delete_Given_ValidUserId_Expect_OkStatus() {
        // Given: a valid user
        Users validUser = Users.builder()
                .id(444L)
                .firstName("Valid")
                .lastName("User")
                .email("validuser@gmail.com")
                .username("validuser")
                .status(UserStatus.ACTIVE)
                .password("Password123!")
                .createdAt(ZonedDateTime.now())
                .build();
        userRepository.save(validUser).block();

        // When: delete is called
        var result = client.delete()
                .uri("/users/{id}", validUser.getId())
                .exchange()
                // Then: Check that the response is as expected
                .expectStatus().isOk()

                .expectBody()
                .jsonPath(".message").isEqualTo("User deleted")
                .consumeWith(response -> {
                    // Log the response body
                    System.out.println("Response body: " + response.getResponseBody());
                })
                .returnResult();
    }

    @Test
    void delete_Given_NonExistentUserId_Expect_NotFoundStatus() {
        // Given: a non-existent user ID
        Long nonExistentUserId = 999L;

        // When: delete is called
        var result = client.delete()
                .uri("/users/{id}", nonExistentUserId)
                .exchange()
                // Then: Check that the response is as expected
                .expectStatus().isNotFound()
                .expectBody(String.class)
                .consumeWith(response -> {
                    // Log the response body
                    System.out.println("Response body: " + response.getResponseBody());
                })
                .returnResult();
    }
    @Test
    void findAll_Given_UsersExist_When_RequestIsMade_Then_ReturnAllUsers() {
        // Given: some users in the database
        Users user1 = Users.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .username("johndoe")
                .status(UserStatus.ACTIVE)
                .password("Password123!")
                .createdAt(ZonedDateTime.now())
                .build();

        Users user2 = Users.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .username("janedoe")
                .status(UserStatus.ACTIVE)
                .password("Password123!")
                .createdAt(ZonedDateTime.now())
                .build();

        client.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(user1));
        var users = (Arrays.asList(user1, user2));
        userService.saveAll(Flux.fromIterable(users)).collectList().block();
        // When: a GET request is made to the /users endpoint
        var result = client.get()
                .uri("/users")
                .exchange()
                // Then: the response status is OK and the body contains the expected users
                .expectStatus().isOk()
                .expectBody(ResponseDTO.class)
                .consumeWith(response -> {
                    // Log the response body
                    assertEquals("All users retrieved", response.getResponseBody().getMessage());
                    System.out.println("Response body: " + response.getResponseBody());
                })
                .returnResult();
    }

    @Test
    void findByParams_Given_UsersExist_When_RequestIsMade_Then_ReturnUsersByParams() {
        // Given: some users in the database
        Users user1 = Users.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .username("johndoe")
                .status(UserStatus.ACTIVE)
                .password("Password123!")
                .createdAt(ZonedDateTime.now())
                .build();
        Users user3 = Users.builder()
                .id(122L)
                .firstName("Jojon")
                .lastName("Dodo")
                .email("jojon.dodo@example.com")
                .username("jojondodo")
                .status(UserStatus.ACTIVE)
                .password("Password123!")
                .createdAt(ZonedDateTime.now())
                .build();

        Users user2 = Users.builder()
                .id(2L)
                .firstName("Jane")
                .lastName("Doe")
                .email("jane.doe@example.com")
                .username("janedoe")
                .status(UserStatus.ACTIVE)
                .password("Password123!")
                .createdAt(ZonedDateTime.now())
                .build();

        var users = (Arrays.asList(user1, user2, user3));
        userService.saveAll(Flux.fromIterable(users)).collectList().block();

        // When: a GET request is made to the /users endpoint with query parameters
        var result = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users/")
                        .queryParam("firstName", "J")
                        .queryParam("lastName", "D")
                        .build())
                .exchange()
                // Then: the response status is OK and the body contains the expected users
                .expectStatus().isOk()
                .expectBody(ResponseDTO.class)
                .consumeWith(response -> {
                    int size = 0;
                    Object data = response.getResponseBody().getData();

                    if (data instanceof Collection<?>) {
                        size = ((Collection) data).size();
                    } else if (data instanceof Object[]) {
                        size = ((Object[]) data).length;
                    }
                    // Log the response body
                    assertEquals("Users retrieved by parameters", response.getResponseBody().getMessage());
                    assertEquals(3, size);
                    System.out.println("Response body: " + response.getResponseBody());
                })
                .returnResult();
    }
}