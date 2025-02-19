package com.aurgiyalgo.SlimefunNukes.items;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.aurgiyalgo.SlimefunNukes.SFNukesUtils;
import com.aurgiyalgo.SlimefunNukes.SlimefunNukes;
import com.aurgiyalgo.SlimefunNukes.SlimefunNukes.Configuration;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.core.attributes.Radioactive;
import io.github.thebusybiscuit.slimefun4.core.attributes.Radioactivity;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

public class Nuke extends SlimefunItem implements Radioactive {
	
	private final int radius;
	private final int fuse;
	private final boolean incendiary;

	public Nuke(ItemGroup category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, int radius, int fuse, boolean incendiary) {
		super(category, item, recipeType, recipe);
		this.radius = radius;
		this.incendiary = incendiary;
		this.fuse = fuse;
	}

	@Override
	public void preRegister() {
		BlockUseHandler blockUseHandler = this::onBlockRightClick;
		addItemHandler(blockUseHandler);
	}

	private void onBlockRightClick(PlayerRightClickEvent event) {
		if (!event.getItem().getType().equals(Material.FLINT_AND_STEEL)) return;
		event.cancel();
		Location blockLocation = event.getClickedBlock().get().getLocation().add(0.5, 0, 0.5);
		TNTPrimed tnt = (TNTPrimed) blockLocation.getWorld().spawnEntity(blockLocation, EntityType.PRIMED_TNT);
		tnt.setFuseTicks(fuse * 20);
		tnt.setIsIncendiary(incendiary);
		tnt.setYield(radius);
		tnt.setCustomName(getItemName());
		tnt.setCustomNameVisible(true);
		event.getClickedBlock().get().setType(Material.AIR);
		
		BukkitRunnable sphereShapeTask = new BukkitRunnable() {
			
			@Override
			public void run() {
				Thread t = new Thread(() -> {
					List<Location> sphereBlocks = SFNukesUtils.getSphereBlocks(tnt.getLocation(), radius);
					BlockStorage.clearBlockInfo(event.getClickedBlock().get().getLocation());
					AtomicReference<Integer> iteratorCount = new AtomicReference<Integer>();
					iteratorCount.set(0);
					int blocksPerSecond = Configuration.BLOCKS_PER_SECOND;
					CoreProtectAPI coreProtectAPI = SlimefunNukes.getInstance().getCoreProtect();
					BukkitRunnable sphereRemoveBlocksTask = new BukkitRunnable() {
						
						@Override
						public void run() {
							if (sphereBlocks.size() - iteratorCount.get() < blocksPerSecond) {
								for (Location l : sphereBlocks) {
									if (BlockStorage.hasBlockInfo(l)) continue;
									if (coreProtectAPI != null) coreProtectAPI.logRemoval("Nuke", l, l.getBlock().getType(), null);
									l.getBlock().setType(Material.AIR);
								}
								cancel();
								return;
							}
							for (int i = 0; i < blocksPerSecond; i++) {
								if (BlockStorage.hasBlockInfo(sphereBlocks.get(i + iteratorCount.get()))) continue;
								sphereBlocks.get(i + iteratorCount.get()).getBlock().setType(Material.AIR);
							}
							iteratorCount.set(iteratorCount.get() + blocksPerSecond);
						}
					};
					sphereRemoveBlocksTask.runTaskTimer(SlimefunNukes.getInstance(), 0, 20);
				});
				t.start();
			}
		};
		
		sphereShapeTask.runTaskLater(SlimefunNukes.getInstance(), fuse * 20);
	}

	@Override
	public Radioactivity getRadioactivity() {
		return Radioactivity.LOW;
	}

}
