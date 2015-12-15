package com.moosd.kitchensyncd.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.apache.commons.lang3.SerializationUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;

import com.moosd.kitchensyncd.networking.Networking;
import com.moosd.kitchensyncd.networking.PacketHandler;
import com.moosd.kitchensyncd.sync.proto.FileSend;
import com.moosd.kitchensyncd.sync.proto.ReqFile;
import com.moosd.kitchensyncd.sync.proto.SyncRequest;

public abstract class OneWaySync extends SyncPrototype {

	public static int SYNC_BDCST = 1;
	public static int SYNC_LIST = 2;
	public static int SYNC_FILEIN = 3;
	public static int SYNC_FILEPLZ = 4;
	public static int SYNC_ASKMEFOR = 5;
	public static int SYNC_BCAST_INIT = 6;

	public static int MODE_UPLOAD = 0;
	public static int MODE_DWNLOAD = 1;
	public static int MODE_UPDWNLOAD = 2;
	
	public static int CHUNKSIZE = 1024 * 1024;

	Networking net;
	int upload = MODE_UPDWNLOAD;
	List<String> connectedUids;
	
	long lastTrigger = 0;

	int type, ourPort;

	public OneWaySync(Networking net, int upload, int type, int port)
			throws Exception {
		if (upload > MODE_UPDWNLOAD || upload < 0)
			throw new Exception(
					"Invalid options specified for the one-way sync");
		this.net = net;
		this.upload = upload;
		this.type = type;
		ourPort = port;
		connectedUids = new LinkedList<String>();

		registerSync();
	}

	public int getType() {
		return type;
	}

	public abstract List<ReqFile> getNodes() throws Exception;

	public abstract byte[] getNodeContents(String fname) throws Exception;

	public abstract byte[] getNodeContentsChunk(String fname, int chunk) throws Exception;

	public abstract int getNodeNumberChunks(String fname) throws Exception;

	public abstract void writeNode(String fname, byte[] data, long ts)
			throws Exception;
	
	public abstract void writeNodeChunk(String fname, byte[] data, long ts, int chunk)
			throws Exception;

	// doSync(ip, port)
	// bcast_init
	// bcast_pingback
	// list
	// filestoreq
	// fetch

	public void doSync(String ip, int port) {
		/*if (upload != MODE_UPLOAD) {
			if (ip.equals("")) {
				net.directSend
						.dump(ourPort, getType(), SerializationUtils
								.serialize(new SyncRequest(
										OneWaySync.SYNC_BDCST)));
			} else {
				net.directSend(ip, port, getType(), SerializationUtils
						.serialize(new SyncRequest(OneWaySync.SYNC_BDCST)));
			}
		} else {
				if (ip.equals("")) {
					net.directSend
							.dump(ourPort, getType(), SerializationUtils
									.serialize(new SyncRequest(
											OneWaySync.SYNC_BCAST_INIT)));
				} else {
					net.directSend(ip, port, getType(), SerializationUtils
							.serialize(new SyncRequest(OneWaySync.SYNC_BCAST_INIT)));
				}
		}*/
	}

	public void sync() {
		doSync("", 0);
		triggerUpload();
		// todo :: logsync and filesync classes
		// todo :: android app version w/ ipc, then integrate texts and ims for
		// logs, calls and music for files.
	}

	public void registerSync() {
		// send list of files
		net.hooks.addDtgmHook(getType(), new PacketHandler() {
			@Override
			public void handle(String senderUid, String senderIp,
					int senderPort, byte[] data) {
				try {
					SyncRequest sRq = (SyncRequest) SerializationUtils
							.deserialize(data);

					if (upload != MODE_UPLOAD) {
						if (sRq.type == OneWaySync.SYNC_ASKMEFOR) {
							if (!senderUid.equals(net.instanceId)) {
							doSync(senderIp, senderPort);
							}
						}
					}
				} catch (Exception e) {
				}
			}
		});

		net.hooks.addDirectHook(getType(), new PacketHandler() {
			@Override
			public void handle(String senderUid, String senderIp,
					int senderPort, byte[] data) {
				try {
					SyncRequest sRq = (SyncRequest) SerializationUtils
							.deserialize(data);

					if (upload != MODE_DWNLOAD) {
						if (sRq.type == OneWaySync.SYNC_BDCST) {
							SyncRequest rq = new SyncRequest(
									OneWaySync.SYNC_LIST);
							rq.payload = (Serializable) getNodes();
							net.directSend(senderIp, senderPort, getType(),
									SerializationUtils.serialize(rq));
							System.out.println("Received bcast reply, sending list");
						}

						if (sRq.type == OneWaySync.SYNC_FILEPLZ) {
							ReqFile req = (ReqFile) sRq.payload;
							SyncRequest send = new SyncRequest(
									OneWaySync.SYNC_FILEIN);
							FileSend sendFile = new FileSend();
							sendFile.fname = req.fname;
							sendFile.ts = req.ts;
							System.out.println("Received request for file "	+ sendFile.fname);
							sendFile.err = false;
							
							int chks = getNodeNumberChunks(req.fname);
							for(int i=0;i<chks;i++) {
							try {
								sendFile.data = getNodeContentsChunk(req.fname, i);
								sendFile.chunk = i;
							} catch (Exception e) {
								e.printStackTrace();
								sendFile.err = true;
							}
							send.payload = sendFile;

							net.directSend(senderIp, senderPort, getType(),
									SerializationUtils.serialize(send));
							}
						}
					}

					if (upload != MODE_UPLOAD) {
						if (sRq.type == OneWaySync.SYNC_BCAST_INIT) {
							if (!senderUid.equals(net.instanceId)) {
								net.directSend(senderIp, senderPort, getType(),
										SerializationUtils
												.serialize(new SyncRequest(
														OneWaySync.SYNC_BDCST)));
								//System.out.println("Received bcast init, sending bcast reply");
							}
						}
						if (sRq.type == OneWaySync.SYNC_ASKMEFOR) {
							if (!senderUid.equals(net.instanceId)) {
							doSync(senderIp, senderPort);
							}
						}
						if (sRq.type == OneWaySync.SYNC_LIST) {
							System.out.println("Received list, working out what to request");
							if (!connectedUids.contains(senderUid))
								synchronized (OneWaySync.this) {
									connectedUids.add(senderUid);
									List<ReqFile> recvd = (List<ReqFile>) sRq.payload;
									List<ReqFile> curr = getNodes();
									
									List<ReqFile> filesToRequest = new LinkedList<ReqFile>();

									for (ReqFile rfile : recvd) {
										boolean shouldAdd = true;

										boolean wasPresent = false;
										if (!wasPresent)
											for (ReqFile ccurr : curr) {
												if (ccurr.fname
														.equals(rfile.fname)
														&& ccurr.ts >= rfile.ts && ccurr.checksum.equals(rfile.checksum))
													shouldAdd = false;
												// if (ccurr.ts == rfile.ts)
												// shouldAdd = false;
											}

										if (shouldAdd) {
											filesToRequest.add(rfile);
										}
									}

									int sz = filesToRequest.size();

									for (int i = 0; i < sz; i++) {
										ReqFile rfile = filesToRequest.get(i);
										SyncRequest send = new SyncRequest(
												OneWaySync.SYNC_FILEPLZ);
										send.payload = rfile;

										net.directSend(senderIp, senderPort,
												getType(), SerializationUtils
														.serialize(send));
									}
									
									try {
										Thread.sleep(5000);
									} catch(Exception e){}

									connectedUids.remove(senderUid);
									System.out.println("Sent!");
								}

						}

						if (sRq.type == OneWaySync.SYNC_FILEIN) {
							FileSend rcv = (FileSend) sRq.payload;
							System.out.println("Received file " + rcv.fname+" chunk "+rcv.chunk);
							if (rcv.err)
								return;
							writeNodeChunk(rcv.fname, rcv.data, rcv.ts, rcv.chunk);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public void triggerUpload() {
		long now = System.currentTimeMillis();
		if(now > lastTrigger + 2000)
		try {
			net.broadcast(getType(), SerializationUtils
					.serialize(new SyncRequest(OneWaySync.SYNC_ASKMEFOR)));
			/*net.directSend.dump(ourPort, getType(), SerializationUtils
					.serialize(new SyncRequest(OneWaySync.SYNC_ASKMEFOR)));*/
			lastTrigger = now;
		} catch (InvalidCipherTextException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		final boolean uploader = false;

		try {
			final Networking net = new Networking("TESTEST_1234",
					(uploader ? 2000 : 2001));
			final SyncScheduler scheduler = new SyncScheduler(net);

			// Add sync processes
			scheduler.addSyncProcess(new OneWaySync(net,
					(uploader ? MODE_UPLOAD : MODE_DWNLOAD), 6,
					(uploader ? 2000 : 2001)) {

				@Override
				public List<ReqFile> getNodes() {
					String nn = "";
					if (uploader)
						nn = "one";
					else
						nn = "two";
					File dir = new File("/tmp/synctest/" + nn);
					dir.mkdirs();

					List<ReqFile> flist = new LinkedList<ReqFile>();
					for (File f : dir.listFiles()) {
						flist.add(new ReqFile(f.getName(), f.lastModified(), ""));
					}
					return flist;
				}

				@Override
				public byte[] getNodeContents(String fname) {
					String nn = "";
					if (uploader)
						nn = "one";
					else
						nn = "two";
					File f = new File("/tmp/synctest/" + nn + "/" + fname);
					FileInputStream fin = null;
					FileChannel ch = null;
					try {
						fin = new FileInputStream(f);
						ch = fin.getChannel();
						int size = (int) ch.size();
						MappedByteBuffer buf = ch.map(MapMode.READ_ONLY, 0,
								size);
						byte[] bytes = new byte[size];
						buf.get(bytes);
						return bytes;

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						try {
							if (fin != null) {
								fin.close();
							}
							if (ch != null) {
								ch.close();
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					return null;
				}

				@Override
				public byte[] getNodeContentsChunk(String fname, int chunk) {
					String nn = "";
					if (uploader)
						nn = "one";
					else
						nn = "two";
					File f = new File("/tmp/synctest/" + nn + "/" + fname);
					FileInputStream fin = null;
					FileChannel ch = null;
					byte[] bytes = null;
					try {
						fin = new FileInputStream(f);
						ch = fin.getChannel();
						long size = (long) ch.size();
						
						long pos = CHUNKSIZE * chunk;
						if(pos + CHUNKSIZE > size)
							size -= pos;
						else
							size = CHUNKSIZE;
						
						MappedByteBuffer buf = ch.map(MapMode.READ_ONLY, pos,
								size);
						bytes = new byte[(int)size];
						buf.get(bytes);

					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						try {
							if (fin != null) {
								fin.close();
							}
							if (ch != null) {
								ch.close();
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					return bytes;
				}
				
				@Override
				public int getNodeNumberChunks(String fname) {
					String nn = "";
					if (uploader)
						nn = "one";
					else
						nn = "two";
					File f = new File("/tmp/synctest/" + nn + "/" + fname);
					FileInputStream fin = null;
					FileChannel ch = null;
					int num = 0;
					
					try {
						fin = new FileInputStream(f);
						ch = fin.getChannel();
						long size = (long) ch.size();
						
						num = (int)Math.ceil(size / (double)CHUNKSIZE);
System.out.println("Number chunks: "+num);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						try {
							if (fin != null) {
								fin.close();
							}
							if (ch != null) {
								ch.close();
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					return num;
				}

				@Override
				public void writeNode(String fname, byte[] data, long ts) {
					String nn = "";
					if (uploader)
						nn = "one";
					else
						nn = "two";

					System.out.println("Writing " + fname);

					FileOutputStream fos;
					try {
						fos = new FileOutputStream("/tmp/synctest/" + nn + "/"
								+ fname);
						fos.write(data);
						fos.close();

						new File("/tmp/synctest/" + nn + "/" + fname)
								.setLastModified(ts);
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				@Override
				public void writeNodeChunk(String fname, byte[] data, long ts, int chunk) {
					String nn = "";
					if (uploader)
						nn = "one";
					else
						nn = "two";

					System.out.println("Writing " + fname + "chunk "+chunk);

					RandomAccessFile fos;
					try {
						fos = new RandomAccessFile("/tmp/synctest/" + nn + "/"
								+ fname, "rws");
						fos.seek(chunk * CHUNKSIZE);
						fos.write(data);
						fos.close();

						new File("/tmp/synctest/" + nn + "/" + fname)
								.setLastModified(ts);
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			});

			scheduler.triggerSync();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
