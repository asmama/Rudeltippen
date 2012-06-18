package interfaces;

public interface AppConstants {
	public static final String APPNAME = "rudeltippen";
	public static final String APIURL = "http://api.rudeltippen.de";
	public static final String CONFIRMATIONPATTERN = "\\w{8,8}-\\w{4,4}-\\w{4,4}-\\w{4,4}-\\w{12,12}";
	public static final String EMAILPATTERN = ".+@.+\\.[a-z]+";
	public static final String USERNAMEPATTERN = "[a-zA-Z0-9-_]+";
}