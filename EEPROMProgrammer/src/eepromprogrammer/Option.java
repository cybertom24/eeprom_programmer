package eepromprogrammer;

import java.util.stream.Collectors;
import java.util.stream.Stream;

final class Option {
	
	public static final String ACTION_READ = "read";
	public static final String ACTION_WRITE = "write";
	public static final String ACTION_EVERY = "every";
	
	private String path;
	private String port;
	private String action;
	private boolean validate;

	public Option(String port, String path, String action, boolean validate) {
		this.port = port;
		this.path = path;
		this.action = action;
		this.validate = validate;
	}
	
	public void overrideUndefined(Option overrider) {
		if(port == null)
			port = overrider.getPort();
		if(path == null)
			path = overrider.getPath();
		if(action == null)
			action = overrider.getAction();
	}
	
	public boolean isAction(String which) {
		return (which.compareToIgnoreCase(action) == 0);
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

	public boolean getValidate() {
		return validate;
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

	public void setValidate(boolean validate) {
		this.validate = validate;
	}

	public static Option parseArguments(String[] args) {
		String port = getParameter(args, "-port=");
		String path = getParameter(args, "-path=");
		String action = getParameter(args, "-action=");
		boolean validate = !isParameterPresent(args, "-novalid");
		
		return new Option(port, path, action, validate);
	}
	
	private static String getParameter(String[] array, String parameterStart) {
		String result = null;
		
		try {
			String res = (String) Stream.of(array).filter(str -> str.startsWith(parameterStart))
					.collect(Collectors.toSet()).toArray()[0];
			
			result = res.split(parameterStart)[1];
		} catch (IndexOutOfBoundsException e) {
			System.out.println("> No \"" + parameterStart + "\" argument found");
			//e.printStackTrace();
		}
		
		return result;
	}

	private static boolean isParameterPresent(String[] array, String parameter) {
		// If the parameter is present the size will be greater than 0
		return Stream.of(array).filter(str -> str.startsWith(parameter))
					.collect(Collectors.toSet()).size() > 0;
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

		builder.append("\n-validate = ");
		builder.append(validate);
		
		return builder.toString();
	}
}
