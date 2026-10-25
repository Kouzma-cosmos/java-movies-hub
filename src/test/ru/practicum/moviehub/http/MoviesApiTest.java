package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {

    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;
    private final Gson gson = new Gson();

    @BeforeAll
    static void beforeAll() {
        store = new MoviesStore();
        server = new MoviesServer(store, 8080);
        server.start();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @BeforeEach
    void setUp() {
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        store.clear();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies"))
                .GET().build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue, "Content-Type должен " +
                "содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"), "Ожидается JSON-массив");
    }

    @Test
    void getMovies_whenNotEmpty_returnsMoviesArray() throws Exception {
        Movie movie = new Movie(1, "Inception", 2010);
        store.add(movie);

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).GET().build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, resp.statusCode());

        Gson gson = new Gson();
        List<Movie> movies = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());

        assertEquals(1, movies.size(), "В списке должен быть ровно 1 фильм");
        assertEquals("Inception", movies.get(0).getTitle(), "Название фильма должно совпадать");
        assertEquals(2010, movies.get(0).getYear(), "Год фильма должен совпадать");
    }

    @Test
    void postMovie_whenValidData_returns201AndMovieWithId() throws Exception {
        String movieJson = "{\"title\":\"Interstellar\",\"year\":2014}";

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).header("Content-Type",
                "application/json").POST(HttpRequest.BodyPublishers.ofString(movieJson)).build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, resp.statusCode());

        String body = resp.body();
        assertTrue(body.contains("\"id\":"), "Ответ должен содержать сгенерированный ID");
        assertTrue(body.contains("Interstellar"), "Ответ должен содержать название фильма");
    }

    @Test
    void postMovie_whenEmptyTitle_returns422AndErrorResponse() throws Exception {
        String movieJson = "{\"title\":\" \",\"year\":2014}";

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).header("Content-Type",
                "application/json").POST(HttpRequest.BodyPublishers.ofString(movieJson)).build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, resp.statusCode());

        String body = resp.body();
        assertTrue(body.contains("\"error\":"), "Ответ должен содержать короткое описание ошибки");
        assertTrue(body.contains("details"), "Ответ должен содержать массив строк с деталями проблемы");
    }

    @Test
    void postMovie_whenTitleIsMoreHundredSymbols_returns422AndErrorResponse() throws Exception {
        String movieJson = "{\"title\":\"Гори, гори, моя звезда: Жизнь, творчество и трагическая судьба автора " +
                "знаменитого романса Петра Булахова\",\"year\":2024}";

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).header("Content-Type",
                "application/json").POST(HttpRequest.BodyPublishers.ofString(movieJson)).build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, resp.statusCode());

        String body = resp.body();
        assertTrue(body.contains("\"error\":"), "Ответ должен содержать короткое описание ошибки");
        assertTrue(body.contains("details"), "Ответ должен содержать массив строк с деталями проблемы");
    }

    @Test
    void postMovie_whenInvalidYear_returns422AndErrorResponse() throws Exception {
        String movieJson = "{\"title\":\"Прибытие поезда\",\"year\":1874}";

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).header("Content-Type",
                "application/json").POST(HttpRequest.BodyPublishers.ofString(movieJson)).build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, resp.statusCode());

        String body = resp.body();
        assertTrue(body.contains("\"error\":"), "Ответ должен содержать короткое описание ошибки");
        assertTrue(body.contains("details"), "Ответ должен содержать массив строк с деталями проблемы");
    }

    @Test
    void postMovie_whenInvalidContentType_returns415AndErrorResponse() throws Exception {

        String movieJson = "{\"title\":\"Брат\",\"year\":1997}";

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).header("Content-Type",
                        "text/html")
                .POST(HttpRequest.BodyPublishers.ofString(movieJson)).build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(415, resp.statusCode());

        String body = resp.body();
        assertTrue(body.contains("\"error\":"), "Ответ должен содержать короткое описание ошибки");
        assertTrue(body.contains("details"), "Ответ должен содержать массив строк с деталями проблемы");
    }

    @Test
    void postMovie_whenInvalidJson_returns415AndErrorResponse() throws Exception {
        String brokenJson = "{\"title\":\"Interstellar\",\"year\":2014";

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).header("Content-Type",
                "text/html").POST(HttpRequest.BodyPublishers.ofString(brokenJson)).build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(415, resp.statusCode());

        String body = resp.body();
        assertTrue(body.contains("\"error\":"), "Ответ должен содержать короткое описание ошибки");
        assertTrue(body.contains("details"), "Ответ должен содержать массив строк с деталями проблемы");
    }

    @Test
    void postMovie_whenEmptyBody_returns422AndErrorResponse() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(""))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(422, resp.statusCode(), "Пустое тело должно возвращать 422");
        assertTrue(resp.body().contains("\"error\":"));
    }

    @Test
    void getMovieById_whenCorrectDate_returnsMovie() throws Exception {
        Movie movie = new Movie(1, "Inception", 2010);
        store.add(movie);

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/1"))
                .GET().build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies/id должен вернуть 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue, "Content-Type должен " +
                "содержать формат данных и кодировку");

        String body = resp.body();
        assertTrue(body.contains("\"id\":1"), "Ответ должен содержать правильный id фильма");
        assertTrue(body.contains("\"title\":\"Inception\""), "Ответ должен содержать правильное название " +
                "фильма");
        assertTrue(body.contains("\"year\":2010"), "Ответ должен содержать правильный год фильма");
    }

    @Test
    void getMovieById_whenIdIsNotNumber_returns400AndErrorResponse() throws Exception {

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/second")).GET().build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "GET /movies/second должен вернуть 400");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue, "Content-Type должен " +
                "содержать формат данных и кодировку");

        String body = resp.body();
        assertTrue(body.contains("\"error\":"), "Ответ должен содержать короткое описание ошибки");
        assertTrue(body.contains("details"), "Ответ должен содержать массив строк с деталями проблемы");
    }

    @Test
    void getMovieById_whenMovieByIdIsNotFinding_returns404AndErrorResponse() throws Exception {
        Movie interstellar = new Movie(1, "Интерстеллар", 2014);
        Movie martian = new Movie(2, "Марсианин", 2015);
        Movie gravity = new Movie(3, "Гравитация", 2013);
        store.add(interstellar);
        store.add(martian);
        store.add(gravity);

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/4")).GET().build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "GET /movies/4 должен вернуть 404");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue, "Content-Type должен " +
                "содержать формат данных и кодировку");

        String body = resp.body();
        assertTrue(body.contains("\"error\":"), "Ответ должен содержать короткое описание ошибки");
        assertTrue(body.contains("details"), "Ответ должен содержать массив строк с деталями проблемы");
    }

    @Test
    void deleteMovieById_whenCorrectData_deleteMovieAndReturns204() throws Exception {
        Movie interstellar = new Movie(1, "Интерстеллар", 2014);
        Movie martian = new Movie(2, "Марсианин", 2015);
        Movie gravity = new Movie(3, "Гравитация", 2013);
        store.add(interstellar);
        store.add(martian);
        store.add(gravity);

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/2")).DELETE().build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode(), "DELETE /movies/2 должен вернуть 204");

        boolean hasMartian = store.getAll().stream().anyMatch(m -> m.getId() == 2);
        assertFalse(hasMartian, "В коллекции больше не должно быть удаленного фильма");
    }

    @Test
    void deleteMovieById_whenIdIsNotNumber_returns400AndErrorResponse() throws Exception {

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/first")).DELETE().build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "DELETE /movies/first должен вернуть 400");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue, "Content-Type должен " +
                "содержать формат данных и кодировку");

        String body = resp.body();
        assertTrue(body.contains("\"error\":"), "Ответ должен содержать короткое описание ошибки");
        assertTrue(body.contains("details"), "Ответ должен содержать массив строк с деталями проблемы");
    }

    @Test
    void deleteMovieById_whenMovieByIdIsNotFinding_returns404AndErrorResponse() throws Exception {
        Movie interstellar = new Movie(1, "Интерстеллар", 2014);
        Movie martian = new Movie(2, "Марсианин", 2015);
        Movie gravity = new Movie(3, "Гравитация", 2013);
        store.add(interstellar);
        store.add(martian);
        store.add(gravity);

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies/4")).DELETE().build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "DELETE /movies/4 должен вернуть 404");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue, "Content-Type должен " +
                "содержать формат данных и кодировку");

        String body = resp.body();
        assertTrue(body.contains("\"error\":"), "Ответ должен содержать короткое описание ошибки");
        assertTrue(body.contains("details"), "Ответ должен содержать массив строк с деталями проблемы");
    }

    @Test
    void getMoviesByYear_whenCorrectDate_returnsMoviesArray() throws Exception {
        Movie interstellar = new Movie(1, "Интерстеллар", 2014);
        Movie martian = new Movie(2, "Марсианин", 2015);
        Movie gravity = new Movie(3, "Гравитация", 2013);
        Movie inception = new Movie(4, "Inception", 2010);
        Movie hotelBudapest = new Movie(5, "Отель «Гранд Будапешт»", 2014);
        Movie johnWick = new Movie(6, "Джон Уик", 2014);
        Movie goneGirl = new Movie(7, "Исчезнувшая", 2014);

        store.add(interstellar);
        store.add(martian);
        store.add(gravity);
        store.add(inception);
        store.add(hotelBudapest);
        store.add(johnWick);
        store.add(goneGirl);

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies?year=2014")).GET().build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "/movies?year=2014 должен вернуть 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue, "Content-Type должен " +
                "содержать формат данных и кодировку");

        List<Movie> moviesOfThe2014 = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());

        assertEquals(4, moviesOfThe2014.size(), "Должно вернуться ровно 4 фильма за 2014 год");

        boolean hasGoneGirl = moviesOfThe2014.stream().anyMatch(m -> m.getTitle().equals("Исчезнувшая"));
        assertTrue(hasGoneGirl, "Ответ должен содержать фильм «Исчезнувшая»");

        boolean all2014 = moviesOfThe2014.stream().allMatch(m -> m.getYear() == 2014);
        assertTrue(all2014, "Все возвращенные фильмы должны быть 2014 года");
    }

    @Test
    void getMoviesByYear_whenThereAreNotMoviesByYear_returnsEmptyArray() throws Exception {
        Movie interstellar = new Movie(1, "Интерстеллар", 2014);
        Movie martian = new Movie(2, "Марсианин", 2015);
        Movie gravity = new Movie(3, "Гравитация", 2013);
        Movie inception = new Movie(4, "Inception", 2010);
        Movie hotelBudapest = new Movie(5, "Отель «Гранд Будапешт»", 2014);
        Movie johnWick = new Movie(6, "Джон Уик", 2014);
        Movie goneGirl = new Movie(7, "Исчезнувшая", 2014);

        store.add(interstellar);
        store.add(martian);
        store.add(gravity);
        store.add(inception);
        store.add(hotelBudapest);
        store.add(johnWick);
        store.add(goneGirl);

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies?year=2012")).GET().build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "/movies?year=2012 должен вернуть 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue, "Content-Type должен " +
                "содержать формат данных и кодировку");

        List<Movie> moviesOfThe2012 = gson.fromJson(resp.body(), new ListOfMoviesTypeToken().getType());

        assertTrue(moviesOfThe2012.isEmpty(), "Список должен быть пустым");
    }

    @Test
    void getMoviesByYear_whenYearIsNotNumber_returns400AndErrorResponse() throws Exception {

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies?year=lastyear")).GET().build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "GET /movies?year=lastyear должен вернуть 400");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue, "Content-Type должен " +
                "содержать формат данных и кодировку");

        String body = resp.body();
        assertTrue(body.contains("\"error\":"), "Ответ должен содержать короткое описание ошибки");
        assertTrue(body.contains("details"), "Ответ должен содержать массив строк с деталями проблемы");
    }

    @Test
    void handle_whenUnsupportedMethod_returns405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + "/movies")).method("PATCH",
                        HttpRequest.BodyPublishers.noBody()).build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, resp.statusCode(), "Неподдерживаемый метод должен возвращать код 405");
    }

}
