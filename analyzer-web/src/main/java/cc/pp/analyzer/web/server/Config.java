package cc.pp.analyzer.web.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Config {

	private static Logger logger = LoggerFactory.getLogger(Config.class);

	static Properties conf = new Properties() {

		private static final long serialVersionUID = -2493912332089543431L;

		{
			try (InputStream in = Config.class.getClassLoader().getResourceAsStream("XXXX.properties");) {
				this.load(in);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

	};

	public static String get(String key) {
		return conf.getProperty(key);
	}

	public static Properties getProps(String confFileName) {
		Properties result = new Properties();
		logger.info("Load resource: " + confFileName);
		try (InputStream in = Config.class.getClassLoader().getResourceAsStream(confFileName);) {
			result.load(in);
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
