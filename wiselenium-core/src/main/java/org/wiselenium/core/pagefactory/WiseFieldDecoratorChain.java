package org.wiselenium.core.pagefactory;

import static org.wiselenium.core.pagefactory.AnnotationUtils.isAnnotationPresent;
import static org.wiselenium.core.pagefactory.ClasspathUtils.findImplementationClass;

import java.lang.reflect.Field;

import net.sf.cglib.proxy.Enhancer;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.WrapsElement;
import org.openqa.selenium.support.pagefactory.ElementLocator;
import org.openqa.selenium.support.pagefactory.ElementLocatorFactory;

/**
 * Class responsible for decorating WebElements into Fields.
 * 
 * @author Andre Ricardo Schaffer
 * @since 0.0.1
 */
class WiseFieldDecoratorChain extends ExtendedDefaultSeleniumDecoratorChain {
	
	WiseFieldDecoratorChain(ElementLocatorFactory factory) {
		super(factory);
	}
	
	private static Enhancer createEnhancer(WebElement webElement, Class<?> implentationClass) {
		Enhancer e = new Enhancer();
		e.setSuperclass(implentationClass);
		e.setInterfaces(new Class[] { WrapsElement.class });
		e.setCallback(WiseElementProxy.getInstance(webElement));
		
		return e;
	}
	
	private static Object createInstanceWithEmptyConstructor(WebElement webElement,
		Class<?> implentationClass) {
		Enhancer e = createEnhancer(webElement, implentationClass);
		return e.create();
	}
	
	private static Object createInstanceWithWebElementConstructor(WebElement webElement,
		Class<?> implentationClass) {
		
		try {
			return implentationClass.getConstructor(WebElement.class).newInstance(webElement);
		} catch (Exception e) {
			throw new ClassWithoutConstructorWithWebElementException(e);
		}
	}
	
	@Override
	protected Object decorateField(ClassLoader loader, Field field, ElementLocator locator) {
		WebElement webElement = this.proxyForLocator(loader, locator);
		return this.decorate(field.getType(), webElement);
	}
	
	@Override
	protected Object decorateWebElement(Class<?> clazz, WebElement webElement) {
		Class<?> implentationClass = findImplementationClass(clazz);
		try {
			return createInstanceWithWebElementConstructor(webElement, implentationClass);
		} catch (ClassWithoutConstructorWithWebElementException e) {
			return createInstanceWithEmptyConstructor(webElement, implentationClass);
		}
	}
	
	@Override
	protected boolean shouldDecorate(Class<?> clazz) {
		// TODO decorate lists of fields as well
		return isAnnotationPresent(clazz, org.wiselenium.core.Field.class);
	}
	
	@Override
	protected boolean shouldDecorate(Field field) {
		return this.shouldDecorate(field.getType());
	}
	
}