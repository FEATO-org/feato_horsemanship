package jp.feato.horsemanship.logic;

import me.athlaeos.valhallammo.utility.MathEval;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class ExperienceCurveTest {
    @Test void configuredCurveUsesValhallaEvaluationAndKeepsMilestones() throws Exception {
        var yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(
            getClass().getResourceAsStream("/horsemanship.yml"), StandardCharsets.UTF_8));
        String curve = yaml.getString("experience.exp_level_curve");
        assertEquals("(%level% + 75 * 2^(%level%/17.0)) + 300", curve);
        assertEquals(100, yaml.getInt("experience.max_level"));
        // Use Valhalla's expression evaluator without initializing its server-only Utils.
        var evaluator = new MathEval();
        int[] levels = {10, 50, 80, 100};
        double[] totals = {4000, 28815, 74354, 143905};
        for (int milestone = 0; milestone < levels.length; milestone++) {
            double cumulative = 0;
            for (int level = 1; level <= levels[milestone]; level++)
                cumulative += evaluator.evaluate(curve.replace("%level%", Integer.toString(level)));
            assertEquals(totals[milestone], cumulative, 2, "Lv" + levels[milestone]);
        }
    }
}
