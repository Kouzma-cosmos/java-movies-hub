package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesStore {

    private final Map<Integer, Movie> movies = new HashMap<>();
    private int idCounter = 0;

    public Movie add(Movie movie) {
        idCounter++;

        Movie movieWithId = new Movie(idCounter, movie.getTitle(), movie.getYear());

        movies.put(movieWithId.getId(), movieWithId);

        return movieWithId;
    }

    public Movie getById(int id) {
        return movies.get(id);
    }

    public List<Movie> getAll() {
        return new ArrayList<>(movies.values());
    }

    public void remove(int id) {
        movies.remove(id);
    }

    public void clear() {
        movies.clear();
        idCounter = 0;
    }

}
