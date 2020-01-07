package com.rajanainart.common.mail;

import java.util.HashMap;
import java.util.Map;

public final class MimeTypeConstant
{
   /**
    * Default Mime type
    */
   protected static final String S_STRDEFAULTMIMETYPE = "text/html";

   /**
    * Hash map that will be stored types in
    */
   protected static Map<String,String> mimeType = null;
   
   /**
    * Static initializer;
    */
   static {
      mimeType = new HashMap(161);
      mimeType.put("ai", "application/postscript");
      mimeType.put("aif", "audio/x-aiff");
      mimeType.put("aifc", "audio/x-aiff");
      mimeType.put("aiff", "audio/x-aiff");
      mimeType.put("asc", "text/plain");
      mimeType.put("asf", "video/x.ms.asf");
      mimeType.put("asx", "video/x.ms.asx");
      mimeType.put("au", "audio/basic");
      mimeType.put("avi", "video/x-msvideo");
      mimeType.put("bcpio", "application/x-bcpio");
      mimeType.put("bin", "application/octet-stream");
      mimeType.put("cab", "application/x-cabinet");
      mimeType.put("cdf", "application/x-netcdf");
      mimeType.put("class", "application/java-vm");
      mimeType.put("cpio", "application/x-cpio");
      mimeType.put("cpt", "application/mac-compactpro");
      mimeType.put("crt", "application/x-x509-ca-cert");
      mimeType.put("csh", "application/x-csh");
      mimeType.put("css", "text/css");
      mimeType.put("csv", "text/comma-separated-values");
      mimeType.put("dcr", "application/x-director");
      mimeType.put("dir", "application/x-director");
      mimeType.put("dll", "application/x-msdownload");
      mimeType.put("dms", "application/octet-stream");
      mimeType.put("doc", "application/msword");
      mimeType.put("dtd", "application/xml-dtd");
      mimeType.put("dvi", "application/x-dvi");
      mimeType.put("dxr", "application/x-director");
      mimeType.put("eps", "application/postscript");
      mimeType.put("etx", "text/x-setext");
      mimeType.put("exe", "application/octet-stream");
      mimeType.put("ez", "application/andrew-inset");
      mimeType.put("gif", "image/gif");
      mimeType.put("gtar", "application/x-gtar");
      mimeType.put("gz", "application/gzip");
      mimeType.put("gzip", "application/gzip");
      mimeType.put("hdf", "application/x-hdf");
      mimeType.put("htc", "text/x-component");
      mimeType.put("hqx", "application/mac-binhex40");
      mimeType.put("html", "text/html");
      mimeType.put("htm", "text/html");
      mimeType.put("ice", "x-conference/x-cooltalk");
      mimeType.put("ief", "image/ief");
      mimeType.put("iges", "model/iges");
      mimeType.put("igs", "model/iges");
      mimeType.put("jar", "application/java-archive");
      mimeType.put("java", "text/plain");
      mimeType.put("jnlp", "application/x-java-jnlp-file");
      mimeType.put("jpeg", "image/jpeg");
      mimeType.put("jpe", "image/jpeg");
      mimeType.put("jpg", "image/jpeg");
      mimeType.put("js", "application/x-javascript");
      mimeType.put("jsp", "text/plain");
      mimeType.put("kar", "audio/midi");
      mimeType.put("latex", "application/x-latex");
      mimeType.put("lha", "application/octet-stream");
      mimeType.put("lzh", "application/octet-stream");
      mimeType.put("man", "application/x-troff-man");
      mimeType.put("mathml", "application/mathml+xml");
      mimeType.put("me", "application/x-troff-me");
      mimeType.put("mesh", "model/mesh");
      mimeType.put("mid", "audio/midi");
      mimeType.put("midi", "audio/midi");
      mimeType.put("mif", "application/vnd.mif");
      mimeType.put("mol", "chemical/x-mdl-molfile");
      mimeType.put("movie", "video/x-sgi-movie");
      mimeType.put("mov", "video/quicktime");
      mimeType.put("mp2", "audio/mpeg");
      mimeType.put("mp3", "audio/mpeg");
      mimeType.put("mpeg", "video/mpeg");
      mimeType.put("mpe", "video/mpeg");
      mimeType.put("mpga", "audio/mpeg");
      mimeType.put("mpg", "video/mpeg");
      mimeType.put("ms", "application/x-troff-ms");
      mimeType.put("msh", "model/mesh");
      mimeType.put("msi", "application/octet-stream");
      mimeType.put("nc", "application/x-netcdf");
      mimeType.put("oda", "application/oda");
      mimeType.put("ogg", "application/ogg");
      mimeType.put("pbm", "image/x-portable-bitmap");
      mimeType.put("pdb", "chemical/x-pdb");
      mimeType.put("pdf", "application/pdf");
      mimeType.put("pgm", "image/x-portable-graymap");
      mimeType.put("pgn", "application/x-chess-pgn");
      mimeType.put("png", "image/png");
      mimeType.put("pnm", "image/x-portable-anymap");
      mimeType.put("ppm", "image/x-portable-pixmap");
      mimeType.put("ppt", "application/vnd.ms-powerpoint");
      mimeType.put("ps", "application/postscript");
      mimeType.put("qt", "video/quicktime");
      mimeType.put("ra", "audio/x-pn-realaudio");
      mimeType.put("ra", "audio/x-realaudio");
      mimeType.put("ram", "audio/x-pn-realaudio");
      mimeType.put("ras", "image/x-cmu-raster");
      mimeType.put("rdf", "application/rdf+xml");
      mimeType.put("rgb", "image/x-rgb");
      mimeType.put("rm", "audio/x-pn-realaudio");
      mimeType.put("roff", "application/x-troff");
      mimeType.put("rpm", "application/x-rpm");
      mimeType.put("rpm", "audio/x-pn-realaudio");
      mimeType.put("rtf", "application/rtf");
      mimeType.put("rtx", "text/richtext");
      mimeType.put("ser", "application/java-serialized-object");
      mimeType.put("sgml", "text/sgml");
      mimeType.put("sgm", "text/sgml");
      mimeType.put("sh", "application/x-sh");
      mimeType.put("shar", "application/x-shar");
      mimeType.put("silo", "model/mesh");
      mimeType.put("sit", "application/x-stuffit");
      mimeType.put("skd", "application/x-koan");
      mimeType.put("skm", "application/x-koan");
      mimeType.put("skp", "application/x-koan");
      mimeType.put("skt", "application/x-koan");
      mimeType.put("smi", "application/smil");
      mimeType.put("smil", "application/smil");
      mimeType.put("snd", "audio/basic");
      mimeType.put("spl", "application/x-futuresplash");
      mimeType.put("src", "application/x-wais-source");
      mimeType.put("sv4cpio", "application/x-sv4cpio");
      mimeType.put("sv4crc", "application/x-sv4crc");
      mimeType.put("svg", "image/svg+xml");
      mimeType.put("swf", "application/x-shockwave-flash");
      mimeType.put("t", "application/x-troff");
      mimeType.put("tar", "application/x-tar");
      mimeType.put("tar.gz", "application/x-gtar");
      mimeType.put("tcl", "application/x-tcl");
      mimeType.put("tex", "application/x-tex");
      mimeType.put("texi", "application/x-texinfo");
      mimeType.put("texinfo", "application/x-texinfo");
      mimeType.put("tgz", "application/x-gtar");
      mimeType.put("tiff", "image/tiff");
      mimeType.put("tif", "image/tiff");
      mimeType.put("tr", "application/x-troff");
      mimeType.put("tsv", "text/tab-separated-values");
      mimeType.put("txt", "text/plain");
      mimeType.put("ustar", "application/x-ustar");
      mimeType.put("vcd", "application/x-cdlink");
      mimeType.put("vrml", "model/vrml");
      mimeType.put("vxml", "application/voicexml+xml");
      mimeType.put("wav", "audio/x-wav");
      mimeType.put("wbmp", "image/vnd.wap.wbmp");
      mimeType.put("wmlc", "application/vnd.wap.wmlc");
      mimeType.put("wmlsc", "application/vnd.wap.wmlscriptc");
      mimeType.put("wmls", "text/vnd.wap.wmlscript");
      mimeType.put("wml", "text/vnd.wap.wml");
      mimeType.put("wrl", "model/vrml");
      mimeType.put("wtls-ca-certificate", "application/vnd.wap.wtls-ca-certificate");
      mimeType.put("xbm", "image/x-xbitmap");
      mimeType.put("xht", "application/xhtml+xml");
      mimeType.put("xhtml", "application/xhtml+xml");
      mimeType.put("xls", "application/vnd.ms-excel");
      mimeType.put("xml", "application/xml");
      mimeType.put("xpm", "image/x-xpixmap");
      mimeType.put("xpm", "image/x-xpixmap");
      mimeType.put("xsl", "application/xml");
      mimeType.put("xslt", "application/xslt+xml");
      mimeType.put("xul", "application/vnd.mozilla.xul+xml");
      mimeType.put("xwd", "image/x-xwindowdump");
      mimeType.put("xyz", "chemical/x-xyz");
      mimeType.put("z", "application/compress");
      mimeType.put("zip", "application/zip");
   }

   /** 
    * Private constructor since this class cannot be instantiated
    */
   private MimeTypeConstant() { }
  
   public static String getMimeType(String strKey ) {
      return mimeType.getOrDefault(strKey, S_STRDEFAULTMIMETYPE);
   }
}