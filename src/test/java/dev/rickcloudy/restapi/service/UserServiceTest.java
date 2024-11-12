package dev.rickcloudy.restapi.service;

import dev.rickcloudy.restapi.config.TestContainerBeanConfiguration;
import dev.rickcloudy.restapi.config.UnitTestingContainerized;
import dev.rickcloudy.restapi.dto.ResponseDTO;
import dev.rickcloudy.restapi.dto.UserDTO;
import dev.rickcloudy.restapi.entity.Users;
import dev.rickcloudy.restapi.enums.UserStatus;
import dev.rickcloudy.restapi.exception.custom.EmailAlreadyExistsException;
import dev.rickcloudy.restapi.exception.HttpException;
import dev.rickcloudy.restapi.mapper.UserMapper;
import dev.rickcloudy.restapi.repository.UserRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test-container")
@Import(TestContainerBeanConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserServiceTest {

    private final Logger log = LogManager.getLogger(UserServiceTest.class);
    @Autowired
    UserService userService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    UserMapper mapper;

// ---------------------------------------------------------------------------------------

    static Users user = Users.builder()
            .id(111L)
            .firstName("Ricky")
            .lastName("Cahyadi")
            .email("rickycahyadi23@gmail.com")
            .username("rckychydii1da")
            .status(UserStatus.ACTIVE)
            .password("anjing123")
            .createdAt(ZonedDateTime.now())
            .build();
    static Users user2 = Users.builder()
            .id(222L)
            .firstName("Syahna")
            .lastName("Indira")
            .email("syahnaindira@gmail.com1")
            .username("syhnaa")
            .status(UserStatus.ACTIVE)
            .password("anjing123")
            .createdAt(ZonedDateTime.now())
            .build();
    static Users user3 = Users.builder()
            .id(333L)
            .firstName("Mimi")
            .lastName("Knnnn")
            .email("mimilovee@gmail.com1")
            .username("mimilovee")
            .status(UserStatus.ACTIVE)
            .password("anjing123")
            .createdAt(ZonedDateTime.now())
            .build();

    @BeforeEach
    void setUp() {
        userRepository.deleteAll().block();
        userService.saveAll(Flux.just(user, user2, user3)).collectList().block();
        long count = userRepository.count().block();
        System.out.println("Number of users in the database before method: " + count);
    }

    @AfterEach
    void tearDown() {
//        userRepository.deleteAll().block();
        long count = userRepository.count().block();
        System.out.println("Number of users in the database after deletion: " + count);
    }




    @Order(1)
    @Test
    void save_Given_newUser_When_noEmailExists_Expect_successReturnedDTO() {
        userRepository.deleteAll().block();
        Users user = Users.builder()
                .id(111L)
                .firstName("Ricky")
                .lastName("Cahyadi")
                .email("rickycahyadi23@gmail.com")
                .username("rckychydii1da")
                .status(UserStatus.ACTIVE)
                .password("anjing123")
                .createdAt(ZonedDateTime.now())
                .build();
        UserDTO userDTO = mapper.userToDto(user);
        StepVerifier.create(userService.save(user))
                .expectNext(userDTO)
                .verifyComplete();
    }

    @Order(2)
    @Test
    void save_Given_existingUserFromList_When_emailAlreadyExists_Expect_httpExceptionReturned() {
        Users user = Users.builder()
                .id(111L)
                .firstName("Ricky")
                .lastName("Cahyadi")
                .email("rickycahyadi23@gmail.com")
                .username("rckychydii1da")
                .status(UserStatus.ACTIVE)
                .password("anjing123")
                .createdAt(ZonedDateTime.now())
                .build();
        StepVerifier.create(userService.save(user))
                .expectErrorMatches(throwable -> throwable instanceof HttpException
                        && ((HttpException) throwable).getHttpStatus() == HttpStatus.BAD_REQUEST
                        && throwable.getMessage().contains("User with email " + user.getEmail() + " already exists"))
                .verify();
    }

    @Order(3)
    @Test
    void save_Given_existingUserFromList_When_usernameAlreadyExists_Expect_httpExceptionReturned() {
        Users user = Users.builder()
                .id(111L)
                .firstName("Ricky")
                .lastName("Cahyadi")
                .email("rickycahyadi2333@gmail.com")
                .username("rckychydii1da")
                .status(UserStatus.ACTIVE)
                .password("anjing123")
                .createdAt(ZonedDateTime.now())
                .build();
        StepVerifier.create(userService.save(user))
                .expectErrorMatches(throwable ->
                        throwable instanceof HttpException
                        && ((HttpException) throwable).getHttpStatus() == HttpStatus.BAD_REQUEST
                        && throwable.getMessage().contains("User with username " + user.getUsername() + " already exists"))
                .verify();
    }


    @Order(4)
    @Test
    void saveAll_Given_listOfUser_When_someEmailAlreadyExistsOnDatabase_Expect_emailOfFailedSavingsReturned() {
        // Existing data
        Users newUser = Users.builder()
                .id(444L)
                .firstName("New")
                .lastName("User")
                .email("newuser@gmail.com")
                .username("newuser")
                .status(UserStatus.ACTIVE)
                .password("Password1")
                .createdAt(ZonedDateTime.now())
                .build();

        Users existingUser = Users.builder()
                .id(111L)
                .firstName("Ricky")
                .lastName("Cahyadi")
                .email("rickycahyadi23@gmail.com")
                .username("rckychydii1da")
                .status(UserStatus.ACTIVE)
                .password("password")
                .createdAt(ZonedDateTime.now())
                .build();

        Users existingUser2 = Users.builder()
                .id(222L)
                .firstName("Syahna")
                .lastName("Indira")
                .email("syahnaindira@gmail.com1")
                .username("syhnaa")
                .status(UserStatus.ACTIVE)
                .password("password")
                .createdAt(ZonedDateTime.now())
                .build();

        List<Users> users = Arrays.asList(newUser, existingUser, existingUser2);
        System.out.println("Hereeeeeeeeeeee");
        System.out.println(users);
        // Call the method under test and collect the results into a list
        List<ResponseDTO<UserDTO>> results = userService.saveAll(Flux.fromIterable(users)).log().collectList().block();

        // Check that the list contains the expected results
        assertTrue(results.stream().anyMatch(response ->
                !response.isSuccess() &&
                        response.getMessage().equals("User with email " + existingUser.getEmail() + " already exists") &&
                        response.getData().getEmail().equals(existingUser.getEmail())
        ));
        assertTrue(results.stream().anyMatch(response ->
                response.isSuccess() &&
                        response.getMessage().equals("User saved successfully") &&
                        response.getData().getEmail().equals(newUser.getEmail())
        ));
        assertTrue(results.stream().anyMatch(response ->
                !response.isSuccess() &&
                        response.getMessage().equals("User with email " + existingUser2.getEmail() + " already exists") &&
                        response.getData().getEmail().equals(existingUser2.getEmail())
        ));
    }


    @Order(5)
    @Test
    void findById_Given_savedUserFromSetUp_When_findById_Expect_dtoReturned() {
        UserDTO dto = mapper.userToDto(user);
        StepVerifier.create(userService.findById(user.getId()))
                .consumeNextWith(res -> {
                    Assertions.assertEquals(dto, res);
                    Assertions.assertInstanceOf(UserDTO.class, res);
                    Assertions.assertEquals(111L, res.getId());
                })
                .verifyComplete();
    }

    @Order(6)
    @Test
    void findById_Given_savedUserFromSetUp_When_idNotExists_Expect_httpException() {
        Long randomInt = 12121212L;
        StepVerifier.create(userService.findById(randomInt))
                .expectErrorMatches(throwable ->
                                throwable instanceof HttpException
                                && ((HttpException) throwable).getHttpStatus() == HttpStatus.BAD_REQUEST
                                && throwable.getMessage().contains("User with ID " + randomInt + " does not exists"))
                .verify();
    }


    @Order(7)
    @Test
    void findByEmail_Given_savedUserFromSetUp_When_findByEmail_Expect_dtoReturned() {
        UserDTO dto = mapper.userToDto(user);
        StepVerifier.create(userService.findByEmail(user.getEmail()))
                .consumeNextWith(res -> {
                    Assertions.assertEquals(dto, res);
                    Assertions.assertInstanceOf(UserDTO.class, res);
                    Assertions.assertEquals("rickycahyadi23@gmail.com", res.getEmail());
                })
                .verifyComplete();
    }


    @Order(8)
    @Test
    void findByEmail_Given_savedUserFromSetUp_When_emailNotExists_Expect_httpException() {
        Long randomInt = 12121212L;
        StepVerifier.create(userService.findById(randomInt))
                .expectErrorMatches(throwable ->
                        throwable instanceof HttpException
                                && ((HttpException) throwable).getHttpStatus() == HttpStatus.BAD_REQUEST
                                && throwable.getMessage().contains("User with ID " + randomInt + " does not exists"))
                .verify();
    }


    @Order(9)
    @Test
    void findByParams_Given_Params_When_findByParams_Expect_ReturnCorrectUsers() {

        // Given
        Map<String, String> params = new HashMap<>();
        params.put("firstName", "Ric%");
        params.put("lastName", "Cah%");

        // When
        Flux<UserDTO> users = userService.findByParams(params).log();

        // Expect
        StepVerifier.create(users)
                .assertNext(user -> {
                    Assertions.assertTrue(user.getFirstName().startsWith("Ric"));
                    Assertions.assertTrue(user.getLastName().startsWith("Cah"));
                })
                .verifyComplete();
    }

    @Order(10)
    @Test
    void findAll_Given_When_Expect() {
        var result = userService.findAll().log().collectList().block();
        assertTrue(result.stream().anyMatch(mapper.userToDto(user)::equals));
        assertTrue(result.stream().anyMatch(mapper.userToDto(user2)::equals));
        assertTrue(result.stream().anyMatch(mapper.userToDto(user3)::equals));
    }


    @Order(11)
    @Test
    void update_GivenUpdatingUserNameWithNewValue_WhenNoInteruption_Expect_UpdateSuccess() {
        String newFirstName = "Asik Asik";
        String newLastName = "Josss";
        user.setFirstName(newFirstName);
        user.setLastName(newLastName);

        StepVerifier.create(userService.update(user).log())
                .expectNext(mapper.userToDto(user))
                .verifyComplete();
    }

    @Order(12)
    @Test
    void update_GivenUpdatingUserNameWithNewValue_When_UpdateUserInputEmailExists_Expect_ExceptionReturned() {
        String newFirstName = "Asik Asik";
        String newLastName = "Josss";
        user.setFirstName(newFirstName);
        user.setLastName(newLastName);
        user.setEmail(user2.getEmail());

        StepVerifier.create(userService.update(user).log())
                .expectError(EmailAlreadyExistsException.class)
                .verify();
    }


}