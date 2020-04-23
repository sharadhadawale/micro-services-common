package com.rajanainart.nas;

import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;

import java.io.Closeable;
import java.io.IOException;

public class NasSession implements Closeable {
    private NasConfig.NasInfo info;
    private SMBClient  smbClient ;
    private Connection connection;
    private Session    session   ;
    private DiskShare  share     ;

    public NasConfig.NasInfo getNasInfo  () { return info ; }
    public DiskShare         getDiskShare() { return share; }

    public NasSession(NasConfig.NasInfo info) throws IOException {
        AuthenticationContext authContext = new AuthenticationContext(info.getUserName(), info.getPassword().toCharArray(), info.getDomain());
        smbClient  = new SMBClient();
        connection = smbClient.connect(info.getServer());
        session    = connection.authenticate(authContext);
        share      = (DiskShare)session.connectShare(info.getShare());
        this.info  = info;
    }

    @Override
    public void close() {
        try {
            if (share      != null) share     .close();
            if (session    != null) session   .close();
            if (connection != null) connection.close();
            if (smbClient  != null) smbClient .close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
