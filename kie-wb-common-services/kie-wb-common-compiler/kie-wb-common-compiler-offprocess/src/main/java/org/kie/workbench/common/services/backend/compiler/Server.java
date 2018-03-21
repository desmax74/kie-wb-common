package org.kie.workbench.common.services.backend.compiler;

import java.io.File;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

public class Server {
    public static void main( String[] args ) throws Throwable {
        File f = new File("FILE_NAME" );

        FileChannel channel = FileChannel.open(f.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE );

        MappedByteBuffer b = channel.map(FileChannel.MapMode.READ_WRITE, 0, 4096 );
        CharBuffer charBuf = b.asCharBuffer();

        char[] string = "Hello client\0".toCharArray();
        charBuf.put( string );

        System.out.println( "Waiting for client." );
        while( charBuf.get( 0 ) != '\0' );
        System.out.println( "Finished waiting." );
    }

}
