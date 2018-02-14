package edmarreports;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class EdmarReports extends JavaPlugin {
	
	private static Connection conn;
	protected static Inventory GUI;
	protected static Inventory OPTIONS_GUI;
	private static boolean needUpdate = true;
	private static boolean updating;
	private static ArrayList<String> reporting = new ArrayList<>();
	private static HashMap<Integer, Report> cache = new HashMap<>();
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy H:m");
	
	@Override
	public void onEnable() {
		saveDefaultConfig();
		reloadConfig();
		
		String host = getConfig().getString("MySQL.host").trim(), user = getConfig().getString("MySQL.user").trim(),
			   pass = getConfig().getString("MySQL.pass").trim(), dbname = getConfig().getString("MySQL.dbname").trim();
		int port = getConfig().getInt("MySQL.port");
		
		try {
			conn = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + dbname, user, pass);
			
			conn.prepareStatement("CREATE TABLE IF NOT EXISTS reports "
					+ "(id INT PRIMARY KEY AUTO_INCREMENT, player VARCHAR(16), reportado VARCHAR(16), "
					+ "motivo VARCHAR(32), data TIMESTAMP);").executeUpdate();
		} catch (SQLException e) {
			getLogger().severe("*** Erro ao iniciar MySQL: " + e.getMessage());
			setEnabled(false);
			return;
		}
		
		int size = getConfig().getInt("TamanhoGUI");
		String title = getMsg("TituloGUI");
		GUI = Bukkit.createInventory(null, size, title);
		
		OPTIONS_GUI = Bukkit.createInventory(null, 9, getMsg("GUIOpcoes.Titulo"));
		OPTIONS_GUI.setItem(2, getItem(Material.COMPASS, getMsg("GUIOpcoes.Teleportar")));
		OPTIONS_GUI.setItem(4, getItem(Material.EYE_OF_ENDER, getMsg("GUIOpcoes.Inocentar")));
		OPTIONS_GUI.setItem(6, getItem(Material.LEASH, getMsg("GUIOpcoes.Banir")));
		
		Bukkit.getPluginManager().registerEvents(new Listeners(), this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("report")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("[EdmarReports] Comando apenas para jogadores in-game.");
				return false;
			}
			Player p = (Player) sender;
			if (args.length == 0) {
				p.sendMessage(getMsg("uso-correto"));
				return false;
			}
			Player t = Bukkit.getPlayerExact(args[0]);
			if (t == null) {
				p.sendMessage(getMsg("jogador-invalido"));
				return false;
			}
			p.sendMessage(getMsg("report-cabecalho").replace("@player", t.getName()));
			for (String reason : getConfig().getStringList("motivos")) {
				TextComponent cmp = new TextComponent(getMsg("report-motivo").replace("@motivo", reason));
				BaseComponent[] hover = new ComponentBuilder(getMsg("report-hover")
						.replace("@motivo", reason)
						.replace("@player", t.getName()))
						.create();
				cmp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
				cmp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dreport " + t.getName() + " " + reason));
				p.spigot().sendMessage(cmp);
			}
			p.sendMessage(getMsg("report-rodape"));
			
			reporting.add(p.getName());
			
		} else if (command.getName().equalsIgnoreCase("dreport")) {
			if (!(sender instanceof Player)) return false;
			if (args.length < 2) return false;
			Player p = (Player) sender;
			if (!(reporting.contains(p.getName()))) {
				p.sendMessage(getMsg("reportar-novamente"));
				return false;
			}
			Player t = Bukkit.getPlayerExact(args[0]);
			if (t == null) return false;
			
			String reason = "";
			for (int i = 1; i < args.length; i++) {
				reason += args[i] + " ";
			}
			reason = reason.trim();
			
			if (!(getConfig().getStringList("motivos").contains(reason))) return false;
			
			reporting.remove(p.getName());
			
			Report report = new Report(p.getName(), t.getName(), reason, new Date());
			boolean success = registerReport(report);
			if (success) {
				needUpdate = true;
				p.sendMessage(getMsg("sucesso-report").replace("@player", t.getName()));
			} else {
				p.sendMessage(getMsg("falha-report"));
			}
		} else if (command.getName().equalsIgnoreCase("reports")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("[EdmarReports] Comando apenas para jogadores in-game.");
				return false;
			}
			Player p = (Player) sender;
			if (!(p.hasPermission("edmarreports.staff"))) {
				p.sendMessage(getMsg("sem-permissao"));
				return false;
			}
			if (updating) {
				p.sendMessage(getMsg("atualizando"));
				return false;
			}
			if (needUpdate) {
				cache.clear();
				GUI.clear();
				updating = true;
				
				for (HumanEntity he : GUI.getViewers()) {
					he.closeInventory();
					he.sendMessage(getMsg("necessario-atualizar"));
				}
				
				new Thread() {
					@Override
					public void run() {
						ArrayList<Report> reports = getReports();
						if (reports.isEmpty()) {
							needUpdate = false;
							updating = false;
							p.sendMessage(getMsg("nenhum-report"));
							return;
						}
						for (int i = 0; i < reports.size(); i++) {
							Report r = reports.get(i);
							ItemStack skull = generateSkull(r);
							GUI.setItem(i, skull);
							cache.put(i, r);
						}
						needUpdate = false;
						updating = false;
						
						p.openInventory(GUI);
					}
				}.start();
			} else {
				p.openInventory(GUI);
			}
		}
		return true;
	}
	
	private boolean registerReport(Report r) {
		try {
			PreparedStatement st = conn.prepareStatement("INSERT INTO reports "
					+ "(player, reportado, motivo, data) VALUES (?, ?, ?, ?);");
			st.setString(1, r.getSender());
			st.setString(2, r.getReported());
			st.setString(3, r.getReason());
			st.setTimestamp(4, new Timestamp(r.getDate().getTime()));
			st.executeUpdate();
			return true;
		} catch (SQLException e) {
			getLogger().severe("*** Erro ao adicionar report: " + e.getMessage());
			return false;
		}
	}
	
	private ArrayList<Report> getReports() {
		ArrayList<Report> reports = new ArrayList<>();
		try {
			PreparedStatement st = conn.prepareStatement("SELECT * FROM reports ORDER BY data LIMIT " + GUI.getSize() + ";");
			ResultSet rs = st.executeQuery();
			
			while (rs.next()) {
				int id = rs.getInt("id");
				String player = rs.getString("player");
				String reported = rs.getString("reportado");
				String reason = rs.getString("motivo");
				Date date = new Date(rs.getTimestamp("data").getTime());
				
				Report report = new Report(player, reported, reason, date);
				report.setId(id);
				
				reports.add(report);
			}
		} catch (SQLException e) {
			getLogger().severe("*** Erro ao obter reports: " + e.getMessage());
		}
		return reports;
	}
	
	protected static void deleteReport(Report r) {
		try {
			PreparedStatement st = conn.prepareStatement("DELETE FROM reports WHERE id = ?;");
			st.setInt(1, r.getId());
			st.executeUpdate();
			needUpdate = true;
		} catch (SQLException e) {
			System.out.println("*** Erro ao deletar report: " + e.getMessage());
		}
	}
	
	protected static Report getReport(int slot) {
		return cache.get(slot);
	}
	
	@SuppressWarnings("deprecation")
	private ItemStack generateSkull(Report r) {
		ItemStack is = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
		SkullMeta sm = (SkullMeta) is.getItemMeta();
		sm.setDisplayName(getMsg("Cabecas.nome").replace("@player", r.getReported()));
		ArrayList<String> lore = new ArrayList<>();
		for (String line : getConfig().getStringList("Cabecas.lore")) {
			lore.add(line.replace('&', '\u00a7')
					.replace("@motivo", r.getReason())
					.replace("@reportador", r.getSender())
					.replace("@data", DATE_FORMAT.format(r.getDate()))
					.replace("@id", Integer.toString(r.getId())));
		}
		sm.setLore(lore);
		sm.setOwner(r.getReported());
		is.setItemMeta(sm);
		return is;
	}
	
	private ItemStack getItem(Material m, String display) {
		ItemStack is = new ItemStack(m);
		ItemMeta im = is.getItemMeta();
		im.setDisplayName(display);
		is.setItemMeta(im);
		return is;
	}
	
	private String getMsg(String key) {
		return getConfig().getString(key).replace('&', '\u00a7');
	}
	
}
