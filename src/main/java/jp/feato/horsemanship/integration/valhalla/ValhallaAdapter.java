package jp.feato.horsemanship.integration.valhalla;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import jp.feato.horsemanship.integration.valhalla.skill.HorsemanshipProfile;
import jp.feato.horsemanship.integration.valhalla.skill.HorsemanshipSkill;
import me.athlaeos.valhallammo.event.PlayerSkillExperienceGainEvent.ExperienceGainReason;
import me.athlaeos.valhallammo.playerstats.profiles.ProfileRegistry;
import me.athlaeos.valhallammo.playerstats.profiles.implementations.PowerProfile;
import me.athlaeos.valhallammo.skills.skills.SkillRegistry;
import java.util.Collection;

/** Only Valhalla's public surface is invoked here; no profile internals are accessed. */
public final class ValhallaAdapter {
    private final HorsemanshipSkill skill;
    public ValhallaAdapter(Plugin valhalla) {
        if (!(SkillRegistry.getSkill("HORSEMANSHIP") instanceof HorsemanshipSkill registered))
            throw new IllegalStateException("HORSEMANSHIP dedicated skill is not registered");
        skill = registered;
    }
    public int level(Player player) {
        return ProfileRegistry.getPersistentProfile(player, HorsemanshipProfile.class).getLevel();
    }
    public int newGamePlus(Player player) { return newGamePlus(permanentPerks(player)); }
    static int newGamePlus(Collection<?> permanent) {
        if (permanent.contains("ng_plus_legend")) return 2;
        return permanent.contains("ng_plus_master") ? 1 : 0;
    }
    public boolean has(Player player, String perk) {
        PowerProfile profile = ProfileRegistry.getPersistentProfile(player, PowerProfile.class);
        return profile.getUnlockedPerks().contains(perk) || profile.getPermanentlyUnlockedPerks().contains(perk);
    }
    public void addExperience(Player player, double amount) {
        if (amount > 0) skill.addEXP(player, amount, true, ExperienceGainReason.SKILL_ACTION);
    }
    private Collection<?> permanentPerks(Player player) {
        return ProfileRegistry.getPersistentProfile(player, PowerProfile.class).getPermanentlyUnlockedPerks();
    }
}
