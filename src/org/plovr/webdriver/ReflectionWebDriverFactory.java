package org.plovr.webdriver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import com.google.common.base.Preconditions;

public final class ReflectionWebDriverFactory implements WebDriverFactory {

  private final String webDriverClassName;

  /**
   * @param webDriverClassName a fully-qualified class name of a class that
   *     implements {@link WebDriver}, such as
   *     "org.openqa.selenium.firefox.FirefoxDriver" or
   *     "org.openqa.selenium.htmlunit.HtmlUnitDriver"
   */
  public ReflectionWebDriverFactory(String webDriverClassName) {
    Preconditions.checkNotNull(webDriverClassName);
    this.webDriverClassName = webDriverClassName;
  }

  @Override
  public WebDriver newInstance() {
    try {
      @SuppressWarnings("unchecked")
      Class<? extends WebDriver> clazz = (Class<? extends WebDriver>)Class.
          forName(webDriverClassName);
      WebDriver webDriver = clazz.newInstance();

      if (webDriver instanceof HtmlUnitDriver) {
        // TODO: Suppress the
        // "WARNING: Obsolete content type encountered: 'text/javascript'"
        // junk that HtmlUnit spits out because it is cluttering up the test
        // output.
        ((HtmlUnitDriver)webDriver).setJavascriptEnabled(true);
      }
      return webDriver;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
