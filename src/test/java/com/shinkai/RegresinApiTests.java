package com.shinkai;

import com.shinkai.generators.UserDataGenerator;
import com.shinkai.models.CreateUserResponse;
import com.shinkai.models.ErrorResponse;
import com.shinkai.models.UserData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static com.shinkai.Specs.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegresinApiTests {
    private final UserDataGenerator generator = new UserDataGenerator();

    @Test
    @DisplayName("Get data of known user")
    public void getSingleUser() {
        int userId = 2;

        UserData userData = given()
                .spec(request)
                .when()
                .get("/users/" + userId)
                .then()
                .spec(responseOk)
                .log().body()
                .extract().as(UserData.class);

        assertThat(userData.getUser().getId(), equalTo(userId));
        assertThat(userData.getUser().getEmail(), endsWith("reqres.in"));
        assertThat(userData.getUser().getAvatar(), startsWith("https://reqres.in"));
    }

    @Test
    @DisplayName("Successful user deletion")
    public void deleteUser() {
        int userId = 5;

        given()
                .spec(request)
                .when()
                .delete("/users/" + userId)
                .then()
                .spec(responseNoContent)
                .log().body();
    }

    @Test
    @DisplayName("Get list of users")
    public void getListOfUsers() {
        int perPage = 5;

        given()
                .spec(request)
                .when()
                .get("/users/?per_page=" + perPage)
                .then()
                .spec(responseOk)
                .log().body()
                .assertThat()
                .body("data.findAll{it.email =~/.*?@reqres.in/}.email.flatten()",
                        hasItem("emma.wong@reqres.in"))
                .and()
                .body("data.last_name[2]", equalTo("Wong"))
                .and()
                .body("data.findAll{it.last_name =~/^\\w{1,10}$/}.last_name.flatten()",
                        hasSize(perPage));
    }

    @Test
    @DisplayName("Successful user creation")
    public void createUser() {
        String name = generator.getFullName();
        String job = generator.getJob();
        String email = generator.getEmail();
        String avatar = generator.getAvatarLink();
        Map<String, String> data = new HashMap<>();
        data.put("name", name);
        data.put("job", job);
        data.put("email", email);
        data.put("avatar", avatar);

        CreateUserResponse newUser =
                given()
                        .spec(request)
                        .body(data)
                        .when()
                        .post("/users/")
                        .then()
                        .spec(responseCreated)
                        .log().body()
                        .extract().as(CreateUserResponse.class);

        assertAll("Checking actual and expected name, email, job and avatar",
                () -> assertEquals(name, newUser.getName()),
                () -> assertEquals(job, newUser.getJob()),
                () -> assertEquals(email, newUser.getEmail()),
                () -> assertThat(newUser.getAvatar(), startsWith("https://s3.amazonaws.com")),
                () -> assertThat(Integer.parseInt(newUser.getId()), greaterThanOrEqualTo(1))
        );
    }

    @ValueSource(strings = {"email", "password"})
    @ParameterizedTest(name = "Unsuccessful user registration: missing parameter {0}")
    public void registerWithoutOneParam(String parameter) {
        String email = "lindsay.ferguson@reqres.in";
        String password = generator.getPassword();
        Map<String, String> data = new HashMap<>();
        data.put("email", email);
        data.put("password", password);

        data.remove(parameter);

        ErrorResponse registerError =
                given()
                        .spec(request)
                        .body(data)
                        .when()
                        .post("/register/")
                        .then()
                        .spec(responseBadRequest)
                        .log().body()
                        .extract().as(ErrorResponse.class);

        assertThat(registerError.getError(), containsString("Missing " + parameter));
    }
}
