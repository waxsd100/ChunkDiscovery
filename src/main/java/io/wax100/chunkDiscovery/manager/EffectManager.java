package io.wax100.chunkDiscovery.manager;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.Random;

/**
 * パーティクルや花火演出などを管理するクラス
 */
public class EffectManager {
    private static final Random RANDOM = new Random();


    /**
     * 指定位置に任意の花火エフェクト＋サウンドを再生する汎用メソッド
     */
    public static void spawnFirework(Location loc) {
        // サウンド再生
        loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(Color.AQUA)
                .withFade(Color.WHITE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
    }

    /**
     * ランダムな色・タイプで花火を１つ打ち上げる
     */
    public static void spawnRandomFirework(Location loc) {
        Firework fw = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = fw.getFireworkMeta();

        // ランダムエフェクト設定
        FireworkEffect.Type[] types = FireworkEffect.Type.values();
        FireworkEffect.Builder builder = FireworkEffect.builder()
                .withColor(randomColor())
                .withFade(randomColor())
                .with(types[RANDOM.nextInt(types.length)])
                .flicker(RANDOM.nextBoolean())
                .trail(RANDOM.nextBoolean());

        meta.addEffect(builder.build());
        meta.setPower(RANDOM.nextInt(2) + 1);  // 1～2 の力
        fw.setFireworkMeta(meta);
    }


    public static void celebrateMilestone(Location loc) {
        World world = loc.getWorld();
        world.spawnParticle(
                Particle.VILLAGER_HAPPY,
                loc.clone().add(0, 1.0, 0),    // 少し上で
                50,                            // 数量
                1.5, 1.5, 1.5,                 // spread x,y,z
                0.05                           // speed
        );
        // TODO うるさいのでどうにかする
//        world.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.2f, 1.0f);

        for (int i = 0; i < 5; i++) {
            // 少しだけ横にずらして配置
            Location burstLoc = loc.clone().add(
                    (RANDOM.nextDouble() * 4 - 2),
                    0,
                    (RANDOM.nextDouble() * 4 - 2)
            );
            spawnRandomFirework(burstLoc);
        }
    }

    /** 色の候補からランダムで１つ返す */
    private static Color randomColor() {
        Color[] colors = {
                Color.WHITE, Color.AQUA, Color.GREEN,
                Color.YELLOW, Color.ORANGE, Color.RED,
                Color.FUCHSIA, Color.PURPLE
        };
        return colors[RANDOM.nextInt(colors.length)];
    }
}
