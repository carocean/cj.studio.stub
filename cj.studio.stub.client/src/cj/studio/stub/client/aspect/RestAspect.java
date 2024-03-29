package cj.studio.stub.client.aspect;

import cj.studio.ecm.CJSystem;
import cj.studio.ecm.adapter.IAdaptable;
import cj.studio.ecm.annotation.CjService;
import cj.studio.ecm.annotation.CjServiceRef;
import cj.studio.ecm.bridge.IAspect;
import cj.studio.ecm.bridge.ICutpoint;
import cj.studio.ecm.net.CircuitException;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.studio.stub.client.AsyncInvocationHandler;
import cj.studio.stub.client.SyncInvocationHandler;
import cj.studio.stub.service.annotation.CjStubRef;
import cj.ultimate.net.sf.cglib.proxy.Enhancer;

import java.lang.reflect.Field;

@CjService(name = "@rest")
public class RestAspect implements IAspect {
	@CjServiceRef(refByName = "$.output.selector")
	IOutputSelector selector;

	@Override
	public Object cut(Object obj, Object[] args, ICutpoint cut) throws Throwable {
		return cut.cut(obj, args);
	}

	@Override
	public Class<?>[] getCutInterfaces() {
		return new Class<?>[] { IGatewayAppSiteWayWebView.class };
	}

	@Override
	public void observe(Object service) {
		Class<?> c = service.getClass();
		do {
			Field[] arr = c.getDeclaredFields();
			for (Field f : arr) {
				CjStubRef sr = f.getAnnotation(CjStubRef.class);
				if (sr == null) {
					continue;
				}
				try {
					Object stub = createStub(sr);
					f.setAccessible(true);
					f.set(service, stub);
				} catch (Exception e) {
					CJSystem.logging().error(getClass(), e);
				}
			}
			c = c.getSuperclass();
		} while (!Object.class.equals(c));
	}

	private Object createStub(CjStubRef sr) throws CircuitException {
		// 实现代理
		Enhancer en = new Enhancer();
		en.setClassLoader(sr.stub().getClassLoader());
		en.setSuperclass(Object.class);
		en.setInterfaces(new Class<?>[] { sr.stub(), IAdaptable.class });
		if (sr.async()) {
			en.setCallback(new AsyncInvocationHandler(selector, sr));
		} else {
			en.setCallback(new SyncInvocationHandler(selector, sr));
		}
		return en.create();
	}

}
