package jp.feato.horsemanship.integration.valhalla;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Only Valhalla's public surface is invoked here; no profile internals are accessed. */
public final class ValhallaAdapter {
    private final Object skill;
    private final Method addExperience;
    private final Method getProfile;
    private final Method getPersistentProfile;
    private final Method getInt;
    private final Method getLevel;
    private final Method getNewGamePlus;
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
        getProfile = profiles.getMethod("getSkillConfigurableProfile", Player.class, String.class);
        getPersistentProfile = profiles.getMethod("getPersistentConfigurableProfile", Player.class, String.class);
        Class<?> profileType = getProfile.getReturnType();
        getInt = profileType.getMethod("getInt", String.class);
        getLevel = profileType.getMethod("getLevel");
        getNewGamePlus = profileType.getMethod("getNewGamePlus");
    }
    public int level(Player player) { return ((Number) invoke(getLevel, invoke(getPersistentProfile, null, player, "HORSEMANSHIP"))).intValue(); }
    public int newGamePlus(Player player) { return ((Number) invoke(getNewGamePlus, invoke(getPersistentProfile, null, player, "HORSEMANSHIP"))).intValue(); }
    public boolean has(Player player, String perk) { return ((Number) invoke(getInt, profile(player), perk)).intValue() > 0; }
    public void addExperience(Player player, double amount) {
        if (amount > 0) invoke(addExperience, skill, player, amount, true, skillAction);
    }
    private Object profile(Player player) { return invoke(getProfile, null, player, "HORSEMANSHIP"); }
    private static Object invoke(Method method, Object receiver, Object... args) {
        try { return method.invoke(receiver, args); }
        catch (IllegalAccessException | InvocationTargetException error) { throw new IllegalStateException("Valhalla public API failed: " + method.getName(), error); }
    }
}
