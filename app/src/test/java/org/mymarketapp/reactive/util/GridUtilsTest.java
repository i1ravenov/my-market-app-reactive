package org.mymarketapp.reactive.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GridUtilsTest {

    @Test
    void splitIntoRows_exactFit_noEmptySlots() {
        List<List<Optional<String>>> rows = GridUtils.splitIntoRows(List.of("A", "B", "C"), 3);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).map(Optional::get).containsExactly("A", "B", "C");
    }

    @Test
    void splitIntoRows_incompleteLastRow_padsWithEmpty() {
        List<List<Optional<String>>> rows = GridUtils.splitIntoRows(List.of("A"), 3);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get(0)).isPresent();
        assertThat(rows.get(0).get(1)).isEmpty();
        assertThat(rows.get(0).get(2)).isEmpty();
    }

    @Test
    void splitIntoRows_multipleRows() {
        List<List<Optional<String>>> rows = GridUtils.splitIntoRows(List.of("A", "B", "C", "D"), 3);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).map(Optional::get).containsExactly("A", "B", "C");
        assertThat(rows.get(1).get(0).get()).isEqualTo("D");
        assertThat(rows.get(1).get(1)).isEmpty();
    }

    @Test
    void splitIntoRows_emptyInput_returnsEmpty() {
        assertThat(GridUtils.splitIntoRows(List.of(), 3)).isEmpty();
    }
}
