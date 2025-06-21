package io.wax100.chunkDiscovery.manager;

import org.bukkit.Location;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

/**
 * パーティクルや花火演出などを管理するクラス
 */
public class EffectManager {
    /**
     * 指定位置に花火を打ち上げる
     */
    public static void spawnFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(org.bukkit.Color.AQUA)
                .withFade(org.bukkit.Color.WHITE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }
}
