package cj.studio.stub.service.printer;

import cj.studio.ecm.IChip;
import cj.studio.ecm.IChipInfo;
import cj.studio.ecm.IServiceSite;
import cj.studio.ecm.ServiceCollection;
import cj.studio.ecm.annotation.CjServiceSite;
import cj.studio.ecm.net.Circuit;
import cj.studio.ecm.net.CircuitException;
import cj.studio.ecm.net.Frame;
import cj.studio.gateway.socket.app.IGatewayAppSiteResource;
import cj.studio.gateway.socket.app.IGatewayAppSiteWayWebView;
import cj.studio.stub.service.annotation.*;
import cj.ultimate.util.StringUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public abstract class PrintStubAppSiteWebView implements IGatewayAppSiteWayWebView {
	@CjServiceSite
	IServiceSite __site;

	@Override
	public final synchronized void flow(Frame frame, Circuit circuit, IGatewayAppSiteResource resource) throws CircuitException {
		IChip chip=(IChip)__site.getService(IChip.class.getName());
		onprintChipInfo(chip.info(),frame,circuit,resource);
		ServiceCollection<IGatewayAppSiteWayWebView> col = __site.getServices(IGatewayAppSiteWayWebView.class);
		for (IGatewayAppSiteWayWebView view : col) {
			Class<?>[] faces = view.getClass().getInterfaces();
			for (Class<?> c : faces) {
				CjStubService ss = c.getDeclaredAnnotation(CjStubService.class);
				if (ss == null)
					continue;
				onprintStubService(ss, c, frame, circuit, resource);
				Method[] methods = c.getDeclaredMethods();
				for (Method m : methods) {
					CjStubMethod sm = m.getDeclaredAnnotation(CjStubMethod.class);
					if (sm == null)
						continue;
					String mName = sm.alias();
					if (StringUtil.isEmpty(mName)) {
						mName = m.getName();
					}
					onprintStubMethod(sm, m, frame, circuit, resource);
					Parameter[] params = m.getParameters();
					for (int i = 0; i < params.length; i++) {
						Parameter p = params[i];
						CjStubInHead sih = p.getAnnotation(CjStubInHead.class);
						if (sih != null) {
							onprintStubMethodArgInHead(sih, p, m, frame, circuit, resource);
						}
						CjStubInParameter sip = p.getAnnotation(CjStubInParameter.class);
						if (sip != null) {
							onprintStubMethodArgInParameter(sip, p, m, frame, circuit, resource);
						}
						CjStubInContentKey sic = p.getAnnotation(CjStubInContentKey.class);
						if (sic != null) {
							onprintStubMethodArgInContent(sic, p, m, frame, circuit, resource);
						}
					}
					onprintMethodEnd(frame, circuit, resource);
				}
				onprintStubServiceEnd(frame, circuit, resource);
			}
		}
		onprintEnd(frame, circuit, resource);
	}

	protected abstract void onprintChipInfo(IChipInfo info, Frame frame, Circuit circuit,
			IGatewayAppSiteResource resource);

	protected abstract void onprintEnd(Frame frame, Circuit circuit, IGatewayAppSiteResource resource);

	protected abstract void onprintStubServiceEnd(Frame frame, Circuit circuit, IGatewayAppSiteResource resource);

	protected abstract void onprintMethodEnd(Frame frame, Circuit circuit, IGatewayAppSiteResource resource);

	protected abstract void onprintStubMethod(CjStubMethod sm,  Method m, Frame frame, Circuit circuit,
			IGatewayAppSiteResource resource);

	protected abstract void onprintStubMethodArgInContent(CjStubInContentKey sic, Parameter p, Method m, Frame frame,
			Circuit circuit, IGatewayAppSiteResource resource);

	protected abstract void onprintStubMethodArgInParameter(CjStubInParameter sip, Parameter p, Method m, Frame frame,
			Circuit circuit, IGatewayAppSiteResource resource);

	protected abstract void onprintStubMethodArgInHead(CjStubInHead sih, Parameter p, Method m, Frame frame,
			Circuit circuit, IGatewayAppSiteResource resource);

	protected abstract void onprintStubService(CjStubService ss, Class<?> c, Frame frame, Circuit circuit,
			IGatewayAppSiteResource resource);

}
