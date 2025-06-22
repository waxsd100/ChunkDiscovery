package io.wax100.chunkDiscovery.model;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 報酬アイテム・経験値・効果を保持するモデルクラス
 */
public record RewardItem(ItemStack item, int experience, List<PotionEffect> effects) {

    /**
     * コンフィグから RewardItem を生成する
     *
     * @param section config.yml の該当セクション
     * @return RewardItem インスタンス
     */
    public static RewardItem fromConfig(ConfigurationSection section) {
        if (section == null) {
            return new RewardItem(null, 0, new ArrayList<>());
        }

        // アイテム構築
        ItemStack stack = null;
        if (section.isConfigurationSection("item")) {
            ConfigurationSection itemSec = section.getConfigurationSection("item");
            String matName = Objects.requireNonNull(itemSec).getString("material", "STONE").toUpperCase();
            Material material = Material.getMaterial(matName);
            int amount = itemSec.getInt("amount", 1);
            if (material != null) {
                stack = new ItemStack(material, amount);
            }
        }

        // 経験値取得
        int exp = section.getInt("experience", 0);

        // ポーション効果取得
        List<PotionEffect> effectList = new ArrayList<>();
        if (section.isList("effects")) {
            for (Map<?, ?> map : section.getMapList("effects")) {
                String typeName = String.valueOf(map.get("type")).toUpperCase();
                PotionEffectType pet = PotionEffectType.getByName(typeName);
                if (pet == null) continue;
                int duration = map.get("duration") instanceof Number ? ((Number) map.get("duration")).intValue() : 0;
                int amplifier = map.get("amplifier") instanceof Number ? ((Number) map.get("amplifier")).intValue() : 0;
                effectList.add(new PotionEffect(pet, duration, amplifier));
            }
        }

        return new RewardItem(stack, exp, effectList);
    }
}
