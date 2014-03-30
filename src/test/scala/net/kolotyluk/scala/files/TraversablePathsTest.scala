/*  Copyright © 2014 by Eric Kolotyluk <eric@kolotyluk.net>

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/

package net.kolotyluk.scala.files

import org.junit._
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters.NAME_ASCENDING
import Assert._
import java.nio.file.FileSystems
import java.io.File
import java.nio.file.Path
import java.nio.file.Files.createSymbolicLink
import java.nio.file.Files.createTempDirectory
import java.nio.file.Files.createTempFile
import java.nio.file.Files.exists
import java.nio.file.LinkPermission
import java.nio.file.FileSystemException

import scala.sys.process.Process
import scala.sys.process.ProcessLogger

/** Static (object) declarations needed for the test fixtures.
  * Create a temporary test folder layout for all the tests.
  * @author Eric Kolotyluk
  */
object TraversablePathsTest {
  
  val testFolder = createTempDirectory("testFolder-")
  val file1 = createTempFile(testFolder, "file1-", ".txt")
  val file2 = createTempFile(testFolder, "file2-", ".txt")
  val file3 = createTempFile(testFolder, "file3-", ".txt")
  
  val folder1 = createTempDirectory(testFolder, "folder1-")
  val file11 = createTempFile(folder1, "file11-", ".txt")
  val file12 = createTempFile(folder1, "file12-", ".txt")
  val file13 = createTempFile(folder1, "file13-", ".txt")
  
  val folder2 = createTempDirectory(testFolder, "folder2-")
  val file21 = createTempFile(folder2, "file21-", ".txt")
  val file22 = createTempFile(folder2, "file22-", ".txt")
  val file23 = createTempFile(folder2, "file23-", ".txt")
  
  val link21 = FileSystems.getDefault().getPath(folder2.toString(), "link21")

  try {
    createSymbolicLink(link21, folder1)
  }
  catch {
    case fileSystemException: FileSystemException =>
      System.err.println("\n\t**** Error configuring test fixture ***\n\n")
      println(fileSystemException)
      if (fileSystemException.getMessage().contains("A required privilege is not held by the client")) {
        println("You need to set permissions by...\n")
        
        val elevate = FileSystems.getDefault().getPath("target\\elevate.exe")
      
        println("elevate is " + elevate.toAbsolutePath())
        
        val linkCommand = List("cmd", "/c", elevate.toAbsolutePath().toString(), "mklink", "/D", link21.toString(), folder1.toString())
        
        println("linkCommand: " + linkCommand)
        
        val echoCommand = List("cmd", "/c", "echo", "%JAVA_HOME%")

        Run(linkCommand) match {
          case NormalResult(process, outLines, errLines) =>
            println("process: " + process)
            println(outLines)
          case ErrorResult(process, outLines, errLines) =>
            System.err.println("exit code: " + process.exitValue)
            System.err.println(process)
            System.err.println(outLines)
            System.err.println(errLines)
          case _ =>
            println("WTF?")
        }
        
        ///println("WTF!")
      }


  }
    
  @BeforeClass
  def buildTestFolder() {
    println("Test Fixture: Build Test Folder")
    
    println("Created " + testFolder)
    
  }

  @AfterClass
  def deleteTestFolder() {
    println("Test Fixture: Delete Test Folder")
    
    // wow, talk about eating your own dog food, we are using the Unit Under Test to
    // clean up after we test it. Good thing the tests are run before the clean up. EK
    
//    val files = PathsTraversable(PathsTraversableTest.testFolder, false, true, true)
//    files.foreach(Files.delete(_))
//
//    val folders = PathsTraversable(PathsTraversableTest.testFolder, true, false, true)
//    folders.toList.reverse.foreach(Files.delete(_))
//    
//    println(testFolder + " deleted")
//    
//    assertFalse(Files.exists(PathsTraversableTest.testFolder))
  }

}

/**
 * Test suite for PathsTraversable class.
 * @author Eric Kolotyluk
 */
@Test
@FixMethodOrder(NAME_ASCENDING)	// JUnit 4.11 or later
class TraversablePathsTest {

  // Normally it is good to write test cases that are independent of order,
  // but in this case I am using the test infrastructure to test the sanity
  // of the test fixtures too. Technically, testing the sanity after the
  // actual tests should work, but there are edge cases that could lead to
  // problems, so better safe than sorry. EK

  /**
   * Test case for fixture sanity. Make sure the folder configuration
   * actually exists the way the fixtures intended it to be.
   */
  @Test
  def case01_FixtureSanity_FoldersExist = {
    assertTrue(exists(TraversablePathsTest.testFolder))

  }
  
  @Test
  def case02_FixtureSanity_FilesExist = {
    assertTrue(exists(TraversablePathsTest.file21))
    assertTrue(exists(TraversablePathsTest.file22))
    assertTrue(exists(TraversablePathsTest.file23))

  }
  
  @Test
  def case03_FixtureSanity_LinksExist = {
    assertTrue(exists(TraversablePathsTest.link21))

  }

    
  /**
   * Test case traversing over just files.
   */
  @Test
  def case04_FilesTraversable() = {
      
    val paths = TraversablePaths(TraversablePathsTest.folder2)()
    println("Files:")
    paths.foreach(println(_))
    
    val pathSet = paths.toSet
    assertFalse(pathSet.contains(TraversablePathsTest.folder2))
    assertTrue(pathSet.contains(TraversablePathsTest.file21))
    assertTrue(pathSet.contains(TraversablePathsTest.file22))
    assertTrue(pathSet.contains(TraversablePathsTest.file23))
  }
}

abstract class RunResult
case class NormalResult(process:Process, outString:String, errString:String) extends RunResult
case class ErrorResult(process:Process, outString:String, errString:String) extends RunResult


object Run {
  //val properties = Properties.getScriptProperties();	// must call before getLogger() - EK
  //lazy val logger = Properties.getLogger(getClass())

  def apply(command:List[String], workingDirectory:Option[File] = None) : RunResult = {
    println("running " + command)
    //logger.info(command.foldLeft("run:") {(string, argument) => string + " " + argument})
    val outBuilder = new StringBuilder()
    val errBuilder = new StringBuilder()
    val processLogger = ProcessLogger(
      (outLine: String) => outBuilder.append(outLine).append("\n"),
      (errLine: String) => errBuilder.append(errLine).append("\n"))
    val processBuilder = Process(command, workingDirectory)
    val process = processBuilder.run(processLogger, false)
    
    // Now block waiting for an exit value
    if (process.exitValue == 0)
      NormalResult(process, outBuilder.toString(), errBuilder.toString())
    else
      ErrorResult(process, outBuilder.toString(), errBuilder.toString())
  }
}

/*	Scala Newbie Notes

	The 'object' keyword is like class, except it directly creates a singleton object.
	This helps reduce boilerplate.

	The 'apply' method is special such that you can say Run(command) instead of Run.apply(command)
	This helps reduce boilerplate.

	The 'Option' class is the superclass of 'None' and 'Some'
	In Scala returning null is frowned upon as it can result in NullPointerException, which
	are undesireable. If you return an Option you should never have to worry about dealing
	with NullPointerException. To access an Option use option.getOrElse(default) which return
	either the value of Some or the value default. For example

		val value = Some("string")
		value string = value.getOrElse("uknown")

	ProcessLogger is used to capture the stdout and stderr as strings.

	The last statement of a block is also the return value of the block.

	A case class is used to reduce a bunch of standard boilerplate. For example, you don't have
	to use 'new' to instantiate one, or write any 'apply' methods. Also, all the constructor
	variables are publically accessible. Using case class is very handy if you do any pattern
	matching with 'match'
*/