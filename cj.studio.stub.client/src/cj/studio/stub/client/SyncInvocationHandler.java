package cj.studio.stub.client;

import cj.studio.ecm.EcmException;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.ecm.net.IInputChannel;
import cj.studio.ecm.net.io.MemoryContentReciever;
import cj.studio.ecm.net.io.MemoryInputChannel;
import cj.studio.ecm.net.io.MemoryOutputChannel;
import cj.studio.gateway.socket.pipeline.IOutputSelector;
import cj.studio.gateway.socket.pipeline.IOutputer;
import cj.studio.gateway.socket.util.SocketContants;
import cj.studio.stub.service.annotation.*;
import cj.studio.stub.service.util.StringTypeConverter;
import cj.ultimate.gson2.com.google.gson.Gson;
import cj.ultimate.net.sf.cglib.proxy.InvocationHandler;
import cj.ultimate.util.StringUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SyncInvocationHandler implements InvocationHandler, StringTypeConverter {
	IOutputSelector selector;
	CjStubService stubService;
	private String contentPath;
	String stubClassName;
	private String dest;
	ThreadLocal<IOutputer> local;

	public SyncInvocationHandler(IOutputSelector selector, String remote, Class<?> stub) throws CircuitException {
		if (remote.startsWith("rest://")) {
			dest = remote.substring("rest:/".length(), remote.length());
		} else {
			dest = remote;
		}
		this.selector = selector;
		this.stubService = stub.getDeclaredAnnotation(CjStubService.class);
		this.contentPath = getContentPath(dest);
		stubClassName = stub.getName();
		this.local = new ThreadLocal<>();
	}

	public SyncInvocationHandler(IOutputSelector selector, CjStubRef sr) throws CircuitException {
		if (sr.remote().startsWith("rest://")) {
			dest = sr.remote().substring("rest:/".length(), sr.remote().length());
		} else {
			dest = sr.remote();
		}
		this.selector = selector;
		this.stubService = sr.stub().getAnnotation(CjStubService.class);
		contentPath = getContentPath(dest);
		stubClassName = sr.stub().getName();
		this.local = new ThreadLocal<>();
	}

	private String getContentPath(String dest) throws CircuitException {
		boolean isStarts = false;
		while (dest.startsWith("/")) {
			dest = dest.substring(1, dest.length());
			isStarts = true;
		}
		if (!isStarts) {
			throw new CircuitException("503", "目标格式错误，没有以/开头:" + dest);
		}
		int pos = dest.indexOf("/");
		if (pos < 0) {
			throw new CircuitException("503", "目标格式错误，没有指定要访问的后端应用上下文:" + dest);
		}
		String contentPath = dest.substring(pos, dest.length());
		return contentPath;
	}

	@Override
	public Object invoke(Object obj, Method m, Object[] args) throws Throwable {
		IOutputer out = local.get();
		if (out != null) {
			return out;
		}
		try {
			out = selector.select(this.dest);
			local.set(out);
			return invoke(out, obj, m, args);
		} catch (Exception e) {
			throw e;
		} finally {
			if (out != null) {
				local.remove();
				out.releasePipeline();
			}
		}

	}

	private Object invoke(IOutputer out, Object obj, Method m, Object[] args) throws Exception {
		CjStubMethod sm = m.getDeclaredAnnotation(CjStubMethod.class);
		if (sm == null) {
			throw new Exception("缺少存根方法注解:" + m);
		}
		String name = sm.alias();
		if (StringUtil.isEmpty(name)) {
			name = m.getName();
		}
		if (name.indexOf("/") > -1) {
			throw new Exception("CjStubMethod注解错误，别名不能含有/。在：" + m);
		}
		String uri = contentPath;
		if (uri.endsWith("/")) {
			if (stubService.bindService().startsWith("/")) {
				String sub = stubService.bindService();
				while (sub.startsWith("/")) {
					sub = sub.substring(1, sub.length());
				}
				uri += sub;
			} else {
				uri += "/" + stubService.bindService();
			}
		} else {
			if (stubService.bindService().startsWith("/")) {
				uri += stubService.bindService();
			} else {
				uri += "/" + stubService.bindService();
			}
		}
		if (!uri.endsWith("/")) {
			int pos=uri.lastIndexOf("/");
			String tmp=uri.substring(pos+1,uri.length());
			if(tmp.indexOf(".")<0) {
				uri += "/";
			}
		}

		String fline = String.format("%s %s %s", sm.command(), uri, sm.protocol());
		IInputChannel ic = new MemoryInputChannel();
		Frame frame = new Frame(ic, fline);
		MemoryContentReciever mcr = new MemoryContentReciever();
		frame.content().accept(mcr);
		frame.head(SocketContants.__frame_Head_Rest_Command, name);
		frame.head(SocketContants.__frame_Head_Rest_Stub_Interface, stubClassName);

		fillFrame(frame, ic, m, args, sm);

		String sline = String.format("%s 200 OK", sm.protocol());
		MemoryOutputChannel oc = new MemoryOutputChannel();
		Circuit circuit = new Circuit(oc, sline);

		out.send(frame, circuit);
		int state = Integer.valueOf(circuit.status());
		if (state >= 400) {
			throw new CircuitException(circuit.status(), circuit.message());
		}
		if (m.getReturnType() == null) {
			return null;
		}
		circuit.content().close();
		byte[] b = oc.readFully();
		CjStubReturn sr = m.getDeclaredAnnotation(CjStubReturn.class);
//		if (!m.getReturnType().isPrimitive() && !m.getReturnType().equals(Void.TYPE) && sr == null) {// 除了基本型，返回类型如果是抽象基类则无法确认反射实例
//			throw new EcmException("缺少CjStubReturn注解。在：" + m);
//		} else {
		Class<?> retType = sr == null ? null : sr.type();
		Class<?> eleType[] = sr == null ? null : sr.elementType();
		Class<?> rawType = m.getReturnType();
		if (Collection.class.isAssignableFrom(rawType) || Map.class.isAssignableFrom(rawType)) {
			if(retType!=null&&retType!=Void.class&&!rawType.isAssignableFrom(retType)) {
				throw new EcmException(String.format("方法返回集合时注解CjStubReturn未定义或其定义的type不是方法返回类型或其派生类型", args));
			}
			
		}
		if (retType == null||retType.equals(Void.class)) {
			retType = rawType;
		}
		String feed = new String(b);
		Object ret = convertFrom(retType,eleType, feed,String.format("方法：%s,返回类型：%s", m,retType));
		return ret;
//		}

	}

	private void fillFrame(Frame frame, IInputChannel ic, Method m, Object[] args, CjStubMethod sm)
			throws CircuitException {
		Map<String, String> postContent = null;
		if ("post".equalsIgnoreCase(frame.command())) {
			postContent = new HashMap<>();
		}
		Parameter[] arr = m.getParameters();
		for (int i = 0; i < arr.length; i++) {
			Annotation[] argAnnos = arr[i].getAnnotations();
			for (Annotation a : argAnnos) {
				if (a instanceof CjStubInHead) {
					CjStubInHead sih = (CjStubInHead) a;
					if (sih != null) {
						String key = sih.key();
						String v = "";
						if (args[i] == null) {
							v = "";
						} else if (args[i].getClass().equals(String.class)) {
							v = (String) args[i];
						} else {
							v = new Gson().toJson(args[i]);
						}
						if (v.indexOf("\r") > -1 || v.indexOf("\n") > -1) {
							throw new EcmException("含有\r或\n");
						}
						frame.head(key, v);
					}
				} else if (a instanceof CjStubInParameter) {
					CjStubInParameter sip = (CjStubInParameter) a;
					if (sip != null) {
						String key = sip.key();
						String v = "";
						if (args[i] == null) {
							v = "";
						} else if (args[i].getClass().equals(String.class)) {
							v = (String) args[i];
						} else {
							v = new Gson().toJson(args[i]);
						}
						if (v.indexOf("\r") > -1 || v.indexOf("\n") > -1) {
							throw new EcmException("含有\r或\n");
						}
						frame.parameter(key, v);
					}
				} else if (a instanceof CjStubInContentKey) {
					CjStubInContentKey sic = (CjStubInContentKey) a;
					if (sic != null) {
						if (postContent == null) {
							throw new EcmException("有内容且command不是post");
						}
						String v = "";
						if (args[i] == null) {
							v = "";
						} else if (args[i].getClass().equals(String.class)) {
							v = (String) args[i];
						} else {
							v = new Gson().toJson(args[i]);
						}
						postContent.put(sic.key(), v);
					}
				}
			}
		}
		if (postContent != null) {
			byte[] b = new Gson().toJson(postContent).getBytes();
			ic.begin(frame);
			ic.done(b, 0, b.length);
		}
	}

}