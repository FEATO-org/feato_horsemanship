package jp.feato.horsemanship.integration.valhalla;

import jp.feato.horsemanship.config.HorsemanshipConfig;
import jp.feato.horsemanship.integration.valhalla.skill.HorsemanshipSkill;
import me.athlaeos.valhallammo.skills.skills.SkillRegistry;
import me.athlaeos.valhallammo.event.PlayerSkillExperienceGainEvent.ExperienceGainReason;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.util.logging.Logger;
import static org.mockito.Mockito.*;

class ExperienceFeedbackTest {
    @Test void defaultsAndReloadToggleFeedbackWithoutChangingAmountOrReason() {
        var yaml = new YamlConfiguration();
        var config = new HorsemanshipConfig(yaml, Logger.getAnonymousLogger());
        var skill = mock(HorsemanshipSkill.class);
        var player = mock(Player.class);
        try (var registry = mockStatic(SkillRegistry.class)) {
            registry.when(() -> SkillRegistry.getSkill("HORSEMANSHIP")).thenReturn(skill);
            var adapter = new ValhallaAdapter(config);
            adapter.addExperience(player, 5);
            yaml.set("experience.show-gain-feedback", false);
            config.reload(yaml);
            adapter.addExperience(player, 5);
            yaml.set("experience.show-gain-feedback", true);
            config.reload(yaml);
            adapter.addExperience(player, 5);
            adapter.addExperience(player, 0);
            adapter.addExperience(player, -5);
            verify(skill, times(2)).addEXP(player, 5, false, ExperienceGainReason.SKILL_ACTION);
            verify(skill).addEXP(player, 5, true, ExperienceGainReason.SKILL_ACTION);
            verifyNoMoreInteractions(skill);
        }
    }
}
