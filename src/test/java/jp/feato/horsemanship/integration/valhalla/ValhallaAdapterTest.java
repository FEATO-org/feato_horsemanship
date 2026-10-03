package jp.feato.horsemanship.integration.valhalla;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValhallaAdapterTest {
    @Test void newGamePlusFollowsPermanentPerkRewards() {
        assertEquals(0, ValhallaAdapter.newGamePlus(List.of()));
        assertEquals(1, ValhallaAdapter.newGamePlus(List.of("ng_plus_master")));
        assertEquals(2, ValhallaAdapter.newGamePlus(List.of("ng_plus_master", "ng_plus_legend")));
        assertEquals(2, ValhallaAdapter.newGamePlus(List.of("ng_plus_legend")));
    }
}
