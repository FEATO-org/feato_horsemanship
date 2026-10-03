package jp.feato.horsemanship.integration.valhalla;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

/** Only Valhalla's public surface is invoked here; no profile internals are accessed. */
public final class ValhallaAdapter {
    private final Object skill;
    private final Method addExperience;
    private final Method getPersistentConfigurableProfile;
    private final Method getPersistentProfile;
    private final Class<?> powerProfile;
    private final Method getUnlockedPerks;
    private final Method getPermanentlyUnlockedPerks;
    private final Method getLevel;
    private final Object skillAction;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ValhallaAdapter(Plugin valhalla) throws ReflectiveOperationException {
        ClassLoader loader = valhalla.getClass().getClassLoader();
        Class<?> registry = loader.loadClass("me.athlaeos.valhallammo.skills.skills.SkillRegistry");
        skill = registry.getMethod("getSkill", String.class).invoke(null, "HORSEMANSHIP");
        if (skill == null) throw new IllegalStateException("HORSEMANSHIP is not registered; install horsemanship.yml before server startup");
        Class<?> reason = loader.loadClass("me.athlaeos.valhallammo.event.PlayerSkillExperienceGainEvent$ExperienceGainReason");
        skillAction = Enum.valueOf((Class<? extends Enum>) reason.asSubclass(Enum.class), "SKILL_ACTION");
        addExperience = skill.getClass().getMethod("addEXP", Player.class, double.class, boolean.class, reason);
        Class<?> profiles = loader.loadClass("me.athlaeos.valhallammo.playerstats.profiles.ProfileRegistry");
        powerProfile = loader.loadClass("me.athlaeos.valhallammo.playerstats.profiles.implementations.PowerProfile");
        getPersistentConfigurableProfile = profiles.getMethod("getPersistentConfigurableProfile", Player.class, String.class);
        getPersistentProfile = profiles.getMethod("getPersistentProfile", Player.class, Class.class);
        getUnlockedPerks = powerProfile.getMethod("getUnlockedPerks");
        getPermanentlyUnlockedPerks = powerProfile.getMethod("getPermanentlyUnlockedPerks");
        Class<?> profileType = getPersistentConfigurableProfile.getReturnType();
        getLevel = profileType.getMethod("getLevel");
    }
    public int level(Player player) { return ((Number) invoke(getLevel, configurableProfile(player))).intValue(); }
    public int newGamePlus(Player player) { return newGamePlus(permanentPerks(player)); }
    static int newGamePlus(Collection<?> permanent) {
        if (permanent.contains("ng_plus_legend")) return 2;
        return permanent.contains("ng_plus_master") ? 1 : 0;
    }
    public boolean has(Player player, String perk) {
        Object profile = powerProfile(player);
        return ((Collection<?>) invoke(getUnlockedPerks, profile)).contains(perk)
            || ((Collection<?>) invoke(getPermanentlyUnlockedPerks, profile)).contains(perk);
    }
    public void addExperience(Player player, double amount) {
        if (amount > 0) invoke(addExperience, skill, player, amount, true, skillAction);
    }
    private Collection<?> permanentPerks(Player player) {
        return (Collection<?>) invoke(getPermanentlyUnlockedPerks, powerProfile(player));
    }
    private Object powerProfile(Player player) { return invoke(getPersistentProfile, null, player, powerProfile); }
    private Object configurableProfile(Player player) { return invoke(getPersistentConfigurableProfile, null, player, "HORSEMANSHIP"); }
    private static Object invoke(Method method, Object receiver, Object... args) {
        try { return method.invoke(receiver, args); }
        catch (IllegalAccessException | InvocationTargetException error) { throw new IllegalStateException("Valhalla public API failed: " + method.getName(), error); }
    }
}
