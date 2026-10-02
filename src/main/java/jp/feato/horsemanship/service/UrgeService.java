package jp.feato.horsemanship.service;

import jp.feato.horsemanship.config.HorsemanshipConfig;
import jp.feato.horsemanship.logic.BalanceMath;
import jp.feato.horsemanship.state.RiderState;
import jp.feato.horsemanship.state.RiderStateStore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.concurrent.ThreadLocalRandom;

public final class UrgeService {
    private final HorsemanshipConfig config;
    private final ExclusivePerkService perks;
    private final jp.feato.horsemanship.integration.valhalla.ValhallaAdapter valhalla;
    private final RiderStateStore states;
    private final Mounts mounts;
    private final MountedEffectService effects;
    public UrgeService(HorsemanshipConfig config, ExclusivePerkService perks, jp.feato.horsemanship.integration.valhalla.ValhallaAdapter valhalla, RiderStateStore states, Mounts mounts, MountedEffectService effects) {
        this.config = config; this.perks = perks; this.valhalla = valhalla; this.states = states; this.mounts = mounts; this.effects = effects;
    }
    public void trigger(Player player, long now) {
        if (!mounts.isDriver(player) || !perks.has(player, "urge")) return;
        RiderState state = states.get(player.getUniqueId());
        var mount = mounts.eligibleMount(player);
        if (mount == null) return;
        if (!mount.getUniqueId().equals(state.mount)) {
            effects.clearMount(state);
            state.mount = mount.getUniqueId();
            state.mountedSince = now;
        }
        if (now < state.fatigueEnd) { feedback(player, state, now, "疲労中"); return; }
        if (now < state.cooldownEnd) { feedback(player, state, now, "追う: 再使用まで " + ((state.cooldownEnd - now + 999) / 1000) + "秒"); return; }
        String profile = "urge";
        if (perks.has(player, "breakaway")) profile = "urge.breakaway";
        else if (perks.has(player, "relentless_pace")) profile = "urge.relentless-pace";
        double response = config.number("urge.response-delay-seconds");
        String[] reductions = {"good_hands", "seasoned_rider", "fleetfoot", "quick_response", "steady_hands", "calm_rein", "master_of_reins", "windborne", "light_cavalry", "swift_rider"};
        for (String perk : reductions) if (perks.has(player, perk)) response -= config.number("urge.response-reduction." + perk.replace('_', '-'));
        if (valhalla.newGamePlus(player) >= 1) response -= config.number("urge.response-reduction.ng-plus-master");
        if (valhalla.newGamePlus(player) >= 2) response -= config.number("urge.response-reduction.ng-plus-legend");
        response = Math.max(config.number("urge.minimum-response-seconds"), response);
        double duration = config.number(profile + ".duration-seconds");
        if (perks.has(player, "second_wind")) duration += config.number("urge.duration-bonus.second-wind");
        double cooldown = config.number(profile + ".cooldown-seconds");
        if (perks.has(player, "quick_response")) cooldown -= config.number("urge.cooldown-reduction.quick-response");
        if (perks.has(player, "trailwise")) cooldown -= config.number("urge.cooldown-reduction.trailwise");
        if (valhalla.newGamePlus(player) >= 2) cooldown -= config.number("urge.cooldown-reduction.ng-plus-legend");
        state.urgeStart = now;
        state.urgeActive = now + Math.round(response * 1000);
        state.urgeEnd = state.urgeActive + Math.round(duration * 1000);
        state.cooldownEnd = now + Math.round(Math.max(1, cooldown) * 1000);
        state.lastUrgeBonus = config.number(profile + ".speed-bonus");
        state.firstImpactUsed = false;
        state.urgeEndingProcessed = false;
        state.fatigueEnd = state.oneAsOneEnd = 0;
        feedback(player, state, now, "追う: 発動");
    }
    public void tick(long now) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            RiderState state = states.peek(player.getUniqueId());
            if (state == null || state.mount == null || state.urgeEnd == 0 || state.urgeEndingProcessed || now < state.urgeEnd) continue;
            state.urgeEndingProcessed = true;
            double reduction = 0;
            String[] perksWithReduction = {"mutual_trust", "steady_hands", "calm_rein", "master_of_reins"};
            for (String perk : perksWithReduction) if (perks.has(player, perk)) reduction += config.number("fatigue.chance-reduction." + perk.replace('_', '-'));
            if (valhalla.newGamePlus(player) >= 1) reduction += config.number("fatigue.chance-reduction.ng-plus-master");
            if (valhalla.newGamePlus(player) >= 2) reduction += config.number("fatigue.chance-reduction.ng-plus-legend");
            double chance = BalanceMath.fatigueChance(config.number("fatigue.chance"), reduction, config.number("fatigue.minimum-chance"));
            if (ThreadLocalRandom.current().nextDouble() < chance) {
                double duration = config.number("fatigue.duration-seconds");
                String[] reductions = {"seasoned_rider", "mutual_trust", "second_wind", "breeders_insight", "bloodline_study", "horse_whisperer"};
                for (String perk : reductions) if (perks.has(player, perk)) duration -= config.number("fatigue.duration-reduction." + perk.replace('_', '-'));
                state.fatigueEnd = now + Math.round(Math.max(0, duration) * 1000);
                feedback(player, state, now, "追う: 疲労");
            } else if (perks.has(player, "one_as_one") && ThreadLocalRandom.current().nextDouble() < config.number("one-as-one.chance-after-no-fatigue")) {
                state.oneAsOneEnd = now + Math.round(config.number("one-as-one.duration-seconds") * 1000);
                feedback(player, state, now, "人馬一体");
            }
        }
    }
    private void feedback(Player player, RiderState state, long now, String text) {
        if (now - state.lastFeedback < 700) return;
        state.lastFeedback = now;
        player.sendActionBar(Component.text(text));
    }
}
