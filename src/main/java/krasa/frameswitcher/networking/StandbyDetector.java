package krasa.frameswitcher.networking;

import com.intellij.openapi.diagnostic.Logger;

public abstract class StandbyDetector {
	private static final Logger LOG = Logger.getInstance(StandbyDetector.class);

	private final long timeoutMillis;
	private final long sleepMillis;
	
	private Thread thread;
	private boolean enabled = true;
	private long lastTimeStamp= System.currentTimeMillis();

	public StandbyDetector(final long timeoutMillis) {
		this.timeoutMillis = timeoutMillis;   
		this.sleepMillis = timeoutMillis/2;   
		start();
	}

	public final long getTimeoutMs() {
		return this.timeoutMillis;
	}

	private synchronized void start() {
		if (thread == null || !thread.isAlive()) {
			thread = new Thread("FrameSwitcher-StandByDetector") {

				@Override
				public void run() {
					while (enabled) { 
						try {
							Thread.sleep(sleepMillis);
						} catch (final InterruptedException e) {
							break;
						}
						check();
					}
				}
			};
			thread.setDaemon(true);
			thread.start();
		}
	}

	public synchronized void check() {
		long now = System.currentTimeMillis();
		final long timeStampGap = now - lastTimeStamp;
		if (timeStampGap > this.timeoutMillis) {
			try {
				LOG.debug("Standby detected");
				standbyDetected();
			} catch (final Throwable e) {
				LOG.error(e);
			}
		}
		lastTimeStamp = now;
	}

	public abstract void standbyDetected() ;

	public final synchronized boolean isRunning() {
		return thread != null && thread.isAlive();
	}

	public final synchronized void stop() {
        enabled = false;
	}
}
