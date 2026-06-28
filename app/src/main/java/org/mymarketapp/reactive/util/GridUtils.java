package org.mymarketapp.reactive.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GridUtils {

    private GridUtils() {}

    public static <T> List<List<Optional<T>>> splitIntoRows(List<T> items, int cols) {
        List<List<Optional<T>>> rows = new ArrayList<>();
        int i = 0;
        while (i < items.size()) {
            List<Optional<T>> row = new ArrayList<>();
            for (int c = 0; c < cols; c++) {
                row.add(i < items.size() ? Optional.of(items.get(i++)) : Optional.empty());
            }
            rows.add(row);
        }
        return rows;
    }
}
