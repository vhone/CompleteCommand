package vhone.api.completecommand;

import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public interface Function {
	public void run(Player player, Command command, Object[] args) throws Exception;
}
