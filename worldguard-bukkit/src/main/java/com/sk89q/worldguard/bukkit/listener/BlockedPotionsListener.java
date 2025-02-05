/*
 * WorldGuard, a suite of tools for Minecraft
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldGuard team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.worldguard.bukkit.listener;

import com.sk89q.worldguard.bukkit.BukkitWorldConfiguration;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.bukkit.event.entity.DamageEntityEvent;
import com.sk89q.worldguard.bukkit.event.inventory.UseItemEvent;
import com.sk89q.worldguard.bukkit.util.Entities;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Handles blocked potions.
 */
public class BlockedPotionsListener extends AbstractListener {

    /**
     * Construct the listener.
     *
     * @param plugin an instance of WorldGuardPlugin
     */
    public BlockedPotionsListener(WorldGuardPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onProjectile(DamageEntityEvent event) {
        if (event.getOriginalEvent() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent originalEvent = (EntityDamageByEntityEvent) event.getOriginalEvent();
            if (Entities.isPotionArrow(originalEvent.getDamager())) { // should take care of backcompat
                BukkitWorldConfiguration wcfg = getWorldConfig(event.getWorld());
                PotionEffectType blockedEffect = null;
                if (originalEvent.getDamager() instanceof SpectralArrow) {
                    if (wcfg.blockPotions.contains(PotionEffectType.GLOWING)) {
                        blockedEffect = PotionEffectType.GLOWING;
                    }
                } else if (originalEvent.getDamager() instanceof Arrow) {
                    Arrow tippedArrow = (Arrow) originalEvent.getDamager();
                    PotionEffectType baseEffect = tippedArrow.getBasePotionData().getType().getEffectType();
                    if (wcfg.blockPotions.contains(baseEffect)) {
                        blockedEffect = baseEffect;
                    } else {
                        for (PotionEffect potionEffect : tippedArrow.getCustomEffects()) {
                            if (wcfg.blockPotions.contains(potionEffect.getType())) {
                                blockedEffect = potionEffect.getType();
                                break;
                            }
                        }
                    }
                }
                if (blockedEffect != null) {
                    Player player = event.getCause().getFirstPlayer();
                    if (player != null) {
                        if (getPlugin().hasPermission(player, "worldguard.override.potions")) {
                            return;
                        }
                        player.sendMessage(ChatColor.RED + "К сожалению, стрелы с "
                                + blockedEffect.getName() + " в настоящее время отключены.");
                    }
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onItemInteract(UseItemEvent event) {
        BukkitWorldConfiguration wcfg = getWorldConfig(event.getWorld());
        ItemStack item = event.getItemStack();

        if (item.getType() != Material.POTION
                && item.getType() != Material.SPLASH_POTION
                && item.getType() != Material.LINGERING_POTION) {
            return;
        }

        if (!wcfg.blockPotions.isEmpty()) {
            PotionEffectType blockedEffect = null;

            PotionMeta meta;
            if (item.getItemMeta() instanceof PotionMeta) {
                meta = ((PotionMeta) item.getItemMeta());
            } else {
                return; // ok...?
            }

            // Find the first blocked effect
            PotionEffectType baseEffect = meta.getBasePotionData().getType().getEffectType();
            if (wcfg.blockPotions.contains(baseEffect)) {
                blockedEffect = baseEffect;
            }

            if (blockedEffect == null && meta.hasCustomEffects()) {
                for (PotionEffect effect : meta.getCustomEffects()) {
                    if (wcfg.blockPotions.contains(effect.getType())) {
                        blockedEffect = effect.getType();
                        break;
                    }
                }
            }

            if (blockedEffect != null) {
                Player player = event.getCause().getFirstPlayer();

                if (player != null) {
                    if (getPlugin().hasPermission(player, "worldguard.override.potions")) {
                        if (wcfg.blockPotionsAlways && (item.getType() == Material.SPLASH_POTION
                                || item.getType() == Material.LINGERING_POTION)) {
                            player.sendMessage(ChatColor.RED + "К сожалению, стрелы с " +
                                    blockedEffect.getName() + " нельзя бросить, " +
                                    "даже если у вас есть разрешение обойти это, " +
                                    "из-за ограничений (и из-за чрезмерно надежной блокировки зелья).");
                            event.setCancelled(true);
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "К сожалению, стрелы с "
                                + blockedEffect.getName() + " в настоящее время отключены.");
                        event.setCancelled(true);
                    }
                } else {
                    event.setCancelled(true);
                }
            }
        }
    }

}
