package vhone.api.completecommand;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public abstract class CompleteCommand implements CommandExecutor, TabCompleter {

	/*
	 * Logger
	 */
	public static class Logger {
		
		final java.util.logging.Logger logger;
		
		public Logger() {
			this.logger = java.util.logging.Logger.getLogger("CompleteCommand");
		}
		
		public void errer(Throwable e) {
			logger.log(java.util.logging.Level.SEVERE, "", e);
		}
		public void info(String msg) {
			logger.info(msg);
		}
			
	}
	
	/*
	 * CommandNode 받아오기 
	 */
	abstract protected CommandNode createCommandNode();
	final private CommandNode node;
	private int node_amount;
	private int help_page;
	
	{
		this.node = createCommandNode();
		this.node_amount = this.node.getHelpList().size();
		this.help_page = (int)Math.ceil( (double)this.node_amount/10 );
	}
	
	/*
	 * 플러그인 세팅
	 */
	
	

	@Override
	final public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
		
		// 플레이어 세팅
		Player player = null;
		if (sender instanceof Player) player = (Player) sender;
		
		//
		List<Object> arg_list = new ArrayList<Object>();
		
		//
		CommandNode node = this.node;
		int size = args.length;
		
		// 입력값 루프
		for (int i=0; i<size; i++) {
			String arg = args[i];
			
			// 자식노드 루프
			for(Iterator<CommandNode> iter = node.iterator(); iter.hasNext();) {
				CommandNode cn = iter.next();
				Object obj = getArgument(cn, arg);
				
				// 자식노드에서 데이터를 찾으면 다음 자식으로 넘어간다.
				if (obj != null) {
					arg_list.add(obj);
					node = cn;
					break;
				}
			}
		}
		
		// Footer까지 찾았으면 명령 실행
		if (node instanceof CommandNode.Footer) {
			((CommandNode.Footer) node).run(player, command, arg_list.toArray());
			
		// 못찾으면 메세지
		} else {
			System.out.println(args.length > 0);
			if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
				if (args.length == 1) {
					sendHelper(player, 0, this.node.getHelpList());
					
				} else if (Pattern.matches(Type.INTEGER.getRegex(), args[1])) {
					int page = Integer.valueOf(args[1]);
					if (0 < page && page < this.help_page+1 ) {
						sendHelper(player, page, this.node.getHelpList());
					} else {
						player.sendMessage("없는 페이지");
					}
					
				}
			} else {
				if (player != null) player.sendMessage("◇ 잘못된 명령어 입니다.");
			}
		}
		
		return true;
	}
	
	
	
	private void sendHelper(Player player, int page, List<String> list) {
		System.out.println("아");
		int size = 10;
		int start = (page-1)*size;
		int list_size = list.size();
		
		if (list_size < start) return;
		
		int end = (list_size < start+size) ? list_size : start+size;
		
		for (int i=start; i<end; i++) player.sendMessage(list.get(i));
	}



	@Override
	final public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		
		List<String> result = new ArrayList<String>();
		
		//
		CommandNode node = this.node;
		int count = 0;
		int size = args.length;
		
		// 입력값 루프
		for (int i=0; i<size; i++) {
			String arg = args[i];

			// 노드와 입력값의 위치가 같지 않으면 실패
			if (count != i) break;
			
			// 자식노드 루프
			for(Iterator<CommandNode> iter = node.iterator(); iter.hasNext();) {
				CommandNode cn = iter.next();
				
				// 자식노드와 일치하면 카운트를 하고 다음 자식으로 넘어감.
				// 자식노드와 불일치하면 카운트를 하지않음.
				if (checkAgumentKey(cn, arg)) {
					node = cn;
					count++;
					break;
				}
			}

		}
		
		if (count == size-1) result = getArgumentKey(node, args[count]);
		
		if (size == 1
				&& !result.contains("help"))
			result.add("help");
		if (size == 2
				&& args[0].equalsIgnoreCase("help")
				&& !result.contains("<Integer>")
				&& Pattern.matches(Type.INTEGER.getRegex(), args[1])) {
			for (int i=0; i<this.help_page; i++) {
				result.add(String.valueOf(i+1));
			}
		}
		
		return result;
	}


	private List<String> getArgumentKey(CommandNode node, String arg) {
		
		List<String> result = new ArrayList<String>();
		String low_arg = arg.toLowerCase();
	
		// 모든 자식 노드를 가져온다.
		for(Iterator<CommandNode> iter = node.iterator(); iter.hasNext();) {
			CommandNode cn = iter.next();
			
			// 노드의 인수를 가져온다.
			Object obj = cn.getArgument();
	
			// 인수가 단항인 경우 : 노드키가 입력값을 포함하면 노드키를 넘기면 된다.
			// 인수가 오브젝트인 경우 : 노드키가 입력값을 포함하면 노드키를 넘기면 된다.
			if (cn.isString() || cn.isObject()) {
				String k = cn.getSimpleKey();
				if (cn.getSimpleKey().toLowerCase().contains(low_arg)) {
					result.add(k);
					continue;
				}
				
			// 인수가 타입인 경우 : 정규식이 맞으면 타입키를 넘기면 된다.
			} else if (cn.isType()) {
				Type type = (Type) obj;
				if (Pattern.matches(type.getRegex(), low_arg)) {
					result.add(type.toString());
					continue;
				}
			
			// 인수가 다항인 경우 : 다항을 루프해서 입력값을 포함하는 다항키만 넘기면 된다.
			// 인수가 이넘인 경우 : 입력값을 포함하는 이넘키만 넘기면 된다.
			} else if (cn.isArray() || cn.isEnum()) {
				Object[] array = (Object[]) obj;
				for (Object o : array) {
					String k = o.toString();
					if (k.toLowerCase().contains(low_arg)) {
						result.add(k);
						continue;
					}
				}
			}
		}	
		return result;
	}


	// 입력값 중에 현재 입력값을 제외한 이전 입력값을 비교함.
	private boolean checkAgumentKey(CommandNode node, String arg) {

		Object obj = node.getArgument();
		
		// 체크
		Boolean pass = false;

		// 인수가 단항인 경우 : 노드키가 입력값을 포함하면 노드키를 넘기면 된다.
		// 인수가 오브젝트인 경우 : 노드키가 입력값을 포함하면 노드키를 넘기면 된다.
		if (node.isString() || node.isObject()) {
			if (node.getSimpleKey().equalsIgnoreCase(arg)) {
				pass = true;
			}
			
		// 인수가 타입인 경우 : 정규식이 맞으면 타입키를 넘기면 된다.
		// 다른 타입에선 ""를 비교하지 않지만, 정규식에서는 ""를 피해야한다.
		} else if (node.isType()) {
			Type type = (Type) obj;
			if (!arg.isEmpty() && Pattern.matches(type.getRegex(), arg)) {
				pass = true;
			}
		
		// 인수가 다항인 경우 : 다항을 루프해서 입력값을 포함하는 다항키만 넘기면 된다.
		// 인수가 이넘인 경우 : 입력값을 포함하는 이넘키만 넘기면 된다.
		} else if (node.isArray() || node.isEnum()) {
			Object[] array = (Object[]) obj;
			for (Object o : array) {
				String k = o.toString();
				if (k.toLowerCase().equalsIgnoreCase(arg)) {
					pass = true;
					break;
				}
			}
		}
		
		return pass;
	}

	
	private Object getArgument(CommandNode node, String arg) {

		Object argument = null;

		Object obj = node.getArgument();

		// 인수가 단항인 경우 : 노드키가 입력값을 포함하면 노드키를 넘기면 된다.
		// 인수가 오브젝트인 경우 : 노드키가 입력값을 포함하면 노드키를 넘기면 된다.
		if (node.isString() || node.isObject()) {
			String k = node.getSimpleKey();
			if (k.equalsIgnoreCase(arg)) {
				argument = k;
			}
			
		// 인수가 타입인 경우 : 정규식이 맞으면 타입키를 넘기면 된다.
		} else if (node.isType()) {
			Type type = (Type) obj;
			if (Pattern.matches(type.getRegex(), arg)) {
				argument = type.parsedValue(arg);
			}
		
		// 인수가 다항인 경우 : 다항을 루프해서 입력값을 포함하는 다항키만 넘기면 된다.
		// 인수가 이넘인 경우 : 입력값을 포함하는 이넘키만 넘기면 된다.
		} else if (node.isArray() || node.isEnum()) {
			Object[] array = (Object[]) obj;
			for (Object o : array) {
				String k = o.toString();
				if (k.toLowerCase().contains(arg)) {
					argument = o;
				}
			}
		}
		
		return argument;
	}
	
}


