package jp.feato.horsemanship.integration.valhalla;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;

/** Registers a public Valhalla unlock condition before Horsemanship loads its skill configuration. */
public final class ExclusiveUnlockRegistration {
    public static final String KEY = "horsemanship_exclusive";

    private ExclusiveUnlockRegistration() {}

    public static void register(Plugin valhalla) throws ReflectiveOperationException {
        ClassLoader loader = valhalla.getClass().getClassLoader();
        Class<?> conditionType = loader.loadClass("me.athlaeos.valhallammo.skills.perkunlockconditions.UnlockCondition");
        Class<?> registry = loader.loadClass("me.athlaeos.valhallammo.skills.perkunlockconditions.UnlockConditionRegistry");
        Class<?> profileRegistry = loader.loadClass("me.athlaeos.valhallammo.playerstats.profiles.ProfileRegistry");
        Class<?> powerProfile = loader.loadClass("me.athlaeos.valhallammo.playerstats.profiles.implementations.PowerProfile");
        Method getProfile = profileRegistry.getMethod("getPersistentProfile", Player.class, Class.class);
        Method unlocked = powerProfile.getMethod("getUnlockedPerks");
        Method permanent = powerProfile.getMethod("getPermanentlyUnlockedPerks");
        Ownership ownership = player -> {
            try {
                Object profile = getProfile.invoke(null, player, powerProfile);
                return new Acquired((Collection<?>) unlocked.invoke(profile), (Collection<?>) permanent.invoke(profile));
            } catch (IllegalAccessException | InvocationTargetException exception) {
                throw new IllegalStateException("Cannot read Valhalla perk ownership", exception);
            }
        };
        Object condition = create(conditionType, loader, ownership);
        registry.getMethod("register", conditionType).invoke(null, condition);
    }

    private static Object create(Class<?> type, ClassLoader loader, Ownership ownership) {
        InvocationHandler handler = new Condition(ownership, type, loader);
        return Proxy.newProxyInstance(loader, new Class<?>[]{type}, handler);
    }

    public static boolean canAcquire(Collection<?> unlocked, Collection<?> permanent, String opposite) {
        return opposite == null || (!unlocked.contains(opposite) && !permanent.contains(opposite));
    }

    private interface Ownership { Acquired get(Player player); }
    private record Acquired(Collection<?> unlocked, Collection<?> permanent) {}

    private static final class Condition implements InvocationHandler {
        private final Ownership ownership;
        private final Class<?> type;
        private final ClassLoader loader;
        private String opposite;

        private Condition(Ownership ownership, Class<?> type, ClassLoader loader) {
            this.ownership = ownership;
            this.type = type;
            this.loader = loader;
        }

        @Override public Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "initCondition" -> { opposite = (String) args[0]; yield null; }
                case "getValuePlaceholder" -> KEY;
                case "getFailurePlaceholder" -> "warning_horsemanship_exclusive";
                case "getFailedConditionMessage" -> "排他する馬術Perkを取得済みです";
                case "getConditionMessages" -> List.of("排他条件: " + opposite + " が未取得");
                case "canUnlock" -> {
                    if ((boolean) args[1]) yield true;
                    Acquired acquired = ownership.get((Player) args[0]);
                    yield canAcquire(acquired.unlocked(), acquired.permanent(), opposite);
                }
                case "createInstance" -> create(type, loader, ownership);
                case "canRegister" -> true;
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                case "toString" -> "Horsemanship exclusive condition: " + opposite;
                default -> throw new UnsupportedOperationException("Unknown UnlockCondition method: " + method.getName());
            };
        }
    }
}
