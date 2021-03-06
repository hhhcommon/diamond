package com.taobao.diamond.client.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import com.taobao.middleware.logger.Logger;

public class ConcurrentDiskUtil {

	
	public static String getFileContent(String path, String charsetName)
			throws IOException {
		File file = new File(path);
		return getFileContent(file, charsetName);
	}

	
	public static String getFileContent(File file, String charsetName)
			throws IOException {
		RandomAccessFile fis = null;
		FileLock rlock = null;
		try {
			fis = new RandomAccessFile(file, "r");
			FileChannel fcin = fis.getChannel();
			int i = 0;
			do {
				try {
					rlock = fcin.tryLock(0L, Long.MAX_VALUE, true);
				} catch (Exception e) {
					++i;
					if (i > RETRY_COUNT) {
						log.error("read {} fail;retryed time:{}",
								file.getName(), i);
						throw new IOException("read " + file.getAbsolutePath()
								+ " conflict");
					}
					sleep(SLEEP_BASETIME * i);
					log.warn("read {} conflict;retry time:{}", file.getName(),
							i);
				}
			} while (null == rlock);
			int fileSize = (int) fcin.size();
			ByteBuffer byteBuffer = ByteBuffer.allocate(fileSize);
			fcin.read(byteBuffer);
			byteBuffer.flip();
			return byteBufferToString(byteBuffer, charsetName);
		} finally {
			if (rlock != null) {
				rlock.release();
				rlock = null;
			}
			if (fis != null) {
				fis.close();
				fis = null;
			}
		}
	}

	
	public static Boolean writeFileContent(String path, String content,
			String charsetName) throws IOException {
		File file = new File(path);
		return writeFileContent(file, content, charsetName);
	}

	
	public static Boolean writeFileContent(File file, String content,
			String charsetName) throws IOException {
		if (!file.exists()) {
			file.createNewFile();
		}
		FileChannel channel = null;
		FileLock lock = null;
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "rw");
			channel = raf.getChannel();
			int i = 0;
			do {
				try {
					lock = channel.tryLock();
				} catch (Exception e) {
					++i;
					if (i > RETRY_COUNT) {
						log.error("write {} fail;retryed time:{}",
								file.getName(), i);
						throw new IOException("write " + file.getAbsolutePath()
								+ " conflict");
					}
					sleep(SLEEP_BASETIME * i);
					log.warn("write {} conflict;retry time:{}", file.getName(),
							i);
				}
			} while (null == lock);

			ByteBuffer sendBuffer = ByteBuffer.wrap(content
					.getBytes(charsetName));
			while (sendBuffer.hasRemaining()) {
				channel.write(sendBuffer);
			}
			channel.truncate(content.length());
		} catch (FileNotFoundException e) {
			throw new IOException("file not exist");
		} finally {
			if (lock != null) {
				try {
					lock.release();
					lock = null;
				} catch (IOException e) {
					log.warn("close wrong", e);
				}
			}
			if (channel != null) {
				try {
					channel.close();
					channel = null;
				} catch (IOException e) {
					log.warn("close wrong", e);
				}
			}
			if (raf != null) {
				try {
					raf.close();
					raf = null;
				} catch (IOException e) {
					log.warn("close wrong", e);
				}
			}

		}
		return true;
	}

	
	public static String byteBufferToString(ByteBuffer buffer,
			String charsetName) throws IOException {
		Charset charset = null;
		CharsetDecoder decoder = null;
		CharBuffer charBuffer = null;
		charset = Charset.forName(charsetName);
		decoder = charset.newDecoder();
		charBuffer = decoder.decode(buffer.asReadOnlyBuffer());
		return charBuffer.toString();
	}

	private static void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			log.warn("sleep wrong", e);
		}
	}

	public static void main(String[] args) {
		try {
			for (int i = 0; i < 10000; i++) {
				writeFileContent("D:/test.txt", "test\r\ntest1", "GBK");
				String abc = getFileContent("D:/test.txt", "GBK");
				if (!"test\r\ntest1".equals(abc)) {
					System.out.println(abc);
					System.out.println("diff");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static final public Logger log = LogUtils.logger(ConcurrentDiskUtil.class);
	static final int RETRY_COUNT = 10;
	static final int SLEEP_BASETIME = 10; // ms
}
