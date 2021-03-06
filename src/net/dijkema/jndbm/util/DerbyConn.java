/*
 * $Id: DerbyConn.java,v 1.9 2009/04/15 21:22:45 cvs Exp $
 * 
 * This source code is copyright of Hans Dijkema.
 * © 2008-2009 Hans Dijkema. All rights reserved.
 * 
 * This source code is property of it's author: Hans Dijkema.
 * Nothing of this code may be copied, (re)used or multiplied without
 * permission of the author. 
*/
package net.dijkema.jndbm.util;


import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import net.dijkema.jndbm2.exceptions.NDbmAlreadyInUseException;
import net.dijkema.jndbm2.exceptions.NDbmException;

public class DerbyConn extends IConn {
	//static Logger logger=zc3Logger.getLogger(DerbyConn.class);
	
	private Connection conn=null;
	private Integer    commitNestCount=0;
	private Long       transactionStartTime=0L;
	private int        transactionCount=0;
	private File       _db_location;  
	
	public String driver() {
		return "derby";
	}
	
	public void shutdown() throws Exception {
		try {
			DriverManager.getConnection("jdbc:derby:;shutdown=true");
		} catch (SQLException e) {
			if (e.getSQLState().equals("XJ015")) {
				// do nothing, this is to be expected
			} else {
				throw new SQLException(e);
			}
		}
	}
	
	public Connection getConn() throws Exception {
		return conn;
	}
	
	private Connection newConn() throws Exception {
		String dbname=_db_location.getAbsolutePath();

		Connection myconn;
		try {
			String derbyurl="jdbc:derby:"+dbname+";create=true";
			myconn=DriverManager.getConnection(derbyurl); 
		} catch (Exception E1) {
			if (E1 instanceof SQLException) {
				SQLException c=(SQLException) E1;
				if (c.getSQLState().equals("XJ040")) {
					throw new NDbmAlreadyInUseException();
				}
			}
			myconn=null;
		}
		return myconn;
	}
	
	public boolean hasConn() {
		return conn!=null;
	}
	
	public void initConn() throws Exception {
		if (conn==null) {
			conn=newConn();
		}
	}
	
	public void closeConn() throws Exception {
		while (commitNestCount>0) { commit(); }
		conn.close();
		conn=null;
	}
	
	public void begin() throws Exception {
		if (commitNestCount==0) {
			conn.setAutoCommit(false);
			transactionStartTime=System.currentTimeMillis();
		}
		commitNestCount+=1;
		if (System.currentTimeMillis()>transactionStartTime+(600*1000)) {  // 10 minutes for debugging purposes
			throw new Exception("DerbyConn: Suspected nested transaction going on, taking longer then 60 seconds");
		}
	}
	
	public void commit() throws Exception {
		commitNestCount-=1;
		if (commitNestCount==0) {
			conn.commit();
			conn.setAutoCommit(true);
			//vacuum(false);
		}
		else if (commitNestCount<0) {
			throw new Exception("DerbyConn: Commit count drops below zero!");
		}
		else {
			int time=(120*1000);
			if (System.currentTimeMillis()>transactionStartTime+(time)) { // 10 minutes for debugging purposes
				throw new Exception("DerbyConn: Suspected nested transaction going on, taking longer then 60 seconds");
			}	
		}
	}
	
	public int numOfBackupSteps() {
		return 5;
	}
	
	/*public void backup(Jzc3Progress p,ZipOutputStream zip,final Directories.EntryMaker em) throws Exception {
		Directories dir=new Directories();
		File d=dir.dataDirectory("dbbck");
		try {
			p.step();p.message(_.t("Verwijderen mogelijke voorgaande backup"));
			dir.deleteDirectory(d);
			p.step();p.message(_.t("Maken backup database"));
			PreparedStatement stm=conn.prepareStatement("CALL SYSCS_UTIL.SYSCS_BACKUP_DATABASE(?)");
			stm.setString(1, d.getAbsolutePath());
			stm.execute();
			stm.close();
			p.step();p.message(_.t("Toevoegen database backup aan ZIP bestand"));
			dir.zip(d,zip,new Directories.EntryMaker() {
				public ZipEntry makeEntry(File f) {
					ZipEntry e=em.makeEntry(f);
					String q=e.getName();
					//System.out.println("q="+q+", replace="+File.separator+"dbbck");
					return new ZipEntry(q.replace(File.separator+"dbbck",""));
				}
			});
			p.step();p.message(_.t("Verwijderen tijdelijke bestanden"));
			dir.deleteDirectory(d);
			p.step();p.message(_.t("Database backup klaar"));
		} catch (Exception e) {
			throw new Exception(String.format(_.t("Kan geen backup maken (%s)"),e.getMessage()));
		}
	}*/
	
	public DerbyConn(File db_location) throws NDbmException {
		_db_location=db_location;
		try {
			Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
		} catch (ClassNotFoundException e) {
			throw new NDbmException(e);
		}
	}
	
}
