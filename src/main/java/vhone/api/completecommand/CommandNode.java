package vhone.api.completecommand;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.entity.Player;

import vhone.api.exception.InvalidAliasException;

public class CommandNode implements Iterable<CommandNode> {
	
	// 로거
	final static CompleteCommand.Logger logger = new CompleteCommand.Logger();
	
	// 기본값
	final private String key;
	final private Object argument;
	final private CommandNode parent;
	final private List<CommandNode> children;
	
	// 분기 설정
	private boolean mark = false;
	
			
	// 생성자
	private CommandNode() {
		this.key = null;
		this.argument = null;
		this.parent = null;
		this.children = new LinkedList<CommandNode>();
	}
	public CommandNode(String label) {
		this.key = label;
		this.argument = null;
		this.parent = null;
		this.children = new LinkedList<CommandNode>();
	}
	public CommandNode(String key, Object argument, CommandNode parent) {
		this.key = key;
		this.argument = argument;
		this.parent = parent;
		if (parent.children != null) parent.children.add(this);
		this.children = new LinkedList<CommandNode>();
	}



	public class Footer extends CommandNode {
		
		// 말단 노드는 함수와 변수를 반드시 가져야함.
		private Function function;
		private String discription; // 설명
		
		// 생성자
		private Footer() {
			super();
		}
		public Footer(final String key, final Object argument, final CommandNode parent) {
			super(key, argument, parent);
		}
		
		//함수 실행
		public void run(Player player, Command cmd, Object[] args) {
			try {
				this.function.run(player, cmd, args);
			} catch (Exception e) {
				if (player != null) {
					player.sendMessage(e.getClass().getSimpleName());
					player.sendMessage("- 잘못된 명령입니다. Exception Message : " + e.getMessage());	
				}
			}
		}

		// 함수를 추가한다.
		public Footer setFunction(final Function function) {
			this.function = function;
			return this;
		}
		
		// 설명을 추가한다.
		public Footer setDiscription(final String discriptions) {
			this.discription = discriptions;
			return this;
		}
		public String getdiscription() {
			return (this.discription != null) ? this.discription : ""; 
		}
	}
	
	
	
	
	/*
	 * 상태 확인
	 */
	
	// 부모노드가 없는 노드 (해더)
	public boolean isHeader() {
		return (this.parent == null);
	}
	// 함수를 가지고 있는 노드 (말단)
	public boolean isFooter() {
		return (this instanceof Footer);
	}
	// 분기를 가진 노드
	public boolean hasMark() {
		return this.mark;
	}
	// 정규식 선택지 : 타입을 포함 
	public boolean isType() {
		return (this.argument instanceof Type);
	}
	// 다항 선택지 : 이넘을 포함 
	public boolean isEnum() {
		return (this.argument instanceof Enum[]);
	}
	// 단항 선택지: 일반 노드
	public boolean isString() {
		return (this.argument instanceof String);
	}
	// 다항 선택지 : 다항 노드
	public boolean isArray() {
		return (this.argument instanceof String[]);
	}
	// 단항 선택지 : 별칭 노드
	public boolean isObject() {
		return (this.argument.getClass().equals(Object.class));
	}
	
	
	
	
	/*
	 * 키 받아오기
	 */
	
	// 현재 노드의 전체 키를 리턴한다.
	public String getKey() {
		List<String> parent_path = getParentPath(this);
		parent_path.add(this.key);
		return margePath(parent_path);
	}
	//현재 노드의 키를 리턴한다.
	public String getSimpleKey() {
		return this.key;
	}
	// 자신을 제외한 부모의 노드를 리턴한다.
	public String getPath() {
		return margePath(getParentPath(this));
	}
	
	// 부모의 노드만 역순으로 넣는다.
	private List<String> getParentPath(CommandNode node) {
		List<String> list = new ArrayList<String>();
		while (!node.isHeader()) {
			node = node.getParent();
			list.add(0, node.getSimpleKey());
		}
		return list;
	}
	// 글자를 합친다
	private String margePath(List<String> list) {
		StringBuilder result = new StringBuilder();
		for (int i=0; i < list.size(); i++) result.append(list.get(i) + ".");
		return result.deleteCharAt(result.length()-1).toString();
	}
	
	
	
	
	/*
	 *  노드 받아오기
	 */
	
	// 부모 노드 리턴
	public CommandNode getParent() {
		return this.parent;
	}
	// key에 해당하는 노드를 불러온다.
	public CommandNode getNode(final String key) {
		String[] split = key.split("\\.");
		CommandNode node = this;
		try {
	
			for (String s : split) {
				boolean pass = false;
				for(Iterator<CommandNode> iter = node.iterator(); iter.hasNext();) {
					CommandNode n = iter.next();
					if (n.getSimpleKey().equals(s)) {
						node = n;
						pass = true;
						break;
					}
				}
				if (pass==false) {
					throw new InvalidAliasException("'" + s + "' not found.");
				}
			}
			if (split[split.length-1].equals(node.getSimpleKey())) {
				return node;
			} else {
				throw new InvalidAliasException("'" + key + "' not found.");
			}
		} catch (InvalidAliasException e) {
			logger.errer(e);
			return null;
		}
	}
	
	
	

	/*
	 * 노드 추가
	 */
	
	// 자식 노드를 추가한다.
	public CommandNode add(final String argument) {
		return createNode(argument, argument, this);
	}
	public CommandNode add(final Type argument) {
		return createNode(argument.toString(), argument, this);
	}
	public CommandNode add(final Enum<?>[] argument) {
		if (checkKeyArray(argument)) {
			String key = "["+argument.getClass().getComponentType().getSimpleName()+"]";
			return createNode(key, argument, this);
		}
		return new CommandNode();
	}
	public CommandNode add(final Aliasable argument) {
		String k = argument.toString();
		Object o = argument.getData();
		if (argument instanceof AliasArray) {
			AliasArray alias_array = (AliasArray) argument;
			if (checkKeyArray(alias_array.getData()))
				return createNode(k, o, this);
		} else {
			try {
				if (o == null) throw new InvalidAliasException("Data is null. -> Aliasable(" + k + ")");
				return createNode(k, o, this);
			} catch (InvalidAliasException e) {
				logger.errer(e);
			}
		}
		return new CommandNode();
	}

	// Footer 연결
	public Footer addFooter(final String argument) {
		return createFooter(argument, argument, this);
	}
	public Footer addFooter(final Type argument) {
		return createFooter(argument.toString(), argument, this);
	}
	public Footer addFooter(final Enum<?>[] argument) {
		if (checkKeyArray(argument)) {
			String key = "["+argument[0].getClass().getSimpleName()+"]";
			return createFooter(key, argument, this);
		}
		return new Footer();
	}
	public Footer addFooter(final Aliasable argument) {
		String k = argument.toString();
		Object o = argument.getData();
		if (argument instanceof AliasArray) {
			AliasArray alias_array = (AliasArray) argument;
			if (checkKeyArray(alias_array.getData()))
				return createFooter(k, o, this);
		} else {
			try {
				if (o == null) throw new InvalidAliasException("Data is null. -> Aliasable(" + k + ")");
				return createFooter(k, o, this);
			} catch (InvalidAliasException e) {
				logger.errer(e);
			}
		}
		return new Footer();
	}

	// 노드 생성 : 노드 생성을 실패하는 경우 빈 객체 리턴 ( NullPointerException 방지 )
	private CommandNode createNode(final String key, Object data, final CommandNode parent) {
		if (checkNodeKey(key)) return new CommandNode(key, data, parent);
		else return new CommandNode();
	}
	private Footer createFooter(final String key, Object data, final CommandNode parent) {
		if (checkNodeKey(key)) return new Footer(key, data, parent);
		else return new CommandNode.Footer();
	}
	
	// 키 체크
	private boolean checkNodeKey(String key) {

		try {
			
			// 키값이 없으면 오류
			if (key == null || key.isEmpty())
					throw new InvalidAliasException("Invalid Node key.");
	
			// 빈 객체가 아니고, 자식 노드 중에 같은 키가 있으면 오류
			if (this.children != null)
				for(CommandNode n : this.children)
					if (n.getSimpleKey().equalsIgnoreCase(key))
						throw new InvalidAliasException("Key has already been added. -> " + n.getKey());
			return true;

		} catch (InvalidAliasException e) {
			logger.errer(e);
			return false;
		}
	}
	
	// 다항 키 체크
	private boolean checkKeyArray(Object[] objects) {
		for (Iterator<CommandNode> iter = this.iterator(); iter.hasNext();) {
			CommandNode node = iter.next();
			if (node.isArray()) {
				for (String s : (String[]) node.getArgument()) {
					if (s.equalsIgnoreCase(objects.toString()))
						return false;
				}
			} else {
				if (node.getSimpleKey().equalsIgnoreCase(objects.toString()))
					return false;	
			}
		}
		return true;
	}
	
	
	
	
	
	/*
	 * 노드 이동
	 */
	
	// Retace의 도착지를 만든다.
	public CommandNode mark() {
		this.mark = true;
		return this;
	}
	// Mark된 노드까지 거슬러 올라간다.
	public CommandNode retrace() {
		CommandNode node = this;
		if (!node.isHeader() && node.hasMark()) node = node.parent;
		while (!node.isHeader() && !node.hasMark()) node = node.parent;
		return node;
	}
	
	
	
	
	/*
	 * 이터레이터 구현
	 */
	
	// 이터레이터
	public Iterator<CommandNode> iterator() {
		return children.iterator();
	}
	
	
	
	
	/*
	 * 손버깅할때 노드 구조 확인하기 ㅠㅠ
	 */
	
	// 모든 커맨드 출력하기
	public void printCommand() {
		printCommand(false);
	}
	public void printCommand(final boolean isDeep) {
		if (this.isFooter()) {
			Footer footer = (Footer) this;
			System.out.println(" - " + footer.getKey() + " : " + footer.getdiscription());	
			if (isDeep) {
				System.out.println("깊게 출력하기 : Footer 정보 보여줘야함.");
			}
		} else {
			for(Iterator<CommandNode> iter = this.iterator(); iter.hasNext();) {
				iter.next().printCommand(false);
			}
		}
	}
	// 해당 노드의 Argument출력
	public void printArgument() {
		System.out.println("========================================");
		System.out.println("Node: " + this.getSimpleKey());
		System.out.println("Argument: " + this.argument.getClass().getName());
	}
	
	public List<String> getHelpList(){
		List<String> list = new ArrayList<String>();
		if (this.isFooter()) {
			Footer footer = (Footer) this;
			list.add(" - /" + footer.getKey().replace('.', ' ') + " : " + footer.getdiscription());
		} else {
			for(Iterator<CommandNode> iter = this.iterator(); iter.hasNext();) {
				list.addAll(iter.next().getHelpList());
			}
		}
		return list;
	}
	
	
	
	
	/*
	 * CompleteCommand에서 사용할 메소드
	 */
	
	Object getArgument() {
		return this.argument;
	}
	
	
	
	
}
