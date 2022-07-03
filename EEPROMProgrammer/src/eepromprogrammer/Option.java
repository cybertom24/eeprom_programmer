package eepromprogrammer;

import java.util.stream.Collectors;
import java.util.stream.Stream;

final class Option {
	
	public static final String ACTION_READ = "read";
	public static final String ACTION_WRITE = "write";
	public static final String ACTION_EVERY = "every";
	
	public static final String MODE_SINGLE = "s";
	public static final String MODE_MULTIPLE = "m";
	
	private String path;
	private String port;
	private String action;
	private String mode;

	public Option(String port, String path, String action, String mode) {
		this.port = port;
		this.path = path;
		this.action = action;
		this.mode = mode;
	}
	
	public void overrideUndefined(Option overrider) {
		if(port == null)
			port = overrider.getPort();
		if(path == null)
			path = overrider.getPath();
		if(action == null)
			action = overrider.getAction();
		if(mode == null)
			mode = overrider.getMode();
	}
	
	public boolean isAction(String which) {
		return (which.compareToIgnoreCase(action) == 0);
	}
	
	public boolean isMode(String which) {
		return (which.compareToIgnoreCase(mode) == 0);
	}
	
	public String getPath() {
		return path;
	}

	public String getPort() {
		return port;
	}

	public String getAction() {
		return action;
	}
	
	public String getMode() {
		return mode;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public void setAction(String action) {
		this.action = action;
	}
	
	public void setMode(String mode) {
		this.mode = mode;
	}

	public static Option parseArguments(String[] args) {
		String port = getParameter(args, "-port=");
		String path = getParameter(args, "-path=");
		String action = getParameter(args, "-action=");
		String mode = getParameter(args, "-mode=");
		
		return new Option(port, path, action, mode);
	}
	
	private static String getParameter(String[] array, String parameterStart) {
		String result = null;
		
		try {
			String res = (String) Stream.of(array).filter(str -> str.startsWith(parameterStart))
					.collect(Collectors.toSet()).toArray()[0];
			
			result = res.split(parameterStart)[1];
		} catch (IndexOutOfBoundsException e) {
			//e.printStackTrace();
		}
		
		return result;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("Options:\n-port = ");

		if (port == null)
			builder.append("undefined");
		else
			builder.append(port);
		
		builder.append("\n-path = ");
		if (path == null)
			builder.append("undefined");
		else
			builder.append(path);
		
		builder.append("\n-action = ");
		if (action == null)
			builder.append("undefined");
		else
			builder.append(action);
		
		builder.append("\n-mode = ");
		if (mode == null)
			builder.append("undefined");
		else
			builder.append(mode);
		
		return builder.toString();
	}
}
