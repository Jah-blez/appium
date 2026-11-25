package Appium;

import java.io.File;
import java.net.URL;

import org.openqa.selenium.remote.DesiredCapabilities;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;

/**
 * Installs and launches the Africhange staging build (app-staging-release.apk, v3.7.2 / 212).
 *
 * The APK path can be overridden with the APP_APK_PATH environment variable
 * or the -Dapp.apk.path system property.
 */
public class AfrichangeTest {

	static final String DEFAULT_APK_PATH =
			"/tmp/claude-0/-home-user-appium/0ac09379-f1e3-5c07-894e-c7e5658da4f3/scratchpad/apk_download/app-staging-release.apk";

	static AppiumDriver<MobileElement> driver;

	public static void main(String[] args) {
		try {
			openApp();
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.out.println(e.getCause());
			e.printStackTrace();
		} finally {
			if (driver != null) {
				driver.quit();
			}
		}
	}

	static String resolveApkPath() {
		String path = System.getProperty("app.apk.path");
		if (path == null || path.isEmpty()) {
			path = System.getenv("APP_APK_PATH");
		}
		if (path == null || path.isEmpty()) {
			path = DEFAULT_APK_PATH;
		}
		return new File(path).getAbsolutePath();
	}

	public static void openApp() throws Exception {

		DesiredCapabilities cap = new DesiredCapabilities();
		cap.setCapability("deviceName", "SMG610F");
		cap.setCapability("udid", "52036bc260e174e9");
		cap.setCapability("platformName", "Android");
		cap.setCapability("platformVersion", "8.1.0");
		cap.setCapability("automationName", "UiAutomator2");
		cap.setCapability("app", resolveApkPath());
		cap.setCapability("appPackage", "com.africhange.Africhange.staging");
		cap.setCapability("appActivity", "com.africhange.Africhange.MainActivity");
		cap.setCapability("autoGrantPermissions", true);
		cap.setCapability("newCommandTimeout", 300);

		URL url = new URL("http://127.0.0.1:4723/wd/hub");

		driver = new AppiumDriver<MobileElement>(url, cap);

		System.out.println("Africhange staging app started....");
	}

}
