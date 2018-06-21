/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.workbench.common.services.backend.compiler.impl.offprocess;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

import org.kie.workbench.common.services.backend.compiler.CompilationResponse;
import org.kie.workbench.common.services.backend.compiler.impl.DefaultKieCompilationResponse;
import org.kie.workbench.common.services.backend.compiler.impl.OffProcessDefaultCompilationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.file.Paths;

/**
 * Map to share Compilation Responses between different process using the UUID as a identifier key
 * */
public class OffProcessSharedMap {

    static long length = 10485760;//10mb

    private static final Logger logger = LoggerFactory.getLogger(OffProcessSharedMap.class);
    //private static final ConcurrentHashMap<String, MappedByteBuffer> internalMap = new ConcurrentHashMap<>();

    /*public static Enumeration<String> getKeys() {
        return internalMap.keys();
    }*/



    public static CompilationResponse getCompilationResponse(String uuid, Boolean defaultValue) throws IOException, ClassCastException{
        MappedByteBuffer buffer = readMMFBuffer(uuid);
        CompilationResponse res = new DefaultKieCompilationResponse(defaultValue);
        if(buffer != null){
            try {
                int bufferSize = buffer.limit();
                byte[] bytez = new byte[bufferSize];
                for (int i = 0; i < bufferSize; i++) {
                    bytez[i] = (buffer.get(i));
                }
                if(bytez.length >0) {
                    Object o = deserialize(bytez);
                    OffProcessDefaultCompilationResponse offResponse = (OffProcessDefaultCompilationResponse) o;
                    res = convertIntoKieCompilationResponse(offResponse);
                }else {
                    return new DefaultKieCompilationResponse(false);
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        }
        return res;
    }

    public static void writeFile(String uuid, OffProcessDefaultCompilationResponse res) throws IOException{
        byte[] bytez = serialize(res);

        MappedByteBuffer buffer = createBuffer(uuid, bytez.length);
        for (int i = 0; i < bytez.length; i++) {
            buffer.put(bytez[i]);
        }
        //buffer.force();

        System.out.println("writeFile:"+buffer.isLoaded());  //prints false
        System.out.println("writeFile:"+buffer.capacity());  //Get the size based on content size of file
        //internalMap.put(uuid, buf);
    }

    /*public static void release(String uuid){
        MappedByteBuffer buffer = internalMap.get(uuid);
        Cleaner cleaner = ((sun.nio.ch.DirectBuffer) buffer).cleaner();//TODO works only on sun jvm
        if (cleaner != null) {
            cleaner.clean();
        }
        internalMap.remove(uuid);
    }*/

    private static MappedByteBuffer readMMFBuffer(String uuid) throws FileNotFoundException, IOException {
        //File file = new File(uuid);
        File f = File.createTempFile("mmf",uuid);
        System.out.println("readMMFBuffer f:::::::::::::::"+f.toString());

        FileChannel fileChannel = new RandomAccessFile(f, "r").getChannel();

        MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0,fileChannel.size());
        buffer.load();

        System.out.println("readMMFBuffer:"+buffer.isLoaded());  //prints false
        System.out.println("readMMFBuffer:"+buffer.capacity());  //Get the size based on content size of file

        //You can read the file from this buffer the way you like.
       /* for (int i = 0; i < buffer.limit(); i++)
        {
            System.out.print((char) buffer.get()); //Print the content of file
        }*/
       return buffer;
    }

    private static MappedByteBuffer createBuffer(String uuid, int length) throws IOException {
        File f = File.createTempFile("mmf",uuid);
        System.out.println("f:::::::::::::::"+f.toString());
        //File f = new File(uuid);
        FileChannel channel = FileChannel.open(f.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE );
        MappedByteBuffer b = channel.map(FileChannel.MapMode.READ_WRITE, 0, length );
        return b;
    }

    private static byte[] serialize(Object obj) throws IOException {
        try(ByteArrayOutputStream b = new ByteArrayOutputStream()){
            try(ObjectOutputStream o = new ObjectOutputStream(b)){
                o.writeObject(obj);
            }
            return b.toByteArray();
        }
    }

    private static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        try(ByteArrayInputStream b = new ByteArrayInputStream(bytes)){
            try(ObjectInputStream o = new ObjectInputStream(b)){
                return o.readObject();
            }
        }
    }

    private static CompilationResponse convertIntoKieCompilationResponse(OffProcessDefaultCompilationResponse res){
        DefaultKieCompilationResponse response = new DefaultKieCompilationResponse(res.isSuccessful(),
                                                                                   res.getKieModuleMetaInfo(),
                                                                                   res.getKieModule(),
                                                                                   res.getProjectClassLoaderStore(),
                                                                                   res.getMavenOutput(),
                                                                                   res.getTargetContent(),
                                                                                   res.getProjectDependencies(),
                                                                                   res.getWorkingDir().isPresent() ? Paths.get(res.getWorkingDir().get()) : null,
                                                                                   res.getEventsTypeClasses());
        return response;
    }

}
