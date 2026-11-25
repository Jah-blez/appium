package Appium;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.DesiredCapabilities;

import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;

/**
 * Installs and launches the Africhange staging build (app-staging-release.apk, v3.7.2 / 212)
 * and runs the email + password + emailed-OTP login flow.
 *
 * Configuration (system property, then environment variable):
 *   app.apk.path   / APP_APK_PATH   - path to the APK
 *   login.email    / LOGIN_EMAIL    - account email (required)
 *   login.password / LOGIN_PASSWORD - account password (required)
 *   login.otp      / LOGIN_OTP      - emailed OTP; if unset, it is read from the console when needed
 *   login.pin      / LOGIN_PIN      - 6-digit app PIN, only used if a PIN screen appears after login
 *
 * The app is built with Flutter, so elements are located by their visible label
 * (text / content-desc) and by EditText position rather than by resource-id.
 */
public class AfrichangeTest {

	static final String DEFAULT_APK_PATH =
			"/tmp/claude-0/-home-user-appium/0ac09379-f1e3-5c07-894e-c7e5658da4f3/scratchpad/apk_download/app-staging-release.apk";

	static final int SHORT_WAIT = 5;
	static final int LONG_WAIT = 30;

	static AndroidDriver<MobileElement> driver;

	public static void main(String[] args) {
		try {
			openApp();
			login();
			System.out.println("LOGIN PASSED");
		} catch (Exception e) {
			System.out.println("LOGIN FAILED: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		} finally {
			if (driver != null) {
				driver.quit();
			}
		}
	}

	// ---------------------------------------------------------------- setup

	static String config(String property, String env, String fallback) {
		String value = System.getProperty(property);
		if (value == null || value.isEmpty()) {
			value = System.getenv(env);
		}
		return (value == null || value.isEmpty()) ? fallback : value;
	}

	static String required(String property, String env) {
		String value = config(property, env, null);
		if (value == null) {
			throw new IllegalStateException("Set " + env + " (or -D" + property + ")");
		}
		return value;
	}

	public static void openApp() throws Exception {

		DesiredCapabilities cap = new DesiredCapabilities();
		cap.setCapability("deviceName", "SMG610F");
		cap.setCapability("udid", "52036bc260e174e9");
		cap.setCapability("platformName", "Android");
		cap.setCapability("platformVersion", "8.1.0");
		cap.setCapability("automationName", "UiAutomator2");
		cap.setCapability("app", new File(config("app.apk.path", "APP_APK_PATH", DEFAULT_APK_PATH)).getAbsolutePath());
		cap.setCapability("appPackage", "com.africhange.Africhange.staging");
		cap.setCapability("appActivity", "com.africhange.Africhange.MainActivity");
		cap.setCapability("autoGrantPermissions", true);
		cap.setCapability("newCommandTimeout", 300);

		URL url = new URL("http://127.0.0.1:4723/wd/hub");

		driver = new AndroidDriver<MobileElement>(url, cap);
		driver.manage().timeouts().implicitlyWait(0, TimeUnit.SECONDS);

		System.out.println("Africhange staging app started....");
	}

	// ---------------------------------------------------------------- login flow

	public static void login() throws Exception {
		String email = required("login.email", "LOGIN_EMAIL");
		String password = required("login.password", "LOGIN_PASSWORD");

		// 1. Welcome / onboarding screen: go to login.
		WebElement loginEntry = waitForAny(LONG_WAIT, "Log in", "Login");
		if (loginEntry == null) {
			throw new IllegalStateException("Login entry button ('Log in' / 'Login') not found on launch screen");
		}
		loginEntry.click();
		System.out.println("Step 1: opened login screen");

		// 2. Email.
		List<MobileElement> fields = waitForFields(LONG_WAIT, 1);
		WebElement emailField = fieldByLabel(fields, "Email Address", "Email", "Enter your email address");
		type(emailField == null ? fields.get(0) : emailField, email);
		System.out.println("Step 2: entered email");

		boolean passwordOnSameScreen = fields.size() >= 2;
		if (passwordOnSameScreen) {
			// Email and password share a screen: password is the field after email.
			type(fields.get(1), password);
			System.out.println("Step 3: entered password (same screen as email)");
		}
		hideKeyboard();
		tap(SHORT_WAIT, "Continue", "Log in", "Login", "Next");

		// 3. Password + emailed OTP screen
		//    ("Enter your password and the code we emailed you to continue.").
		if (!passwordOnSameScreen) {
			waitForAny(LONG_WAIT, "Enter your password", "Password");
			List<MobileElement> pwFields = waitForFields(LONG_WAIT, 1);
			WebElement pwField = fieldByLabel(pwFields, "Password", "Enter your password");
			type(pwField == null ? pwFields.get(0) : pwField, password);
			System.out.println("Step 3: entered password");
		}

		// 4. OTP (sent to the account email).
		if (waitForAny(SHORT_WAIT, "code we emailed", "OTP", "6-digit code") != null) {
			enterCode(otp(), passwordOnSameScreen ? 0 : 1);
			System.out.println("Step 4: entered OTP");
		}
		hideKeyboard();
		tap(SHORT_WAIT, "Continue", "Verify", "Submit", "Log in", "Login");

		// 5. Optional post-login prompts.
		handlePostLoginPrompts();

		// 6. Assert we reached the home screen.
		if (waitForAny(LONG_WAIT, "Send Money", "Send money", "Home", "Wallet", "Transactions") == null) {
			throw new IllegalStateException("Home screen not reached after login");
		}
		System.out.println("Step 6: home screen visible");
	}

	static void handlePostLoginPrompts() {
		for (int i = 0; i < 3; i++) {
			if (waitForAny(SHORT_WAIT, "Enter your 6-digit PIN", "Create PIN") != null) {
				String pin = required("login.pin", "LOGIN_PIN");
				enterCode(pin, 0);
				System.out.println("Step 5: entered PIN");
				continue;
			}
			WebElement dismiss = waitForAny(SHORT_WAIT, "Not now", "Skip");
			if (dismiss != null) {
				dismiss.click();
				System.out.println("Step 5: dismissed prompt (biometric / passkey)");
				continue;
			}
			return;
		}
	}

	static String otp() {
		String code = config("login.otp", "LOGIN_OTP", null);
		if (code != null) {
			return code;
		}
		System.out.print("Enter the OTP emailed to the account: ");
		return new Scanner(System.in).nextLine().trim();
	}

	// ---------------------------------------------------------------- helpers

	/** Matches an element whose text, content-desc (first line) or hint equals or contains the label. */
	static By label(String text) {
		String t = xpathLiteral(text);
		return By.xpath("//*[@text=" + t + " or @content-desc=" + t
				+ " or starts-with(@content-desc, concat(" + t + ", '\n'))"
				+ " or contains(@text, " + t + ") or contains(@content-desc, " + t + ")]");
	}

	static String xpathLiteral(String s) {
		return s.contains("'") ? "\"" + s + "\"" : "'" + s + "'";
	}

	/** Polls until any of the labels is visible; returns the first match or null on timeout. */
	static WebElement waitForAny(int seconds, String... labels) {
		long end = System.currentTimeMillis() + seconds * 1000L;
		do {
			for (String l : labels) {
				List<MobileElement> found = driver.findElements(label(l));
				for (MobileElement e : found) {
					if (e.isDisplayed()) {
						return e;
					}
				}
			}
			sleep(500);
		} while (System.currentTimeMillis() < end);
		return null;
	}

	static void tap(int seconds, String... labels) {
		WebElement e = waitForAny(seconds, labels);
		if (e == null) {
			throw new IllegalStateException("None of these buttons found: " + String.join(", ", labels));
		}
		e.click();
	}

	static List<MobileElement> waitForFields(int seconds, int min) {
		long end = System.currentTimeMillis() + seconds * 1000L;
		List<MobileElement> fields;
		do {
			fields = driver.findElements(By.className("android.widget.EditText"));
			if (fields.size() >= min) {
				return fields;
			}
			sleep(500);
		} while (System.currentTimeMillis() < end);
		throw new IllegalStateException("Expected at least " + min + " input field(s), found " + fields.size());
	}

	static WebElement fieldByLabel(List<MobileElement> fields, String... labels) {
		for (MobileElement f : fields) {
			String desc = String.valueOf(f.getAttribute("content-desc"));
			String text = String.valueOf(f.getText());
			String hint = String.valueOf(f.getAttribute("hint"));
			for (String l : labels) {
				if (desc.contains(l) || text.contains(l) || hint.contains(l)) {
					return f;
				}
			}
		}
		return null;
	}

	static void type(WebElement field, String value) {
		field.click();
		field.clear();
		field.sendKeys(value);
	}

	/**
	 * Enters a numeric code. Flutter code inputs are either one EditText or one box per digit
	 * that auto-advances focus, so tap the first box and type through the keyboard.
	 */
	static void enterCode(String code, int firstFieldIndex) {
		List<MobileElement> fields = waitForFields(LONG_WAIT, firstFieldIndex + 1);
		fields.get(firstFieldIndex).click();
		new Actions(driver).sendKeys(code).perform();
	}

	static void hideKeyboard() {
		try {
			if (driver.isKeyboardShown()) {
				driver.hideKeyboard();
			}
		} catch (Exception ignored) {
			// Keyboard already hidden or not supported.
		}
	}

	static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
