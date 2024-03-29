package cj.studio.stub.service.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 远程服务存根
 * @author caroceanjofers
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface CjStubService {
	
	/**
	 * 用法说明
	 * @return
	 */
	String usage();

	String bindService();
}
