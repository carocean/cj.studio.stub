package cj.studio.stub.client;

import cj.studio.ecm.net.CircuitException;

public interface IRest {
	IRemote forRemote(String remote) throws CircuitException;
}
