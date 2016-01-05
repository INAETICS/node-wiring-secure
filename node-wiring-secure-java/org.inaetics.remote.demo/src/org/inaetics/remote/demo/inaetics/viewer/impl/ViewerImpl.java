package org.inaetics.remote.demo.inaetics.viewer.impl;

import java.util.concurrent.atomic.AtomicInteger;

import org.inaetics.remote.demo.inaetics.viewer.Viewer;

public class ViewerImpl implements Viewer {

	private AtomicInteger counter = new AtomicInteger(0);
	
	protected final void addProcessor() {
		counter.incrementAndGet();
	}

	protected final void removeProcessor() {
		counter.decrementAndGet();
	}
}
