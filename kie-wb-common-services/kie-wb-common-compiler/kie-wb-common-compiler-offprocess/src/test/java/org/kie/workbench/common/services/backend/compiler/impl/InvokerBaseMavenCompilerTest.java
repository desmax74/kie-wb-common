package org.kie.workbench.common.services.backend.compiler.impl;

import java.io.OutputStream;

import org.junit.Test;

public class InvokerBaseMavenCompilerTest {
@Test
public void test(){
    try {

        // print a message
        System.out.println("Executing compiler");

        // create a process and execute notepad.exe
        Process process = Runtime.getRuntime().exec("notepad.exe");

        // print another message
        OutputStream out = process.getOutputStream();
        System.out.println("Notepad should now open.");

    } catch (Exception ex) {
        ex.printStackTrace();
    }
}

}
