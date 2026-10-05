package jp.feato.horsemanship.integration.valhalla;

import jp.feato.horsemanship.integration.valhalla.skill.HorsemanshipProfile;
import jp.feato.horsemanship.integration.valhalla.skill.HorsemanshipSkill;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class DedicatedProfileTest {
    @Test void skillAndProfileDeclareDedicatedTypes() throws Exception {
        String root = "src/main/java/jp/feato/horsemanship/integration/valhalla/skill/";
        String skill = Files.readString(Path.of(root + "HorsemanshipSkill.java"));
        String profile = Files.readString(Path.of(root + "HorsemanshipProfile.java"));
        assertTrue(skill.contains("extends Skill"));
        assertTrue(skill.contains("return HorsemanshipProfile.class"));
        assertTrue(profile.contains("extends Profile"));
        assertTrue(profile.contains("return HorsemanshipSkill.class"));
        assertFalse(profile.contains("intStat("));
        assertFalse(profile.contains("stringSetStat("));
    }
    @Test void registrationPrecedesListenersAndRejectsDuplicate() throws Exception {
        String source = Files.readString(Path.of("src/main/java/jp/feato/horsemanship/HorsemanshipPlugin.java"));
        int duplicate = source.indexOf("SkillRegistry.getSkill(\"HORSEMANSHIP\") != null");
        int condition = source.indexOf("ExclusiveUnlockRegistration.register(dependency)");
        int profile = source.indexOf("ProfileRegistry.registerProfileType");
        int skill = source.indexOf("SkillRegistry.registerSkill(skill)");
        int adapter = source.indexOf("new ValhallaAdapter(dependency)");
        int listener = source.indexOf("registerEvents(");
        assertTrue(duplicate >= 0 && duplicate < condition && condition < profile && profile < skill && skill < adapter && adapter < listener);
        String integration = Files.readString(Path.of("src/main/java/jp/feato/horsemanship/integration/valhalla/ValhallaAdapter.java"));
        assertFalse(integration.contains("ConfigurableProfile"));
        assertTrue(integration.contains("skill.addEXP(player"));
    }
}
