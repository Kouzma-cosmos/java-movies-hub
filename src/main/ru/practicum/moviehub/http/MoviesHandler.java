package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    private static final int CINEMA_BIRTH_YEAR = 1888;

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

        if (isInvalidPath(pathParts)) {
            ErrorResponse error = new ErrorResponse("Ошибка валидации!", new String[]{"Отсутствует Id фильма"});
            sendJson(ex, 400, gson.toJson(error));
            return;
        }

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
            return;
        }

        if (movieFromUser.getTitle().length() > 100) {
            ErrorResponse error = new ErrorResponse("Ошибка валидации!", new String[]{"Превышено допустимое " +
                    "кол-во символов в поле 'title'"});
            String errorJson = gson.toJson(error);
            sendJson(ex, 422, errorJson);
            return;
        }

        int maxAllowedYear = LocalDateTime.now().plusYears(1).getYear();

        if (movieFromUser.getYear() < CINEMA_BIRTH_YEAR || movieFromUser.getYear() > maxAllowedYear) {
            ErrorResponse error = new ErrorResponse("Ошибка валидации!", new String[]{"Значение поля 'year' " +
                    "должно быть между " + CINEMA_BIRTH_YEAR + " и " + maxAllowedYear});
            String errorJson = gson.toJson(error);
            sendJson(ex, 422, errorJson);
            return;
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

            List<Movie> moviesByYear = store.getByYear(year);

            String json = gson.toJson(moviesByYear);
            sendJson(ex, 200, json);

        } else if (!isInvalidPath(pathParts)) {

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

    private boolean isInvalidPath(String[] pathParts) {
        return pathParts.length <= 2 || pathParts[2].isEmpty();
    }


}

