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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.apache.commons.lang3.SerializationUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;

import com.moosd.kitchensyncd.networking.Networking;
import com.moosd.kitchensyncd.networking.PacketHandler;
import com.moosd.kitchensyncd.sync.proto.FileSend;
import com.moosd.kitchensyncd.sync.proto.ReqFile;
import com.moosd.kitchensyncd.sync.proto.SyncRequest;

public abstract class TwoWaySync extends SyncPrototype {

	public static int SYNC_BDCST = 1;
	public static int SYNC_LIST = 2;
	public static int SYNC_FILEIN = 3;
	public static int SYNC_FILEPLZ = 4;
	public static int SYNC_ASKMEFOR = 5;
	public static int SYNC_FILEREM = 6;
	public static int SYNC_BCAST_INIT = 7;
	public static int SYNC_BCAST_PINGBACK = 8;

	public static int NODE_DEL = 1;
	public static int NODE_ADD = 2;
	
	public static int CHUNKSIZE = 1024*1024;

	Networking net = null;

	// List<ReqFile> filesToRequest = null;
	String syncDbLoc = null;
	Connection dbConn = null;
	int type;
	int port;
	List<String> connectedUids = null;

	public TwoWaySync(Networking net, String syncDbLoc, int type, int port)
			throws SQLException, ClassNotFoundException {
		this.net = net;
		this.syncDbLoc = syncDbLoc;
		this.type = type;
		this.port = port;
		connectedUids = new LinkedList<String>();

		//Class.forName("org.h2.Driver");
		//dbConn = DriverManager.getConnection("jdbc:h2:" + syncDbLoc);
		Class.forName("org.hsqldb.jdbcDriver");
		dbConn = DriverManager.getConnection("jdbc:hsqldb:file:" + syncDbLoc, "SA", "");
				
		createTable();

		registerSync();
	}

	public void createTable() throws SQLException {
		/*String createString = "create table if not exists nodelist"
		+ "(ID integer NOT NULL AUTO_INCREMENT, "
		+ "NAME varchar NOT NULL, " + "TIMESTAMP BIGINT NOT NULL, "
		+ "ACTION integer NOT NULL, "
		+ "METATIMESTAMP BIGINT NOT NULL, "
		+ "CHECKSUM varchar NOT NULL, " + "PRIMARY KEY (ID))";*/
		String createString = "create table if not exists nodelist"
		+ "(ID integer, "
		+ "NAME longvarchar NOT NULL, " + "TIMESTAMP BIGINT NOT NULL, "
		+ "ACTION integer NOT NULL, "
		+ "METATIMESTAMP BIGINT NOT NULL, "
		+ "CHECKSUM longvarchar NOT NULL)";

		Statement stmt = null;
		try {
			stmt = dbConn.createStatement();
			stmt.executeUpdate(createString);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (stmt != null) {
				stmt.close();
			}
		}
	}

	public int getType() {
		return type;
	}

	public abstract List<ReqFile> getNodes();

	public abstract byte[] getNodeContents(String fname) throws Exception;

	public abstract byte[] getNodeContentsChunk(String fname, int chunk) throws Exception;

	public abstract int getNodeNumberChunks(String fname) throws Exception;

	public abstract void writeNode(String fname, byte[] data, long ts)
			throws Exception;
	
	public abstract void writeNodeChunk(String fname, byte[] data, long ts, int chunk)
			throws Exception;

	public abstract void removeNode(String fname) throws Exception;

	public synchronized void updateDb() throws SQLException {
		// update database for added/removed files
		List<ReqFile> currFiles = getNodes();
		addFiles = new LinkedList<ReqFile>();
		delFiles = new LinkedList<ReqFile>();
		List<ReqFile> toDel = new LinkedList<ReqFile>(), toAdd = new LinkedList<ReqFile>();
		// get current database stuff
		Statement stmt = dbConn.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT * FROM nodelist");

		while (rs.next()) {
			int act = rs.getInt(4);
			ReqFile op = new ReqFile(rs.getString(2), rs.getLong(3),
					rs.getLong(5), rs.getString(6));
			if (act == NODE_DEL) {
				delFiles.add(op);
			} else if (act == NODE_ADD) {
				addFiles.add(op);
			}
		}
		rs.close();
		stmt.close();

		// anything in addFiles not in currFiles has been deleted
		int sz = addFiles.size();
		int szC = currFiles.size();

		for (int i = 0; i < sz; i++) {
			ReqFile r = addFiles.get(i);
			boolean present = false;
			for (int ii = 0; ii < szC; ii++) {
				ReqFile q = currFiles.get(ii);
				if (r.compareTo(q) == 0)
					present = true;
			}
			if (!present)
				toDel.add(r);
		}
		// anything in currFiles not in addFiles has been added
		for (int i = 0; i < szC; i++) {
			ReqFile r = currFiles.get(i);
			boolean present = false;
			for (int ii = 0; ii < sz; ii++) {
				ReqFile q = addFiles.get(ii);
				if (r.compareTo(q) == 0)
					present = true;
			}
			if (!present)
				toAdd.add(r);
		}

		// update database
		long metaTs = System.currentTimeMillis();
		
		PreparedStatement statement = null;
		PreparedStatement statement2 = null;
		try {
			statement = dbConn
					.prepareStatement("INSERT INTO nodelist (NAME, TIMESTAMP, ACTION, METATIMESTAMP, CHECKSUM) VALUES (?,?,?,?,?)");
			statement2 = dbConn
					.prepareStatement("DELETE FROM nodelist WHERE NAME=? AND TIMESTAMP=?");

			for (ReqFile r : toDel) {
				statement.setString(1, r.fname);
				statement.setLong(2, r.ts);
				statement.setInt(3, NODE_DEL);
				statement.setLong(4, metaTs);
				statement.setString(5, r.checksum);
				statement.addBatch();
				statement2.setString(1, r.fname);
				statement2.setLong(2, r.ts);
				statement2.addBatch();
			}
			for (ReqFile r : toAdd) {
				statement.setString(1, r.fname);
				statement.setLong(2, r.ts);
				statement.setInt(3, NODE_ADD);
				statement.setLong(4, metaTs);
				statement.setString(5, r.checksum);
				statement.addBatch();
				statement2.setString(1, r.fname);
				statement2.setLong(2, r.ts);
				statement2.addBatch();
			}
			if(toAdd.size() > 0 || toDel.size() > 0) {
				statement2.executeBatch();
				statement.executeBatch();
			}
		} finally {
			if (statement != null)
				try {
					statement.close();
				} catch (SQLException logOrIgnore) {
				}
			if (statement2 != null)
				try {
					statement2.close();
				} catch (SQLException logOrIgnore) {
				}
		}
		delFiles.addAll(toDel);
		addFiles.addAll(toAdd);
		addFiles.removeAll(delFiles);
	}

	List<ReqFile> delFiles = null;
	List<ReqFile> addFiles = null;

	public void doSync(String ipSend, int portSend) {
		try {
			// ask for stuff
			SyncRequest syncRq = new SyncRequest(TwoWaySync.SYNC_BCAST_INIT);
			/*if (ipSend.equals(""))
				net.directSend.dump(port, getType(),
						SerializationUtils.serialize(syncRq));
			else*/
				net.directSend(ipSend, portSend, getType(),
						SerializationUtils.serialize(syncRq));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void sync() {
		doSync("", 0);
		triggerUpload();
	}

	boolean locktrigger = false;

	public void registerSync() {
		// send list of files
		// take request for file and send it
		PacketHandler main = new PacketHandler() {
			@Override
			public void handle(String senderUid, String senderIp,
					int senderPort, byte[] data) {
				try {
					SyncRequest sRq = (SyncRequest) SerializationUtils
							.deserialize(data);

					if (sRq.type == TwoWaySync.SYNC_BCAST_INIT) {
						if (!senderUid.equals(net.instanceId)) {
							SyncRequest syncRq = new SyncRequest(
									TwoWaySync.SYNC_BCAST_PINGBACK);

							net.directSend(senderIp, senderPort, getType(),
									SerializationUtils.serialize(syncRq));
						}
					} else if (sRq.type == TwoWaySync.SYNC_BCAST_PINGBACK) {

						SyncRequest syncRq = new SyncRequest(
								TwoWaySync.SYNC_BDCST);

						syncRq.payload = (Serializable) getNodes();
						net.directSend(senderIp, senderPort, getType(),
								SerializationUtils.serialize(syncRq));

					} else if (sRq.type == TwoWaySync.SYNC_BDCST) {
						System.out.println("listed!");

						List<ReqFile> fromSyncer = (List<ReqFile>) sRq.payload;
						if(fromSyncer == null) {
							System.out.println("received null list");
							return;
						}

						updateDb();

						List<ReqFile> toSend = new LinkedList<ReqFile>();

						// add all my files
						for (ReqFile r : addFiles) {
							toSend.add(r);
						}
						
						toSend.removeAll(fromSyncer);
						
						// remove all deleted files
						List<ReqFile> tellToRm = new LinkedList<ReqFile>();

						for (ReqFile recv : delFiles) {
							if (fromSyncer.contains(recv))
								tellToRm.add(recv);
						}

						for (ReqFile r : toSend) {
							System.out.println("sendme:" + r.fname + " - "+ r.checksum + ", "+r.hashCode());
						}

						SyncRequest rq = new SyncRequest(TwoWaySync.SYNC_LIST);
						rq.payload = (Serializable) toSend;
						net.directSend(senderIp, senderPort, getType(),
								SerializationUtils.serialize(rq));


						if (tellToRm.size() > 0) {
							rq = new SyncRequest(TwoWaySync.SYNC_FILEREM);
							rq.payload = (Serializable) tellToRm;

							net.directSend(senderIp, senderPort, getType(),
									SerializationUtils.serialize(rq));
						}
					} else if (sRq.type == TwoWaySync.SYNC_ASKMEFOR) {
						if (!senderUid.equals(net.instanceId)) {
							System.out.println("Should sync now!");
							doSync(senderIp, senderPort);
						}
					} else if (sRq.type == TwoWaySync.SYNC_FILEPLZ) {
						ReqFile req = (ReqFile) sRq.payload;
						SyncRequest send = new SyncRequest(
								TwoWaySync.SYNC_FILEIN);
						FileSend sendFile = new FileSend();
						sendFile.fname = req.fname;
						sendFile.err = false;
						sendFile.ts = req.ts;
						PreparedStatement statementCheck = null;
						statementCheck = dbConn
								.prepareStatement("SELECT METATIMESTAMP, CHECKSUM FROM nodelist WHERE NAME=? AND TIMESTAMP=? AND ACTION="
										+ TwoWaySync.NODE_ADD
										+ " ORDER BY timestamp DESC");
						statementCheck.setString(1, req.fname);
						statementCheck.setLong(2, req.ts);
						ResultSet rs = statementCheck.executeQuery();
						long metats = 0;
						String chksum = "";
						while (rs.next()) {
							metats = rs.getLong(1);
							chksum = rs.getString(2);
						}
						rs.close();
						statementCheck.close();
						sendFile.metaTs = metats;
						sendFile.checksum = chksum;

						if (!connectedUids.contains(senderUid)) {
							connectedUids.add(senderUid);
						int chks = getNodeNumberChunks(req.fname);
						
						for(int i=0;i<chks;i++) {
						try {
							preFileOp();
							locktrigger = true;
							sendFile.data = getNodeContentsChunk(req.fname, i);
							sendFile.chunk=i;
							locktrigger = false;
							postFileOp();
						} catch (Exception e) {
							System.out.println("Error!");
							e.printStackTrace();
							sendFile.err = true;
							return;
						}
						
						send.payload = sendFile;

						net.directSend(senderIp, senderPort, getType(),
								SerializationUtils.serialize(send));
						}
						
						try {
							Thread.sleep(5000);
						} catch(Exception e){}

						connectedUids.remove(senderUid);
						}
					}

					if (sRq.type == TwoWaySync.SYNC_LIST) {
						if (!connectedUids.contains(senderUid))
							synchronized (TwoWaySync.this) {
								connectedUids.add(senderUid);
								updateDb();
								List<ReqFile> recvd = (List<ReqFile>) sRq.payload;
								List<ReqFile> curr = getNodes();

								int szR = recvd.size();
								int szC = curr.size();
								List<ReqFile> filesToRequest = new LinkedList<ReqFile>();

								for (ReqFile rfile : recvd) {
									if (!curr.contains(rfile)) {
										rfile.ip = senderIp;
										rfile.port = senderPort;
										filesToRequest.add(rfile);
									}
								}

								// remove deleted items from items to request
								for (int i = 0; i < filesToRequest.size(); i++) {
									ReqFile r = filesToRequest.get(i);
									for (ReqFile d : delFiles) {
										if (r.fname.equals(d.fname)
												&& r.ts == d.ts
												&& r.checksum
														.equals(d.checksum)
												&& r.metaTs < d.metaTs) {
											filesToRequest.remove(i);
											i--;
											break;
										}
									}
								}
								// filesToRequest.removeAll(delFiles);

								// request files
								int sz = filesToRequest.size();

								for (int i = 0; i < sz; i++) {
									ReqFile rfile = filesToRequest.get(i);
									String ip = rfile.ip;
									int port = rfile.port;
									SyncRequest send = new SyncRequest(
											TwoWaySync.SYNC_FILEPLZ);
									send.payload = rfile;

									net.directSend(ip, port, getType(),
											SerializationUtils.serialize(send));
								}
								connectedUids.remove(senderUid);
							}
					}

					if (sRq.type == TwoWaySync.SYNC_FILEIN) {
						synchronized (syncDbLoc) {
							FileSend rcv = (FileSend) sRq.payload;
							if (rcv.err)
								return;
							if (rcv.fname.trim().equals(""))
								return;
							PreparedStatement statementCheck = null;
							statementCheck = dbConn
									.prepareStatement("SELECT METATIMESTAMP FROM nodelist WHERE NAME=? AND TIMESTAMP=? AND ACTION="
											+ TwoWaySync.NODE_DEL
											+ " ORDER BY timestamp DESC");
							statementCheck.setString(1, rcv.fname);
							statementCheck.setLong(2, rcv.ts);
							ResultSet rs = statementCheck.executeQuery();
							boolean shouldAdd = true;
							while (rs.next()) {
								// if metaTs > curr_metaTs then remove
								if (rs.getLong(1) > rcv.metaTs)
									shouldAdd = false;
							}
							rs.close();
							statementCheck.close();

							if (shouldAdd) {
								preFileOp();
								locktrigger = true;
								writeNodeChunk(rcv.fname, rcv.data, rcv.ts, rcv.chunk);
								locktrigger = false;
								postFileOp();
								PreparedStatement statement = null;
								try {
									statement = dbConn
											.prepareStatement("INSERT INTO nodelist (NAME, TIMESTAMP, ACTION, METATIMESTAMP, CHECKSUM) VALUES (?,?,?,?,?)");
									statement.setString(1, rcv.fname);
									statement.setLong(2, rcv.ts);
									statement.setInt(3, NODE_ADD);
									statement.setLong(4, rcv.metaTs);
									statement.setString(5, rcv.checksum);
									statement.execute();
								} finally {
									if (statement != null)
										try {
											statement.close();
										} catch (SQLException logOrIgnore) {
										}
								}
							}
						}
					}

					if (sRq.type == TwoWaySync.SYNC_FILEREM) {
						List<ReqFile> toRm = (List<ReqFile>) sRq.payload;
						PreparedStatement statement = null, statement2 = null;
						try {
							statement = dbConn
									.prepareStatement("DELETE FROM nodelist WHERE NAME=? AND TIMESTAMP=?");
							statement2 = dbConn
									.prepareStatement("INSERT INTO nodelist (NAME, TIMESTAMP, ACTION, METATIMESTAMP, CHECKSUM) VALUES (?,?,?,?,?)");
 boolean didanything = false;
							for (ReqFile f : toRm) {
								PreparedStatement statementCheck = null;
								statementCheck = dbConn
										.prepareStatement("SELECT METATIMESTAMP FROM nodelist WHERE NAME=? AND TIMESTAMP=? AND ACTION="
												+ TwoWaySync.NODE_ADD
												+ " ORDER BY timestamp DESC");
								statementCheck.setString(1, f.fname);
								statementCheck.setLong(2, f.ts);
								ResultSet rs = statementCheck.executeQuery();
								boolean shouldRm = false;
								while (rs.next()) {
									// if metaTs > curr_metaTs then remove
									if (rs.getLong(1) < f.metaTs)
										shouldRm = true;
								}
								rs.close();
								statementCheck.close();

								if (shouldRm) {
									didanything=true;
									statement.setString(1, f.fname);
									statement.setLong(2, f.ts);
									statement.addBatch();
									statement2.setString(1, f.fname);
									statement2.setLong(2, f.ts);
									statement2.setInt(3, TwoWaySync.NODE_DEL);
									statement2.setLong(4, f.metaTs);
									statement2.setString(5, f.checksum);
									statement2.addBatch();
									preFileOp();
									locktrigger = true;
									removeNode(f.fname);
									locktrigger = false;
									postFileOp();
								}
							}

							if(didanything) {
							statement.executeBatch();
							statement2.executeBatch();
							}
							updateDb();
						} finally {
							if (statement != null)
								try {
									statement.close();
								} catch (SQLException logOrIgnore) {
								}
							if (statement2 != null)
								try {
									statement2.close();
								} catch (SQLException logOrIgnore) {
								}
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		net.hooks.addDirectHook(getType(), main);
		net.hooks.addDtgmHook(getType(), new PacketHandler() {

			@Override
			public void handle(String senderUid, String senderIp,
					int senderPort, byte[] data) {
				// TODO Auto-generated method stub
				System.out.println("Dtgm sync initiated...");
				doSync("", 0);
			}

		});
	}

	public void preFileOp() {
	}

	public void postFileOp() {
	}

	public void triggerUpload() {
		if (locktrigger)
			return;
		System.out.println("Trigger upload");
		/*net.directSend.dump(port, getType(), SerializationUtils
				.serialize(new SyncRequest(TwoWaySync.SYNC_ASKMEFOR)));*/
		try {
			net.broadcast(getType(), SerializationUtils
					.serialize(new SyncRequest(TwoWaySync.SYNC_ASKMEFOR)));
		} catch (InvalidCipherTextException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		final boolean uploader = false;
		
		/*List<ReqFile> test = new LinkedList<ReqFile>();
		ReqFile a = new ReqFile("a", 1000, "a");
		ReqFile b = new ReqFile("a", 1001, "a");
		test.add(a);
		System.out.println("?" + test.remove(b));*/

		try {
			final Networking net = new Networking("TESTEST_1234",
					(uploader ? 2000 : 2001));
			final SyncScheduler scheduler = new SyncScheduler(net);

			// Add sync processes
			scheduler.addSyncProcess(new TwoWaySync(net,
					uploader ? "/tmp/s1.db" : "/tmp/s2.db", 2, (uploader ? 2000
							: 2001)) {

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
						CheckedInputStream cis = null;
						long checksum = System.currentTimeMillis();
						try {
							cis = new CheckedInputStream(
									new FileInputStream(f), new CRC32());

							byte[] buf = new byte[128];
							while (cis.read(buf) >= 0) {
							}

							checksum = cis.getChecksum().getValue();
							cis.close();
						} catch (Exception e) {
							e.printStackTrace();
						}
						flist.add(new ReqFile(f.getName(), f.lastModified(), ""
								+ checksum));
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
				public void removeNode(String fname) {
					String nn = "";
					if (uploader)
						nn = "one";
					else
						nn = "two";
					new File("/tmp/synctest/" + nn + "/" + fname).delete();
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
			});

			scheduler.triggerSync();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
