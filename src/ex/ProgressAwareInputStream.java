package ex;

import java.io.IOException;
import java.io.InputStream;

//Borrowed from: "android-flickr-search" "on code.google.com"
/**
 * InputStream that notifies listeners of its progress.
 */
public class ProgressAwareInputStream extends InputStream
{
	private InputStream wrappedInputStream;
	private long size;
    private long counter;
    private long lastCounter;
	private double lastPercent;
	private OnProgressListener _listener;
	
	public ProgressAwareInputStream(InputStream in, long size) {
		wrappedInputStream = in;
		this.size = size;
	}
	
	public void setOnProgressListener(OnProgressListener listener) { _listener = listener; }
	
	@Override
	public int read() throws IOException {
		counter += 1;
		check();
		return wrappedInputStream.read();
	}
	
	@Override
	public int read(byte[] b) throws IOException {
		int retVal = wrappedInputStream.read(b);
		counter += retVal;
		check();
		return retVal;
	}

	@Override
	public int read(byte[] b, int offset, int length) throws IOException {
		int retVal = wrappedInputStream.read(b, offset, length);
		counter += retVal;
		check();
		return retVal;
	}
	
	private void check() {
		double percent = (int) ( counter * 100 / size );
		if (percent - lastPercent > 0)
        {
            long difference = counter - lastCounter;
            lastCounter = counter;
			lastPercent = percent;
			if (_listener != null)
				_listener.onProgress(difference, percent);
		}
	}
	
	@Override
	public void close() throws IOException { wrappedInputStream.close(); }
	@Override
	public int available() throws IOException { return wrappedInputStream.available(); }
	@Override
	public void mark(int readlimit) { wrappedInputStream.mark(readlimit); }
	@Override
	public synchronized void reset() throws IOException { wrappedInputStream.reset(); }
	@Override
	public boolean markSupported() { return wrappedInputStream.markSupported(); }
	@Override
	public long skip(long n) throws IOException { return wrappedInputStream.skip(n); }
}
