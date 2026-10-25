package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class MoviesHandler extends BaseHttpHandler {

    public MoviesHandler(MoviesStore store) {
        super(store);
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod().toUpperCase();

        switch (method) {
            case "GET":
                handleGet(ex);
                break;

            case "POST":
                handlePost(ex);
                break;

            case "DELETE":
                handleDelete(ex);
                break;

            default:
                ErrorResponse error = new ErrorResponse("Ошибка: ", new String[]{"метод не поддерживается"});
                sendJson(ex, 405, gson.toJson(error));
        }
    }

    private void handleDelete(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String[] pathParts = path.split("/");

        String idString = pathParts[2];
        int id;

        try {
            id = Integer.parseInt(idString);
        } catch (NumberFormatException excp) {
            ErrorResponse error = new ErrorResponse("Ошибка валидации!", new String[]{"Id не число"});
            sendJson(ex, 400, gson.toJson(error));
            return;
        }

        Movie requestedMovie = store.getById(id);

        if (requestedMovie == null) {
            ErrorResponse error = new ErrorResponse("Ошибка валидации!", new String[]{"Фильм не найден"});
            sendJson(ex, 404, gson.toJson(error));
            return;
        }

        store.remove(id);
        sendNoContent(ex);
    }

    private void handlePost(HttpExchange ex) throws IOException {
        Headers headers = ex.getRequestHeaders();

        String contentType = headers.getFirst("Content-Type");

        if (contentType == null || !contentType.equalsIgnoreCase("application/json")) {
            ErrorResponse error = new ErrorResponse("Ошибка валидации!", new String[]{"Неподдерживаемый тип"});
            String errorJson = gson.toJson(error);
            sendJson(ex, 415, errorJson);
            return;
        }

        Movie movieFromUser;
        try {
            String requestBody = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            movieFromUser = gson.fromJson(requestBody, Movie.class);

            if (movieFromUser == null) {
                throw new JsonSyntaxException("Пустое тело запроса");
            }

        } catch (JsonSyntaxException e) {
            ErrorResponse error = new ErrorResponse("Ошибка валидации!", new String[]{"Некорректный формат JSON"});
            sendJson(ex, 422, gson.toJson(error));
            return;
        }

        if (movieFromUser.getTitle() == null || movieFromUser.getTitle().isBlank()) {
            ErrorResponse error = new ErrorResponse("Ошибка валидации!", new String[]{"Поле 'title' не должно " +
                    "быть пустым"});
            String errorJson = gson.toJson(error);
            sendJson(ex, 422, errorJson);
        }

        if (movieFromUser.getTitle().length() > 100) {
            ErrorResponse error = new ErrorResponse("Ошибка валидации!", new String[]{"Превышено допустимое " +
                    "кол-во символов в поле 'title'"});
            String errorJson = gson.toJson(error);
            sendJson(ex, 422, errorJson);
        }

        if (movieFromUser.getYear() < 1888 || movieFromUser.getYear() > 2027) {
            ErrorResponse error = new ErrorResponse("Ошибка валидации!", new String[]{"Значение поля 'year' " +
                    "должно быть между 1888 и 2027"});
            String errorJson = gson.toJson(error);
            sendJson(ex, 422, errorJson);
        }

        store.add(movieFromUser);
        sendJson(ex, 201, gson.toJson(movieFromUser));
    }

    private void handleGet(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getQuery();
        String[] pathParts = ex.getRequestURI().getPath().split("/");

        if (query != null && query.contains("year")) {
            String yearString = query.split("=")[1];

            int year;
            try {
                year = Integer.parseInt(yearString);
            } catch (NumberFormatException excp) {
                ErrorResponse error = new ErrorResponse("Ошибка валидации!", new String[]{"Некорректный параметр " +
                        "запроса — 'year'"});
                sendJson(ex, 400, gson.toJson(error));
                return;
            }

            List<Movie> moviesByYear =
                    store.getAll().stream().filter(movie -> movie.getYear() == year).collect(Collectors.toList());

            String json = gson.toJson(moviesByYear);
            sendJson(ex, 200, json);
        } else if (pathParts.length > 2) {

            String idString = pathParts[2];
            int id;
            try {
                id = Integer.parseInt(idString);
            } catch (NumberFormatException excp) {
                ErrorResponse error = new ErrorResponse("Ошибка валидации!", new String[]{"Id не число"});
                sendJson(ex, 400, gson.toJson(error));
                return;
            }
            Movie requestedMovie = store.getById(id);

            if (requestedMovie == null) {
                ErrorResponse error = new ErrorResponse("Ошибка валидации!", new String[]{"Фильм не найден"});
                sendJson(ex, 404, gson.toJson(error));
                return;
            }
            String json = gson.toJson(requestedMovie);
            sendJson(ex, 200, json);
        } else {
            List<Movie> movies = store.getAll();
            String json = gson.toJson(movies);
            sendJson(ex, 200, json);
        }
    }
}

