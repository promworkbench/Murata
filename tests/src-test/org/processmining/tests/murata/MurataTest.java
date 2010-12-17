package org.processmining.tests.murata;
import org.junit.Test;
import org.processmining.contexts.cli.CLI;

import junit.framework.Assert;
import junit.framework.TestCase;
import junit.framework.AssertionFailedError;

public class MurataTest extends TestCase {

  @Test
  public void testMurata1() throws Throwable {
    String args[] = new String[] {"-l"};
    CLI.main(args);
  }

  @Test
  public void testMurata2() throws Throwable {
    String testFileRoot = System.getProperty("test.testFileRoot", ".");
    String args[] = new String[] {"-f", testFileRoot+"/Murata_Example.txt"};
    
    CLI.main(args);
  }
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(UmaTest.class);
  }
  
}
