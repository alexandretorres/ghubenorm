package gitget;

public class Config {
	public String oauth;
	public Config(String oauth) {
		this.oauth=oauth;
	}
	private Config() {};
	public static Config getDefault() {
		Config config = new Config();
		config.oauth=Auth.getProperty("oauth");
		return config;
	}
}
