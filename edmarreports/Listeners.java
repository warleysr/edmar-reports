package edmarreports;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class Listeners implements Listener {
	
	private static HashMap<String, Report> managing = new HashMap<>();
	
	@EventHandler
	public void onClick(InventoryClickEvent e) {
		if ((e.getClickedInventory() != null) && e.getClickedInventory().getTitle().equals(EdmarReports.GUI.getTitle())) {
			e.setCancelled(true);
			if (e.getCurrentItem() == null) return;
			if (e.getCurrentItem().getType() != Material.SKULL_ITEM) return;
			if (!(e.getCurrentItem().hasItemMeta())) return;
			Player p = (Player) e.getWhoClicked();
			Report r = EdmarReports.getReport(e.getSlot());
			openReportOptions(p, r);
			
		} else if ((e.getClickedInventory() != null) 
				&& e.getClickedInventory().getTitle().equals(EdmarReports.OPTIONS_GUI.getTitle())) {
			e.setCancelled(true);
			if (e.getCurrentItem() == null) return;
			Player p = (Player) e.getWhoClicked();
			Report r = managing.get(p.getName());
			if (e.getCurrentItem().getType() == Material.COMPASS) {
				p.closeInventory();
				Player t = Bukkit.getPlayerExact(r.getReported());
				if (t == null) {
					p.sendMessage(getMsg("jogador-invalido"));
					return;
				}
				p.teleport(t);
				
			} else if (e.getCurrentItem().getType() == Material.EYE_OF_ENDER) {
				p.closeInventory();
				EdmarReports.deleteReport(r);
				p.sendMessage(getMsg("inocentado").replace("@player", r.getReported()));
				
			} else if (e.getCurrentItem().getType() == Material.LEASH) {
				p.closeInventory();
				Bukkit.dispatchCommand(p, getMsg("ComandoBanir").replace("@player", r.getReported()));
				EdmarReports.deleteReport(r);
				p.sendMessage(getMsg("banido").replace("@player", r.getReported()));
			}
		}
	}
	
	private void openReportOptions(Player p, Report r) {
		managing.put(p.getName(), r);
		p.openInventory(EdmarReports.OPTIONS_GUI);
	}
	
	private String getMsg(String key) {
		return Bukkit.getPluginManager().getPlugin("EdmarReports").getConfig().getString(key).replace('&', '\u00a7');
	}

}
