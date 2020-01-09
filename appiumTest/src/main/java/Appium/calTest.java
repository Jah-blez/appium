package Appium;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.openqa.selenium.By;
import org.openqa.selenium.remote.DesiredCapabilities;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;

public class calTest {

	static AppiumDriver<MobileElement> driver;
	public static void main(String[] args) {

try {
	openCal();
} catch (Exception e) {
	System.out.println(e.getMessage());
	System.out.println(e.getCause());
	e.printStackTrace();
}
	}

	public static void openCal() throws Exception {

		DesiredCapabilities cap = new DesiredCapabilities();
		cap.setCapability("deviceName", "SMG610F");
		cap.setCapability("udid", "52036bc260e174e9");
		cap.setCapability("platformName", "Android");
		cap.setCapability("platformVersion", "8.1.0");
		cap.setCapability("appPackage", "com.sec.android.app.popupcalculator");
		cap.setCapability("appActivity", "com.sec.android.app.popupcalculator.Calculator");
		
		URL url = new URL("http://127.0.0.1:4723/wd/hub");
		
		driver = new AppiumDriver<MobileElement>(url, cap);
		
		System.out.println("Application Started....");
		
		MobileElement nine = driver.findElement(By.id("com.sec.android.app.popupcalculator:id/bt_09"));
		MobileElement mul = driver.findElement(By.id("com.sec.android.app.popupcalculator:id/bt_mul"));
		MobileElement eight = driver.findElement(By.id("com.sec.android.app.popupcalculator:id/bt_08"));
		MobileElement equals = driver.findElement(By.id("com.sec.android.app.popupcalculator:id/bt_equal"));
		MobileElement result = driver.findElement(By.id("com.sec.android.app.popupcalculator:id/txtCalc"));
		
		nine.click();
		mul.click();
		eight.click();
		equals.click();
		
		String res= result.getText();
		
		System.out.println("The result is:\n" + res);
		
	}

}
