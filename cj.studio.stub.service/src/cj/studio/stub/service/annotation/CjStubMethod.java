package cj.studio.stub.service.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 远程方法存根
 * @author caroceanjofers
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface CjStubMethod {
	/**
	 * 方法别名
	 * @return
	 */
	String alias()default "";
	
	/**
	 * 用法
	 * @return
	 */
	String usage();

	String command() default "get";

	String protocol() default "http/1.1";
}
