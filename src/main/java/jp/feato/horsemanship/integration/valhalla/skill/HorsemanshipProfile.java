package jp.feato.horsemanship.integration.valhalla.skill;

import me.athlaeos.valhallammo.playerstats.profiles.Profile;
import me.athlaeos.valhallammo.playerstats.profiles.ProfileRegistry;
import me.athlaeos.valhallammo.skills.skills.Skill;
import java.util.UUID;

/** Perk ownership remains in Valhalla's PowerProfile; only standard progression is stored here. */
public final class HorsemanshipProfile extends Profile {
    public HorsemanshipProfile(UUID owner) { super(owner); }
    @Override public String getTableName() { return "profiles_horsemanship"; }
    @Override public Class<? extends Skill> getSkillType() { return HorsemanshipSkill.class; }
    @Override public Profile getBlankProfile(UUID owner) {
        HorsemanshipProfile profile = new HorsemanshipProfile(owner);
        profile.initStats();
        return ProfileRegistry.copyDefaultStats(profile);
    }
}
