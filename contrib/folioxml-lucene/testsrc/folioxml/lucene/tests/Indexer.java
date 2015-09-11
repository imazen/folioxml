/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package folioxml.lucene.tests;


import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
/**
 *
 * @author dlinde
 */
public class Indexer {
      public static void main(String[] args) throws Exception {
          if(args.length != 2){
              throw new Exception("Usage: java " + Indexer.class.getName()
                      + " <index dir> <data dir>");
          }
          Path indexDir = Paths.get(args[0]);
          File dataDir = new File(args[1]);

          long start = new Date().getTime();
          int numIndexed = index(indexDir, dataDir) ;
          long end = new Date().getTime();

          System.out.println("Indexing " + numIndexed + " files took " +
                  (end - start) + " Milliseconds");
      }
      //open an index and start file directory traversial
      public static int index(Path indexDir, File dataDir)throws IOException{
          if(!dataDir.exists() || !dataDir.isDirectory()){
              throw new IOException(dataDir +
                      " does not exist or is not a directory");
          }
          IndexWriter writer = new IndexWriter(FSDirectory.open(indexDir), new IndexWriterConfig(new StandardAnalyzer()));

          indexDirectory(writer, dataDir);

          int numIndexed = writer.numDocs();

          writer.close();
          return numIndexed;

      }
      // recursive method that calls itself when it finds a directory
      private static void indexDirectory(IndexWriter writer, File dir)throws
              IOException{
          File[] files = dir.listFiles();

          for(int i = 0; i < files.length; i++){
              File f = files[i];
              if (f.isDirectory()){
                  indexDirectory(writer, f);
              }else if (f.getName().endsWith(".txt")){
                  indexFile(writer,f);
              }
          }
      }
      /**
       * readTextFile
       * @param fullPathFilename
       * @return String records
       * @throws java.io.IOException
       */
      public static String readTextFile(String fullPathFilename) throws IOException {
          StringBuilder sb = new StringBuilder(1024);
          BufferedReader reader = new BufferedReader(new FileReader(fullPathFilename));

          char[] chars = new char[1024];
          int numRead = 0;
          while ((numRead = reader.read(chars)) > -1) {
              sb.append(String.valueOf(chars));
          }

          reader.close();

          return sb.toString();
      }

      // method to actually index a file using Lucene
      private static void indexFile(IndexWriter writer, File f) throws IOException{
          if (f.isHidden() || !f.exists() || !f.canRead()){
              return;
          }

          System.out.println("Indexing " + f.getCanonicalPath());

          Document doc = new Document();


          doc.add(new TextField("contents",readTextFile(f.getPath())
                    ,Field.Store.YES));


          doc.add(new StoredField("filename", f.getCanonicalPath()));

          writer.addDocument(doc);


     }

}
